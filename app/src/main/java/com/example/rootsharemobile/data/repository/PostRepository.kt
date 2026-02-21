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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

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

    fun observeAllPosts(): LiveData<List<PostEntity>> =
        postDao.observeAllPosts()

    fun observeUserPosts(userId: String): LiveData<List<PostEntity>> =
        postDao.observeUserPosts(userId)

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
                // Preserve any locally-stored like state before wiping the cache,
                // so the UI remains consistent during a feed refresh.
                val likedIds = postDao.getLikedPostIds().toSet()
                postDao.deleteAllPosts()
                val entities = posts.map { it.toEntity(isLikedByMe = it.id in likedIds) }
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

    suspend fun updatePost(token: String, postId: String, request: UpdatePostRequest): Result<Post> {
        return try {
            val response = apiService.updatePost("Bearer $token", postId, request)
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

    suspend fun deletePost(token: String, postId: String): Result<Unit> {
        return try {
            val response = apiService.deletePost("Bearer $token", postId)
            if (response.isSuccessful) {
                postDao.deletePostById(postId)
                Result.success(Unit)
            } else {
                Result.failure(Exception(mapHttpError(response.code(), "post")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapNetworkError(e)))
        }
    }

    /**
     * Toggle the like on a post.
     * Removed the optimistic update to prevent the UI from "jumping" when the
     * server returns an error (like the current 500 Internal Server Error).
     * The UI will now only update once the server confirms the operation.
     */
    suspend fun toggleLike(token: String, post: PostEntity): Result<Unit> {
        return try {
            val response = apiService.togglePostLike("Bearer $token", post.id)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    // Update Room only after a successful server response.
                    postDao.insertPosts(listOf(post.copy(
                        isLikedByMe = body.liked,
                        likesCount = body.count
                    )))
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception(mapHttpError(response.code(), "like")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapNetworkError(e)))
        }
    }

    /**
     * Re-sync isLikedByMe for posts that have at least one like.
     * Called after a fresh fetch so that like state is correct after re-login or
     * on a new device. Runs all checks concurrently. Silently ignores failures.
     */
    suspend fun syncLikeStatuses(token: String) {
        val postsToCheck = postDao.getPostsWithPositiveLikeCount()
        if (postsToCheck.isEmpty()) return
        coroutineScope {
            postsToCheck.map { post ->
                async {
                    try {
                        val response = apiService.isPostLiked("Bearer $token", post.id)
                        if (response.isSuccessful) {
                            val liked = response.body()?.liked ?: false
                            if (liked != post.isLikedByMe) {
                                postDao.updateIsLikedByMe(post.id, liked)
                            }
                        }
                    } catch (_: Exception) {
                        // Best-effort sync — don't disrupt the UI on partial failure
                    }
                }
            }.awaitAll()
        }
    }

    /** Clear the local post cache (called on logout). */
    suspend fun clearLocalCache() = postDao.deleteAllPosts()

    // -------------------------------------------------------------------------
    // Mapper helpers
    // -------------------------------------------------------------------------

    private fun Post.toEntity(isLikedByMe: Boolean = false) = PostEntity(
        id = this.id,
        userId = this.userId,
        authorUsername = this.author?.username,
        authorImageUrl = this.author?.profileImageUrl,
        plantId = this.plant?.id,
        plantName = this.plant?.name,
        plantSpecies = this.plant?.species,
        postType = this.type.name,
        content = this.content,
        imagesJson = this.images.joinToString(","),
        likesCount = this.likesCount,
        commentsCount = this.commentsCount,
        isLikedByMe = isLikedByMe,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}
