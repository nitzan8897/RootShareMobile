package com.example.rootsharemobile.data.model

import com.google.gson.annotations.SerializedName

data class ChatParticipant(
    @SerializedName("_id") val id: String,
    val username: String = "",
    val profileImageUrl: String? = null
)

data class ChatLastMessage(
    @SerializedName("_id") val id: String,
    val content: String = "",
    val senderId: ChatParticipant? = null,
    val createdAt: String = ""
)

data class ChatResponse(
    @SerializedName("_id") val id: String,
    val participants: List<ChatParticipant> = emptyList(),
    val lastMessage: ChatLastMessage? = null,
    val unreadCount: Map<String, Int>? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class ChatMessageResponse(
    @SerializedName("_id") val id: String,
    val chatId: String = "",
    val senderId: ChatParticipant? = null,
    val content: String = "",
    val createdAt: String = ""
)

data class CreateChatRequest(val userId: String)
