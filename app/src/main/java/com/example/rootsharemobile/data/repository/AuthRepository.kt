package com.example.rootsharemobile.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.rootsharemobile.data.local.TokenManager
import com.example.rootsharemobile.data.local.db.dao.PlantDao
import com.example.rootsharemobile.data.local.db.dao.UserDao
import com.example.rootsharemobile.data.local.db.entity.UserEntity
import com.example.rootsharemobile.data.model.AuthResponse
import com.example.rootsharemobile.data.model.GoogleTokenRequest
import com.example.rootsharemobile.data.model.LoginRequest
import com.example.rootsharemobile.data.model.RegisterRequest
import com.example.rootsharemobile.data.model.User
import com.example.rootsharemobile.data.remote.RetrofitClient
import kotlinx.coroutines.flow.Flow
import okhttp3.MultipartBody

/**
 * Repository for authentication operations.
 *
 * After every successful network call that returns a [User], the result is
 * persisted in both [TokenManager] (JWT tokens + lightweight user cache) and
 * [UserDao] (the Room Single Source of Truth for the UI layer).
 */
class AuthRepository(
    private val tokenManager: TokenManager,
    private val userDao: UserDao,
    private val plantDao: PlantDao? = null
) {

    private val apiService = RetrofitClient.apiService

    // -------------------------------------------------------------------------
    // Room LiveData — observed by ViewModels.
    // -------------------------------------------------------------------------

    /** Emits the cached user whenever it changes in Room. */
    fun observeCurrentUser(): LiveData<UserEntity?> =
        userDao.observeCurrentUser()

    /** Reactive login state backed by DataStore. */
    val isLoggedIn: Flow<Boolean> = tokenManager.isLoggedIn

    // -------------------------------------------------------------------------
    // Authentication operations.
    // -------------------------------------------------------------------------

    /** Register a new account. */
    suspend fun register(email: String, username: String, password: String): Result<AuthResponse> {
        return try {
            val request = RegisterRequest(email = email.trim(), username = username.trim(), password = password)
            val response = apiService.register(request)

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                tokenManager.saveAuth(authResponse.user, authResponse.tokens)
                userDao.insertUser(authResponse.user.toEntity())
                Result.success(authResponse)
            } else {
                val message = when (response.code()) {
                    409  -> "An account with this email or username already exists."
                    400  -> "Please check your input and try again."
                    else -> "Registration failed: ${response.message()}"
                }
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    /** Login with email and password. */
    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return try {
            val request = LoginRequest(email = email.trim(), password = password)
            val response = apiService.login(request)

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                tokenManager.saveAuth(authResponse.user, authResponse.tokens)
                userDao.insertUser(authResponse.user.toEntity())
                Result.success(authResponse)
            } else {
                val message = when (response.code()) {
                    401  -> "Invalid email or password."
                    400  -> "Please check your input and try again."
                    else -> "Login failed: ${response.message()}"
                }
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    /** Authenticate with a Google ID token. */
    suspend fun googleAuth(idToken: String): Result<AuthResponse> {
        return try {
            Log.d("AuthRepository", "Starting Google auth with idToken.")
            val request = GoogleTokenRequest(idToken = idToken)
            val response = apiService.googleAuth(request)

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                tokenManager.saveAuth(authResponse.user, authResponse.tokens)
                userDao.insertUser(authResponse.user.toEntity())
                Result.success(authResponse)
            } else {
                val message = when (response.code()) {
                    401  -> "Invalid Google token."
                    400  -> "Google authentication failed."
                    else -> "Google sign-in failed: ${response.message()}"
                }
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    /** Refresh the access token silently. */
    suspend fun refreshToken(): Result<Boolean> {
        return try {
            val refreshToken = tokenManager.getRefreshToken()
                ?: return Result.failure(Exception("No refresh token found."))

            val response = apiService.refreshToken("Bearer $refreshToken")
            if (response.isSuccessful && response.body() != null) {
                val tokens = response.body()!!
                tokenManager.updateTokens(tokens.accessToken, tokens.refreshToken)
                Result.success(true)
            } else {
                tokenManager.clearAuth()
                userDao.deleteAllUsers()
                Result.failure(Exception("Session expired. Please log in again."))
            }
        } catch (e: Exception) {
            tokenManager.clearAuth()
            userDao.deleteAllUsers()
            Result.failure(e)
        }
    }

    /**
     * Logout: call the API, then wipe tokens and the local Room cache.
     * Always succeeds from the UI's perspective so the user is never stuck.
     */
    suspend fun logout(): Result<Boolean> {
        try {
            val accessToken = tokenManager.getAccessToken()
            if (accessToken != null) {
                apiService.logout("Bearer $accessToken")
            }
        } catch (_: Exception) {
            // Ignore network failures — local state is cleared in finally.
        } finally {
            tokenManager.clearAuth()
            userDao.deleteAllUsers()
            plantDao?.deleteAllPlants()
        }
        return Result.success(true)
    }

    /**
     * Fetch the current user from the API, update DataStore, and sync Room.
     *
     * Uses a merge strategy: if the API response omits image URL fields (e.g.
     * the /auth/me endpoint returns only JWT-payload fields), we preserve
     * whatever image URLs are already stored in Room so Glide can still load them.
     */
    suspend fun getCurrentUser(): Result<User> {
        return try {
            val accessToken = tokenManager.getAccessToken()
                ?: return Result.failure(Exception("Not authenticated."))

            val response = apiService.getCurrentUser("Bearer $accessToken")
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!
                val existing = userDao.getCurrentUser()

                // Merge: keep existing image URLs when the API returns null for them
                val merged = user.toEntity().copy(
                    id = user.id.ifBlank { existing?.id ?: user.id },
                    profileImageUrl = user.profileImageUrl?.takeIf { it.isNotBlank() }
                        ?: existing?.profileImageUrl,
                    localProfileImageUrl = user.localProfileImageUrl?.takeIf { it.isNotBlank() }
                        ?: existing?.localProfileImageUrl
                )
                userDao.insertUser(merged)
                Result.success(user)
            } else {
                Result.failure(Exception("Failed to load user profile."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Upload a profile picture, then update both DataStore and Room. */
    suspend fun uploadProfileImage(imagePart: MultipartBody.Part): Result<User> {
        return try {
            val accessToken = tokenManager.getAccessToken()
                ?: return Result.failure(Exception("Not authenticated."))

            val response = apiService.uploadProfileImage("Bearer $accessToken", imagePart)
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!
                tokenManager.saveUser(user)
                userDao.insertUser(user.toEntity())
                Result.success(user)
            } else {
                Result.failure(Exception("Failed to upload image: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    /** Remove the locally stored profile image URL so Glide falls back to the server URL. */
    suspend fun removeLocalProfileImage() {
        userDao.clearLocalProfileImage()
    }

    /** Update the user's profile on the server and sync to Room. */
    suspend fun updateProfile(username: String): Result<User> {
        return try {
            val token = tokenManager.getAccessToken()
                ?: return Result.failure(Exception("Not authenticated"))
            val body = mapOf("username" to username)
            val response = apiService.updateProfile("Bearer $token", body)
            if (response.isSuccessful) {
                val user = response.body()!!
                userDao.insertUser(user.toEntity())
                Result.success(user)
            } else {
                Result.failure(Exception("Update failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    /** Convenience accessor for the current access token. */
    suspend fun getAccessToken(): String? = tokenManager.getAccessToken()

    // -------------------------------------------------------------------------
    // Mapper helper
    // -------------------------------------------------------------------------

    private fun User.toEntity() = UserEntity(
        id = this.id,
        email = this.email,
        username = this.username,
        profileImageUrl = this.profileImageUrl,
        localProfileImageUrl = this.localProfileImageUrl,
        role = this.role.name,
        authProvider = this.authProvider.name,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}
