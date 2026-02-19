package com.example.rootsharemobile.ui.screens.chat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rootsharemobile.databinding.ItemMessageReceivedBinding
import com.example.rootsharemobile.databinding.ItemMessageSentBinding
import com.example.rootsharemobile.databinding.ItemMessageSystemBinding
import com.example.rootsharemobile.ui.screens.chat.ChatMessage

class MessageAdapter(
    private val onAvatarClick: ((senderId: String) -> Unit)? = null
) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
        private const val VIEW_TYPE_SYSTEM = 3
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when {
            item.isSystem -> VIEW_TYPE_SYSTEM
            item.isFromMe -> VIEW_TYPE_SENT
            else -> VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SENT -> SentViewHolder(
                ItemMessageSentBinding.inflate(inflater, parent, false)
            )
            VIEW_TYPE_SYSTEM -> SystemViewHolder(
                ItemMessageSystemBinding.inflate(inflater, parent, false)
            )
            else -> ReceivedViewHolder(
                ItemMessageReceivedBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentViewHolder -> holder.bind(message)
            is ReceivedViewHolder -> holder.bind(message)
            is SystemViewHolder -> holder.bind(message)
        }
    }

    class SentViewHolder(
        private val binding: ItemMessageSentBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            binding.messageText.text = message.text
            binding.messageTimestamp.text = message.timestamp
            binding.avatarInitial.text = message.senderInitial.ifEmpty { "?" }
        }
    }

    inner class ReceivedViewHolder(
        private val binding: ItemMessageReceivedBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            binding.messageText.text = message.text
            binding.messageTimestamp.text = message.timestamp
            binding.avatarInitial.text = message.senderInitial.ifEmpty { "?" }
            binding.senderName.text = message.senderName
            binding.senderName.isVisible = message.senderName.isNotBlank()
            binding.avatarFrame.setOnClickListener {
                if (message.senderId.isNotBlank()) {
                    onAvatarClick?.invoke(message.senderId)
                }
            }
        }
    }

    class SystemViewHolder(
        private val binding: ItemMessageSystemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            binding.systemText.text = message.text
        }
    }

    private class MessageDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }
}
