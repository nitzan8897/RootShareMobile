package com.example.rootsharemobile.data.repository

import androidx.lifecycle.LiveData
import com.example.rootsharemobile.data.local.db.dao.PostDao
import com.example.rootsharemobile.data.local.db.entity.PostEntity
import com.example.rootsharemobile.data.model.CreatePostRequest
import com.example.rootsharemobile.data.model.Post
import com.example.rootsharemobile.data.model.UpdatePostRequest
import com.example.rootsharemobile.data.remote.RetrofitClient
import com.example.rootsharemobile.data.remote.mapHttpError
import com.example.rootsharemobile.data.remote.mapNetworkError

/**
 * Repository for community post data.
 *
 * Enforces the offline-first pattern:
 *  1. Fetch from the remote API.
 *  2. Save to Room (Single Source of Truth).
 *  3. The UI observes Room LiveData exclusively.
 */
class PostRepository(private val postDao: PostDao) {

    private val apiService = RetrofitClient.apiService

    // -------------------------------------------------------------------------
    // Room LiveData — observed by ViewModels.
    // -------------------------------------------------------------------------

    /** Emits the full post feed whenever the Room cache changes. */
    fun observeAllPosts(): LiveData<List<PostEntity>> =
        postDao.observeAllPosts()

    // -------------------------------------------------------------------------
    // Network + cache operations.
    // -------------------------------------------------------------------------

    /**
     * Fetch all community posts from the API, persist them to Room,
     * and return a success/failure result for error handling in the ViewModel.
     */
    suspend fun fetchAndStorePosts(token: String): Result<Unit> {
        return try {
            val response = apiService.getPosts("Bearer $token")
            if (response.isSuccessful) {
                val posts = response.body() ?: emptyList()
                postDao.deleteAllPosts()
                val entities = posts.map { it.toEntity() }
                postDao.insertPosts(entities)
                Result.success(Unit)
            } else {
                Result.failure(Exception(mapHttpError(response.code(), "posts")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapNetworkError(e)))
        }
    }

    /**
     * Create a new post via the API and insert the result into Room.
     */
    suspend fun createPost(token: String, request: CreatePostRequest): Result<Post> {
        return try {
            val response = apiService.createPost("Bearer $token", request)
            if (response.isSuccessful && response.body() != null) {
                val post = response.body()!!
                postDao.insertPosts(listOf(post.toEntity()))
                Result.success(post)
            } else {
                Result.failure(Exception(mapHttpError(response.code(), "post")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapNetworkError(e)))
        }
    }

    /** Clear the local post cache (called on logout). */
    suspend fun clearLocalCache() = postDao.deleteAllPosts()

    // -------------------------------------------------------------------------
    // Mapper helpers
    // -------------------------------------------------------------------------

    private fun Post.toEntity() = PostEntity(
        id = this.id,
        userId = this.userId,
        plantId = this.plant?.id,
        plantName = this.plant?.name,
        plantSpecies = this.plant?.species,
        postType = this.type.name,
        content = this.content,
        imagesJson = this.images.joinToString(","),
        likesCount = this.likesCount,
        commentsCount = this.commentsCount,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}
