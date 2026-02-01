package com.example.rootsharemobile.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.rootsharemobile.data.model.AuthTokens
import com.example.rootsharemobile.data.model.User
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rootshare_auth")

/**
 * Manages authentication tokens and user data using DataStore.
 */
class TokenManager(private val context: Context) {

    private val gson = Gson()

    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val USER_KEY = stringPreferencesKey("user")
    }

    /**
     * Save authentication tokens.
     */
    suspend fun saveTokens(tokens: AuthTokens) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = tokens.accessToken
            preferences[REFRESH_TOKEN_KEY] = tokens.refreshToken
        }
    }

    /**
     * Save user data.
     */
    suspend fun saveUser(user: User) {
        context.dataStore.edit { preferences ->
            preferences[USER_KEY] = gson.toJson(user)
        }
    }

    /**
     * Save both tokens and user.
     */
    suspend fun saveAuth(user: User, tokens: AuthTokens) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = tokens.accessToken
            preferences[REFRESH_TOKEN_KEY] = tokens.refreshToken
            preferences[USER_KEY] = gson.toJson(user)
        }
    }

    /**
     * Get access token as Flow.
     */
    val accessToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[ACCESS_TOKEN_KEY]
    }

    /**
     * Get refresh token as Flow.
     */
    val refreshToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[REFRESH_TOKEN_KEY]
    }

    /**
     * Get user data as Flow.
     */
    val user: Flow<User?> = context.dataStore.data.map { preferences ->
        preferences[USER_KEY]?.let { json ->
            try {
                gson.fromJson(json, User::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Check if user is logged in.
     */
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { preferences ->
        !preferences[ACCESS_TOKEN_KEY].isNullOrEmpty()
    }

    /**
     * Get access token synchronously (for API calls).
     */
    suspend fun getAccessToken(): String? {
        return context.dataStore.data.first()[ACCESS_TOKEN_KEY]
    }

    /**
     * Get refresh token synchronously.
     */
    suspend fun getRefreshToken(): String? {
        return context.dataStore.data.first()[REFRESH_TOKEN_KEY]
    }

    /**
     * Get user synchronously.
     */
    suspend fun getUser(): User? {
        val json = context.dataStore.data.first()[USER_KEY]
        return json?.let {
            try {
                gson.fromJson(it, User::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Clear all auth data (logout).
     */
    suspend fun clearAuth() {
        context.dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY)
            preferences.remove(REFRESH_TOKEN_KEY)
            preferences.remove(USER_KEY)
        }
    }

    /**
     * Update only the tokens (after refresh).
     */
    suspend fun updateTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = accessToken
            preferences[REFRESH_TOKEN_KEY] = refreshToken
        }
    }
}
