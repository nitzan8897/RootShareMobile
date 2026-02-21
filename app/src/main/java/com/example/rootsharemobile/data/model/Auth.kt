package com.example.rootsharemobile.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String
)

data class RegisterRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("username")
    val username: String,

    @SerializedName("password")
    val password: String
)

data class AuthResponse(
    @SerializedName("user")
    val user: User,

    @SerializedName("tokens")
    val tokens: AuthTokens
)

data class AuthTokens(
    @SerializedName("accessToken")
    val accessToken: String,

    @SerializedName("refreshToken")
    val refreshToken: String
)

data class RefreshTokenResponse(
    @SerializedName("accessToken")
    val accessToken: String,

    @SerializedName("refreshToken")
    val refreshToken: String
)

data class GoogleTokenRequest(
    @SerializedName("idToken")
    val idToken: String
)
