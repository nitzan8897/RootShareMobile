package com.example.rootsharemobile.data.model

import com.google.gson.annotations.SerializedName

/**
 * Login request DTO
 */
data class LoginRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String
)

/**
 * Register request DTO
 */
data class RegisterRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("username")
    val username: String,

    @SerializedName("password")
    val password: String
)

/**
 * Auth response containing user and tokens
 */
data class AuthResponse(
    @SerializedName("user")
    val user: User,

    @SerializedName("tokens")
    val tokens: AuthTokens
)

/**
 * JWT tokens
 */
data class AuthTokens(
    @SerializedName("accessToken")
    val accessToken: String,

    @SerializedName("refreshToken")
    val refreshToken: String
)

/**
 * Response for token refresh
 */
data class RefreshTokenResponse(
    @SerializedName("accessToken")
    val accessToken: String,

    @SerializedName("refreshToken")
    val refreshToken: String
)

/**
 * Google token auth request (for mobile)
 */
data class GoogleTokenRequest(
    @SerializedName("idToken")
    val idToken: String
)
