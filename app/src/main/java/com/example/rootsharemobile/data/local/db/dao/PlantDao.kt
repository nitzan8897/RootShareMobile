package com.example.rootsharemobile.data.local.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.rootsharemobile.data.local.db.entity.PlantEntity

/**
 * Data Access Object for plant-related database operations.
 */
@Dao
interface PlantDao {

    /**
     * Insert or replace a list of plants.
     * Called after every successful API fetch to keep the cache fresh.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlants(plants: List<PlantEntity>)

    /**
     * Observe all featured plants (shown on the home screen).
     * The UI automatically updates when this data changes in Room.
     */
    @Query("SELECT * FROM plants WHERE isFeatured = 1 ORDER BY createdAt DESC")
    fun observeFeaturedPlants(): LiveData<List<PlantEntity>>

    /**
     * Observe every plant belonging to the current user (full garden list).
     */
    @Query("SELECT * FROM plants ORDER BY createdAt DESC")
    fun observeAllPlants(): LiveData<List<PlantEntity>>

    /**
     * Clear all plants (e.g., on logout or full refresh).
     */
    @Query("DELETE FROM plants")
    suspend fun deleteAllPlants()

    /**
     * Clear only the featured-plant cache before re-fetching.
     */
    @Query("UPDATE plants SET isFeatured = 0")
    suspend fun clearFeaturedFlag()
}
