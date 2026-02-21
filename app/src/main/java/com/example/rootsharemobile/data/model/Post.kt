package com.example.rootsharemobile.data.model

import com.google.gson.annotations.SerializedName

data class PostAuthor(
    @SerializedName("_id")
    val id: String = "",

    @SerializedName("username")
    val username: String = "",

    @SerializedName("profileImageUrl")
    val profileImageUrl: String? = null
)


 //Data class representing a Post from the backend API.
 //Matches the backend schema at /api/posts
 //The backend populates `userId` as a full user object via Mongoose `.populate()`,
 //so Gson maps it to [PostAuthor]. Use the computed [userId] property when you
 // need just the ID string (e.g. for Room queries).

data class Post(
    @SerializedName("_id")
    val id: String = "",

    @SerializedName("userId")
    val author: PostAuthor? = null,

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
    val userId: String get() = author?.id ?: ""

    val tags: List<String>
        get() = Regex("#\\w+").findAll(content).map { it.value }.toList()

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

data class CreatePostRequest(
    val plantId: String? = null,
    val type: PostType,
    val content: String,
    val images: List<String>? = null
)

data class UpdatePostRequest(
    val plantId: String? = null,
    val type: PostType? = null,
    val content: String? = null,
    val images: List<String>? = null
)
