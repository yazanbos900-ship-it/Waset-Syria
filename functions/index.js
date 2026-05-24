const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { getFirestore } = require("firebase-admin/firestore");
const { getAuth } = require("firebase-admin/auth");
const admin = require("firebase-admin");

admin.initializeApp();

exports.registerUser = onCall(async (request) => {
    const data = request.data;
    const { username, phone, email, password } = data;

    if (!username || !phone || !password) {
        throw new HttpsError("invalid-argument", "Missing required fields.");
    }

    const normalizedUsername = username.trim().toLowerCase();
    const normalizedPhone = phone.replace(/[^0-9+]/g, "");
    
    // We will use a synthetic email if not provided since Firebase Auth prefers either email or phone natively.
    // For simplicity, we create user via Auth SDK using email (synthetic if none provided).
    const authEmail = email && email.trim() !== "" ? email.trim() : `${normalizedUsername}@marketplace-temp.com`;

    const db = getFirestore();
    const usersRef = db.collection("users");
    
    let uid;
    
    // Use a transaction to ensure username & phone are unique
    try {
        await db.runTransaction(async (transaction) => {
            // Check if username already exists
            const usernameQuery = db.collection("users").where("normalizedUsername", "==", normalizedUsername);
            const usernameSnapshot = await transaction.get(usernameQuery);
            if (!usernameSnapshot.empty) {
                throw new HttpsError("already-exists", "Username already taken");
            }

            // Check if phone already exists
            const phoneQuery = db.collection("users").where("phoneNumber", "==", normalizedPhone);
            const phoneSnapshot = await transaction.get(phoneQuery);
            if (!phoneSnapshot.empty) {
                throw new HttpsError("already-exists", "Phone number already registered");
            }
            
            // Create user in Firebase Auth
            // Note: Since this is inside transaction, if it fails later, the Auth user might be orphaned. 
            // In a production-grade system, a cleanup mechanism or orchestrator should be used.
            try {
                const userRecord = await getAuth().createUser({
                    email: authEmail,
                    password: password,
                    displayName: username,
                    phoneNumber: normalizedPhone.startsWith("+") ? normalizedPhone : undefined
                });
                uid = userRecord.uid;
            } catch (authError) {
                if (authError.code === 'auth/email-already-exists') {
                    throw new HttpsError("already-exists", "Email already in use");
                }
                if (authError.code === 'auth/phone-number-already-exists') {
                    throw new HttpsError("already-exists", "Phone number already in use");
                }
                throw new HttpsError("internal", "Failed to create authentication user: " + authError.message);
            }

            // Set the Firestore document
            const newUserRef = usersRef.doc(uid);
            transaction.set(newUserRef, {
                uid: uid,
                username: username,
                normalizedUsername: normalizedUsername,
                phoneNumber: normalizedPhone,
                email: email || null,
                role: "customer",
                profileImage: null,
                isVerified: false,
                createdAt: admin.firestore.FieldValue.serverTimestamp()
            });
        });
        
        return { success: true, uid: uid, authEmail: authEmail };
        
    } catch (error) {
        if (error instanceof HttpsError) {
            throw error;
        }
        throw new HttpsError("internal", "Transaction failed: " + error.message);
    }
});

exports.checkUsernameAvailability = onCall(async (request) => {
    const data = request.data;
    const { username } = data;
    if (!username) return { available: false };
    
    const normalizedUsername = username.trim().toLowerCase();
    const db = getFirestore();
    const snapshot = await db.collection("users").where("normalizedUsername", "==", normalizedUsername).get();
    
    return { available: snapshot.empty };
});

exports.getLoginEmail = onCall(async (request) => {
    const data = request.data;
    const { identifier } = data;
    if (!identifier) throw new HttpsError("invalid-argument", "Missing identifier");
    
    const db = getFirestore();
    const cleanIdentifier = identifier.trim().toLowerCase();
    
    // Check if it's a username
    const userSnap = await db.collection("users").where("normalizedUsername", "==", cleanIdentifier).get();
    if (!userSnap.empty) {
        const userDoc = userSnap.docs[0].data();
        return { email: userDoc.email || `${cleanIdentifier}@marketplace-temp.com` };
    }
    
    // Check if it's a phone
    const cleanPhone = identifier.replace(/[^0-9+]/g, "");
    if (cleanPhone.length > 0) {
        const phoneSnap = await db.collection("users").where("phoneNumber", "==", cleanPhone).get();
        if (!phoneSnap.empty) {
            const userDoc = phoneSnap.docs[0].data();
            return { email: userDoc.email || `${userDoc.normalizedUsername}@marketplace-temp.com` };
        }
    }
    
    throw new HttpsError("not-found", "User not found");
});
