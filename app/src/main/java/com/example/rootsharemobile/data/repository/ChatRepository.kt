package com.example.rootsharemobile.data.repository

import android.util.Log
import com.example.rootsharemobile.data.local.db.dao.ChatDao
import com.example.rootsharemobile.data.local.db.dao.MessageDao
import com.example.rootsharemobile.data.local.db.entity.ChatEntity
import com.example.rootsharemobile.data.local.db.entity.MessageEntity
import com.example.rootsharemobile.data.model.ChatMessageResponse
import com.example.rootsharemobile.data.model.ChatResponse
import com.example.rootsharemobile.data.model.CreateChatRequest
import com.example.rootsharemobile.data.model.CreateGroupChatRequest
import com.example.rootsharemobile.data.remote.RetrofitClient
import com.example.rootsharemobile.data.remote.mapHttpError
import com.example.rootsharemobile.data.remote.mapNetworkError
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository for chat and message data.
 *
 * Enforces the offline-first pattern:
 *  1. Fetch from the remote API.
 *  2. Save to Room (Single Source of Truth).
 *  3. The UI observes Room Flow exclusively.
 */
class ChatRepository(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao
) {
    private val apiService = RetrofitClient.apiService

    companion object {
        private const val TAG = "ChatRepository"
    }

    // Stores the current user ID for mapping operations
    private var currentUserId: String? = null

    fun setCurrentUserId(userId: String?) {
        currentUserId = userId
    }

    // -------------------------------------------------------------------------
    // Room Flow — observed by ViewModels.
    // -------------------------------------------------------------------------

    fun observeAllChats(): Flow<List<ChatEntity>> =
        chatDao.observeAllChats()

    fun observeChatById(chatId: String): Flow<ChatEntity?> =
        chatDao.observeChatById(chatId)

    fun observeMessagesByChatId(chatId: String): Flow<List<MessageEntity>> =
        messageDao.observeMessagesByChatId(chatId)

    fun observeTotalUnreadCount(): Flow<Int?> =
        chatDao.observeTotalUnreadCount()

    // -------------------------------------------------------------------------
    // Network + cache operations for Chats.
    // -------------------------------------------------------------------------

    /**
     * Fetch all chats from the API, persist them to Room,
     * and return a success/failure result for error handling in the ViewModel.
     */
    suspend fun fetchAndStoreChats(token: String): Result<List<ChatEntity>> {
        return try {
            val response = apiService.getChats("Bearer $token")
            if (response.isSuccessful) {
                val chats = response.body() ?: emptyList()
                val entities = chats.map { it.toEntity() }
                chatDao.insertChats(entities)
                Result.success(entities)
            } else {
                Log.e(TAG, "Failed to fetch chats: ${response.code()}")
                Result.failure(Exception(mapHttpError(response.code(), "chats")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch chats", e)
            Result.failure(Exception(mapNetworkError(e)))
        }
    }

    /**
     * Create or get an existing 1-on-1 chat with a user.
     */
    suspend fun createOrGetChat(token: String, userId: String): Result<ChatEntity> {
        return try {
            val response = apiService.createOrGetChat("Bearer $token", CreateChatRequest(userId))
            if (response.isSuccessful && response.body() != null) {
                val chat = response.body()!!
                val entity = chat.toEntity()
                chatDao.insertChat(entity)
                Result.success(entity)
            } else {
                Result.failure(Exception(mapHttpError(response.code(), "chat")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapNetworkError(e)))
        }
    }

    /**
     * Create a new group chat.
     */
    suspend fun createGroupChat(token: String, name: String, userIds: List<String>): Result<ChatEntity> {
        return try {
            val response = apiService.createGroupChat(
                "Bearer $token",
                CreateGroupChatRequest(name, userIds)
            )
            if (response.isSuccessful && response.body() != null) {
                val chat = response.body()!!
                val entity = chat.toEntity()
                chatDao.insertChat(entity)
                Result.success(entity)
            } else {
                Result.failure(Exception(mapHttpError(response.code(), "group chat")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapNetworkError(e)))
        }
    }

    /**
     * Leave a group chat.
     */
    suspend fun leaveGroupChat(token: String, chatId: String): Result<Unit> {
        return try {
            val response = apiService.leaveGroupChat("Bearer $token", chatId)
            if (response.isSuccessful) {
                chatDao.deleteChatById(chatId)
                Result.success(Unit)
            } else {
                Result.failure(Exception(mapHttpError(response.code(), "chat")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapNetworkError(e)))
        }
    }

    /**
     * Mark a chat as read on the server and in the local cache.
     */
    suspend fun markChatAsRead(token: String, chatId: String): Result<Unit> {
        return try {
            val response = apiService.markChatAsRead("Bearer $token", chatId)
            if (response.isSuccessful) {
                chatDao.markAsRead(chatId)
                Result.success(Unit)
            } else {
                // Still mark locally even if server fails
                chatDao.markAsRead(chatId)
                Result.failure(Exception(mapHttpError(response.code(), "chat")))
            }
        } catch (e: Exception) {
            // Mark locally even if network fails (offline-first)
            chatDao.markAsRead(chatId)
            Result.failure(Exception(mapNetworkError(e)))
        }
    }

    // -------------------------------------------------------------------------
    // Network + cache operations for Messages.
    // -------------------------------------------------------------------------

    /**
     * Fetch messages for a chat from the API and persist to Room.
     */
    suspend fun fetchAndStoreMessages(token: String, chatId: String): Result<List<MessageEntity>> {
        return try {
            val response = apiService.getChatMessages("Bearer $token", chatId)
            if (response.isSuccessful) {
                val messages = response.body() ?: emptyList()
                // API returns newest first, we reverse for chronological order
                val entities = messages.map { it.toEntity() }.reversed()
                messageDao.insertMessages(entities)
                Result.success(entities)
            } else {
                Log.e(TAG, "Failed to fetch messages: ${response.code()}")
                Result.failure(Exception(mapHttpError(response.code(), "messages")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch messages", e)
            Result.failure(Exception(mapNetworkError(e)))
        }
    }

    /**
     * Get cached messages for a chat (non-reactive).
     */
    suspend fun getCachedMessages(chatId: String): List<MessageEntity> =
        messageDao.getMessagesByChatId(chatId)

    /**
     * Get a chat by ID from the cache.
     */
    suspend fun getChatById(chatId: String): ChatEntity? =
        chatDao.getChatById(chatId)

    // -------------------------------------------------------------------------
    // Local cache updates (for socket events).
    // -------------------------------------------------------------------------

    /**
     * Insert a new message into the local cache.
     * Used for incoming socket messages.
     */
    suspend fun insertMessage(message: MessageEntity) {
        messageDao.insertMessage(message)
    }

    /**
     * Update the last message preview for a chat.
     */
    suspend fun updateChatLastMessage(chatId: String, lastMessage: String, timestamp: String) {
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
        chatDao.updateLastMessage(chatId, lastMessage, timestamp, now)
    }

    /**
     * Update the chat name (for group rename events).
     */
    suspend fun updateChatName(chatId: String, name: String) {
        chatDao.updateChatName(chatId, name)
    }

    /**
     * Remove a chat from the local cache.
     */
    suspend fun deleteChatById(chatId: String) {
        chatDao.deleteChatById(chatId)
    }

    /**
     * Insert or update a chat in the local cache.
     */
    suspend fun insertChat(chat: ChatEntity) {
        chatDao.insertChat(chat)
    }

    // -------------------------------------------------------------------------
    // Clear cache (for logout).
    // -------------------------------------------------------------------------

    /**
     * Clear all chat and message data from the local cache.
     */
    suspend fun clearLocalCache() {
        chatDao.deleteAllChats()
        messageDao.deleteAllMessages()
    }

    // -------------------------------------------------------------------------
    // Mapper helpers
    // -------------------------------------------------------------------------

    private fun ChatResponse.toEntity(): ChatEntity {
        val userId = currentUserId
        val isGroupChat = isGroup || !name.isNullOrBlank() || participants.size > 2 || !admins.isNullOrEmpty()

        val displayName = if (isGroupChat && !name.isNullOrBlank()) {
            name
        } else {
            val other = participants.firstOrNull { it.id != userId }
                ?: participants.firstOrNull()
            other?.username ?: "Unknown"
        }

        val other = participants.firstOrNull { it.id != userId }
            ?: participants.firstOrNull()

        // Convert participants to JSON for storage
        val participantsList = participants.map { p ->
            ChatEntity.Participant(
                id = p.id,
                username = p.username,
                profileImageUrl = p.profileImageUrl
            )
        }

        return ChatEntity(
            id = id,
            name = displayName,
            isGroup = isGroupChat,
            avatarUrl = other?.profileImageUrl,
            lastMessage = lastMessage?.content ?: "",
            lastMessageTime = formatTimestamp(lastMessage?.createdAt ?: updatedAt),
            unreadCount = unreadCount?.get(userId) ?: 0,
            participantsJson = ChatEntity.participantsToJson(participantsList),
            adminsJson = admins?.joinToString(",") ?: "",
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun ChatMessageResponse.toEntity(): MessageEntity {
        val userId = currentUserId
        val name = senderId?.username ?: ""
        return MessageEntity(
            id = id,
            chatId = chatId,
            senderId = senderId?.id ?: "",
            senderName = name,
            content = content,
            timestamp = formatTimestamp(createdAt),
            createdAt = createdAt,
            isFromMe = senderId?.id == userId,
            isSystem = false
        )
    }

    private fun formatTimestamp(timestamp: String): String {
        if (timestamp.isBlank()) return ""
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = sdf.parse(timestamp)
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(date!!)
        } catch (e: Exception) {
            timestamp
        }
    }
}
