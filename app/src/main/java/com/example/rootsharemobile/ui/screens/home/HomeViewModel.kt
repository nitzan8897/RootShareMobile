package com.example.rootsharemobile.ui.screens.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rootsharemobile.data.model.Plant
import com.example.rootsharemobile.data.model.Post
import com.example.rootsharemobile.data.repository.PlantRepository
import com.example.rootsharemobile.data.repository.PostRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home screen.
 * Manages UI state and data loading for featured plants and community feed.
 */
class HomeViewModel : ViewModel() {

    private val plantRepository = PlantRepository()
    private val postRepository = PostRepository()

    // UI State
    private val _uiState = MutableLiveData<HomeUiState>(HomeUiState.Loading)
    val uiState: LiveData<HomeUiState> = _uiState

    // Featured Plants
    private val _featuredPlants = MutableLiveData<List<Plant>>(emptyList())
    val featuredPlants: LiveData<List<Plant>> = _featuredPlants

    // Community Feed Posts
    private val _feedPosts = MutableLiveData<List<Post>>(emptyList())
    val feedPosts: LiveData<List<Post>> = _feedPosts

    // Error message
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Loading states
    private val _isLoadingPlants = MutableLiveData(false)
    val isLoadingPlants: LiveData<Boolean> = _isLoadingPlants

    private val _isLoadingPosts = MutableLiveData(false)
    val isLoadingPosts: LiveData<Boolean> = _isLoadingPosts

    /**
     * Load all home screen data.
     * @param token JWT authentication token
     */
    fun loadHomeData(token: String) {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            loadFeaturedPlants(token)
            loadFeedPosts(token)
            updateUiState()
        }
    }

    /**
     * Load featured plants from all users.
     */
    private suspend fun loadFeaturedPlants(token: String) {
        _isLoadingPlants.value = true
        val result = plantRepository.getFeaturedPlants(token, limit = 10)
        result.fold(
            onSuccess = { plants ->
                _featuredPlants.value = plants
            },
            onFailure = { error ->
                _errorMessage.value = "Failed to load plants: ${error.message}"
            }
        )
        _isLoadingPlants.value = false
    }

    /**
     * Load community feed posts from the API.
     */
    private suspend fun loadFeedPosts(token: String) {
        _isLoadingPosts.value = true
        val result = postRepository.getPosts(token)
        result.fold(
            onSuccess = { posts ->
                _feedPosts.value = posts
            },
            onFailure = { error ->
                _errorMessage.value = "Failed to load posts: ${error.message}"
            }
        )
        _isLoadingPosts.value = false
    }

    /**
     * Refresh all data.
     */
    fun refresh(token: String) {
        loadHomeData(token)
    }

    /**
     * Clear error message after it's been shown.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    private fun updateUiState() {
        val plants = _featuredPlants.value ?: emptyList()
        val posts = _feedPosts.value ?: emptyList()
        val error = _errorMessage.value

        _uiState.value = when {
            error != null -> HomeUiState.Error(error)
            plants.isEmpty() && posts.isEmpty() -> HomeUiState.Empty
            else -> HomeUiState.Success(plants, posts)
        }
    }
}

/**
 * Sealed class representing the UI state for the Home screen.
 */
sealed class HomeUiState {
    data object Loading : HomeUiState()
    data object Empty : HomeUiState()
    data class Success(
        val featuredPlants: List<Plant>,
        val feedPosts: List<Post>
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
