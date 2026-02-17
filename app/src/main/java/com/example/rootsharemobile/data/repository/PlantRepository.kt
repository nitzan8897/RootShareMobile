package com.example.rootsharemobile.data.repository

import androidx.lifecycle.LiveData
import com.example.rootsharemobile.data.local.db.dao.PlantDao
import com.example.rootsharemobile.data.local.db.entity.PlantEntity
import com.example.rootsharemobile.data.model.CreatePlantRequest
import com.example.rootsharemobile.data.model.Plant
import com.example.rootsharemobile.data.model.UpdatePlantRequest
import com.example.rootsharemobile.data.remote.RetrofitClient
import com.example.rootsharemobile.data.remote.mapHttpError
import com.example.rootsharemobile.data.remote.mapNetworkError

/**
 * Repository for plant data.
 *
 * Follows the offline-first pattern mandated by the course:
 *  1. Fetch fresh data from the remote API.
 *  2. Persist it into Room (the Single Source of Truth).
 *  3. The UI observes Room LiveData — it never reads from the network directly.
 */
class PlantRepository(private val plantDao: PlantDao) {

    private val apiService = RetrofitClient.apiService

    // -------------------------------------------------------------------------
    // Room LiveData — observed by ViewModels and, through them, by Fragments.
    // -------------------------------------------------------------------------

    /** Emits featured plants whenever the Room cache changes. */
    fun observeFeaturedPlants(): LiveData<List<PlantEntity>> =
        plantDao.observeFeaturedPlants()

    /** Emits the full plant list whenever the Room cache changes. */
    fun observeAllPlants(): LiveData<List<PlantEntity>> =
        plantDao.observeAllPlants()

    // -------------------------------------------------------------------------
    // Network + cache operations — called by ViewModels inside a coroutine.
    // -------------------------------------------------------------------------

    /**
     * Fetch featured plants from the API, save them to Room, then return
     * a success/failure result (the UI reads from Room, not from this result).
     */
    suspend fun fetchAndStoreFeaturedPlants(token: String, limit: Int? = null): Result<Unit> {
        return try {
            val response = apiService.getFeaturedPlants("Bearer $token", limit)
            if (response.isSuccessful) {
                val plants = response.body() ?: emptyList()
                // Mark existing cached plants as non-featured before inserting new ones.
                plantDao.clearFeaturedFlag()
                val entities = plants.map { it.toEntity(isFeatured = true) }
                plantDao.insertPlants(entities)
                Result.success(Unit)
            } else {
                Result.failure(Exception(mapHttpError(response.code(), "plants")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapNetworkError(e)))
        }
    }

    /**
     * Fetch all user plants from the API and persist them to Room.
     */
    suspend fun fetchAndStorePlants(token: String): Result<Unit> {
        return try {
            val response = apiService.getPlants("Bearer $token")
            if (response.isSuccessful) {
                val plants = response.body() ?: emptyList()
                val entities = plants.map { it.toEntity(isFeatured = false) }
                plantDao.insertPlants(entities)
                Result.success(Unit)
            } else {
                Result.failure(Exception(mapHttpError(response.code(), "plants")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapNetworkError(e)))
        }
    }

    /**
     * Create a plant via the API, then refresh the local cache.
     */
    suspend fun createPlant(token: String, request: CreatePlantRequest): Result<Plant> {
        return try {
            val response = apiService.createPlant("Bearer $token", request)
            if (response.isSuccessful && response.body() != null) {
                val plant = response.body()!!
                plantDao.insertPlants(listOf(plant.toEntity(isFeatured = false)))
                Result.success(plant)
            } else {
                Result.failure(Exception(mapHttpError(response.code(), "plant")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapNetworkError(e)))
        }
    }

    /**
     * Delete a plant via the API and remove it from Room.
     */
    suspend fun deletePlant(token: String, id: String): Result<Boolean> {
        return try {
            val response = apiService.deletePlant("Bearer $token", id)
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception(mapHttpError(response.code(), "plant")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapNetworkError(e)))
        }
    }

    /** Clear the local plant cache (called on logout). */
    suspend fun clearLocalCache() = plantDao.deleteAllPlants()

    // -------------------------------------------------------------------------
    // Mapper helpers
    // -------------------------------------------------------------------------

    private fun Plant.toEntity(isFeatured: Boolean) = PlantEntity(
        id = this.id,
        userId = this.userId,
        name = this.name,
        species = this.species,
        status = this.status.name,
        imageUrl = this.imageUrl,
        isFeatured = isFeatured,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}
