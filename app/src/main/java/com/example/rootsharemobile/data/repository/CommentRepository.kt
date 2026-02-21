package com.example.rootsharemobile.data.repository

import com.example.rootsharemobile.data.model.Comment
import com.example.rootsharemobile.data.model.CreateCommentRequest
import com.example.rootsharemobile.data.remote.RetrofitClient
import com.example.rootsharemobile.data.remote.mapHttpError
import com.example.rootsharemobile.data.remote.mapNetworkError

class CommentRepository {

    private val apiService = RetrofitClient.apiService

    /** Fetch all comments for a post. */
    suspend fun getComments(token: String, postId: String): Result<List<Comment>> {
        return try {
            val response = apiService.getComments("Bearer $token", postId)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(mapHttpError(response.code(), "comments")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapNetworkError(e)))
        }
    }

    /**
     * Post a new comment on a post.
     *
     * Returns Result<Unit> — we only care whether the request succeeded.
     * We deliberately do NOT call response.body() here because the backend's
     * POST /comments response returns userId as a plain string (not populated),
     * which would cause a JsonSyntaxException when Gson tries to deserialize it
     * as a CommentAuthor object. The caller should reload via getComments() after
     * a successful post to get the authoritative, fully-populated list.
     */
    suspend fun createComment(token: String, postId: String, content: String): Result<Unit> {
        return try {
            val response = apiService.createComment(
                "Bearer $token",
                CreateCommentRequest(postId, content)
            )
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(mapHttpError(response.code(), "comment")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapNetworkError(e)))
        }
    }
}
