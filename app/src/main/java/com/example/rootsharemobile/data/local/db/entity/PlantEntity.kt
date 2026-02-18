package com.example.rootsharemobile.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a cached plant in the local database.
 * The [isFeatured] flag distinguishes plants shown in the home feed
 * from the user's full garden list.
 */
@Entity(tableName = "plants")
data class PlantEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    val species: String,
    val status: String,
    val imageUrl: String,
    val isFeatured: Boolean,
    val createdAt: String,
    val updatedAt: String
) {
    /** Truncated display name for compact UI components. */
    val displayTitle: String
        get() = if (name.length > 20) name.take(20) + "..." else name

    /** Human-readable category label. */
    val displayCategory: String
        get() = "$species Plant"

    /** Status badge text shown on plant cards. */
    val badge: String
        get() = when (status.uppercase()) {
            "ACTIVE"  -> "Active"
            "DEAD"    -> "Deceased"
            "GIFTED"  -> "Gifted"
            else      -> status
        }
}
