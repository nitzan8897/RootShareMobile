package com.example.rootsharemobile.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a cached user in the local database.
 * Acts as the Single Source of Truth for user data in the UI layer.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val email: String,
    val username: String,
    val profileImageUrl: String?,
    val localProfileImageUrl: String?,
    val role: String,
    val authProvider: String,
    val createdAt: String,
    val updatedAt: String
)
