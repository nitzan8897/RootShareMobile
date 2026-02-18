package com.example.rootsharemobile.data.local.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.rootsharemobile.data.local.db.entity.PlantEntity
import com.example.rootsharemobile.data.local.db.entity.PlantWithPostCount

/**
 * Data Access Object for plant-related database operations.
 */
@Dao
interface PlantDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlants(plants: List<PlantEntity>)

    @Query("SELECT * FROM plants WHERE isFeatured = 1 ORDER BY createdAt DESC")
    fun observeFeaturedPlants(): LiveData<List<PlantEntity>>

    @Query("SELECT * FROM plants ORDER BY createdAt DESC")
    fun observeAllPlants(): LiveData<List<PlantEntity>>

    /**
     * Garden list with live post-count per plant, filtered to a specific user.
     */
    @Query("""
        SELECT p.*, (SELECT COUNT(*) FROM posts WHERE plantId = p.id) AS postCount
        FROM plants p
        WHERE p.userId = :userId
        ORDER BY p.createdAt DESC
    """)
    fun observeGardenPlantsWithPostCount(userId: String): LiveData<List<PlantWithPostCount>>

    /**
     * Single plant observation for the details screen.
     */
    @Query("""
        SELECT p.*, (SELECT COUNT(*) FROM posts WHERE plantId = p.id) AS postCount
        FROM plants p
        WHERE p.id = :plantId
        LIMIT 1
    """)
    fun observePlantWithPostCount(plantId: String): LiveData<PlantWithPostCount?>

    /** Observe total plant count for the dashboard, scoped to a user. */
    @Query("SELECT COUNT(*) FROM plants WHERE userId = :userId")
    fun observePlantCount(userId: String): LiveData<Int>

    @Query("SELECT * FROM plants WHERE id = :id LIMIT 1")
    suspend fun getPlantById(id: String): PlantEntity?

    @Query("DELETE FROM plants WHERE id = :id")
    suspend fun deletePlantById(id: String)

    @Query("DELETE FROM plants")
    suspend fun deleteAllPlants()

    /** Remove all non-featured plants so stale rows from other users are evicted. */
    @Query("DELETE FROM plants WHERE isFeatured = 0")
    suspend fun deleteNonFeaturedPlants()

    @Query("UPDATE plants SET isFeatured = 0")
    suspend fun clearFeaturedFlag()
}
