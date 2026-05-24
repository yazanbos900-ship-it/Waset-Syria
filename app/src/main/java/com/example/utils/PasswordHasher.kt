package com.example.utils

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256

    // Generates a salted hash for a new password
    fun hashPassword(password: String): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        
        // Return format: salt.hash
        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP)
        
        return "$saltBase64.$hashBase64"
    }

    // Verifies an input password against a stored salted hash
    fun verifyPassword(password: String, storedHashWithSalt: String): Boolean {
        try {
            val parts = storedHashWithSalt.split(".")
            if (parts.size != 2) return false
            
            val salt = Base64.decode(parts[0], Base64.NO_WRAP)
            val storedHash = Base64.decode(parts[1], Base64.NO_WRAP)
            
            val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val computedHash = factory.generateSecret(spec).encoded
            
            return computedHash.contentEquals(storedHash)
        } catch (e: Exception) {
            return false
        }
    }
}
