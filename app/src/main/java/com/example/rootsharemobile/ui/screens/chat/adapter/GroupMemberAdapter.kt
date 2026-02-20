package com.example.rootsharemobile.ui.screens.chat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rootsharemobile.databinding.ItemGroupMemberBinding

data class GroupMemberItem(
    val userId: String,
    val username: String,
    val profileImageUrl: String? = null,
    val isAdmin: Boolean = false
)

class GroupMemberAdapter(
    private val onMemberClick: (GroupMemberItem) -> Unit,
    private val onMemberLongClick: (GroupMemberItem) -> Boolean
) : ListAdapter<GroupMemberItem, GroupMemberAdapter.ViewHolder>(MemberDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGroupMemberBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemGroupMemberBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(member: GroupMemberItem) {
            binding.memberName.text = member.username
            binding.avatarInitial.text = member.username.take(1).uppercase()
            binding.adminBadge.isVisible = member.isAdmin
            binding.root.setOnClickListener { onMemberClick(member) }
            binding.root.setOnLongClickListener { onMemberLongClick(member) }
        }
    }

    private class MemberDiffCallback : DiffUtil.ItemCallback<GroupMemberItem>() {
        override fun areItemsTheSame(oldItem: GroupMemberItem, newItem: GroupMemberItem): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: GroupMemberItem, newItem: GroupMemberItem): Boolean {
            return oldItem == newItem
        }
    }
}
