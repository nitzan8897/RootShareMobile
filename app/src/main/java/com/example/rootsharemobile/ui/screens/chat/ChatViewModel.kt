package com.example.rootsharemobile.ui.screens.chat

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rootsharemobile.data.local.TokenManager
import com.example.rootsharemobile.data.model.ChatMessageResponse
import com.example.rootsharemobile.data.model.ChatResponse
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

    private var currentChatId: String? = null
    private var currentUserId: String? = null
    private var accessToken: String? = null
    private var isConnected = false

    // Socket listener: backend sends { _id, chatId, senderId, content, timestamp }
    // senderId can be an object { _id, username, ... } or a plain string
    private val messageListener: (Array<Any>) -> Unit = { args ->
        try {
            val data = args[0] as JSONObject
            val chatId = data.optString("chatId", "")
            val content = data.optString("content", "")
            val id = data.optString("_id", UUID.randomUUID().toString())
            val timestamp = data.optString("timestamp",
                data.optString("createdAt", currentTime()))

            val senderId = parseSenderId(data)

            val message = ChatMessage(
                id = id,
                text = content,
                isFromMe = senderId == currentUserId,
                timestamp = formatTimestamp(timestamp)
            )

            if (chatId == currentChatId) {
                _messages.value = _messages.value + message
            }

            updateChatLastMessage(chatId, content, formatTimestamp(timestamp))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse message", e)
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

    fun connect() {
        if (isConnected) return
        viewModelScope.launch {
            accessToken = tokenManager.getAccessToken()
            currentUserId = tokenManager.getUser()?.id
            val token = accessToken
            if (token != null) {
                SocketManager.connect(token)
                SocketManager.on("message", messageListener)
                SocketManager.on("typing", typingListener)
                isConnected = true
                loadChats()
            }
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
        SocketManager.joinRoom(chatId)
        loadMessages(chatId)
        markAsRead(chatId)
    }

    private fun loadMessages(chatId: String) {
        val token = accessToken ?: return
        viewModelScope.launch {
            try {
                val response = api.getChatMessages("Bearer $token", chatId)
                if (response.isSuccessful) {
                    val body = response.body() ?: emptyList()
                    _messages.value = body.map { it.toChatMessage() }
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

        val message = ChatMessage(
            id = "temp_${System.currentTimeMillis()}",
            text = content,
            isFromMe = true,
            timestamp = currentTime()
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

    // --- Mappers ---

    private fun ChatResponse.toChatPreview(): ChatPreview {
        val other = participants.firstOrNull { it.id != currentUserId }
            ?: participants.firstOrNull()
        return ChatPreview(
            chatId = id,
            participantName = other?.username ?: "Unknown",
            participantImageUrl = other?.profileImageUrl,
            lastMessage = lastMessage?.content ?: "",
            timestamp = formatTimestamp(lastMessage?.createdAt ?: updatedAt),
            unreadCount = unreadCount?.get(currentUserId) ?: 0
        )
    }

    private fun ChatMessageResponse.toChatMessage(): ChatMessage {
        return ChatMessage(
            id = id,
            text = content,
            isFromMe = senderId?.id == currentUserId,
            timestamp = formatTimestamp(createdAt)
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
        SocketManager.disconnect()
        isConnected = false
    }

    companion object {
        private const val TAG = "ChatViewModel"
    }
}
