package com.example.rootsharemobile.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.rootsharemobile.data.local.db.AppDatabase
import com.example.rootsharemobile.data.local.db.entity.PlantWithPostCount
import com.example.rootsharemobile.data.model.CreatePlantRequest
import com.example.rootsharemobile.data.model.PlantStatus
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
    private val plantRepository = PlantRepository(database.plantDao())

    // -------------------------------------------------------------------------
    // UI state
    // -------------------------------------------------------------------------

    sealed class GardenUiState {
        object Idle : GardenUiState()
        object Loading : GardenUiState()
        object Success : GardenUiState()
        data class Error(val message: String) : GardenUiState()
    }

    private val _uiState = MutableLiveData<GardenUiState>(GardenUiState.Idle)
    val uiState: LiveData<GardenUiState> = _uiState

    /** Single-shot user-facing message (Snackbar). Null after being consumed. */
    private val _snackMessage = MutableLiveData<String?>(null)
    val snackMessage: LiveData<String?> = _snackMessage

    // -------------------------------------------------------------------------
    // Garden data — sourced from Room LiveData
    // -------------------------------------------------------------------------

    /** All user plants with their associated post count, newest first. */
    val gardenPlants: LiveData<List<PlantWithPostCount>> =
        plantRepository.observeGardenPlantsWithPostCount()

    /** Observe a specific plant for the details screen. */
    fun observePlant(plantId: String): LiveData<PlantWithPostCount?> =
        plantRepository.observePlantWithPostCount(plantId)

    // -------------------------------------------------------------------------
    // Actions — called by Fragments
    // -------------------------------------------------------------------------

    /**
     * Fetch plants from API and save to Room.
     * Skipped if data was already loaded this session.
     */
    fun loadPlants(token: String) {
        if (_uiState.value == GardenUiState.Loading) return
        _uiState.value = GardenUiState.Loading
        viewModelScope.launch {
            val result = plantRepository.fetchAndStorePlants(token)
            _uiState.value = result.fold(
                onSuccess = { GardenUiState.Success },
                onFailure = { GardenUiState.Error(it.message ?: "Failed to load plants.") }
            )
        }
    }

    fun refresh(token: String) {
        _uiState.value = GardenUiState.Loading
        viewModelScope.launch {
            val result = plantRepository.fetchAndStorePlants(token)
            _uiState.value = result.fold(
                onSuccess = { GardenUiState.Success },
                onFailure = { GardenUiState.Error(it.message ?: "Failed to load plants.") }
            )
        }
    }

    fun createPlant(token: String, name: String, species: String, imageUrl: String) {
        _uiState.value = GardenUiState.Loading
        viewModelScope.launch {
            val result = plantRepository.createPlant(
                token,
                CreatePlantRequest(name = name, species = species, imageUrl = imageUrl)
            )
            result.fold(
                onSuccess = {
                    _uiState.value = GardenUiState.Success
                    _snackMessage.value = "Plant added to your garden!"
                },
                onFailure = {
                    _uiState.value = GardenUiState.Error(it.message ?: "Could not add plant.")
                    _snackMessage.value = it.message
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
        _uiState.value = GardenUiState.Loading
        viewModelScope.launch {
            val result = plantRepository.updatePlant(
                token, id,
                UpdatePlantRequest(name = name, species = species, imageUrl = imageUrl, status = status)
            )
            result.fold(
                onSuccess = {
                    _uiState.value = GardenUiState.Success
                    _snackMessage.value = "Plant updated."
                },
                onFailure = {
                    _uiState.value = GardenUiState.Error(it.message ?: "Could not update plant.")
                    _snackMessage.value = it.message
                }
            )
        }
    }

    fun deletePlant(token: String, id: String) {
        _uiState.value = GardenUiState.Loading
        viewModelScope.launch {
            val result = plantRepository.deletePlant(token, id)
            result.fold(
                onSuccess = {
                    _uiState.value = GardenUiState.Success
                    _snackMessage.value = "Plant removed from your garden."
                },
                onFailure = {
                    _uiState.value = GardenUiState.Error(it.message ?: "Could not delete plant.")
                    _snackMessage.value = it.message
                }
            )
        }
    }

    fun clearSnackMessage() {
        _snackMessage.value = null
    }
}
