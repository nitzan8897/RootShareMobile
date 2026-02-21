package com.example.rootsharemobile.ui.screens.chat

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rootsharemobile.R
import com.example.rootsharemobile.data.local.db.entity.ChatEntity
import com.example.rootsharemobile.databinding.DialogGroupInfoBinding
import com.example.rootsharemobile.ui.screens.chat.adapter.GroupMemberAdapter
import com.example.rootsharemobile.ui.screens.chat.adapter.GroupMemberItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class GroupInfoDialogFragment : DialogFragment() {

    private var _binding: DialogGroupInfoBinding? = null
    private val binding get() = _binding!!

    private val chatViewModel: ChatViewModel by activityViewModels()

    private lateinit var memberAdapter: GroupMemberAdapter
    private var chatId: String = ""

    private var allMembers: List<GroupMemberItem> = emptyList()
    private var displayedCount = PAGE_SIZE
    private var searchQuery = ""

    var onNavigateToChat: ((chatId: String) -> Unit)? = null
    var onGroupLeft: (() -> Unit)? = null
    var onGroupDeleted: (() -> Unit)? = null

    override fun getTheme(): Int = R.style.Theme_RootShareMobile_FullScreenDialog

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        @Suppress("DEPRECATION")
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogGroupInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chatId = arguments?.getString(ARG_CHAT_ID) ?: return

        setupRecyclerView()
        setupCloseButton()
        setupSearchToggle()
        setupActionButtons()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        memberAdapter = GroupMemberAdapter(
            onMemberClick = { member -> handleMemberClick(member) },
            onMemberLongClick = { _ -> false }
        )
        binding.membersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = memberAdapter
        }
    }

    private fun setupCloseButton() {
        binding.closeButton.setOnClickListener { dismiss() }
    }

    private fun setupSearchToggle() {
        binding.searchMembersButton.setOnClickListener {
            val isVisible = binding.memberSearchInput.isVisible
            binding.memberSearchInput.isVisible = !isVisible
            if (isVisible) {
                binding.memberSearchInput.text.clear()
                searchQuery = ""
                updateMemberList()
            }
        }
        binding.memberSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim()?.lowercase() ?: ""
                displayedCount = PAGE_SIZE
                updateMemberList()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupActionButtons() {
        binding.leaveGroupButton.setOnClickListener { showLeaveConfirmation() }
        binding.showMoreButton.setOnClickListener {
            displayedCount += PAGE_SIZE
            updateMemberList()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                chatViewModel.currentChatDetail.collect { chatResponse ->
                    chatResponse ?: return@collect
                    updateUI(chatResponse)
                }
            }
        }
    }

    private fun updateUI(chat: ChatEntity) {
        // Group info header
        val groupName = chat.name.ifBlank { "Group" }
        binding.groupNameText.text = groupName
        binding.groupInitial.text = groupName.take(1).uppercase()

        val participants = chat.getParticipants()
        binding.memberCountText.text = "${participants.size} members"

        // Build member list sorted A-Z
        allMembers = participants.map { p ->
            GroupMemberItem(
                userId = p.id,
                username = p.username,
                profileImageUrl = p.profileImageUrl
            )
        }.sortedBy { it.username.lowercase() }

        updateMemberList()
    }

    private fun updateMemberList() {
        val filtered = if (searchQuery.isBlank()) allMembers
        else allMembers.filter { it.username.lowercase().contains(searchQuery) }

        val toShow = filtered.take(displayedCount)
        memberAdapter.submitList(toShow)

        binding.showMoreButton.isVisible = toShow.size < filtered.size
    }

    private fun handleMemberClick(member: GroupMemberItem) {
        val currentUserId = chatViewModel.getCurrentUserId() ?: ""
        if (member.userId == currentUserId) return

        chatViewModel.createNewChat(member.userId) { newChatId ->
            if (newChatId != null) {
                dismiss()
                onNavigateToChat?.invoke(newChatId)
            }
        }
    }

    private fun showLeaveConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Leave Group")
            .setMessage("Are you sure you want to leave this group?")
            .setPositiveButton("Leave") { _, _ ->
                chatViewModel.leaveGroup(chatId) { success ->
                    if (success) {
                        dismiss()
                        onGroupLeft?.invoke()
                    } else if (isAdded) {
                        Toast.makeText(requireContext(), "Failed to leave group", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CHAT_ID = "chat_id"
        private const val PAGE_SIZE = 10

        fun newInstance(chatId: String): GroupInfoDialogFragment {
            return GroupInfoDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CHAT_ID, chatId)
                }
            }
        }
    }
}
