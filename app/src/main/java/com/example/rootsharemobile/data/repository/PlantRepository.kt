package com.example.rootsharemobile.data.repository

import com.example.rootsharemobile.data.model.CreatePlantRequest
import com.example.rootsharemobile.data.model.Plant
import com.example.rootsharemobile.data.model.PlantStatus
import com.example.rootsharemobile.data.model.UpdatePlantRequest
import com.example.rootsharemobile.data.remote.RetrofitClient
import com.example.rootsharemobile.data.remote.mapHttpError
import com.example.rootsharemobile.data.remote.mapNetworkError

/**
 * Repository for Plant data operations.
 * Acts as a single source of truth for plant data.
 */
class PlantRepository {

    private val apiService = RetrofitClient.apiService

    /**
     * Get all plants for the authenticated user.
     */
    suspend fun getPlants(token: String, status: PlantStatus? = null): Result<List<Plant>> {
        return try {
            val response = apiService.getPlants("Bearer $token", status)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(mapHttpError(response.code(), "plants")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapNetworkError(e)))
        }
    }

    /**
     * Get featured plants from all users.
     */
    suspend fun getFeaturedPlants(token: String, limit: Int? = null): Result<List<Plant>> {
        return try {
            val response = apiService.getFeaturedPlants("Bearer $token", limit)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(mapHttpError(response.code(), "plants")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapNetworkError(e)))
        }
    }

    /**
     * Get a specific plant by ID.
     */
    suspend fun getPlantById(token: String, id: String): Result<Plant> {
        return try {
            val response = apiService.getPlantById("Bearer $token", id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(mapHttpError(response.code(), "plant")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapNetworkError(e)))
        }
    }

    /**
     * Create a new plant.
     */
    suspend fun createPlant(token: String, request: CreatePlantRequest): Result<Plant> {
        return try {
            val response = apiService.createPlant("Bearer $token", request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(mapHttpError(response.code(), "plant")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapNetworkError(e)))
        }
    }

    /**
     * Update an existing plant.
     */
    suspend fun updatePlant(token: String, id: String, request: UpdatePlantRequest): Result<Plant> {
        return try {
            val response = apiService.updatePlant("Bearer $token", id, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(mapHttpError(response.code(), "plant")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapNetworkError(e)))
        }
    }

    /**
     * Delete a plant.
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
}
