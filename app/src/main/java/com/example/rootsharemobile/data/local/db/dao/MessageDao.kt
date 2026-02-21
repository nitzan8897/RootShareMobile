package com.example.rootsharemobile.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.rootsharemobile.data.local.db.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for chat message database operations.
 */
@Dao
interface MessageDao {

    /**
     * Insert or replace a list of messages.
     * Called after fetching messages from the API.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    /**
     * Insert or replace a single message.
     * Used for incoming socket messages or sent messages.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    /**
     * Observe messages for a specific chat, sorted by creation time (oldest first).
     * The Fragment observes this Flow and re-renders on every change.
     */
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAt ASC")
    fun observeMessagesByChatId(chatId: String): Flow<List<MessageEntity>>

    /**
     * Get all messages for a specific chat (non-reactive).
     */
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAt ASC")
    suspend fun getMessagesByChatId(chatId: String): List<MessageEntity>

    /**
     * Get the most recent message for a chat.
     */
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestMessage(chatId: String): MessageEntity?

    /**
     * Get a message by ID.
     */
    @Query("SELECT * FROM messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): MessageEntity?

    /**
     * Delete all messages for a specific chat.
     */
    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesByChatId(chatId: String)

    /**
     * Delete all messages (e.g., on logout).
     */
    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()

    /**
     * Count messages for a specific chat.
     */
    @Query("SELECT COUNT(*) FROM messages WHERE chatId = :chatId")
    suspend fun countMessagesByChatId(chatId: String): Int
}
