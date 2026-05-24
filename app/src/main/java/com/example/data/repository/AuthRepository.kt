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
        email: String,
        passwordHash: String
    ): Resource<User> = withContext(Dispatchers.IO) {
        var createdUid: String? = null
        try {
            checkFirebase(context)
            
            val normalizedUsername = username.trim().lowercase()
            val cleanEmail = email.trim()
            val cleanPhone = phone.trim()
            
            // 1. Firebase Create
            val authResult = kotlinx.coroutines.withTimeoutOrNull(15000L) { 
                auth.createUserWithEmailAndPassword(cleanEmail, passwordHash).await() 
            }
            if (authResult == null) return@withContext Resource.Error("تحقق من اتصالك بالإنترنت")
            
            val uid = authResult.user?.uid ?: return@withContext Resource.Error("فشل إنشاء الحساب")
            createdUid = uid
            
            // 2. Save to Firestore
            val userMap = hashMapOf(
                "uid" to uid,
                "username" to username.trim(),
                "normalizedUsername" to normalizedUsername,
                "phone" to cleanPhone,
                "email" to cleanEmail,
                "role" to "buyer",
                "createdAt" to System.currentTimeMillis()
            )
            
            kotlinx.coroutines.withTimeoutOrNull(15000L) { 
                firestore.collection("users").document(uid).set(userMap).await() 
            } ?: throw Exception("Network failure during Firestore setup")
            
            val user = User(
                uid = uid,
                username = username.trim(),
                normalizedUsername = normalizedUsername,
                phoneNumber = cleanPhone,
                email = cleanEmail,
                role = "buyer",
                profileImage = null,
                isVerified = false,
                createdAt = userMap["createdAt"] as Long
            )
            
            userDao.insertUser(user)
            return@withContext Resource.Success(user)
            
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            
            if (createdUid != null) {
                try { auth.currentUser?.delete()?.await() } catch (ex: Exception) {}
            }
            
            val message = e.message?.lowercase() ?: ""
            val displayMessage = when {
                e is kotlinx.coroutines.TimeoutCancellationException || message.contains("network") -> "تحقق من اتصالك بالإنترنت"
                message.contains("email-already-in-use") -> "اسم المستخدم مسجل بالفعل"
                message.contains("weak-password") -> "كلمة المرور ضعيفة جداً"
                else -> "حدث خطأ أثناء التسجيل: ${e.localizedMessage}"
            }
            return@withContext Resource.Error(displayMessage)
        }
    }
    
    suspend fun loginUser(identifier: String, password: String): Resource<User> = withContext(Dispatchers.IO) {
        try {
            checkFirebase(context)
            val loginEmail = identifier.trim()
            
            val signResult = kotlinx.coroutines.withTimeoutOrNull(15000L) { 
                auth.signInWithEmailAndPassword(loginEmail, password).await() 
            }
            if (signResult == null) return@withContext Resource.Error("تحقق من اتصالك بالإنترنت")
            
            val uid = auth.currentUser?.uid ?: return@withContext Resource.Error("المستخدم غير موجود")
            val docSnap = firestore.collection("users").document(uid).get().await()
            
            if (!docSnap.exists()) {
                return@withContext Resource.Error("المستخدم غير موجود")
            }
            
            val user = User(
                uid = uid,
                username = docSnap.getString("username") ?: "",
                normalizedUsername = (docSnap.getString("username") ?: "").lowercase(),
                phoneNumber = docSnap.getString("phone") ?: "",
                email = docSnap.getString("email"),
                role = docSnap.getString("role") ?: "buyer",
                profileImage = docSnap.getString("profileImage"),
                isVerified = docSnap.getBoolean("isVerified") ?: false,
                createdAt = docSnap.getLong("createdAt") ?: System.currentTimeMillis()
            )
            
            userDao.insertUser(user)
            return@withContext Resource.Success(user)
            
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            val message = e.message?.lowercase() ?: ""
            val displayMessage = when {
                e is kotlinx.coroutines.TimeoutCancellationException || message.contains("network") -> "تحقق من اتصالك بالإنترنت"
                message.contains("user-not-found") || message.contains("no user record") || message.contains("not found") -> "المستخدم غير موجود"
                message.contains("wrong-password") || message.contains("invalid-credential") || message.contains("invalid_credential") -> "كلمة المرور غير صحيحة"
                else -> "حدث خطأ أثناء تسجيل الدخول"
            }
            return@withContext Resource.Error(displayMessage)
        }
    }

    suspend fun checkUsernameAvailability(username: String): Resource<Boolean> = withContext(Dispatchers.IO) {
         try {
             checkFirebase(context)
             val normalizedUsername = username.trim().lowercase()
             val snap = kotlinx.coroutines.withTimeoutOrNull(10000L) { 
                 firestore.collection("users").whereEqualTo("normalizedUsername", normalizedUsername).get().await() 
             }
             if (snap == null) return@withContext Resource.Error("Network timeout")
             return@withContext Resource.Success(snap.isEmpty)
         } catch (e: Exception) {
             if (e is kotlinx.coroutines.CancellationException) throw e
             return@withContext Resource.Error(e.message ?: "Failed to check availability")
         }
    }
}
