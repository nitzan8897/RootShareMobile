package com.example.rootsharemobile.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.rootsharemobile.data.local.db.entity.ChatEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for chat/conversation database operations.
 */
@Dao
interface ChatDao {

    /**
     * Insert or replace a list of chats.
     * Called after every successful API fetch to sync the cache.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)

    /**
     * Insert or replace a single chat.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    /**
     * Observe all chats, sorted by last message time (newest first).
     */
    @Query("SELECT * FROM chats ORDER BY updatedAt DESC")
    fun observeAllChats(): Flow<List<ChatEntity>>

    /**
     * Get a single chat by ID.
     */
    @Query("SELECT * FROM chats WHERE id = :chatId LIMIT 1")
    suspend fun getChatById(chatId: String): ChatEntity?

    /**
     * Observe a single chat by ID.
     */
    @Query("SELECT * FROM chats WHERE id = :chatId LIMIT 1")
    fun observeChatById(chatId: String): Flow<ChatEntity?>

    /**
     * Update the last message and timestamp for a chat.
     */
    @Query("UPDATE chats SET lastMessage = :lastMessage, lastMessageTime = :timestamp, updatedAt = :updatedAt WHERE id = :chatId")
    suspend fun updateLastMessage(chatId: String, lastMessage: String, timestamp: String, updatedAt: String)

    /**
     * Update the unread count for a chat.
     */
    @Query("UPDATE chats SET unreadCount = :unreadCount WHERE id = :chatId")
    suspend fun updateUnreadCount(chatId: String, unreadCount: Int)

    /**
     * Mark a chat as read (set unread count to 0).
     */
    @Query("UPDATE chats SET unreadCount = 0 WHERE id = :chatId")
    suspend fun markAsRead(chatId: String)

    /**
     * Update the chat name (for group rename).
     */
    @Query("UPDATE chats SET name = :name WHERE id = :chatId")
    suspend fun updateChatName(chatId: String, name: String)

    /**
     * Delete a chat by ID.
     */
    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChatById(chatId: String)

    /**
     * Delete all chats (e.g., on logout).
     */
    @Query("DELETE FROM chats")
    suspend fun deleteAllChats()

    /**
     * Observe the total unread message count across all chats.
     */
    @Query("SELECT SUM(unreadCount) FROM chats")
    fun observeTotalUnreadCount(): Flow<Int?>
}
