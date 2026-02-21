package com.example.rootsharemobile.data.model

import com.google.gson.annotations.SerializedName

/**
 * Populated author returned by the backend when fetching comments.
 * The backend populates the userId reference into a full user object.
 */
data class CommentAuthor(
    @SerializedName("_id")
    val id: String = "",

    @SerializedName("username")
    val username: String = "",

    @SerializedName("profileImageUrl")
    val profileImageUrl: String? = null
)

/**
 * A single comment on a community post.
 * Matches the backend schema at /api/comments
 */
data class Comment(
    @SerializedName("_id")
    val id: String = "",

    @SerializedName("postId")
    val postId: String = "",

    /** Populated author — backend returns userId as { _id, username, profileImageUrl }. */
    @SerializedName("userId")
    val author: CommentAuthor? = null,

    @SerializedName("content")
    val content: String = "",

    @SerializedName("createdAt")
    val createdAt: String = "",

    @SerializedName("updatedAt")
    val updatedAt: String = ""
)

/** DTO for creating a new comment. */
data class CreateCommentRequest(
    val postId: String,
    val content: String
)
