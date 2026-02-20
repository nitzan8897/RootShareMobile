package com.example.rootsharemobile.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded

/**
 * Room projection that combines a [PlantEntity] with the count of
 * community posts that reference it. Produced by a correlated subquery
 * in [PlantDao] — no schema change required.
 */
data class PlantWithPostCount(
    @Embedded val plant: PlantEntity,
    @ColumnInfo(name = "postCount") val postCount: Int
)
