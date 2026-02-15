package com.example.rootsharemobile.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.rootsharemobile.data.remote.ApiConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import java.security.MessageDigest
import java.util.UUID

sealed class GoogleAuthResult {
    data class Success(val idToken: String) : GoogleAuthResult()
    data class Error(val message: String) : GoogleAuthResult()
    object Cancelled : GoogleAuthResult()
}

class GoogleAuthHelper(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    suspend fun signIn(): GoogleAuthResult {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(ApiConfig.GOOGLE_WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .setNonce(generateNonce())
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            handleSignInResult(result)
        } catch (e: GetCredentialCancellationException) {
            GoogleAuthResult.Cancelled
        } catch (e: NoCredentialException) {
            GoogleAuthResult.Error("No Google account found. Please add a Google account to your device.")
        } catch (e: GetCredentialException) {
            GoogleAuthResult.Error(e.message ?: "Failed to get credentials")
        } catch (e: Exception) {
            GoogleAuthResult.Error(e.message ?: "An unexpected error occurred")
        }
    }

    private fun handleSignInResult(result: GetCredentialResponse): GoogleAuthResult {
        return when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        GoogleAuthResult.Success(googleIdTokenCredential.idToken)
                    } catch (e: GoogleIdTokenParsingException) {
                        GoogleAuthResult.Error("Failed to parse Google ID token")
                    }
                } else {
                    GoogleAuthResult.Error("Unexpected credential type")
                }
            }
            else -> GoogleAuthResult.Error("Unexpected credential type")
        }
    }

    private fun generateNonce(): String {
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
