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
    private val onPostClick: (PostEntity) -> Unit = {},
    private val onLikeClick: (PostEntity) -> Unit = {},
    private val onCommentClick: (PostEntity) -> Unit = {}
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
            bindAuthor(post)
            bindPostContent(post)
            bindFooter(post)
            binding.root.setOnClickListener { onPostClick(post) }
        }

        private fun bindAuthor(post: PostEntity) {
            val username = post.authorUsername
            val imageUrl = post.authorImageUrl?.let { ApiConfig.resolveImageUrl(it) }

            binding.textAuthorName.text = if (!username.isNullOrBlank()) username else "Community Member"

            if (!imageUrl.isNullOrBlank()) {
                binding.imageAuthorAvatar.visibility = View.VISIBLE
                Glide.with(binding.imageAuthorAvatar.context)
                    .load(imageUrl)
                    .transform(CircleCrop())
                    .placeholder(R.drawable.bg_avatar_circle)
                    .error(R.drawable.bg_avatar_circle)
                    .into(binding.imageAuthorAvatar)
                binding.textAuthorInitial.visibility = View.INVISIBLE
            } else {
                binding.imageAuthorAvatar.visibility = View.GONE
                binding.textAuthorInitial.visibility = View.VISIBLE
                val initial = if (!username.isNullOrBlank()) username.first().uppercaseChar().toString() else "?"
                binding.textAuthorInitial.text = initial
            }
        }

        private fun bindPostContent(post: PostEntity) {
            binding.textPostType.text = post.typeBadge
            binding.textContent.text = post.content

            if (!post.plantName.isNullOrBlank()) {
                binding.textPlantName.text = post.plantName
                binding.textPlantName.visibility = View.VISIBLE
            } else {
                binding.textPlantName.visibility = View.GONE
            }

            binding.textTimestamp.text = post.createdAt.take(10)

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
        }

        private fun bindFooter(post: PostEntity) {
            val heartIcon = if (post.isLikedByMe) {
                R.drawable.ic_heart_filled
            } else {
                R.drawable.ic_heart_outline
            }
            binding.btnLike.setImageResource(heartIcon)
            binding.btnLike.setOnClickListener { onLikeClick(post) }

            binding.textLikes.text = binding.root.context.getString(
                R.string.label_likes_count, post.likesCount
            )

            binding.btnComment.setOnClickListener { onCommentClick(post) }
            binding.textComments.setOnClickListener { onCommentClick(post) }
            binding.textComments.text = binding.root.context.getString(
                R.string.label_comments_count, post.commentsCount
            )
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<PostEntity>() {
        override fun areItemsTheSame(oldItem: PostEntity, newItem: PostEntity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: PostEntity, newItem: PostEntity): Boolean =
            oldItem == newItem
    }
}
