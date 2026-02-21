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

    /** Observe current user's post count for the profile badge. */
    @Query("SELECT COUNT(*) FROM posts WHERE userId = :userId")
    fun observeUserPostCount(userId: String): LiveData<Int>

    @Query("SELECT * FROM posts WHERE id = :postId LIMIT 1")
    suspend fun getPostById(postId: String): PostEntity?

    /** Returns the IDs of all posts the current user has liked locally. */
    @Query("SELECT id FROM posts WHERE isLikedByMe = 1")
    suspend fun getLikedPostIds(): List<String>

    /** Returns all posts that have at least one like (used for server sync after re-login). */
    @Query("SELECT * FROM posts WHERE likesCount > 0")
    suspend fun getPostsWithPositiveLikeCount(): List<PostEntity>

    /**
     * Update only the isLikedByMe flag for a post.
     * Used by syncLikeStatuses so it never touches likesCount,
     * eliminating the race condition with optimistic like updates.
     */
    @Query("UPDATE posts SET isLikedByMe = :isLiked WHERE id = :postId")
    suspend fun updateIsLikedByMe(postId: String, isLiked: Boolean)

    /** Increment the cached comment count after a new comment is successfully posted. */
    @Query("UPDATE posts SET commentsCount = commentsCount + 1 WHERE id = :postId")
    suspend fun incrementCommentsCount(postId: String)

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePostById(postId: String)

    /**
     * Remove all posts (e.g., on logout or full refresh).
     */
    @Query("DELETE FROM posts")
    suspend fun deleteAllPosts()
}
