package com.example.rootsharemobile.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

/**
 * Room entity representing a cached chat/conversation.
 *
 * Stores chat metadata including participants (as JSON), last message info,
 * and unread count for the current user.
 */
@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey
    val id: String,
    /** Display name for the chat (group name or other participant's username). */
    val name: String,
    /** Whether this is a group chat. */
    val isGroup: Boolean,
    /** URL of the chat avatar (other participant's image or group image). */
    val avatarUrl: String?,
    /** Last message content for preview. */
    val lastMessage: String,
    /** Timestamp of the last message (formatted for display). */
    val lastMessageTime: String,
    /** Number of unread messages for the current user. */
    val unreadCount: Int,
    /** JSON array string of participants [{id, username, profileImageUrl}, ...]. */
    val participantsJson: String,
    /** JSON string of admin IDs for group chats. */
    val adminsJson: String,
    /** ISO timestamp when the chat was created. */
    val createdAt: String,
    /** ISO timestamp when the chat was last updated. */
    val updatedAt: String
) {
    /**
     * Data class representing a chat participant.
     */
    data class Participant(
        val id: String,
        val username: String,
        val profileImageUrl: String?
    )

    /**
     * Parse the participantsJson into a list of Participant objects.
     */
    fun getParticipants(): List<Participant> {
        if (participantsJson.isBlank()) return emptyList()
        return try {
            val array = JSONArray(participantsJson)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                Participant(
                    id = obj.optString("id", ""),
                    username = obj.optString("username", ""),
                    profileImageUrl = obj.optString("profileImageUrl", null)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get the list of member IDs.
     */
    fun getMemberIds(): List<String> {
        return getParticipants().map { it.id }
    }

    companion object {
        /**
         * Convert a list of participants to JSON string.
         */
        fun participantsToJson(participants: List<Participant>): String {
            val array = JSONArray()
            participants.forEach { p ->
                val obj = JSONObject().apply {
                    put("id", p.id)
                    put("username", p.username)
                    p.profileImageUrl?.let { put("profileImageUrl", it) }
                }
                array.put(obj)
            }
            return array.toString()
        }
    }
}
