package com.example.rootsharemobile.data.repository

import com.example.rootsharemobile.data.model.Comment
import com.example.rootsharemobile.data.model.CreateCommentRequest
import com.example.rootsharemobile.data.remote.RetrofitClient
import com.example.rootsharemobile.data.remote.mapHttpError
import com.example.rootsharemobile.data.remote.mapNetworkError

class CommentRepository {

    private val apiService = RetrofitClient.apiService

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
