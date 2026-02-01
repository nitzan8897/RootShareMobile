package com.example.rootsharemobile.data.remote

import com.example.rootsharemobile.data.model.AuthResponse
import com.example.rootsharemobile.data.model.GoogleTokenRequest
import com.example.rootsharemobile.data.model.LoginRequest
import com.example.rootsharemobile.data.model.RefreshTokenResponse
import com.example.rootsharemobile.data.model.RegisterRequest
import com.example.rootsharemobile.data.model.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit API service interface for RootShare backend.
 * Auth endpoints only.
 */
interface ApiService {

    // ==================== AUTH ====================

    /**
     * Register a new user.
     */
    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthResponse>

    /**
     * Login with email and password.
     */
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    /**
     * Refresh access token using refresh token.
     */
    @POST("auth/refresh")
    suspend fun refreshToken(
        @Header("Authorization") refreshToken: String
    ): Response<RefreshTokenResponse>

    /**
     * Logout and invalidate refresh token.
     */
    @POST("auth/logout")
    suspend fun logout(
        @Header("Authorization") token: String
    ): Response<Unit>

    /**
     * Get current authenticated user.
     */
    @GET("auth/me")
    suspend fun getCurrentUser(
        @Header("Authorization") token: String
    ): Response<User>

    /**
     * Authenticate with Google ID token (for mobile).
     */
    @POST("auth/google/token")
    suspend fun googleAuth(
        @Body request: GoogleTokenRequest
    ): Response<AuthResponse>
}
