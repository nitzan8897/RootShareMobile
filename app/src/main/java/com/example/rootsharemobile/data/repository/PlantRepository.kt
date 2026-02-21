package com.example.rootsharemobile.data.repository

import androidx.lifecycle.LiveData
import com.example.rootsharemobile.data.local.db.dao.PlantDao
import com.example.rootsharemobile.data.local.db.entity.PlantEntity
import com.example.rootsharemobile.data.local.db.entity.PlantWithPostCount
import com.example.rootsharemobile.data.model.CreatePlantRequest
import com.example.rootsharemobile.data.model.Plant
import com.example.rootsharemobile.data.model.Species
import com.example.rootsharemobile.data.model.UpdatePlantRequest
import com.example.rootsharemobile.data.remote.RetrofitClient
import com.example.rootsharemobile.data.remote.mapHttpError
import com.example.rootsharemobile.data.remote.mapNetworkError
import com.example.rootsharemobile.data.remote.parseErrorBody

class PlantRepository(private val plantDao: PlantDao) {

    private val apiService = RetrofitClient.apiService

    fun observeFeaturedPlants(): LiveData<List<PlantEntity>> =
        plantDao.observeFeaturedPlants()

    fun observeGardenPlantsWithPostCount(userId: String): LiveData<List<PlantWithPostCount>> =
        plantDao.observeGardenPlantsWithPostCount(userId)

    fun observePlantWithPostCount(plantId: String): LiveData<PlantWithPostCount?> =
        plantDao.observePlantWithPostCount(plantId)

    suspend fun fetchSpecies(token: String): Result<List<Species>> {
        return try {
            val response = apiService.getSpecies("Bearer $token")
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(mapHttpError(response.code(), "species")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapNetworkError(e)))
        }
    }

    suspend fun fetchAndStoreFeaturedPlants(token: String, limit: Int? = null): Result<Unit> {
        return try {
            val response = apiService.getFeaturedPlants("Bearer $token", limit)
            if (response.isSuccessful) {
                val plants = response.body() ?: emptyList()
                plantDao.clearFeaturedFlag()
                plantDao.insertPlants(plants.map { it.toEntity(isFeatured = true) })
                Result.success(Unit)
            } else {
                Result.failure(Exception(mapHttpError(response.code(), "plants")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapNetworkError(e)))
        }
    }

    suspend fun fetchAndStorePlants(token: String): Result<Unit> {
        return try {
            val response = apiService.getPlants("Bearer $token")
            if (response.isSuccessful) {
                val plants = response.body() ?: emptyList()
                plantDao.deleteNonFeaturedPlants()
                plantDao.insertPlants(plants.map { it.toEntity(isFeatured = false) })
                Result.success(Unit)
            } else {
                Result.failure(Exception(mapHttpError(response.code(), "plants")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapNetworkError(e)))
        }
    }

    suspend fun createPlant(token: String, request: CreatePlantRequest): Result<Plant> {
        return try {
            val response = apiService.createPlant("Bearer $token", request)
            if (response.isSuccessful && response.body() != null) {
                val plant = response.body()!!
                plantDao.insertPlants(listOf(plant.toEntity(isFeatured = false)))
                Result.success(plant)
            } else {
                val msg = parseErrorBody(response.errorBody()) ?: mapHttpError(response.code(), "plant")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapNetworkError(e)))
        }
    }

    suspend fun updatePlant(token: String, id: String, request: UpdatePlantRequest): Result<Plant> {
        return try {
            val response = apiService.updatePlant("Bearer $token", id, request)
            if (response.isSuccessful && response.body() != null) {
                val plant = response.body()!!
                val existing = plantDao.getPlantById(id)
                plantDao.insertPlants(listOf(plant.toEntity(isFeatured = existing?.isFeatured ?: false)))
                Result.success(plant)
            } else {
                val msg = parseErrorBody(response.errorBody()) ?: mapHttpError(response.code(), "plant")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapNetworkError(e)))
        }
    }

    suspend fun deletePlant(token: String, id: String): Result<Boolean> {
        return try {
            val response = apiService.deletePlant("Bearer $token", id)
            if (response.isSuccessful) {
                plantDao.deletePlantById(id)
                Result.success(true)
            } else {
                Result.failure(Exception(mapHttpError(response.code(), "plant")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapNetworkError(e)))
        }
    }

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
