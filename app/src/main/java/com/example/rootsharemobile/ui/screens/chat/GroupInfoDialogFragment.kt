package com.example.rootsharemobile.ui.screens.chat

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rootsharemobile.R
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
        setupEditButtons()
        setupSearchToggle()
        setupActionButtons()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        memberAdapter = GroupMemberAdapter(
            onMemberClick = { member -> handleMemberClick(member) },
            onMemberLongClick = { member -> handleMemberLongClick(member) }
        )
        binding.membersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = memberAdapter
        }
    }

    private fun setupCloseButton() {
        binding.closeButton.setOnClickListener { dismiss() }
    }

    private fun setupEditButtons() {
        binding.editNameButton.setOnClickListener { showRenameDialog() }
        binding.editAvatarButton.setOnClickListener {
            Toast.makeText(requireContext(), "Coming soon", Toast.LENGTH_SHORT).show()
        }
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
        binding.deleteGroupButton.setOnClickListener { showDeleteConfirmation() }
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

    private fun updateUI(chat: com.example.rootsharemobile.data.model.ChatResponse) {
        val currentUserId = chatViewModel.getCurrentUserId() ?: ""
        val adminList = chat.admins.orEmpty()
        val isAdmin = currentUserId in adminList

        // Group info header
        val groupName = chat.name ?: "Group"
        binding.groupNameText.text = groupName
        binding.groupInitial.text = groupName.take(1).uppercase()
        binding.memberCountText.text = "${chat.participants.size} members"

        // Admin-only controls
        binding.editNameButton.isVisible = isAdmin
        binding.editAvatarButton.isVisible = isAdmin
        binding.deleteGroupButton.isVisible = isAdmin

        // Build member list: admins first (A-Z), then non-admins (A-Z)
        allMembers = chat.participants.map { p ->
            GroupMemberItem(
                userId = p.id,
                username = p.username,
                profileImageUrl = p.profileImageUrl,
                isAdmin = p.id in adminList
            )
        }.sortedWith(compareByDescending<GroupMemberItem> { it.isAdmin }.thenBy { it.username.lowercase() })

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

    private fun handleMemberLongClick(member: GroupMemberItem): Boolean {
        val currentUserId = chatViewModel.getCurrentUserId() ?: ""
        val chat = chatViewModel.currentChatDetail.value ?: return false
        val isAdmin = currentUserId in chat.admins.orEmpty()

        if (!isAdmin || member.userId == currentUserId) return false

        val options = arrayOf("Make Admin", "Remove from Group")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(member.username)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showMakeAdminConfirmation(member)
                    1 -> showRemoveMemberConfirmation(member)
                }
            }
            .show()
        return true
    }

    private fun showRenameDialog() {
        val editText = EditText(requireContext()).apply {
            setText(binding.groupNameText.text)
            setPadding(64, 32, 64, 32)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Rename Group")
            .setView(editText)
            .setPositiveButton("Rename") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotBlank()) {
                    chatViewModel.renameGroup(chatId, newName) { success ->
                        if (isAdded) {
                            Toast.makeText(
                                requireContext(),
                                if (success) "Group renamed" else "Failed to rename group",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showMakeAdminConfirmation(member: GroupMemberItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Make Admin")
            .setMessage("Make ${member.username} an admin?")
            .setPositiveButton("Yes") { _, _ ->
                chatViewModel.makeAdmin(chatId, member.userId) { success ->
                    if (isAdded) {
                        Toast.makeText(
                            requireContext(),
                            if (success) "${member.username} is now an admin" else "Failed to make admin",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRemoveMemberConfirmation(member: GroupMemberItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Remove Member")
            .setMessage("Remove ${member.username} from the group?")
            .setPositiveButton("Remove") { _, _ ->
                chatViewModel.removeMember(chatId, member.userId) { success ->
                    if (isAdded) {
                        Toast.makeText(
                            requireContext(),
                            if (success) "${member.username} removed" else "Failed to remove member",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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

    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Group")
            .setMessage("Are you sure you want to delete this group? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                chatViewModel.deleteGroup(chatId) { success ->
                    if (success) {
                        dismiss()
                        onGroupDeleted?.invoke()
                    } else if (isAdded) {
                        Toast.makeText(requireContext(), "Failed to delete group", Toast.LENGTH_SHORT).show()
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
