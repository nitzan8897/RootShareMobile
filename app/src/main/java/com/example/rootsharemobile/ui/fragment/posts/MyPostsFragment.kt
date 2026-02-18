package com.example.rootsharemobile.ui.fragment.posts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.rootsharemobile.R
import com.example.rootsharemobile.data.local.db.entity.PostEntity
import com.example.rootsharemobile.databinding.FragmentMyPostsBinding
import com.example.rootsharemobile.ui.adapter.GridCardAdapter
import com.example.rootsharemobile.ui.adapter.toGridCardItem
import com.example.rootsharemobile.ui.viewmodel.AuthViewModel
import com.example.rootsharemobile.ui.viewmodel.MyPostsViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MyPostsFragment : Fragment() {

    private var _binding: FragmentMyPostsBinding? = null
    private val binding get() = _binding!!

    private val postsViewModel: MyPostsViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()

    private lateinit var gridAdapter: GridCardAdapter

    private var postsList: List<PostEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyPostsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
        setupSwipeRefresh()
        fetchInitialData()
    }

    private fun setupRecyclerView() {
        gridAdapter = GridCardAdapter { item ->
            val post = postsList.find { it.id == item.id } ?: return@GridCardAdapter
            showPostActionsDialog(post)
        }

        binding.recyclerPosts.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = gridAdapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.emerald_500)
        binding.swipeRefresh.setOnRefreshListener {
            lifecycleScope.launch {
                val token = authViewModel.getAccessToken() ?: run {
                    binding.swipeRefresh.isRefreshing = false
                    return@launch
                }
                postsViewModel.refresh(token)
            }
        }

        binding.btnRetry.setOnClickListener { fetchInitialData() }
    }

    private fun observeViewModel() {
        postsViewModel.userPosts.observe(viewLifecycleOwner) { posts ->
            postsList = posts
            gridAdapter.submitList(posts.map { it.toGridCardItem() })
            val hasPosts = posts.isNotEmpty()
            binding.recyclerPosts.visibility = if (hasPosts) View.VISIBLE else View.GONE
            binding.layoutEmpty.visibility   = if (hasPosts) View.GONE    else View.VISIBLE
        }

        postsViewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is MyPostsViewModel.PostsUiState.Loading -> {
                    binding.progress.visibility    = View.VISIBLE
                    binding.layoutError.visibility = View.GONE
                }
                is MyPostsViewModel.PostsUiState.Success -> {
                    binding.progress.visibility       = View.GONE
                    binding.layoutError.visibility    = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                }
                is MyPostsViewModel.PostsUiState.Error -> {
                    binding.progress.visibility       = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    binding.textError.text            = state.message
                    binding.layoutError.visibility    = View.VISIBLE
                }
                else -> binding.progress.visibility = View.GONE
            }
        }

        postsViewModel.snackMessage.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrBlank()) {
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
                postsViewModel.clearSnackMessage()
            }
        }
    }

    private fun showPostActionsDialog(post: PostEntity) {
        val options = arrayOf(
            getString(R.string.dialog_edit_post_title),
            getString(R.string.dialog_delete_post_title)
        )
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_post_actions_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditPostDialog(post)
                    1 -> showDeletePostDialog(post)
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun showEditPostDialog(post: PostEntity) {
        val editText = EditText(requireContext()).apply {
            setText(post.content)
            setPadding(64, 32, 64, 32)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_edit_post_title)
            .setView(editText)
            .setPositiveButton(R.string.btn_save_changes) { _, _ ->
                val newContent = editText.text.toString().trim()
                if (newContent.isNotBlank() && newContent != post.content) {
                    lifecycleScope.launch {
                        val token = authViewModel.getAccessToken() ?: return@launch
                        postsViewModel.updatePost(token, post.id, newContent)
                    }
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun showDeletePostDialog(post: PostEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_post_title)
            .setMessage(R.string.dialog_delete_post_message)
            .setPositiveButton(R.string.btn_confirm_delete) { _, _ ->
                lifecycleScope.launch {
                    val token = authViewModel.getAccessToken() ?: return@launch
                    postsViewModel.deletePost(token, post.id)
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun fetchInitialData() {
        lifecycleScope.launch {
            val token = authViewModel.getAccessToken() ?: return@launch
            postsViewModel.loadPosts(token)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
