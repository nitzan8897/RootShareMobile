package com.example.rootsharemobile.data.remote

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

object SocketManager {

    private const val TAG = "SocketManager"

    private var socket: Socket? = null

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    private val eventListeners = mutableMapOf<String, MutableList<(Array<Any>) -> Unit>>()

    fun connect(token: String) {
        if (socket?.connected() == true) {
            Log.d(TAG, "Already connected")
            return
        }

        _connectionStatus.value = ConnectionStatus.CONNECTING

        try {
            val options = IO.Options().apply {
                forceNew = true
                reconnection = true
                reconnectionAttempts = 5
                reconnectionDelay = 1000
                timeout = ApiConfig.CONNECT_TIMEOUT * 1000
                auth = mapOf("token" to token)
            }

            socket = IO.socket(ApiConfig.SOCKET_URL, options).apply {
                on(Socket.EVENT_CONNECT) {
                    Log.d(TAG, "Connected")
                    _connectionStatus.value = ConnectionStatus.CONNECTED
                }

                on(Socket.EVENT_DISCONNECT) { args ->
                    val reason = args.firstOrNull()?.toString() ?: "unknown"
                    Log.d(TAG, "Disconnected: $reason")
                    _connectionStatus.value = ConnectionStatus.DISCONNECTED
                }

                on(Socket.EVENT_CONNECT_ERROR) { args ->
                    val error = args.firstOrNull()?.toString() ?: "unknown"
                    Log.e(TAG, "Connection error: $error")
                    _connectionStatus.value = ConnectionStatus.DISCONNECTED
                }

                connect()
            }

            // Re-attach any registered listeners to the new socket
            eventListeners.forEach { (event, listeners) ->
                listeners.forEach { listener ->
                    socket?.on(event, listener)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect", e)
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
        eventListeners.clear()
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        Log.d(TAG, "Disconnected and cleaned up")
    }

    fun on(event: String, listener: (Array<Any>) -> Unit) {
        eventListeners.getOrPut(event) { mutableListOf() }.add(listener)
        socket?.on(event, listener)
    }

    fun off(event: String, listener: (Array<Any>) -> Unit) {
        eventListeners[event]?.remove(listener)
        socket?.off(event, listener)
    }

    fun off(event: String) {
        eventListeners.remove(event)
        socket?.off(event)
    }

    fun emit(event: String, data: JSONObject) {
        socket?.emit(event, data)
    }

    fun emit(event: String, data: JSONObject, ack: (Array<Any>) -> Unit) {
        socket?.emit(event, arrayOf(data)) { args -> ack(args) }
    }

    fun joinRoom(roomId: String) {
        emit("join_room", JSONObject().put("roomId", roomId))
        Log.d(TAG, "Joining room: $roomId")
    }

    fun leaveRoom(roomId: String) {
        emit("leave_room", JSONObject().put("roomId", roomId))
        Log.d(TAG, "Leaving room: $roomId")
    }

    fun sendMessage(chatId: String, content: String) {
        val payload = JSONObject().apply {
            put("chatId", chatId)
            put("content", content)
        }
        emit("send_message", payload)
        Log.d(TAG, "Message sent to $chatId")
    }

    fun sendTyping(roomId: String, isTyping: Boolean) {
        val payload = JSONObject().apply {
            put("roomId", roomId)
            put("isTyping", isTyping)
        }
        emit("typing", payload)
    }

    val isConnected: Boolean
        get() = socket?.connected() == true
}
