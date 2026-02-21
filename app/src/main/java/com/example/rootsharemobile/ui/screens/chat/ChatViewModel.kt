package com.example.rootsharemobile.ui.screens.chat

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rootsharemobile.data.local.TokenManager
import com.example.rootsharemobile.data.local.db.AppDatabase
import com.example.rootsharemobile.data.local.db.entity.ChatEntity
import com.example.rootsharemobile.data.local.db.entity.MessageEntity
import com.example.rootsharemobile.data.model.User
import com.example.rootsharemobile.data.remote.ConnectionStatus
import com.example.rootsharemobile.data.remote.RetrofitClient
import com.example.rootsharemobile.data.remote.SocketManager
import com.example.rootsharemobile.data.repository.ChatRepository
import com.example.rootsharemobile.ui.components.ChatPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application)
    private val api = RetrofitClient.apiService

    // Room database and repository
    private val database = AppDatabase.getInstance(application)
    private val chatRepository = ChatRepository(database.chatDao(), database.messageDao())

    // Observe chats from Room - converted to ChatPreview for UI compatibility
    val chats: StateFlow<List<ChatPreview>> = chatRepository.observeAllChats()
        .map { entities -> entities.map { it.toChatPreview() } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Messages for the current chat - observed from Room
    private val _currentChatId = MutableStateFlow<String?>(null)
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    val connectionStatus: StateFlow<ConnectionStatus> = SocketManager.connectionStatus

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isInChatRoom = MutableStateFlow(false)
    val isInChatRoom: StateFlow<Boolean> = _isInChatRoom

    fun setInChatRoom(value: Boolean) {
        _isInChatRoom.value = value
    }

    private var currentChatId: String? = null
    private var currentUserId: String? = null
    private var currentUserName: String? = null
    private var accessToken: String? = null
    private var listenersRegistered = false
    private var currentChatIsGroup = false

    // Cache for ChatResponse data (for group info etc.)
    private var chatEntityCache = mutableMapOf<String, ChatEntity>()

    private val _currentChatDetail = MutableStateFlow<ChatEntity?>(null)
    val currentChatDetail: StateFlow<ChatEntity?> = _currentChatDetail

    private val messageListener: (Array<Any>) -> Unit = { args ->
        try {
            val data = args[0] as JSONObject
            val chatId = data.optString("chatId", "")
            val content = data.optString("content", "")
            val id = data.optString("_id", UUID.randomUUID().toString())
            val timestamp = data.optString("timestamp",
                data.optString("createdAt", currentTimeIso()))

            val senderId = parseSenderId(data)
            val senderName = parseSenderName(data)

            // Skip echo of own messages
            if (senderId != currentUserId) {
                val messageEntity = MessageEntity(
                    id = id,
                    chatId = chatId,
                    senderId = senderId,
                    senderName = senderName,
                    content = content,
                    timestamp = formatTimestamp(timestamp),
                    createdAt = timestamp,
                    isFromMe = false,
                    isSystem = false
                )

                viewModelScope.launch {
                    // Insert into Room
                    chatRepository.insertMessage(messageEntity)
                    // Update chat last message
                    chatRepository.updateChatLastMessage(chatId, content, formatTimestamp(timestamp))

                    // If this is the current chat, update the messages list
                    if (chatId == currentChatId) {
                        _messages.value = _messages.value + messageEntity.toChatMessage()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse message", e)
        }
    }

    private val memberAddedListener: (Array<Any>) -> Unit = { args ->
        try {
            val data = args[0] as JSONObject
            val chatId = data.optString("chatId", "")
            val username = data.optString("username", "Someone")
            val addedBy = data.optString("addedBy", "")
            if (chatId == currentChatId) {
                val msg = ChatMessage(
                    id = "sys_${System.currentTimeMillis()}",
                    text = "'$addedBy' added '$username'",
                    isFromMe = false, timestamp = "", isSystem = true
                )
                _messages.value = _messages.value + msg
                loadChatDetail(chatId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse member_added", e)
        }
    }

    private val memberRemovedListener: (Array<Any>) -> Unit = { args ->
        try {
            val data = args[0] as JSONObject
            val chatId = data.optString("chatId", "")
            val username = data.optString("username", "Someone")
            val removedBy = data.optString("removedBy", "")
            if (chatId == currentChatId) {
                val text = if (removedBy == username) "'$username' left the group"
                else "'$removedBy' removed '$username'"
                val msg = ChatMessage(
                    id = "sys_${System.currentTimeMillis()}",
                    text = text,
                    isFromMe = false, timestamp = "", isSystem = true
                )
                _messages.value = _messages.value + msg
                loadChatDetail(chatId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse member_removed", e)
        }
    }

    private val typingListener: (Array<Any>) -> Unit = { args ->
        try {
            val data = args[0] as JSONObject
            val userId = data.optString("userId", "")
            val typing = data.optBoolean("isTyping", false)

            if (userId != currentUserId) {
                _isTyping.value = typing
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse typing event", e)
        }
    }

    private val groupRenamedListener: (Array<Any>) -> Unit = { args ->
        try {
            val data = args[0] as JSONObject
            val chatId = data.optString("chatId", "")
            val newName = data.optString("name", "")
            if (newName.isNotBlank()) {
                viewModelScope.launch {
                    // Update in Room
                    chatRepository.updateChatName(chatId, newName)

                    // Update cache
                    chatEntityCache[chatId]?.let { entity ->
                        chatEntityCache[chatId] = entity.copy(name = newName)
                    }

                    if (chatId == currentChatId) {
                        _currentChatDetail.value = chatEntityCache[chatId]
                        val msg = ChatMessage(
                            id = "sys_${System.currentTimeMillis()}",
                            text = "Group renamed to \"$newName\"",
                            isFromMe = false, timestamp = "", isSystem = true
                        )
                        _messages.value = _messages.value + msg
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse group_renamed", e)
        }
    }

    fun connect() {
        viewModelScope.launch {
            accessToken = tokenManager.getAccessToken()
            val user = tokenManager.getUser()
            currentUserId = user?.id
            currentUserName = user?.username
            chatRepository.setCurrentUserId(currentUserId)
            val token = accessToken ?: return@launch

            // Register event handlers only once
            if (!listenersRegistered) {
                SocketManager.on("message", messageListener)
                SocketManager.on("typing", typingListener)
                SocketManager.on("member_added", memberAddedListener)
                SocketManager.on("member_removed", memberRemovedListener)
                SocketManager.on("group_renamed", groupRenamedListener)
                listenersRegistered = true
            }

            // Connect socket if not already connected
            if (!SocketManager.isConnected) {
                SocketManager.connect(token)
            }

            // Load chats from server and store in Room (offline-first)
            loadChats()
        }
    }

    private fun loadChats() {
        val token = accessToken ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = chatRepository.fetchAndStoreChats(token)
                if (result.isSuccess) {
                    // Update local cache
                    result.getOrNull()?.forEach { entity ->
                        chatEntityCache[entity.id] = entity
                    }
                } else {
                    Log.e(TAG, "Failed to load chats: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load chats", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun joinRoom(chatId: String) {
        if (currentChatId == chatId) return
        currentChatId?.let { SocketManager.leaveRoom(it) }
        currentChatId = chatId
        _currentChatId.value = chatId
        _messages.value = emptyList()
        _isTyping.value = false
        currentChatIsGroup = isGroupChat(chatId)

        viewModelScope.launch {
            // First show cached messages (offline-first)
            val cachedMessages = chatRepository.getCachedMessages(chatId)
            if (cachedMessages.isNotEmpty()) {
                val messages = cachedMessages.map { it.toChatMessage() }.toMutableList()
                if (currentChatIsGroup) {
                    val groupName = chats.value.find { it.chatId == chatId }?.participantName ?: "Group"
                    messages.add(0, ChatMessage(
                        id = "sys_created",
                        text = "Group \"$groupName\" created",
                        isFromMe = false, timestamp = "", isSystem = true
                    ))
                }
                _messages.value = messages
            }

            // Update current chat detail from cache
            _currentChatDetail.value = chatEntityCache[chatId] ?: chatRepository.getChatById(chatId)
        }

        SocketManager.joinRoom(chatId)
        loadMessages(chatId)
        markAsRead(chatId)
        loadChatDetail(chatId)
    }

    private fun loadChatDetail(chatId: String) {
        loadChats()
    }

    private fun loadMessages(chatId: String) {
        val token = accessToken ?: return
        viewModelScope.launch {
            try {
                val result = chatRepository.fetchAndStoreMessages(token, chatId)
                if (result.isSuccess) {
                    val messages = result.getOrNull()?.map { it.toChatMessage() }?.toMutableList() ?: mutableListOf()
                    if (currentChatIsGroup) {
                        val groupName = chats.value.find { it.chatId == chatId }?.participantName ?: "Group"
                        messages.add(0, ChatMessage(
                            id = "sys_created",
                            text = "Group \"$groupName\" created",
                            isFromMe = false, timestamp = "", isSystem = true
                        ))
                    }
                    _messages.value = messages
                } else {
                    Log.e(TAG, "Failed to load messages: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load messages", e)
            }
        }
    }

    private fun markAsRead(chatId: String) {
        val token = accessToken ?: return
        viewModelScope.launch {
            try {
                chatRepository.markChatAsRead(token, chatId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark as read", e)
            }
        }
    }

    fun leaveRoom() {
        currentChatId?.let { SocketManager.leaveRoom(it) }
        currentChatId = null
        _currentChatId.value = null
        _isTyping.value = false
    }

    fun sendMessage(content: String) {
        val chatId = currentChatId ?: return
        if (content.isBlank()) return

        val name = currentUserName ?: ""
        val now = currentTimeIso()
        val messageEntity = MessageEntity(
            id = "temp_${System.currentTimeMillis()}",
            chatId = chatId,
            senderId = currentUserId ?: "",
            senderName = name,
            content = content,
            timestamp = formatTimestamp(now),
            createdAt = now,
            isFromMe = true,
            isSystem = false
        )

        // Optimistically add to UI
        _messages.value = _messages.value + messageEntity.toChatMessage()

        viewModelScope.launch {
            // Insert into Room
            chatRepository.insertMessage(messageEntity)
            // Update chat last message
            chatRepository.updateChatLastMessage(chatId, content, formatTimestamp(now))
        }

        SocketManager.sendMessage(chatId, content)
    }

    fun sendTyping(isTyping: Boolean) {
        currentChatId?.let { SocketManager.sendTyping(it, isTyping) }
    }

    fun getParticipantName(chatId: String): String {
        return chats.value.find { it.chatId == chatId }?.participantName ?: "Chat"
    }

    fun refreshChats() {
        loadChats()
    }

    fun getCurrentUserId(): String? = currentUserId

    fun isGroupChat(chatId: String): Boolean {
        val preview = chats.value.find { it.chatId == chatId }
        if (preview?.isGroup == true) return true
        val entity = chatEntityCache[chatId]
        if (entity != null) {
            return entity.isGroup || entity.getParticipants().size > 2 || entity.adminsJson.isNotBlank()
        }
        return false
    }

    fun leaveGroup(chatId: String, onResult: (Boolean) -> Unit) {
        val token = accessToken ?: run { onResult(false); return }
        viewModelScope.launch {
            try {
                val result = chatRepository.leaveGroupChat(token, chatId)
                if (result.isSuccess) {
                    chatEntityCache.remove(chatId)
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    // --- Search & Create ---

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    fun searchUsers(query: String) {
        val token = accessToken ?: return
        if (query.length < 2) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            try {
                val response = api.searchUsers("Bearer $token", query)
                if (response.isSuccessful) {
                    _searchResults.value = (response.body() ?: emptyList())
                        .filter { it.id != currentUserId }
                } else {
                    _searchResults.value = emptyList()
                }
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }

    private fun findExistingChatWithUser(userId: String): String? {
        for ((chatId, entity) in chatEntityCache) {
            if (entity.isGroup) continue
            val memberIds = entity.getMemberIds()
            if (memberIds.contains(userId)) return chatId
        }
        return null
    }

    fun createNewChat(userId: String, onResult: (chatId: String?) -> Unit) {
        val existingChatId = findExistingChatWithUser(userId)
        if (existingChatId != null) {
            onResult(existingChatId)
            return
        }

        val token = accessToken ?: run { onResult(null); return }
        viewModelScope.launch {
            try {
                val result = chatRepository.createOrGetChat(token, userId)
                if (result.isSuccess) {
                    val entity = result.getOrNull()
                    if (entity != null) {
                        chatEntityCache[entity.id] = entity
                        onResult(entity.id)
                    } else {
                        onResult(null)
                    }
                } else {
                    onResult(null)
                }
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }

    fun createGroupChat(name: String, userIds: List<String>, onResult: (chatId: String?) -> Unit) {
        val token = accessToken ?: run { onResult(null); return }
        viewModelScope.launch {
            try {
                val result = chatRepository.createGroupChat(token, name, userIds)
                if (result.isSuccess) {
                    val entity = result.getOrNull()
                    if (entity != null) {
                        chatEntityCache[entity.id] = entity
                        onResult(entity.id)
                    } else {
                        onResult(null)
                    }
                } else {
                    onResult(null)
                }
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }

    // --- Mappers ---

    private fun ChatEntity.toChatPreview(): ChatPreview {
        return ChatPreview(
            chatId = id,
            participantName = name,
            participantImageUrl = avatarUrl,
            lastMessage = lastMessage,
            timestamp = lastMessageTime,
            unreadCount = unreadCount,
            isGroup = isGroup
        )
    }

    private fun MessageEntity.toChatMessage(): ChatMessage {
        return ChatMessage(
            id = id,
            text = content,
            isFromMe = isFromMe,
            timestamp = timestamp,
            senderName = senderName,
            senderInitial = senderName.take(1).uppercase(),
            senderId = senderId,
            isSystem = isSystem
        )
    }

    // --- Helpers ---

    private fun parseSenderId(json: JSONObject): String {
        val raw = json.opt("senderId") ?: return ""
        return when (raw) {
            is JSONObject -> raw.optString("_id", "")
            is String -> raw
            else -> raw.toString()
        }
    }

    private fun parseSenderName(json: JSONObject): String {
        val raw = json.opt("senderId") ?: return ""
        return when (raw) {
            is JSONObject -> raw.optString("username", "")
            else -> ""
        }
    }

    private fun currentTimeIso(): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
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

    override fun onCleared() {
        super.onCleared()
        SocketManager.off("message", messageListener)
        SocketManager.off("typing", typingListener)
        SocketManager.off("member_added", memberAddedListener)
        SocketManager.off("member_removed", memberRemovedListener)
        SocketManager.off("group_renamed", groupRenamedListener)
        SocketManager.disconnect()
        listenersRegistered = false
    }

    companion object {
        private const val TAG = "ChatViewModel"
    }
}
