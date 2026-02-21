package com.example.rootsharemobile.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a cached chat message.
 *
 * Messages are linked to their parent chat via chatId.
 * The foreign key ensures referential integrity and cascading deletes.
 */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["chatId"])]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    /** The chat this message belongs to. */
    val chatId: String,
    /** ID of the user who sent this message. */
    val senderId: String,
    /** Username of the sender for display. */
    val senderName: String,
    /** The message content/text. */
    val content: String,
    /** Formatted timestamp for display. */
    val timestamp: String,
    /** ISO timestamp for sorting. */
    val createdAt: String,
    /** Whether this message was sent by the current user. */
    val isFromMe: Boolean,
    /** Whether this is a system message (e.g., "X joined the group"). */
    val isSystem: Boolean = false
)
