package com.example.rootsharemobile.ui.screens.chat

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rootsharemobile.R
import com.example.rootsharemobile.data.model.User
import com.example.rootsharemobile.databinding.DialogNewGroupChatBinding
import com.example.rootsharemobile.ui.screens.chat.adapter.UserSearchAdapter
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NewGroupChatDialogFragment : DialogFragment() {

    private var _binding: DialogNewGroupChatBinding? = null
    private val binding get() = _binding!!

    private val chatViewModel: ChatViewModel by activityViewModels()

    private lateinit var userSearchAdapter: UserSearchAdapter
    private var searchJob: Job? = null

    private val selectedUsers = mutableListOf<User>()

    var onGroupCreated: ((chatId: String) -> Unit)? = null

    override fun getTheme(): Int = R.style.Theme_RootShareMobile_FullScreenDialog

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogNewGroupChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearch()
        setupCloseButton()
        setupCreateButton()
        setupGroupNameInput()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        userSearchAdapter = UserSearchAdapter { user ->
            toggleUserSelection(user)
        }
        binding.searchResultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userSearchAdapter
        }
    }

    private fun toggleUserSelection(user: User) {
        if (selectedUsers.any { it.id == user.id }) {
            selectedUsers.removeAll { it.id == user.id }
        } else {
            selectedUsers.add(user)
        }
        updateChips()
        updateCreateButtonState()
    }

    private fun updateChips() {
        binding.selectedChipGroup.removeAllViews()
        binding.selectedUsersScroll.isVisible = selectedUsers.isNotEmpty()
        selectedUsers.forEach { user ->
            val chip = Chip(requireContext()).apply {
                text = user.username
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    toggleUserSelection(user)
                }
            }
            binding.selectedChipGroup.addView(chip)
        }
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim() ?: ""
                searchJob?.cancel()
                if (query.length < 2) {
                    chatViewModel.clearSearchResults()
                    binding.emptyText.isVisible = true
                    binding.noResultsText.isVisible = false
                    return
                }
                binding.emptyText.isVisible = false
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(300)
                    chatViewModel.searchUsers(query)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupGroupNameInput() {
        binding.groupNameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateCreateButtonState()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updateCreateButtonState() {
        val hasName = !binding.groupNameInput.text.isNullOrBlank()
        val hasMembers = selectedUsers.size >= 2
        binding.createGroupButton.isEnabled = hasName && hasMembers
    }

    private fun setupCloseButton() {
        binding.closeButton.setOnClickListener {
            chatViewModel.clearSearchResults()
            dismiss()
        }
    }

    private fun setupCreateButton() {
        binding.createGroupButton.setOnClickListener {
            val name = binding.groupNameInput.text.toString().trim()
            val userIds = selectedUsers.map { it.id }
            binding.createGroupButton.isEnabled = false
            binding.createGroupButton.text = "Creating..."
            chatViewModel.createGroupChat(name, userIds) { chatId ->
                if (chatId != null) {
                    onGroupCreated?.invoke(chatId)
                    dismiss()
                } else {
                    binding.createGroupButton.isEnabled = true
                    binding.createGroupButton.text = "Create Group"
                }
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    chatViewModel.searchResults.collect { results ->
                        // Filter out already-selected users from results
                        val selectedIds = selectedUsers.map { it.id }.toSet()
                        val filtered = results.filter { it.id !in selectedIds }
                        userSearchAdapter.submitList(filtered)
                        val query = binding.searchInput.text?.toString()?.trim() ?: ""
                        binding.noResultsText.isVisible =
                            filtered.isEmpty() && query.length >= 2 && !chatViewModel.isSearching.value
                        binding.emptyText.isVisible = filtered.isEmpty() && query.length < 2
                        binding.searchResultsRecyclerView.isVisible = filtered.isNotEmpty()
                    }
                }

                launch {
                    chatViewModel.isSearching.collect { isSearching ->
                        binding.searchLoading.isVisible = isSearching
                        if (isSearching) {
                            binding.noResultsText.isVisible = false
                        }
                    }
                }
            }
        }
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
        chatViewModel.clearSearchResults()
        _binding = null
    }
}
