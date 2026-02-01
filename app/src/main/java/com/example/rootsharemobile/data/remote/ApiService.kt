package com.example.rootsharemobile.data.remote

import com.example.rootsharemobile.data.model.CreatePlantRequest
import com.example.rootsharemobile.data.model.CreatePostRequest
import com.example.rootsharemobile.data.model.Plant
import com.example.rootsharemobile.data.model.PlantStatus
import com.example.rootsharemobile.data.model.Post
import com.example.rootsharemobile.data.model.UpdatePlantRequest
import com.example.rootsharemobile.data.model.UpdatePostRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API service interface for RootShare backend.
 * Plants and Posts endpoints.
 */
interface ApiService {

    // ==================== PLANTS ====================

    /**
     * Get all plants for the current authenticated user.
     * @param status Optional filter by plant status
     */
    @GET("plants")
    suspend fun getPlants(
        @Header("Authorization") token: String,
        @Query("status") status: PlantStatus? = null
    ): Response<List<Plant>>

    /**
     * Get featured plants from all users.
     * @param limit Maximum number of plants to return (default: 10)
     */
    @GET("plants/featured")
    suspend fun getFeaturedPlants(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int? = null
    ): Response<List<Plant>>

    /**
     * Get a specific plant by ID.
     */
    @GET("plants/{id}")
    suspend fun getPlantById(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<Plant>

    /**
     * Create a new plant.
     */
    @POST("plants")
    suspend fun createPlant(
        @Header("Authorization") token: String,
        @Body request: CreatePlantRequest
    ): Response<Plant>

    /**
     * Update an existing plant.
     */
    @PATCH("plants/{id}")
    suspend fun updatePlant(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body request: UpdatePlantRequest
    ): Response<Plant>

    /**
     * Delete a plant.
     */
    @DELETE("plants/{id}")
    suspend fun deletePlant(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<DeleteResponse>

    // ==================== POSTS ====================

    /**
     * Get all posts (community feed).
     * Returns posts sorted by createdAt descending (newest first).
     */
    @GET("posts")
    suspend fun getPosts(
        @Header("Authorization") token: String
    ): Response<List<Post>>

    /**
     * Get a specific post by ID.
     */
    @GET("posts/{id}")
    suspend fun getPostById(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<Post>

    /**
     * Create a new post.
     */
    @POST("posts")
    suspend fun createPost(
        @Header("Authorization") token: String,
        @Body request: CreatePostRequest
    ): Response<Post>

    /**
     * Update an existing post.
     */
    @PATCH("posts/{id}")
    suspend fun updatePost(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body request: UpdatePostRequest
    ): Response<Post>

    /**
     * Delete a post.
     */
    @DELETE("posts/{id}")
    suspend fun deletePost(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<DeleteResponse>
}

/**
 * Response for delete operations
 */
data class DeleteResponse(
    val deleted: Boolean,
    val id: String
)
