package com.example.rootsharemobile.data.local.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.rootsharemobile.data.local.db.entity.PostEntity

/**
 * Data Access Object for community post database operations.
 */
@Dao
interface PostDao {

    /**
     * Insert or replace a list of posts.
     * Called after every successful API fetch to sync the cache.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    /**
     * Observe the community feed, sorted newest-first.
     * The Fragment observes this LiveData and re-renders on every change.
     */
    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    fun observeAllPosts(): LiveData<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeUserPosts(userId: String): LiveData<List<PostEntity>>

    /** Observe total post count for the dashboard. */
    @Query("SELECT COUNT(*) FROM posts")
    fun observePostCount(): LiveData<Int>

    @Query("SELECT * FROM posts WHERE id = :postId LIMIT 1")
    suspend fun getPostById(postId: String): PostEntity?

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePostById(postId: String)

    /**
     * Remove all posts (e.g., on logout or full refresh).
     */
    @Query("DELETE FROM posts")
    suspend fun deleteAllPosts()
}
