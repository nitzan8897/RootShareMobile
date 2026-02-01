package com.example.rootsharemobile.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data class representing a Plant from the backend API.
 * Matches the backend schema at /api/plants
 */
data class Plant(
    @SerializedName("_id")
    val id: String = "",

    @SerializedName("userId")
    val userId: String = "",

    @SerializedName("name")
    val name: String = "",

    @SerializedName("species")
    val species: String = "",

    @SerializedName("status")
    val status: PlantStatus = PlantStatus.ACTIVE,

    @SerializedName("imageUrl")
    val imageUrl: String = "",

    @SerializedName("createdAt")
    val createdAt: String = "",

    @SerializedName("updatedAt")
    val updatedAt: String = ""
) {
    // UI helper properties for display
    val displayTitle: String
        get() = name.take(20).let { if (name.length > 20) "$it..." else it }

    val displayCategory: String
        get() = "$species Plant"

    val badge: String
        get() = when (status) {
            PlantStatus.ACTIVE -> "Active"
            PlantStatus.DEAD -> "Dead"
            PlantStatus.GIFTED -> "Gifted"
        }
}

enum class PlantStatus {
    @SerializedName("active")
    ACTIVE,

    @SerializedName("dead")
    DEAD,

    @SerializedName("gifted")
    GIFTED
}

/**
 * DTO for creating a new plant
 */
data class CreatePlantRequest(
    val name: String,
    val species: String,
    val imageUrl: String
)

/**
 * DTO for updating an existing plant
 */
data class UpdatePlantRequest(
    val name: String? = null,
    val species: String? = null,
    val imageUrl: String? = null,
    val status: PlantStatus? = null
)
