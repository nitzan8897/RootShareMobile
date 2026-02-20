package com.example.rootsharemobile.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.rootsharemobile.data.local.db.AppDatabase
import com.example.rootsharemobile.data.local.db.entity.PlantEntity
import com.example.rootsharemobile.data.local.db.entity.PostEntity
import com.example.rootsharemobile.data.repository.PlantRepository
import com.example.rootsharemobile.data.repository.PostRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home screen.
 *
 * Data flow (offline-first / Room as Single Source of Truth):
 *
 *   Fragment calls loadHomeData(token)
 *       │
 *       ▼
 *   Repository fetches from API → saves to Room
 *       │
 *       ▼
 *   Room LiveData emits new data
 *       │
 *       ▼
 *   ViewModel exposes LiveData → Fragment updates UI
 *
 * The Fragment ONLY observes LiveData from this ViewModel —
 * it never talks to the network or Room directly.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val plantRepository = PlantRepository(database.plantDao())
    private val postRepository = PostRepository(database.postDao())

    // -------------------------------------------------------------------------
    // Sealed UI state for the overall home screen
    // -------------------------------------------------------------------------

    sealed class HomeUiState {
        object Loading : HomeUiState()
        object Success : HomeUiState()
        data class Error(val message: String) : HomeUiState()
    }

    // -------------------------------------------------------------------------
    // LiveData observed by HomeFragment
    //
    // These come directly from Room — they update automatically whenever
    // the database is written to by the repositories.
    // -------------------------------------------------------------------------

    /** Featured plants sourced from the Room database. */
    val featuredPlants: LiveData<List<PlantEntity>> =
        plantRepository.observeFeaturedPlants()

    /** Community feed posts sourced from the Room database. */
    val feedPosts: LiveData<List<PostEntity>> =
        postRepository.observeAllPosts()

    private val _uiState = MutableLiveData<HomeUiState>()
    val uiState: LiveData<HomeUiState> = _uiState

    private val _isLoadingPlants = MutableLiveData(false)
    val isLoadingPlants: LiveData<Boolean> = _isLoadingPlants

    private val _isLoadingPosts = MutableLiveData(false)
    val isLoadingPosts: LiveData<Boolean> = _isLoadingPosts

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    // Prevents redundant API calls when switching tabs — Room LiveData delivers
    // cached data instantly, so the UI still feels snappy on return visits.
    private var hasInitiallyLoaded = false

    // -------------------------------------------------------------------------
    // Operations called by HomeFragment
    // -------------------------------------------------------------------------

    /**
     * Trigger a full home-screen data load.
     * Skipped on tab switches after the first load — Room LiveData delivers
     * the cached data instantly without a network round-trip.
     * Use [refresh] for explicit pull-to-refresh.
     */
    fun loadHomeData(accessToken: String) {
        if (hasInitiallyLoaded) return
        hasInitiallyLoaded = true
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            loadFeaturedPlants(accessToken)
            loadFeedPosts(accessToken)
        }
    }

    /**
     * Pull-to-refresh: always re-fetches all data regardless of the flag.
     */
    fun refresh(accessToken: String) {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            loadFeaturedPlants(accessToken)
            loadFeedPosts(accessToken)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private suspend fun loadFeaturedPlants(token: String) {
        _isLoadingPlants.value = true
        val result = plantRepository.fetchAndStoreFeaturedPlants(token, limit = 6)
        _isLoadingPlants.value = false

        result.onFailure { error ->
            _errorMessage.value = error.message
            _uiState.value = HomeUiState.Error(error.message ?: "Failed to load plants.")
        }

        // If plants succeeded but posts haven't been set yet, update the state
        if (result.isSuccess && _uiState.value is HomeUiState.Loading) {
            _uiState.value = HomeUiState.Success
        }
    }

    private suspend fun loadFeedPosts(token: String) {
        _isLoadingPosts.value = true
        val result = postRepository.fetchAndStorePosts(token)
        _isLoadingPosts.value = false

        result.fold(
            onSuccess = { _uiState.value = HomeUiState.Success },
            onFailure = { error ->
                _errorMessage.value = error.message
                _uiState.value = HomeUiState.Error(error.message ?: "Failed to load posts.")
            }
        )
    }
}
