package com.example.rootsharemobile.data.remote

import com.example.rootsharemobile.data.local.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * OkHttp Authenticator that automatically refreshes tokens on 401 responses.
 * This handles token expiration globally for all API calls.
 */
class TokenRefreshAuthenticator(
    private val tokenManager: TokenManager
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Don't retry if we've already tried to refresh
        if (response.request.header("Authorization-Retry") != null) {
            return null
        }

        // Don't try to refresh for auth endpoints (login, register, refresh itself)
        val path = response.request.url.encodedPath
        if (path.contains("/auth/login") ||
            path.contains("/auth/register") ||
            path.contains("/auth/refresh") ||
            path.contains("/auth/google")) {
            return null
        }

        return runBlocking {
            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken == null) {
                // No refresh token, can't refresh - user needs to login again
                tokenManager.clearAuth()
                return@runBlocking null
            }

            try {
                // Call refresh endpoint
                val refreshResponse = RetrofitClient.apiService.refreshToken("Bearer $refreshToken")

                if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                    val tokens = refreshResponse.body()!!
                    tokenManager.updateTokens(tokens.accessToken, tokens.refreshToken)

                    // Retry the original request with new token
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${tokens.accessToken}")
                        .header("Authorization-Retry", "true")
                        .build()
                } else {
                    // Refresh failed, clear auth
                    tokenManager.clearAuth()
                    null
                }
            } catch (e: Exception) {
                // Network error during refresh, clear auth
                tokenManager.clearAuth()
                null
            }
        }
    }
}
