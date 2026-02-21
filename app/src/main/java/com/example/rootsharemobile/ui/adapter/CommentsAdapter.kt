package com.example.rootsharemobile.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.rootsharemobile.R
import com.example.rootsharemobile.data.model.Comment
import com.example.rootsharemobile.data.remote.ApiConfig
import com.example.rootsharemobile.databinding.ItemCommentBinding

class CommentsAdapter : ListAdapter<Comment, CommentsAdapter.CommentViewHolder>(CommentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CommentViewHolder(
        private val binding: ItemCommentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(comment: Comment) {
            val username = comment.author?.username
            val imageUrl = comment.author?.profileImageUrl?.let { ApiConfig.resolveImageUrl(it) }

            binding.textCommentUsername.text =
                if (!username.isNullOrBlank()) username else "Community Member"
            binding.textCommentContent.text = comment.content
            binding.textCommentTimestamp.text = comment.createdAt.take(10)

            if (!imageUrl.isNullOrBlank()) {
                binding.imageCommentAvatar.visibility = View.VISIBLE
                Glide.with(binding.imageCommentAvatar.context)
                    .load(imageUrl)
                    .transform(CircleCrop())
                    .placeholder(R.drawable.bg_avatar_circle)
                    .error(R.drawable.bg_avatar_circle)
                    .into(binding.imageCommentAvatar)
                binding.textCommentInitial.visibility = View.INVISIBLE
            } else {
                binding.imageCommentAvatar.visibility = View.GONE
                binding.textCommentInitial.visibility = View.VISIBLE
                binding.textCommentInitial.text =
                    if (!username.isNullOrBlank()) username.first().uppercaseChar().toString() else "?"
            }
        }
    }

    class CommentDiffCallback : DiffUtil.ItemCallback<Comment>() {
        override fun areItemsTheSame(oldItem: Comment, newItem: Comment) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Comment, newItem: Comment) = oldItem == newItem
    }
}
