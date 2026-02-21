package com.example.rootsharemobile.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.rootsharemobile.data.local.db.entity.ChatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Query("SELECT * FROM chats ORDER BY updatedAt DESC")
    fun observeAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :chatId LIMIT 1")
    suspend fun getChatById(chatId: String): ChatEntity?

    @Query("SELECT * FROM chats WHERE id = :chatId LIMIT 1")
    fun observeChatById(chatId: String): Flow<ChatEntity?>

    @Query("UPDATE chats SET lastMessage = :lastMessage, lastMessageTime = :timestamp, updatedAt = :updatedAt WHERE id = :chatId")
    suspend fun updateLastMessage(chatId: String, lastMessage: String, timestamp: String, updatedAt: String)

    @Query("UPDATE chats SET unreadCount = :unreadCount WHERE id = :chatId")
    suspend fun updateUnreadCount(chatId: String, unreadCount: Int)

    @Query("UPDATE chats SET unreadCount = 0 WHERE id = :chatId")
    suspend fun markAsRead(chatId: String)

    @Query("UPDATE chats SET name = :name WHERE id = :chatId")
    suspend fun updateChatName(chatId: String, name: String)

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChatById(chatId: String)

    @Query("DELETE FROM chats")
    suspend fun deleteAllChats()

    @Query("SELECT SUM(unreadCount) FROM chats")
    fun observeTotalUnreadCount(): Flow<Int?>
}
