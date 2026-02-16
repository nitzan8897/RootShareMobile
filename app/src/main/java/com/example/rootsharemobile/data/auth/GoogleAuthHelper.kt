package com.example.rootsharemobile.data.auth

import android.app.Activity
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.rootsharemobile.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.util.UUID

sealed class GoogleAuthResult {
    data class Success(val idToken: String) : GoogleAuthResult()
    data class Error(val message: String) : GoogleAuthResult()
    object Cancelled : GoogleAuthResult()
}

class GoogleAuthHelper(private val activity: Activity) {

    private val credentialManager = CredentialManager.create(activity)

    suspend fun signIn(): GoogleAuthResult {
        return try {
            signInWithGoogleIdOption()
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google sign-in cancelled by user")
            GoogleAuthResult.Cancelled
        } catch (e: NoCredentialException) {
            Log.i(TAG, "No authorized accounts found, trying Sign In With Google flow")
            signInWithGoogleFallback()
        } catch (e: GetCredentialException) {
            handleCredentialException(e, "primary")
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in failed unexpectedly: ${e.message}", e)
            GoogleAuthResult.Error("An unexpected error occurred during sign-in")
        }
    }

    private suspend fun signInWithGoogleFallback(): GoogleAuthResult {
        return try {
            signInWithGoogleOption()
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google sign-in cancelled by user (fallback flow)")
            GoogleAuthResult.Cancelled
        } catch (e: NoCredentialException) {
            Log.e(TAG, "No Google account found on device")
            GoogleAuthResult.Error("No Google account found. Please add a Google account to your device.")
        } catch (e: GetCredentialException) {
            handleCredentialException(e, "fallback")
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in fallback failed unexpectedly: ${e.message}", e)
            GoogleAuthResult.Error("An unexpected error occurred during sign-in")
        }
    }

    private fun handleCredentialException(e: GetCredentialException, flow: String): GoogleAuthResult {
        val errorMessage = e.message ?: "Unknown credential error"
        Log.e(TAG, "Google sign-in failed ($flow): $errorMessage", e)

        val userMessage = when {
            errorMessage.contains("28444") || errorMessage.contains("Developer console") ->
                "Google Sign-In is not configured correctly. " +
                "Ensure you have both a Web and Android OAuth client in Google Cloud Console, " +
                "the Android client has the correct package name and SHA-1 fingerprint, " +
                "and your Google account is added as a test user in the OAuth consent screen."

            errorMessage.contains("reauth failed") ->
                "Google account verification failed. Try removing and re-adding your Google account on this device."

            errorMessage.contains("network") || errorMessage.contains("timeout") ->
                "Network error during sign-in. Please check your internet connection."

            else -> "Google sign-in failed: $errorMessage"
        }

        return GoogleAuthResult.Error(userMessage)
    }

    private suspend fun signInWithGoogleIdOption(): GoogleAuthResult {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .setNonce(generateNonce())
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(
            request = request,
            context = activity
        )

        return handleSignInResult(result)
    }

    private suspend fun signInWithGoogleOption(): GoogleAuthResult {
        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setNonce(generateNonce())
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        val result = credentialManager.getCredential(
            request = request,
            context = activity
        )

        return handleSignInResult(result)
    }

    private fun handleSignInResult(result: GetCredentialResponse): GoogleAuthResult {
        val credential = result.credential

        if (credential is GoogleIdTokenCredential) {
            Log.i(TAG, "Google sign-in successful")
            return GoogleAuthResult.Success(credential.idToken)
        }

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                Log.i(TAG, "Google sign-in successful")
                GoogleAuthResult.Success(googleIdTokenCredential.idToken)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse Google ID token", e)
                GoogleAuthResult.Error("Failed to parse Google ID token")
            }
        }

        Log.e(TAG, "Unexpected credential type: ${credential::class.java.simpleName}")
        return GoogleAuthResult.Error("Unexpected credential type")
    }

    private fun generateNonce(): String {
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    companion object {
        private const val TAG = "GoogleAuth"
    }
}
