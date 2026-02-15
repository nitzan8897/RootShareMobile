package com.example.rootsharemobile.ui.screens.chat

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rootsharemobile.data.local.TokenManager
import com.example.rootsharemobile.data.remote.ConnectionStatus
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

    private val _chats = MutableStateFlow(sampleChats)
    val chats: StateFlow<List<ChatPreview>> = _chats

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    val connectionStatus: StateFlow<ConnectionStatus> = SocketManager.connectionStatus

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping

    private var currentRoomId: String? = null
    private var currentUserId: String? = null
    private var isConnected = false

    private val messageListener: (Array<Any>) -> Unit = { args ->
        try {
            val data = args[0] as JSONObject
            val roomId = data.getString("roomId")
            val senderId = data.getString("senderId")
            val content = data.getString("content")
            val id = data.optString("id", UUID.randomUUID().toString())
            val timestamp = data.optString("timestamp", currentTime())

            val message = ChatMessage(
                id = id,
                text = content,
                isFromMe = senderId == currentUserId,
                timestamp = formatTimestamp(timestamp)
            )

            if (roomId == currentRoomId) {
                _messages.value = _messages.value + message
            }

            updateChatLastMessage(roomId, content, formatTimestamp(timestamp))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse message", e)
        }
    }

    private val typingListener: (Array<Any>) -> Unit = { args ->
        try {
            val data = args[0] as JSONObject
            val roomId = data.getString("roomId")
            val userId = data.getString("userId")
            val typing = data.getBoolean("isTyping")

            if (roomId == currentRoomId && userId != currentUserId) {
                _isTyping.value = typing
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse typing event", e)
        }
    }

    fun connect() {
        if (isConnected) return
        viewModelScope.launch {
            val token = tokenManager.getAccessToken()
            currentUserId = tokenManager.getUser()?.id
            if (token != null) {
                SocketManager.connect(token)
                SocketManager.on("message", messageListener)
                SocketManager.on("typing", typingListener)
                isConnected = true
            }
        }
    }

    fun joinRoom(chatId: String) {
        if (currentRoomId == chatId) return
        currentRoomId?.let { SocketManager.leaveRoom(it) }
        currentRoomId = chatId
        _messages.value = emptyList()
        _isTyping.value = false
        SocketManager.joinRoom(chatId)
    }

    fun leaveRoom() {
        currentRoomId?.let { SocketManager.leaveRoom(it) }
        currentRoomId = null
        _isTyping.value = false
    }

    fun sendMessage(content: String) {
        val roomId = currentRoomId ?: return
        if (content.isBlank()) return

        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = content,
            isFromMe = true,
            timestamp = currentTime()
        )
        _messages.value = _messages.value + message

        SocketManager.sendMessage(roomId, content)

        updateChatLastMessage(roomId, content, currentTime())
    }

    fun sendTyping(isTyping: Boolean) {
        currentRoomId?.let { SocketManager.sendTyping(it, isTyping) }
    }

    fun getParticipantName(chatId: String): String {
        return _chats.value.find { it.chatId == chatId }?.participantName ?: "Chat"
    }

    private fun updateChatLastMessage(roomId: String, lastMessage: String, timestamp: String) {
        _chats.value = _chats.value.map { chat ->
            if (chat.chatId == roomId) {
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
