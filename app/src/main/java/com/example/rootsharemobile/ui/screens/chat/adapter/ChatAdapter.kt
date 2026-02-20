package com.example.rootsharemobile.ui.screens.chat.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rootsharemobile.R
import com.example.rootsharemobile.databinding.ItemChatBinding
import com.example.rootsharemobile.ui.components.ChatPreview

class ChatAdapter(
    private val onChatClick: (chatId: String) -> Unit
) : ListAdapter<ChatPreview, ChatAdapter.ChatViewHolder>(ChatDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChatViewHolder(
        private val binding: ItemChatBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chat: ChatPreview) {
            val context = binding.root.context

            // Avatar initial
            binding.avatarInitial.text = chat.participantName.take(1).uppercase()

            // Name (bold if unread)
            binding.participantName.text = chat.participantName
            binding.participantName.setTypeface(null,
                if (chat.unreadCount > 0) Typeface.BOLD else Typeface.NORMAL
            )

            // Last message
            binding.lastMessage.text = chat.lastMessage
            binding.lastMessage.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (chat.unreadCount > 0) R.color.gray_900 else R.color.gray_500
                )
            )
            binding.lastMessage.setTypeface(null,
                if (chat.unreadCount > 0) Typeface.BOLD else Typeface.NORMAL
            )

            // Timestamp (emerald if unread, gray if read)
            binding.timestamp.text = chat.timestamp
            binding.timestamp.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (chat.unreadCount > 0) R.color.emerald_500 else R.color.gray_500
                )
            )

            // Unread badge
            if (chat.unreadCount > 0) {
                binding.unreadBadge.isVisible = true
                binding.unreadBadge.text =
                    if (chat.unreadCount > 99) "99+" else "${chat.unreadCount}"
            } else {
                binding.unreadBadge.isVisible = false
            }

            // Click listener
            binding.root.setOnClickListener { onChatClick(chat.chatId) }
        }
    }

    private class ChatDiffCallback : DiffUtil.ItemCallback<ChatPreview>() {
        override fun areItemsTheSame(oldItem: ChatPreview, newItem: ChatPreview): Boolean {
            return oldItem.chatId == newItem.chatId
        }

        override fun areContentsTheSame(oldItem: ChatPreview, newItem: ChatPreview): Boolean {
            return oldItem == newItem
        }
    }
}
