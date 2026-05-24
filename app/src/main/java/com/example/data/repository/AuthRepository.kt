package com.example.data.repository

import com.example.data.local.User
import com.example.data.local.UserDao
import com.example.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepository(
    private val userDao: UserDao,
    private val context: android.content.Context
) {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val functions by lazy { FirebaseFunctions.getInstance() }

    private fun checkFirebase(context: android.content.Context) {
        // Firebase is automatically initialized by the google-services plugin using google-services.json
        try {
            auth // trigger lazy init
        } catch (e: IllegalStateException) {
            // Should not happen with google-services.json
            throw Exception("Firebase is not initialized. Please ensure google-services.json is valid.")
        }
    }
    
    suspend fun registerUser(
        username: String,
        phone: String,
        email: String?,
        passwordHash: String // We reuse this param name but we pass raw password for Firebase
    ): Resource<User> = withContext(Dispatchers.IO) {
        try {
            checkFirebase(context)
            
            val normalizedUsername = username.trim().lowercase()
            val normalizedPhone = phone.replace(Regex("[^0-9+]"), "")
            
            // Check if username exists
            val usernameSnap = kotlinx.coroutines.withTimeout(15000L) { firestore.collection("users").whereEqualTo("normalizedUsername", normalizedUsername).get().await() }
            if (!usernameSnap.isEmpty) {
                return@withContext Resource.Error("Username already taken")
            }
            
            // Check if phone exists
            val phoneSnap = kotlinx.coroutines.withTimeout(5000L) { firestore.collection("users").whereEqualTo("phoneNumber", normalizedPhone).get().await() }
            if (!phoneSnap.isEmpty) {
                return@withContext Resource.Error("Phone number already registered")
            }
            
            val authEmail = if (!email.isNullOrBlank()) email.trim() else "$normalizedUsername@wasetplus.com"
            
            // Create user
            val authResult = kotlinx.coroutines.withTimeout(15000L) { auth.createUserWithEmailAndPassword(authEmail, passwordHash).await() }
            val uid = authResult.user?.uid ?: return@withContext Resource.Error("Failed to create user")
            
            val user = User(
                uid = uid,
                username = username,
                normalizedUsername = normalizedUsername,
                phoneNumber = normalizedPhone,
                email = email,
                role = "customer",
                profileImage = null,
                isVerified = false,
                createdAt = System.currentTimeMillis()
            )
            
            // Save to Firestore
            val userMap = hashMapOf(
                "uid" to user.uid,
                "username" to user.username,
                "normalizedUsername" to user.normalizedUsername,
                "phoneNumber" to user.phoneNumber,
                "email" to user.email,
                "role" to user.role,
                "profileImage" to user.profileImage,
                "isVerified" to user.isVerified,
                "createdAt" to user.createdAt
            )
            kotlinx.coroutines.withTimeout(15000L) { firestore.collection("users").document(uid).set(userMap).await() }
            
            userDao.insertUser(user)
            return@withContext Resource.Success(user)
            
        } catch (e: Exception) {
            val message = e.message ?: "An error occurred during registration."
            val displayMessage = when {
                e is kotlinx.coroutines.TimeoutCancellationException -> "Network timeout. Please check your internet connection or Firebase setup."
                message.contains("email-already-in-use") -> "Email is already in use"
                else -> message
            }
            return@withContext Resource.Error(displayMessage)
        }
    }
    
    suspend fun loginUser(identifier: String, password: String): Resource<User> = withContext(Dispatchers.IO) {
        try {
            checkFirebase(context)
            val cleanIdentifier = identifier.trim().lowercase()
            var resolvedEmail: String? = null
            
            // Is it an email?
            if (cleanIdentifier.contains("@")) {
                resolvedEmail = cleanIdentifier
            } else {
                // Try username
                val usernameSnap = kotlinx.coroutines.withTimeout(5000L) { firestore.collection("users").whereEqualTo("normalizedUsername", cleanIdentifier).get().await() }
                if (!usernameSnap.isEmpty) {
                    resolvedEmail = usernameSnap.documents[0].getString("email") 
                        ?: "$cleanIdentifier@wasetplus.com"
                } else {
                    // Try phone
                    val cleanPhone = cleanIdentifier.replace(Regex("[^0-9+]"), "")
                    if (cleanPhone.isNotEmpty()) {
                        val phoneSnap = kotlinx.coroutines.withTimeout(5000L) { firestore.collection("users").whereEqualTo("phoneNumber", cleanPhone).get().await() }
                        if (!phoneSnap.isEmpty) {
                            val normalizedUser = phoneSnap.documents[0].getString("normalizedUsername") ?: ""
                            resolvedEmail = phoneSnap.documents[0].getString("email") 
                                ?: "$normalizedUser@wasetplus.com"
                        }
                    }
                }
            }
            
            if (resolvedEmail == null) {
                return@withContext Resource.Error("Invalid credentials")
            }
            
            kotlinx.coroutines.withTimeout(15000L) { auth.signInWithEmailAndPassword(resolvedEmail, password).await() }
            val uid = auth.currentUser?.uid ?: return@withContext Resource.Error("Authentication failed")
            
            val docSnap = kotlinx.coroutines.withTimeout(10000L) { firestore.collection("users").document(uid).get().await() }
            if (!docSnap.exists()) {
                return@withContext Resource.Error("User data not found")
            }
            
            val user = User(
                uid = uid,
                username = docSnap.getString("username") ?: "",
                normalizedUsername = docSnap.getString("normalizedUsername") ?: "",
                phoneNumber = docSnap.getString("phoneNumber") ?: "",
                email = docSnap.getString("email"),
                role = docSnap.getString("role") ?: "customer",
                profileImage = docSnap.getString("profileImage"),
                isVerified = docSnap.getBoolean("isVerified") ?: false,
                createdAt = docSnap.getLong("createdAt") ?: System.currentTimeMillis()
            )
            
            userDao.insertUser(user)
            return@withContext Resource.Success(user)
        } catch (e: Exception) {
            val message = e.message?.lowercase() ?: ""
            val displayMessage = when {
                e is kotlinx.coroutines.TimeoutCancellationException -> "Network timeout. Please check your internet connection."
                message.contains("auth/invalid-credential") || message.contains("not found") -> "Invalid credentials"
                else -> "Error: \${e.message}"
            }
            return@withContext Resource.Error(displayMessage)
        }
    }

    suspend fun checkUsernameAvailability(username: String): Resource<Boolean> = withContext(Dispatchers.IO) {
         try {
             checkFirebase(context)
             val normalizedUsername = username.trim().lowercase()
             val snap = kotlinx.coroutines.withTimeout(10000L) { firestore.collection("users").whereEqualTo("normalizedUsername", normalizedUsername).get().await() }
             return@withContext Resource.Success(snap.isEmpty)
         } catch (e: Exception) {
             return@withContext Resource.Error(e.message ?: "Failed to check availability")
         }
    }
}
