package com.example.rootsharemobile.data.repository

import android.util.Log
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

class PostRepository(private val postDao: PostDao) {

    private val apiService = RetrofitClient.apiService

    fun observeAllPosts(): LiveData<List<PostEntity>> =
        postDao.observeAllPosts()

    fun observeUserPosts(userId: String): LiveData<List<PostEntity>> =
        postDao.observeUserPosts(userId)

    fun observePostById(postId: String): LiveData<PostEntity?> =
        postDao.observePostById(postId)

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

    suspend fun createPost(token: String, request: CreatePostRequest): Result<Post> {
        return try {
            val response = apiService.createPost("Bearer $token", request)
            if (response.isSuccessful && response.body() != null) {
                val rawPost = response.body()!!
                // Backend create() returns the raw document without populate(); re-fetch to get
                // the full userId (PostAuthor) and plantId (Plant) objects so Room stores them correctly.
                val post = refetchPost(token, rawPost.id) ?: rawPost
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
                // Backend update() also returns an unpopulated document; re-fetch for full objects.
                val post = refetchPost(token, postId) ?: response.body()!!
                postDao.insertPosts(listOf(post.toEntity()))
                Result.success(post)
            } else {
                Result.failure(Exception(mapHttpError(response.code(), "post")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapNetworkError(e)))
        }
    }

    /** Fetches a single post by ID so we get the fully-populated userId/plantId fields. */
    private suspend fun refetchPost(token: String, postId: String): Post? = try {
        val r = apiService.getPostById("Bearer $token", postId)
        if (r.isSuccessful) r.body() else null
    } catch (_: Exception) {
        null
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

    suspend fun toggleLike(token: String, post: PostEntity): Result<Unit> {
        val wasLiked = post.isLikedByMe
        val optimisticPost = post.copy(
            isLikedByMe = !wasLiked,
            likesCount = maxOf(0, if (wasLiked) post.likesCount - 1 else post.likesCount + 1)
        )
        Log.d("LIKE_DEBUG", "Repo: optimistic insert postId=${post.id} wasLiked=$wasLiked newCount=${optimisticPost.likesCount}")
        postDao.insertPosts(listOf(optimisticPost))

        return try {
            Log.d("LIKE_DEBUG", "Repo: calling togglePostLike API postId=${post.id}")
            val response = apiService.togglePostLike("Bearer $token", post.id)
            Log.d("LIKE_DEBUG", "Repo: API response code=${response.code()} success=${response.isSuccessful}")
            if (response.isSuccessful) {
                val body = response.body()
                Log.d("LIKE_DEBUG", "Repo: body=$body liked=${body?.liked}")
                if (body != null) {
                    postDao.updateIsLikedByMe(post.id, body.liked)
                }
                Result.success(Unit)
            } else {
                Log.d("LIKE_DEBUG", "Repo: FAILED code=${response.code()}, rolling back")
                postDao.insertPosts(listOf(post))
                Result.failure(Exception(mapHttpError(response.code(), "like")))
            }
        } catch (e: Exception) {
            Log.d("LIKE_DEBUG", "Repo: EXCEPTION ${e.javaClass.simpleName}: ${e.message}, rolling back")
            postDao.insertPosts(listOf(post))
            Result.failure(Exception(mapNetworkError(e)))
        }
    }

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
