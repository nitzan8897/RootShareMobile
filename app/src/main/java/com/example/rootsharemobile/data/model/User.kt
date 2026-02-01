package com.example.rootsharemobile.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data class representing a User from the backend API.
 * Matches the backend schema at /api/users
 */
data class User(
    @SerializedName("_id")
    val id: String = "",

    @SerializedName("email")
    val email: String = "",

    @SerializedName("username")
    val username: String = "",

    @SerializedName("profileImageUrl")
    val profileImageUrl: String? = null,

    @SerializedName("role")
    val role: UserRole = UserRole.USER,

    @SerializedName("authProvider")
    val authProvider: AuthProvider = AuthProvider.LOCAL,

    @SerializedName("createdAt")
    val createdAt: String = "",

    @SerializedName("updatedAt")
    val updatedAt: String = ""
)

enum class UserRole {
    @SerializedName("user")
    USER,

    @SerializedName("admin")
    ADMIN
}

enum class AuthProvider {
    @SerializedName("local")
    LOCAL,

    @SerializedName("google")
    GOOGLE
}
