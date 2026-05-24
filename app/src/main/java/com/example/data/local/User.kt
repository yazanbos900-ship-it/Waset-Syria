package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["username"], unique = true),
        Index(value = ["phoneNumber"], unique = true)
    ]
)
data class User(
    @PrimaryKey
    val uid: String,
    val username: String,
    val normalizedUsername: String,
    val phoneNumber: String,
    val email: String? = null,
    val role: String = "customer",
    val profileImage: String? = null,
    val isVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
