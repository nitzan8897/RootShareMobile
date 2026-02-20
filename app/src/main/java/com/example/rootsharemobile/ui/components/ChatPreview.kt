package com.example.rootsharemobile.ui.components

data class ChatPreview(
    val chatId: String,
    val participantName: String,
    val participantImageUrl: String?,
    val lastMessage: String,
    val timestamp: String,
    val unreadCount: Int,
    val isGroup: Boolean = false
)
