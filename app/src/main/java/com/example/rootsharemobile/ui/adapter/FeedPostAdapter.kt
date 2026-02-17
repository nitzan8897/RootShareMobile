package com.example.rootsharemobile.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.rootsharemobile.R
import com.example.rootsharemobile.data.local.db.entity.PostEntity
import com.example.rootsharemobile.data.remote.ApiConfig
import com.example.rootsharemobile.databinding.ItemFeedPostBinding

/**
 * RecyclerView adapter for the vertical community-feed list on the Home screen.
 *
 * Uses [ListAdapter] with [DiffUtil] for efficient updates when Room emits
 * a new list. Images are loaded with Glide.
 */
class FeedPostAdapter(
    private val onPostClick: (PostEntity) -> Unit = {}
) : ListAdapter<PostEntity, FeedPostAdapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemFeedPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PostViewHolder(
        private val binding: ItemFeedPostBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: PostEntity) {
            // Post type badge
            binding.textPostType.text = post.typeBadge

            // Post content
            binding.textContent.text = post.content

            // Plant name (optional)
            if (!post.plantName.isNullOrBlank()) {
                binding.textPlantName.text = post.plantName
                binding.textPlantName.visibility = View.VISIBLE
            } else {
                binding.textPlantName.visibility = View.GONE
            }

            // Footer counters
            binding.textLikes.text = binding.root.context.getString(
                R.string.label_likes_count, post.likesCount
            )
            binding.textComments.text = binding.root.context.getString(
                R.string.label_comments_count, post.commentsCount
            )

            // Timestamp (show first 10 chars = YYYY-MM-DD)
            binding.textTimestamp.text = post.createdAt.take(10)

            // Post image — shown only when the post has at least one image URL
            val firstImage = post.imagesJson.split(",").firstOrNull { it.isNotBlank() }
            if (firstImage != null) {
                binding.imagePost.visibility = View.VISIBLE
                Glide.with(binding.imagePost.context)
                    .load(ApiConfig.resolveImageUrl(firstImage))
                    .placeholder(R.color.gray_100)
                    .error(R.color.gray_200)
                    .centerCrop()
                    .into(binding.imagePost)
            } else {
                binding.imagePost.visibility = View.GONE
            }

            binding.root.setOnClickListener { onPostClick(post) }
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<PostEntity>() {
        override fun areItemsTheSame(oldItem: PostEntity, newItem: PostEntity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: PostEntity, newItem: PostEntity): Boolean =
            oldItem == newItem
    }
}
