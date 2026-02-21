package com.example.rootsharemobile.data.local.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.rootsharemobile.data.local.db.entity.UserEntity

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users LIMIT 1")
    fun observeCurrentUser(): LiveData<UserEntity?>

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Query("UPDATE users SET localProfileImageUrl = NULL")
    suspend fun clearLocalProfileImage()

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}
