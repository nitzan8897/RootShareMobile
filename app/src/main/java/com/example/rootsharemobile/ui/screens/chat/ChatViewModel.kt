package com.example.rootsharemobile.ui.screens.chat

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rootsharemobile.data.local.TokenManager
import com.example.rootsharemobile.data.model.ChatMessageResponse
import com.example.rootsharemobile.data.model.ChatResponse
import com.example.rootsharemobile.data.model.CreateChatRequest
import com.example.rootsharemobile.data.model.CreateGroupChatRequest
import com.example.rootsharemobile.data.model.User
import com.example.rootsharemobile.data.remote.ConnectionStatus
import com.example.rootsharemobile.data.remote.RetrofitClient
import com.example.rootsharemobile.data.remote.SocketManager
import com.example.rootsharemobile.ui.components.ChatPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application)
    private val api = RetrofitClient.apiService

    private val _chats = MutableStateFlow<List<ChatPreview>>(emptyList())
    val chats: StateFlow<List<ChatPreview>> = _chats

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
    private var listenersRegistered = false   // guards against duplicate socket handlers
    private var currentChatIsGroup = false

    private var chatResponseMap = mutableMapOf<String, ChatResponse>()

    private val _currentChatDetail = MutableStateFlow<ChatResponse?>(null)
    val currentChatDetail: StateFlow<ChatResponse?> = _currentChatDetail

    private val messageListener: (Array<Any>) -> Unit = { args ->
        try {
            val data = args[0] as JSONObject
            val chatId = data.optString("chatId", "")
            val content = data.optString("content", "")
            val id = data.optString("_id", UUID.randomUUID().toString())
            val timestamp = data.optString("timestamp",
                data.optString("createdAt", currentTime()))

            val senderId = parseSenderId(data)
            val senderName = parseSenderName(data)

            // Skip echo of own messages
            if (senderId != currentUserId) {
                val message = ChatMessage(
                    id = id,
                    text = content,
                    isFromMe = false,
                    timestamp = formatTimestamp(timestamp),
                    senderName = senderName,
                    senderInitial = senderName.take(1).uppercase(),
                    senderId = senderId
                )

                if (chatId == currentChatId) {
                    _messages.value = _messages.value + message
                }

                updateChatLastMessage(chatId, content, formatTimestamp(timestamp))
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
                _chats.value = _chats.value.map {
                    if (it.chatId == chatId) it.copy(participantName = newName) else it
                }
                chatResponseMap[chatId]?.let { cr ->
                    chatResponseMap[chatId] = cr.copy(name = newName)
                }
                if (chatId == currentChatId) {
                    _currentChatDetail.value = chatResponseMap[chatId]
                    val msg = ChatMessage(
                        id = "sys_${System.currentTimeMillis()}",
                        text = "Group renamed to \"$newName\"",
                        isFromMe = false, timestamp = "", isSystem = true
                    )
                    _messages.value = _messages.value + msg
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
            val token = accessToken ?: return@launch

            // Register event handlers only once — prevents duplicate message/typing callbacks
            // if connect() is called again after a disconnect.
            if (!listenersRegistered) {
                SocketManager.on("message", messageListener)
                SocketManager.on("typing", typingListener)
                SocketManager.on("member_added", memberAddedListener)
                SocketManager.on("member_removed", memberRemovedListener)
                SocketManager.on("group_renamed", groupRenamedListener)
                listenersRegistered = true
            }

            // Always attempt a socket connection when not already connected.
            // Using SocketManager.isConnected instead of a local flag means this works
            // even after a disconnect or a failed first attempt (regular-user offline fix).
            if (!SocketManager.isConnected) {
                SocketManager.connect(token)
            }

            loadChats()
        }
    }

    private fun loadChats() {
        val token = accessToken ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.getChats("Bearer $token")
                if (response.isSuccessful) {
                    val body = response.body() ?: emptyList()
                    body.forEach { chatResponseMap[it.id] = it }
                    _chats.value = body.map { it.toChatPreview() }
                } else {
                    Log.e(TAG, "Failed to load chats: ${response.code()}")
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
        _messages.value = emptyList()
        _isTyping.value = false
        currentChatIsGroup = isGroupChat(chatId)
        _currentChatDetail.value = chatResponseMap[chatId]
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
                val response = api.getChatMessages("Bearer $token", chatId)
                if (response.isSuccessful) {
                    val body = response.body() ?: emptyList()
                    val messages = body.map { it.toChatMessage() }.reversed().toMutableList()
                    if (currentChatIsGroup) {
                        val groupName = _chats.value.find { it.chatId == chatId }?.participantName ?: "Group"
                        messages.add(0, ChatMessage(
                            id = "sys_created",
                            text = "Group \"$groupName\" created",
                            isFromMe = false, timestamp = "", isSystem = true
                        ))
                    }
                    _messages.value = messages
                } else {
                    Log.e(TAG, "Failed to load messages: ${response.code()}")
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
                api.markChatAsRead("Bearer $token", chatId)
                _chats.value = _chats.value.map {
                    if (it.chatId == chatId) it.copy(unreadCount = 0) else it
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark as read", e)
            }
        }
    }

    fun leaveRoom() {
        currentChatId?.let { SocketManager.leaveRoom(it) }
        currentChatId = null
        _isTyping.value = false
    }

    fun sendMessage(content: String) {
        val chatId = currentChatId ?: return
        if (content.isBlank()) return

        val name = currentUserName ?: ""
        val message = ChatMessage(
            id = "temp_${System.currentTimeMillis()}",
            text = content,
            isFromMe = true,
            timestamp = currentTime(),
            senderName = name,
            senderInitial = name.take(1).uppercase(),
            senderId = currentUserId ?: ""
        )
        _messages.value = _messages.value + message

        SocketManager.sendMessage(chatId, content)
        updateChatLastMessage(chatId, content, currentTime())
    }

    fun sendTyping(isTyping: Boolean) {
        currentChatId?.let { SocketManager.sendTyping(it, isTyping) }
    }

    fun getParticipantName(chatId: String): String {
        return _chats.value.find { it.chatId == chatId }?.participantName ?: "Chat"
    }

    fun refreshChats() {
        loadChats()
    }

    fun getCurrentUserId(): String? = currentUserId

    fun isGroupChat(chatId: String): Boolean {
        val preview = _chats.value.find { it.chatId == chatId }
        if (preview?.isGroup == true) return true
        val response = chatResponseMap[chatId]
        if (response != null) {
            return response.isGroup || !response.name.isNullOrBlank() ||
                    response.participants.size > 2 || !response.admins.isNullOrEmpty()
        }
        return false
    }

    fun leaveGroup(chatId: String, onResult: (Boolean) -> Unit) {
        val token = accessToken ?: run { onResult(false); return }
        viewModelScope.launch {
            try {
                val response = api.leaveGroupChat("Bearer $token", chatId)
                if (response.isSuccessful) {
                    _chats.value = _chats.value.filter { it.chatId != chatId }
                    chatResponseMap.remove(chatId)
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
        for ((chatId, chatResponse) in chatResponseMap) {
            val isGroup = chatResponse.isGroup || !chatResponse.name.isNullOrBlank() ||
                    chatResponse.participants.size > 2 || !chatResponse.admins.isNullOrEmpty()
            if (isGroup) continue
            val hasUser = chatResponse.participants.any { it.id == userId }
            if (hasUser) return chatId
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
                val response = api.createOrGetChat("Bearer $token", CreateChatRequest(userId))
                if (response.isSuccessful) {
                    val chat = response.body()
                    if (chat != null) {
                        chatResponseMap[chat.id] = chat
                        val preview = chat.toChatPreview()
                        val existing = _chats.value.any { it.chatId == preview.chatId }
                        if (!existing) {
                            _chats.value = listOf(preview) + _chats.value
                        }
                        onResult(chat.id)
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
                val response = api.createGroupChat(
                    "Bearer $token",
                    CreateGroupChatRequest(name, userIds)
                )
                if (response.isSuccessful) {
                    val chat = response.body()
                    if (chat != null) {
                        chatResponseMap[chat.id] = chat
                        val preview = chat.toChatPreview()
                        val existing = _chats.value.any { it.chatId == preview.chatId }
                        if (!existing) {
                            _chats.value = listOf(preview) + _chats.value
                        }
                        onResult(chat.id)
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

    private fun ChatResponse.toChatPreview(): ChatPreview {
        val isGroupChat = isGroup || !name.isNullOrBlank() || participants.size > 2 || !admins.isNullOrEmpty()
        val displayName = if (isGroupChat && !name.isNullOrBlank()) {
            name
        } else {
            val other = participants.firstOrNull { it.id != currentUserId }
                ?: participants.firstOrNull()
            other?.username ?: "Unknown"
        }
        val other = participants.firstOrNull { it.id != currentUserId }
            ?: participants.firstOrNull()
        return ChatPreview(
            chatId = id,
            participantName = displayName,
            participantImageUrl = other?.profileImageUrl,
            lastMessage = lastMessage?.content ?: "",
            timestamp = formatTimestamp(lastMessage?.createdAt ?: updatedAt),
            unreadCount = unreadCount?.get(currentUserId) ?: 0,
            isGroup = isGroupChat
        )
    }

    private fun ChatMessageResponse.toChatMessage(): ChatMessage {
        val name = senderId?.username ?: ""
        return ChatMessage(
            id = id,
            text = content,
            isFromMe = senderId?.id == currentUserId,
            timestamp = formatTimestamp(createdAt),
            senderName = name,
            senderInitial = name.take(1).uppercase(),
            senderId = senderId?.id ?: ""
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

    private fun updateChatLastMessage(chatId: String, lastMessage: String, timestamp: String) {
        _chats.value = _chats.value.map { chat ->
            if (chat.chatId == chatId) {
                chat.copy(lastMessage = lastMessage, timestamp = timestamp)
            } else {
                chat
            }
        }
    }

    private fun currentTime(): String {
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
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
