package com.example.rootsharemobile.ui.fragment.home

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rootsharemobile.databinding.BottomSheetCommentsBinding
import com.example.rootsharemobile.ui.adapter.CommentsAdapter
import com.example.rootsharemobile.ui.viewmodel.CommentsViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CommentsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCommentsBinding? = null
    private val binding get() = _binding!!

    // Scoped to this dialog — destroyed when the sheet is dismissed.
    private val commentsViewModel: CommentsViewModel by viewModels()

    private lateinit var adapter: CommentsAdapter

    companion object {
        private const val ARG_POST_ID = "post_id"
        private const val ARG_TOKEN  = "token"

        fun newInstance(postId: String, token: String) = CommentsBottomSheet().apply {
            arguments = Bundle().apply {
                putString(ARG_POST_ID, postId)
                putString(ARG_TOKEN, token)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?: return@setOnShowListener
            BottomSheetBehavior.from(sheet).apply {
                state = BottomSheetBehavior.STATE_EXPANDED
                skipCollapsed = true
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetCommentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val postId = arguments?.getString(ARG_POST_ID) ?: return
        val token  = arguments?.getString(ARG_TOKEN)  ?: return

        setupRecyclerView()
        setupSendButton(postId, token)
        binding.btnClose.setOnClickListener { dismiss() }
        observeViewModel()
        commentsViewModel.loadComments(token, postId)
    }

    private fun setupRecyclerView() {
        adapter = CommentsAdapter()
        binding.recyclerComments.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@CommentsBottomSheet.adapter
        }
    }

    private fun setupSendButton(postId: String, token: String) {
        binding.btnSendComment.setOnClickListener {
            val content = binding.editComment.text?.toString()?.trim() ?: return@setOnClickListener
            if (content.isBlank()) return@setOnClickListener
            commentsViewModel.addComment(token, postId, content)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            commentsViewModel.comments.collectLatest { comments ->
                adapter.submitList(comments)
                val isEmpty = comments.isEmpty() && !commentsViewModel.isLoading.value
                binding.textEmptyComments.visibility = if (isEmpty) View.VISIBLE else View.GONE
                // Scroll to newest comment at bottom
                if (comments.isNotEmpty()) {
                    binding.recyclerComments.post {
                        binding.recyclerComments.smoothScrollToPosition(comments.size - 1)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            commentsViewModel.isLoading.collectLatest { loading ->
                binding.progressComments.visibility = if (loading) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            commentsViewModel.commentPosted.collectLatest { posted ->
                if (posted) {
                    binding.editComment.setText("")
                    commentsViewModel.clearCommentPosted()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            commentsViewModel.error.collectLatest { message ->
                if (!message.isNullOrBlank()) {
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                    commentsViewModel.clearError()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
