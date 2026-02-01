package com.example.rootsharemobile.ui.screens.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.rootsharemobile.data.local.TokenManager
import com.example.rootsharemobile.data.model.User
import com.example.rootsharemobile.data.repository.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for authentication screens (Login & Register).
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application)
    private val authRepository = AuthRepository(tokenManager)

    // Auth state
    val isLoggedIn: StateFlow<Boolean> = authRepository.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val currentUser: StateFlow<User?> = authRepository.user
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // UI State
    private val _uiState = MutableLiveData<AuthUiState>(AuthUiState.Idle)
    val uiState: LiveData<AuthUiState> = _uiState

    // Error message
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Field errors for validation
    private val _fieldErrors = MutableLiveData<FieldErrors>(FieldErrors())
    val fieldErrors: LiveData<FieldErrors> = _fieldErrors

    /**
     * Login with email and password.
     */
    fun login(email: String, password: String) {
        // Validate inputs
        val errors = validateLoginInputs(email, password)
        if (errors.hasErrors()) {
            _fieldErrors.value = errors
            return
        }

        _uiState.value = AuthUiState.Loading
        _errorMessage.value = null
        _fieldErrors.value = FieldErrors()

        viewModelScope.launch {
            val result = authRepository.login(email, password)
            result.fold(
                onSuccess = {
                    _uiState.value = AuthUiState.Success
                },
                onFailure = { error ->
                    _errorMessage.value = error.message
                    _uiState.value = AuthUiState.Error(error.message ?: "Login failed")
                }
            )
        }
    }

    /**
     * Register a new user.
     */
    fun register(username: String, email: String, password: String, confirmPassword: String) {
        // Validate inputs
        val errors = validateRegisterInputs(username, email, password, confirmPassword)
        if (errors.hasErrors()) {
            _fieldErrors.value = errors
            return
        }

        _uiState.value = AuthUiState.Loading
        _errorMessage.value = null
        _fieldErrors.value = FieldErrors()

        viewModelScope.launch {
            val result = authRepository.register(email, username, password)
            result.fold(
                onSuccess = {
                    _uiState.value = AuthUiState.Success
                },
                onFailure = { error ->
                    _errorMessage.value = error.message
                    _uiState.value = AuthUiState.Error(error.message ?: "Registration failed")
                }
            )
        }
    }

    /**
     * Logout the current user.
     */
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = AuthUiState.Idle
        }
    }

    /**
     * Get access token for API calls.
     */
    suspend fun getAccessToken(): String? = authRepository.getAccessToken()

    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }

    /**
     * Clear specific field error.
     */
    fun clearFieldError(field: String) {
        _fieldErrors.value = when (field) {
            "username" -> _fieldErrors.value?.copy(username = null) ?: FieldErrors()
            "email" -> _fieldErrors.value?.copy(email = null) ?: FieldErrors()
            "password" -> _fieldErrors.value?.copy(password = null) ?: FieldErrors()
            "confirmPassword" -> _fieldErrors.value?.copy(confirmPassword = null) ?: FieldErrors()
            else -> _fieldErrors.value ?: FieldErrors()
        }
    }

    /**
     * Reset UI state to idle.
     */
    fun resetState() {
        _uiState.value = AuthUiState.Idle
        _errorMessage.value = null
        _fieldErrors.value = FieldErrors()
    }

    // Validation functions
    private fun validateLoginInputs(email: String, password: String): FieldErrors {
        var errors = FieldErrors()

        if (email.isBlank()) {
            errors = errors.copy(email = "Email is required")
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errors = errors.copy(email = "Invalid email format")
        }

        if (password.isBlank()) {
            errors = errors.copy(password = "Password is required")
        } else if (password.length < 8) {
            errors = errors.copy(password = "Password must be at least 8 characters")
        }

        return errors
    }

    private fun validateRegisterInputs(
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ): FieldErrors {
        var errors = FieldErrors()

        // Username validation
        if (username.isBlank()) {
            errors = errors.copy(username = "Username is required")
        } else if (username.length < 3) {
            errors = errors.copy(username = "Username must be at least 3 characters")
        } else if (username.length > 30) {
            errors = errors.copy(username = "Username must be less than 30 characters")
        } else if (!username.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
            errors = errors.copy(username = "Username can only contain letters, numbers, underscore, and hyphen")
        }

        // Email validation
        if (email.isBlank()) {
            errors = errors.copy(email = "Email is required")
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errors = errors.copy(email = "Please enter a valid email address")
        }

        // Password validation
        if (password.isBlank()) {
            errors = errors.copy(password = "Password is required")
        } else if (password.length < 8) {
            errors = errors.copy(password = "Password must be at least 8 characters")
        } else if (!password.contains(Regex("[a-z]"))) {
            errors = errors.copy(password = "Password must contain at least one lowercase letter")
        } else if (!password.contains(Regex("[A-Z]"))) {
            errors = errors.copy(password = "Password must contain at least one uppercase letter")
        } else if (!password.contains(Regex("[0-9]"))) {
            errors = errors.copy(password = "Password must contain at least one number")
        }

        // Confirm password validation
        if (password != confirmPassword) {
            errors = errors.copy(confirmPassword = "Passwords do not match")
        }

        return errors
    }
}

/**
 * UI state for auth screens.
 */
sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

/**
 * Field validation errors.
 */
data class FieldErrors(
    val username: String? = null,
    val email: String? = null,
    val password: String? = null,
    val confirmPassword: String? = null
) {
    fun hasErrors(): Boolean = username != null || email != null || password != null || confirmPassword != null
}
