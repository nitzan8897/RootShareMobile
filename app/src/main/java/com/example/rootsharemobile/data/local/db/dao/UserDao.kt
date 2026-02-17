package com.example.rootsharemobile.data.local.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.rootsharemobile.data.local.db.entity.UserEntity

/**
 * Data Access Object for user-related database operations.
 */
@Dao
interface UserDao {

    /**
     * Insert or replace the cached user record.
     * Only one user is ever stored at a time (the authenticated user).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    /**
     * Observe the currently cached user.
     * Returns null when no user is logged in.
     */
    @Query("SELECT * FROM users LIMIT 1")
    fun observeCurrentUser(): LiveData<UserEntity?>

    /**
     * Read the current user once (non-reactive).
     */
    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    /**
     * Remove all cached user data (called on logout).
     */
    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}
