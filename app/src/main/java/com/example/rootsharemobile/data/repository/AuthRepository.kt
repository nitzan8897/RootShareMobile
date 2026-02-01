package com.example.rootsharemobile.data.repository

import com.example.rootsharemobile.data.local.TokenManager
import com.example.rootsharemobile.data.model.AuthResponse
import com.example.rootsharemobile.data.model.LoginRequest
import com.example.rootsharemobile.data.model.RegisterRequest
import com.example.rootsharemobile.data.model.User
import com.example.rootsharemobile.data.remote.RetrofitClient
import kotlinx.coroutines.flow.Flow

/**
 * Repository for authentication operations.
 */
class AuthRepository(private val tokenManager: TokenManager) {

    private val apiService = RetrofitClient.apiService

    /**
     * Register a new user.
     */
    suspend fun register(
        email: String,
        username: String,
        password: String
    ): Result<AuthResponse> {
        return try {
            val request = RegisterRequest(
                email = email.trim(),
                username = username.trim(),
                password = password
            )
            val response = apiService.register(request)

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                // Save tokens and user to DataStore
                tokenManager.saveAuth(authResponse.user, authResponse.tokens)
                Result.success(authResponse)
            } else {
                val errorMessage = when (response.code()) {
                    409 -> "An account with this email or username already exists"
                    400 -> "Please check your input and try again"
                    else -> "Registration failed: ${response.message()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    /**
     * Login with email and password.
     */
    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return try {
            val request = LoginRequest(
                email = email.trim(),
                password = password
            )
            val response = apiService.login(request)

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                // Save tokens and user to DataStore
                tokenManager.saveAuth(authResponse.user, authResponse.tokens)
                Result.success(authResponse)
            } else {
                val errorMessage = when (response.code()) {
                    401 -> "Invalid email or password"
                    400 -> "Please check your input and try again"
                    else -> "Login failed: ${response.message()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    /**
     * Refresh access token.
     */
    suspend fun refreshToken(): Result<Boolean> {
        return try {
            val refreshToken = tokenManager.getRefreshToken()
                ?: return Result.failure(Exception("No refresh token"))

            val response = apiService.refreshToken("Bearer $refreshToken")

            if (response.isSuccessful && response.body() != null) {
                val tokens = response.body()!!
                tokenManager.updateTokens(tokens.accessToken, tokens.refreshToken)
                Result.success(true)
            } else {
                // Token refresh failed, clear auth
                tokenManager.clearAuth()
                Result.failure(Exception("Token refresh failed"))
            }
        } catch (e: Exception) {
            tokenManager.clearAuth()
            Result.failure(e)
        }
    }

    /**
     * Logout and clear auth data.
     */
    suspend fun logout(): Result<Boolean> {
        return try {
            val accessToken = tokenManager.getAccessToken()
            if (accessToken != null) {
                apiService.logout("Bearer $accessToken")
            }
            tokenManager.clearAuth()
            Result.success(true)
        } catch (e: Exception) {
            // Clear auth even if API call fails
            tokenManager.clearAuth()
            Result.success(true)
        }
    }

    /**
     * Get current user from API.
     */
    suspend fun getCurrentUser(): Result<User> {
        return try {
            val accessToken = tokenManager.getAccessToken()
                ?: return Result.failure(Exception("Not authenticated"))

            val response = apiService.getCurrentUser("Bearer $accessToken")

            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!
                tokenManager.saveUser(user)
                Result.success(user)
            } else if (response.code() == 401) {
                // Try to refresh token
                val refreshResult = refreshToken()
                if (refreshResult.isSuccess) {
                    // Retry with new token
                    val newToken = tokenManager.getAccessToken()!!
                    val retryResponse = apiService.getCurrentUser("Bearer $newToken")
                    if (retryResponse.isSuccessful && retryResponse.body() != null) {
                        val user = retryResponse.body()!!
                        tokenManager.saveUser(user)
                        Result.success(user)
                    } else {
                        Result.failure(Exception("Failed to get user"))
                    }
                } else {
                    Result.failure(Exception("Session expired. Please login again."))
                }
            } else {
                Result.failure(Exception("Failed to get user: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get access token for authenticated requests.
     */
    suspend fun getAccessToken(): String? = tokenManager.getAccessToken()

    /**
     * Check if user is logged in.
     */
    val isLoggedIn: Flow<Boolean> = tokenManager.isLoggedIn

    /**
     * Get cached user data.
     */
    val user: Flow<User?> = tokenManager.user
}
