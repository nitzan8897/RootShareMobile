package com.example.rootsharemobile.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a cached community post.
 * Image URLs are stored as a comma-separated string because Room
 * does not natively support List fields; a [TypeConverter] in
 * [AppDatabase] converts between String and List<String>.
 *
 * Plant data is denormalised (stored inline) to avoid a JOIN query
 * for the common feed display use-case.
 */
@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    /** Denormalised plant fields — null when no plant is attached. */
    val plantId: String?,
    val plantName: String?,
    val plantSpecies: String?,
    val postType: String,
    val content: String,
    /** Comma-separated image URLs; empty string means no images. */
    val imagesJson: String,
    val likesCount: Int,
    val commentsCount: Int,
    val createdAt: String,
    val updatedAt: String
) {
    /** Extracts hashtags from post content for display. */
    val tags: List<String>
        get() = Regex("#\\w+").findAll(content).map { it.value }.toList()

    /** Human-readable badge for post type. */
    val typeBadge: String
        get() = when (postType.uppercase()) {
            "SWAP"     -> "Swap"
            "GIVEAWAY" -> "Giveaway"
            else       -> "Update"
        }
}
