package com.example.rootsharemobile.ui.fragment.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.rootsharemobile.R
import com.example.rootsharemobile.data.local.db.entity.PostEntity
import com.example.rootsharemobile.data.remote.ApiConfig
import com.example.rootsharemobile.databinding.FragmentPostDetailsBinding
import com.example.rootsharemobile.ui.viewmodel.AuthViewModel
import com.example.rootsharemobile.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Shows the full details of a single community post.
 *
 * Receives the post ID via SafeArgs, observes the post LiveData from Room,
 * and allows the user to like the post and view/add comments.
 */
class PostDetailsFragment : Fragment() {

    private var _binding: FragmentPostDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: PostDetailsFragmentArgs by navArgs()
    private val homeViewModel: HomeViewModel by viewModels()
    private val authViewModel: AuthViewModel by activityViewModels()

    private var currentPost: PostEntity? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        observePost()
    }

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    private fun setupToolbar() {
        binding.toolbar.setNavigationIcon(R.drawable.ic_nav_home)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.toolbar.title = getString(R.string.title_post_details)
    }

    // -------------------------------------------------------------------------
    // Observers
    // -------------------------------------------------------------------------

    private fun observePost() {
        homeViewModel.observePostById(args.postId).observe(viewLifecycleOwner) { post ->
            if (post == null) {
                // Post was deleted - navigate back
                findNavController().navigateUp()
                return@observe
            }
            currentPost = post
            bindPost(post)
        }
    }

    // -------------------------------------------------------------------------
    // UI Binding
    // -------------------------------------------------------------------------

    private fun bindPost(post: PostEntity) {
        bindAuthor(post)
        bindPostContent(post)
        bindEngagement(post)
    }

    private fun bindAuthor(post: PostEntity) {
        val username = post.authorUsername
        val imageUrl = post.authorImageUrl?.let { ApiConfig.resolveImageUrl(it) }

        // Show username or fall back to a placeholder
        binding.textAuthorName.text = if (!username.isNullOrBlank()) username else "Community Member"

        // Avatar: try to load profile image; fall back to letter initial
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

        // Timestamp
        binding.textTimestamp.text = formatDate(post.createdAt)

        // Post type badge
        binding.textPostType.text = post.typeBadge
    }

    private fun bindPostContent(post: PostEntity) {
        // Post content
        binding.textContent.text = post.content

        // Plant name (optional)
        if (!post.plantName.isNullOrBlank()) {
            binding.textPlantName.text = post.plantName
            binding.layoutPlantInfo.visibility = View.VISIBLE
        } else {
            binding.layoutPlantInfo.visibility = View.GONE
        }

        // Post image - shown only when the post has at least one image URL
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

    private fun bindEngagement(post: PostEntity) {
        // Like button: filled red heart vs outline gray heart
        val heartIcon = if (post.isLikedByMe) {
            R.drawable.ic_heart_filled
        } else {
            R.drawable.ic_heart_outline
        }
        binding.btnLike.setImageResource(heartIcon)
        binding.btnLike.setOnClickListener {
            lifecycleScope.launch {
                val token = authViewModel.getAccessToken() ?: return@launch
                homeViewModel.toggleLike(token, post)
            }
        }

        // Like count
        binding.textLikes.text = getString(R.string.label_likes_count, post.likesCount)

        // Comment button + count - opens the comments sheet
        binding.btnComment.setOnClickListener { openCommentsSheet(post) }
        binding.textComments.setOnClickListener { openCommentsSheet(post) }
        binding.textComments.text = getString(R.string.label_comments_count, post.commentsCount)
    }

    private fun openCommentsSheet(post: PostEntity) {
        lifecycleScope.launch {
            val token = authViewModel.getAccessToken() ?: return@launch
            CommentsBottomSheet.newInstance(post.id, token)
                .show(parentFragmentManager, "comments_${post.id}")
        }
    }

    private fun formatDate(isoDate: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val date = sdf.parse(isoDate)
            val out = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            out.format(date!!)
        } catch (e: Exception) {
            isoDate.take(10) // fallback to YYYY-MM-DD
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
