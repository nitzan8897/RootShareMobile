package com.example.rootsharemobile.ui.screens.chat

data class ChatMessage(
    val id: String,
    val text: String,
    val isFromMe: Boolean,
    val timestamp: String,
    val senderName: String = "",
    val senderInitial: String = "",
    val senderId: String = "",
    val isSystem: Boolean = false
)
