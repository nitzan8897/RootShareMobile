package com.example.rootsharemobile.data.remote

import com.example.rootsharemobile.data.model.AuthResponse
import com.example.rootsharemobile.data.model.CreatePlantRequest
import com.example.rootsharemobile.data.model.CreatePostRequest
import com.example.rootsharemobile.data.model.GoogleTokenRequest
import com.example.rootsharemobile.data.model.LoginRequest
import com.example.rootsharemobile.data.model.Plant
import com.example.rootsharemobile.data.model.PlantStatus
import com.example.rootsharemobile.data.model.Post
import com.example.rootsharemobile.data.model.RefreshTokenResponse
import com.example.rootsharemobile.data.model.RegisterRequest
import com.example.rootsharemobile.data.model.UpdatePlantRequest
import com.example.rootsharemobile.data.model.UpdatePostRequest
import com.example.rootsharemobile.data.model.User
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
 */
interface ApiService {

    // ==================== AUTH ====================

    /**
     * Register a new user.
     */
    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthResponse>

    /**
     * Login with email and password.
     */
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    /**
     * Refresh access token using refresh token.
     */
    @POST("auth/refresh")
    suspend fun refreshToken(
        @Header("Authorization") refreshToken: String
    ): Response<RefreshTokenResponse>

    /**
     * Logout and invalidate refresh token.
     */
    @POST("auth/logout")
    suspend fun logout(
        @Header("Authorization") token: String
    ): Response<Unit>

    /**
     * Get current authenticated user.
     */
    @GET("auth/me")
    suspend fun getCurrentUser(
        @Header("Authorization") token: String
    ): Response<User>

    /**
     * Authenticate with Google ID token (for mobile).
     */
    @POST("auth/google/token")
    suspend fun googleAuth(
        @Body request: GoogleTokenRequest
    ): Response<AuthResponse>

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
