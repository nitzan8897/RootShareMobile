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

    suspend fun saveTokens(tokens: AuthTokens) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = tokens.accessToken
            preferences[REFRESH_TOKEN_KEY] = tokens.refreshToken
        }
    }

    suspend fun saveUser(user: User) {
        context.dataStore.edit { preferences ->
            preferences[USER_KEY] = gson.toJson(user)
        }
    }

    suspend fun saveAuth(user: User, tokens: AuthTokens) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = tokens.accessToken
            preferences[REFRESH_TOKEN_KEY] = tokens.refreshToken
            preferences[USER_KEY] = gson.toJson(user)
        }
    }

    val accessToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[ACCESS_TOKEN_KEY]
    }

    val refreshToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[REFRESH_TOKEN_KEY]
    }

    val user: Flow<User?> = context.dataStore.data.map { preferences ->
        preferences[USER_KEY]?.let { json ->
            try {
                gson.fromJson(json, User::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { preferences ->
        !preferences[ACCESS_TOKEN_KEY].isNullOrEmpty()
    }

    suspend fun getAccessToken(): String? {
        return context.dataStore.data.first()[ACCESS_TOKEN_KEY]
    }

    suspend fun getRefreshToken(): String? {
        return context.dataStore.data.first()[REFRESH_TOKEN_KEY]
    }

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

    suspend fun clearAuth() {
        context.dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY)
            preferences.remove(REFRESH_TOKEN_KEY)
            preferences.remove(USER_KEY)
        }
    }

    suspend fun updateTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = accessToken
            preferences[REFRESH_TOKEN_KEY] = refreshToken
        }
    }
}
