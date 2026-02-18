package com.example.rootsharemobile.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.rootsharemobile.data.local.TokenManager
import com.example.rootsharemobile.data.local.db.AppDatabase
import com.example.rootsharemobile.data.local.db.entity.PlantWithPostCount
import com.example.rootsharemobile.data.model.CreatePlantRequest
import com.example.rootsharemobile.data.model.PlantStatus
import com.example.rootsharemobile.data.model.Species
import com.example.rootsharemobile.data.model.UpdatePlantRequest
import com.example.rootsharemobile.data.repository.PlantRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for the My Garden screen and its child screens (details, add/edit).
 *
 * Data flow (offline-first / Room as SSOT):
 *   Fragment → ViewModel action → Repository (API + Room) → Room LiveData → UI
 *
 * Scoped to the Activity so AddEditPlantBottomSheet and PlantDetailsFragment
 * share the same instance as MyGardenFragment via activityViewModels().
 */
class MyGardenViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val tokenManager = TokenManager(application)
    private val plantRepository = PlantRepository(database.plantDao())

    /** The logged-in user's ID. Set once at load time. */
    private val _currentUserId = MutableLiveData<String>()

    // -------------------------------------------------------------------------
    // UI state
    // -------------------------------------------------------------------------

    /** State for the garden list loading (load / refresh). */
    sealed class GardenUiState {
        object Idle : GardenUiState()
        object Loading : GardenUiState()
        object Success : GardenUiState()
        data class Error(val message: String) : GardenUiState()
    }

    /** State for CRUD operations (create / update / delete). */
    sealed class OperationState {
        object Idle : OperationState()
        object Loading : OperationState()
        object Success : OperationState()
        data class Error(val message: String) : OperationState()
    }

    private val _uiState = MutableLiveData<GardenUiState>(GardenUiState.Idle)
    val uiState: LiveData<GardenUiState> = _uiState

    private val _operationState = MutableLiveData<OperationState>(OperationState.Idle)
    val operationState: LiveData<OperationState> = _operationState

    /** Available species fetched from the API. */
    private val _speciesList = MutableLiveData<List<Species>>(emptyList())
    val speciesList: LiveData<List<Species>> = _speciesList

    /** Single-shot user-facing message (Snackbar). Null after being consumed. */
    private val _snackMessage = MutableLiveData<String?>(null)
    val snackMessage: LiveData<String?> = _snackMessage

    companion object {
        private val DEFAULT_PLANT_IMAGES = listOf(
            "https://em-content.zobj.net/source/apple/391/sunflower_1f33b.png",
            "https://em-content.zobj.net/source/apple/391/blossom_1f33c.png",
            "https://em-content.zobj.net/source/apple/391/tulip_1f337.png",
            "https://em-content.zobj.net/source/apple/391/rose_1f339.png",
            "https://em-content.zobj.net/source/apple/391/hibiscus_1f33a.png",
            "https://em-content.zobj.net/source/apple/391/cherry-blossom_1f338.png",
            "https://em-content.zobj.net/source/apple/391/bouquet_1f490.png",
            "https://em-content.zobj.net/source/apple/391/seedling_1f331.png",
            "https://em-content.zobj.net/source/apple/391/herb_1f33f.png",
            "https://em-content.zobj.net/source/apple/391/shamrock_2618-fe0f.png"
        )

        fun randomDefaultImage(): String = DEFAULT_PLANT_IMAGES.random()
    }

    // -------------------------------------------------------------------------
    // Garden data — sourced from Room LiveData
    // -------------------------------------------------------------------------

    /** Current user's plants with their associated post count, newest first. */
    val gardenPlants: LiveData<List<PlantWithPostCount>> =
        _currentUserId.switchMap { userId ->
            plantRepository.observeGardenPlantsWithPostCount(userId)
        }

    /** Observe a specific plant for the details screen. */
    fun observePlant(plantId: String): LiveData<PlantWithPostCount?> =
        plantRepository.observePlantWithPostCount(plantId)

    // -------------------------------------------------------------------------
    // Actions — called by Fragments
    // -------------------------------------------------------------------------

    fun fetchSpecies(token: String) {
        if (_speciesList.value?.isNotEmpty() == true) return
        viewModelScope.launch {
            val result = plantRepository.fetchSpecies(token)
            result.fold(
                onSuccess = { list ->
                    Log.d("MyGardenVM", "fetchSpecies: loaded ${list.size} species")
                    _speciesList.value = list
                },
                onFailure = { e ->
                    Log.e("MyGardenVM", "fetchSpecies failed: ${e.message}")
                }
            )
        }
    }

    /**
     * Fetch plants from API and save to Room.
     * Skipped if data was already loaded this session.
     */
    fun loadPlants(token: String) {
        if (_uiState.value == GardenUiState.Loading) return
        _uiState.value = GardenUiState.Loading
        viewModelScope.launch {
            // Always refresh userId so a logout → login with a different account is handled
            val user = tokenManager.getUser()
            val userId = user?.id ?: ""
            if (_currentUserId.value != userId) {
                _currentUserId.value = userId
                Log.d("MyGardenVM", "loadPlants: userId set to $userId")
            }
            fetchSpecies(token)
            val result = plantRepository.fetchAndStorePlants(token)
            Log.d("MyGardenVM", "loadPlants result: $result")
            _uiState.value = result.fold(
                onSuccess = { GardenUiState.Success },
                onFailure = { GardenUiState.Error(it.message ?: "Failed to load plants.") }
            )
        }
    }

    fun refresh(token: String) {
        _uiState.value = GardenUiState.Loading
        viewModelScope.launch {
            val user = tokenManager.getUser()
            val userId = user?.id ?: ""
            if (_currentUserId.value != userId) {
                _currentUserId.value = userId
                Log.d("MyGardenVM", "refresh: userId set to $userId")
            }
            val result = plantRepository.fetchAndStorePlants(token)
            _uiState.value = result.fold(
                onSuccess = { GardenUiState.Success },
                onFailure = { GardenUiState.Error(it.message ?: "Failed to load plants.") }
            )
        }
    }

    fun createPlant(token: String, name: String, species: String, imageUrl: String) {
        Log.d("MyGardenVM", "createPlant called: name=$name, species=$species, imageUrl=$imageUrl")
        _operationState.value = OperationState.Loading
        viewModelScope.launch {
            val result = plantRepository.createPlant(
                token,
                CreatePlantRequest(name = name, species = species, imageUrl = imageUrl)
            )
            Log.d("MyGardenVM", "createPlant result: $result")
            result.fold(
                onSuccess = {
                    _operationState.value = OperationState.Success
                    _snackMessage.value = "Plant added to your garden!"
                },
                onFailure = {
                    Log.e("MyGardenVM", "createPlant failed: ${it.message}")
                    _operationState.value = OperationState.Error(it.message ?: "Could not add plant.")
                }
            )
        }
    }

    fun updatePlant(
        token: String,
        id: String,
        name: String,
        species: String,
        imageUrl: String,
        status: PlantStatus
    ) {
        _operationState.value = OperationState.Loading
        viewModelScope.launch {
            val result = plantRepository.updatePlant(
                token, id,
                UpdatePlantRequest(name = name, species = species, imageUrl = imageUrl, status = status)
            )
            result.fold(
                onSuccess = {
                    _operationState.value = OperationState.Success
                    _snackMessage.value = "Plant updated."
                },
                onFailure = {
                    _operationState.value = OperationState.Error(it.message ?: "Could not update plant.")
                }
            )
        }
    }

    fun deletePlant(token: String, id: String) {
        _operationState.value = OperationState.Loading
        viewModelScope.launch {
            val result = plantRepository.deletePlant(token, id)
            result.fold(
                onSuccess = {
                    _operationState.value = OperationState.Success
                    _snackMessage.value = "Plant removed from your garden."
                },
                onFailure = {
                    _operationState.value = OperationState.Error(it.message ?: "Could not delete plant.")
                }
            )
        }
    }

    fun clearSnackMessage() {
        _snackMessage.value = null
    }

    /** Reset operation state to Idle (e.g., before opening the add/edit sheet). */
    fun resetOperationState() {
        _operationState.value = OperationState.Idle
    }
}
