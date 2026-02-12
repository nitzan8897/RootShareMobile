package com.example.rootsharemobile.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data class representing a Post from the backend API.
 * Matches the backend schema at /api/posts
 */
data class Post(
    @SerializedName("_id")
    val id: String = "",

    @SerializedName("userId")
    val userId: String = "",

    @SerializedName("plantId")
    val plant: Plant? = null,

    @SerializedName("type")
    val type: PostType = PostType.UPDATE,

    @SerializedName("content")
    val content: String = "",

    @SerializedName("images")
    val images: List<String> = emptyList(),

    @SerializedName("likesCount")
    val likesCount: Int = 0,

    @SerializedName("commentsCount")
    val commentsCount: Int = 0,

    @SerializedName("createdAt")
    val createdAt: String = "",

    @SerializedName("updatedAt")
    val updatedAt: String = ""
) {
    // UI helper: extract hashtags from content
    val tags: List<String>
        get() = Regex("#\\w+").findAll(content).map { it.value }.toList()

    // UI helper: type badge text
    val typeBadge: String
        get() = when (type) {
            PostType.UPDATE -> "Update"
            PostType.SWAP -> "Swap"
            PostType.GIVEAWAY -> "Giveaway"
        }
}

enum class PostType {
    @SerializedName("update")
    UPDATE,

    @SerializedName("swap")
    SWAP,

    @SerializedName("giveaway")
    GIVEAWAY
}

/**
 * DTO for creating a new post
 */
data class CreatePostRequest(
    val plantId: String? = null,
    val type: PostType,
    val content: String,
    val images: List<String>? = null
)

/**
 * DTO for updating an existing post
 */
data class UpdatePostRequest(
    val plantId: String? = null,
    val type: PostType? = null,
    val content: String? = null,
    val images: List<String>? = null
)
