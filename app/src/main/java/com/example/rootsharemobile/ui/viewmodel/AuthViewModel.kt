package com.example.rootsharemobile.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.rootsharemobile.data.auth.GoogleAuthHelper
import com.example.rootsharemobile.data.auth.GoogleAuthResult
import com.example.rootsharemobile.data.local.TokenManager
import com.example.rootsharemobile.data.local.db.AppDatabase
import com.example.rootsharemobile.data.local.db.entity.UserEntity
import com.example.rootsharemobile.data.repository.AuthRepository
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

/**
 * ViewModel for authentication screens (Login & Register).
 *
 * Responsibilities:
 *  - Expose authentication state as LiveData for Fragments to observe.
 *  - Delegate all business logic to [AuthRepository].
 *  - Never hold a direct reference to View or Context (beyond Application).
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application)
    private val database = AppDatabase.getInstance(application)
    private val authRepository = AuthRepository(tokenManager, database.userDao(), database.plantDao(), database.postDao())

    /** Dashboard stats from Room — reactive counts for profile mini-dashboard. */
    val plantCount: LiveData<Int> = authRepository.observeCurrentUser().switchMap { user ->
        val userId = user?.id ?: ""
        database.plantDao().observePlantCount(userId)
    }
    val postCount: LiveData<Int> = database.postDao().observePostCount()

    // -------------------------------------------------------------------------
    // Sealed UI state for authentication actions
    // -------------------------------------------------------------------------

    sealed class AuthUiState {
        object Idle : AuthUiState()
        object Loading : AuthUiState()
        object Success : AuthUiState()
        data class Error(val message: String) : AuthUiState()
    }

    sealed class UploadState {
        object Idle : UploadState()
        object Uploading : UploadState()
        object Success : UploadState()
        data class Error(val message: String) : UploadState()
    }

    // -------------------------------------------------------------------------
    // Exposed LiveData observed by Fragments
    // -------------------------------------------------------------------------

    /** Whether the user currently has a valid session (backed by DataStore). */
    val isLoggedIn: LiveData<Boolean> = authRepository.isLoggedIn.asLiveData()

    /** The current user entity from the Room database. */
    val currentUser: LiveData<UserEntity?> = authRepository.observeCurrentUser()

    private val _authState = MutableLiveData<AuthUiState>(AuthUiState.Idle)
    val authState: LiveData<AuthUiState> = _authState

    private val _uploadState = MutableLiveData<UploadState>(UploadState.Idle)
    val uploadState: LiveData<UploadState> = _uploadState

    // -------------------------------------------------------------------------
    // Authentication operations
    // -------------------------------------------------------------------------

    /**
     * Attempt to log in with email and password.
     * The Fragment observes [authState] to react to success/failure.
     */
    fun login(email: String, password: String) {
        if (!validateLoginInput(email, password)) return

        _authState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = authRepository.login(email, password)
            _authState.value = result.fold(
                onSuccess = { AuthUiState.Success },
                onFailure = { AuthUiState.Error(it.message ?: "Login failed.") }
            )
        }
    }

    /**
     * Register a new account with email, username, and password.
     */
    fun register(username: String, email: String, password: String, confirmPassword: String) {
        if (!validateRegisterInput(username, email, password, confirmPassword)) return

        _authState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = authRepository.register(email, username, password)
            _authState.value = result.fold(
                onSuccess = { AuthUiState.Success },
                onFailure = { AuthUiState.Error(it.message ?: "Registration failed.") }
            )
        }
    }

    /**
     * Authenticate with a Google account.
     * The [GoogleAuthHelper] requires an Activity context; the Fragment
     * creates it with [requireActivity()] and passes it here.
     */
    fun signInWithGoogle(googleAuthHelper: GoogleAuthHelper) {
        _authState.value = AuthUiState.Loading
        viewModelScope.launch {
            when (val googleResult = googleAuthHelper.signIn()) {
                is GoogleAuthResult.Success -> {
                    val result = authRepository.googleAuth(googleResult.idToken)
                    _authState.value = result.fold(
                        onSuccess = { AuthUiState.Success },
                        onFailure = { AuthUiState.Error(it.message ?: "Google sign-in failed.") }
                    )
                }
                is GoogleAuthResult.Error -> {
                    _authState.value = AuthUiState.Error(googleResult.message)
                }
                is GoogleAuthResult.Cancelled -> {
                    _authState.value = AuthUiState.Idle
                }
            }
        }
    }

    /**
     * Logout the current user: clears tokens and wipes the Room cache.
     */
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _authState.value = AuthUiState.Idle
        }
    }

    /**
     * Suspend version of logout — the caller awaits completion before navigating.
     * This prevents auto-login in LoginFragment (which observes isLoggedIn) by
     * ensuring the DataStore token is cleared before the Login screen appears.
     */
    suspend fun logoutSuspend() {
        authRepository.logout()
        _authState.value = AuthUiState.Idle
    }

    /**
     * Fetch the current user from the API and sync to Room.
     * ProfileFragment calls this on view creation so the profile data is always fresh.
     */
    fun fetchCurrentUser() {
        viewModelScope.launch {
            authRepository.getCurrentUser()
        }
    }

    /**
     * Upload a new profile picture.
     * Fragments observe [uploadState] for progress and result.
     */
    fun uploadProfileImage(imagePart: MultipartBody.Part) {
        _uploadState.value = UploadState.Uploading
        viewModelScope.launch {
            val result = authRepository.uploadProfileImage(imagePart)
            _uploadState.value = result.fold(
                onSuccess = { UploadState.Success },
                onFailure = { UploadState.Error(it.message ?: "Upload failed.") }
            )
        }
    }

    /**
     * Remove the custom profile photo and revert to the Google/default photo.
     * Clears localProfileImageUrl from Room so Glide falls back to the server URL.
     */
    fun removeProfileImage() {
        viewModelScope.launch {
            authRepository.removeLocalProfileImage()
        }
    }

    // -------------------------------------------------------------------------
    // Profile editing
    // -------------------------------------------------------------------------

    sealed class ProfileUpdateState {
        object Idle : ProfileUpdateState()
        object Saving : ProfileUpdateState()
        object Success : ProfileUpdateState()
        data class Error(val message: String) : ProfileUpdateState()
    }

    private val _profileUpdateState = MutableLiveData<ProfileUpdateState>(ProfileUpdateState.Idle)
    val profileUpdateState: LiveData<ProfileUpdateState> = _profileUpdateState

    fun updateProfile(username: String) {
        _profileUpdateState.value = ProfileUpdateState.Saving
        viewModelScope.launch {
            val result = authRepository.updateProfile(username)
            _profileUpdateState.value = result.fold(
                onSuccess = { ProfileUpdateState.Success },
                onFailure = { ProfileUpdateState.Error(it.message ?: "Update failed.") }
            )
        }
    }

    fun resetProfileUpdateState() {
        _profileUpdateState.value = ProfileUpdateState.Idle
    }

    /** Reset upload state after the Fragment has handled the result. */
    fun resetUploadState() {
        _uploadState.value = UploadState.Idle
    }

    /** Reset auth state to Idle (e.g., after navigating away from an error). */
    fun resetAuthState() {
        _authState.value = AuthUiState.Idle
    }

    /** Convenience for Fragments that need the token for API calls. */
    suspend fun getAccessToken(): String? = authRepository.getAccessToken()

    // -------------------------------------------------------------------------
    // Input validation (keeps validation logic in the ViewModel, not the Fragment)
    // -------------------------------------------------------------------------

    private fun validateLoginInput(email: String, password: String): Boolean {
        if (email.isBlank()) {
            _authState.value = AuthUiState.Error("Email address is required.")
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authState.value = AuthUiState.Error("Please enter a valid email address.")
            return false
        }
        if (password.isBlank()) {
            _authState.value = AuthUiState.Error("Password is required.")
            return false
        }
        if (password.length < 6) {
            _authState.value = AuthUiState.Error("Password must be at least 6 characters.")
            return false
        }
        return true
    }

    private fun validateRegisterInput(
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        if (username.isBlank()) {
            _authState.value = AuthUiState.Error("Username is required.")
            return false
        }
        if (username.length < 3) {
            _authState.value = AuthUiState.Error("Username must be at least 3 characters.")
            return false
        }
        if (email.isBlank()) {
            _authState.value = AuthUiState.Error("Email address is required.")
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authState.value = AuthUiState.Error("Please enter a valid email address.")
            return false
        }
        if (password.isBlank()) {
            _authState.value = AuthUiState.Error("Password is required.")
            return false
        }
        if (password.length < 8) {
            _authState.value = AuthUiState.Error("Password must be at least 8 characters.")
            return false
        }
        if (password != confirmPassword) {
            _authState.value = AuthUiState.Error("Passwords do not match.")
            return false
        }
        return true
    }
}
