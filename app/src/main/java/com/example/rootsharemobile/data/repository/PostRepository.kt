package com.example.rootsharemobile.data.repository

import com.example.rootsharemobile.data.model.CreatePostRequest
import com.example.rootsharemobile.data.model.Post
import com.example.rootsharemobile.data.model.UpdatePostRequest
import com.example.rootsharemobile.data.remote.RetrofitClient

/**
 * Repository for Post data operations.
 * Acts as a single source of truth for post data.
 */
class PostRepository {

    private val apiService = RetrofitClient.apiService

    /**
     * Get all posts (community feed).
     */
    suspend fun getPosts(token: String): Result<List<Post>> {
        return try {
            val response = apiService.getPosts("Bearer $token")
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to fetch posts: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get a specific post by ID.
     */
    suspend fun getPostById(token: String, id: String): Result<Post> {
        return try {
            val response = apiService.getPostById("Bearer $token", id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch post: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create a new post.
     */
    suspend fun createPost(token: String, request: CreatePostRequest): Result<Post> {
        return try {
            val response = apiService.createPost("Bearer $token", request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to create post: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update an existing post.
     */
    suspend fun updatePost(token: String, id: String, request: UpdatePostRequest): Result<Post> {
        return try {
            val response = apiService.updatePost("Bearer $token", id, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to update post: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a post.
     */
    suspend fun deletePost(token: String, id: String): Result<Boolean> {
        return try {
            val response = apiService.deletePost("Bearer $token", id)
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to delete post: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
