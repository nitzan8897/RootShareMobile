package com.example.rootsharemobile.ui.screens.chat

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rootsharemobile.R
import com.example.rootsharemobile.data.remote.ConnectionStatus
import com.example.rootsharemobile.databinding.FragmentChatListBinding
import com.example.rootsharemobile.ui.screens.chat.adapter.ChatAdapter
import kotlinx.coroutines.launch

class ChatListFragment : Fragment() {

    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!

    private val chatViewModel: ChatViewModel by activityViewModels()

    private lateinit var chatAdapter: ChatAdapter
    private var searchQuery: String = ""

    var onChatClick: ((chatId: String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearchBar()
        setupFab()
        observeViewModel()
        chatViewModel.connect()

        // Seed sample data for testing when no chats loaded yet
        if (chatViewModel.chats.value.isEmpty()) {
            chatAdapter.submitList(sampleChats)
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter { chatId ->
            onChatClick?.invoke(chatId)
        }
        binding.chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chatAdapter
        }
    }

    private fun setupFab() {
        binding.fabNewChat.setOnClickListener { view ->
            val popup = PopupMenu(requireContext(), view)
            popup.menu.add(0, 1, 0, "New Chat")
            popup.menu.add(0, 2, 1, "New Group Chat")
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        val dialog = NewChatDialogFragment()
                        dialog.onChatCreated = { chatId ->
                            chatViewModel.refreshChats()
                            onChatClick?.invoke(chatId)
                        }
                        dialog.show(childFragmentManager, "NewChatDialog")
                        true
                    }
                    2 -> {
                        val dialog = NewGroupChatDialogFragment()
                        dialog.onGroupCreated = { chatId ->
                            chatViewModel.refreshChats()
                            onChatClick?.invoke(chatId)
                        }
                        dialog.show(childFragmentManager, "NewGroupChatDialog")
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun setupSearchBar() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString() ?: ""
                applyFilter()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    chatViewModel.chats.collect {
                        applyFilter()
                    }
                }

                launch {
                    chatViewModel.isLoading.collect { isLoading ->
                        updateLoadingState(isLoading)
                    }
                }

                launch {
                    chatViewModel.connectionStatus.collect { status ->
                        updateConnectionIndicator(status)
                    }
                }
            }
        }
    }

    private fun applyFilter() {
        val chats = chatViewModel.chats.value
        val filtered = if (searchQuery.isBlank()) {
            chats
        } else {
            chats.filter {
                it.participantName.contains(searchQuery, ignoreCase = true) ||
                    it.lastMessage.contains(searchQuery, ignoreCase = true)
            }
        }
        chatAdapter.submitList(filtered)
        updateEmptyState(filtered.isEmpty() && !chatViewModel.isLoading.value)
    }

    private fun updateLoadingState(isLoading: Boolean) {
        val hasChats = chatAdapter.currentList.isNotEmpty()
        binding.loadingSpinner.isVisible = isLoading && !hasChats
        if (isLoading && !hasChats) {
            binding.emptyStateText.isVisible = false
            binding.chatRecyclerView.isVisible = false
        } else {
            binding.chatRecyclerView.isVisible = true
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateText.isVisible = isEmpty
        binding.chatRecyclerView.isVisible = !isEmpty
    }

    private fun updateConnectionIndicator(status: ConnectionStatus) {
        val colorRes = when (status) {
            ConnectionStatus.CONNECTED -> R.color.emerald_500
            ConnectionStatus.CONNECTING -> R.color.gray_400
            ConnectionStatus.DISCONNECTED -> android.R.color.holo_red_dark
        }
        val label = when (status) {
            ConnectionStatus.CONNECTED -> "Online"
            ConnectionStatus.CONNECTING -> "Connecting..."
            ConnectionStatus.DISCONNECTED -> "Offline"
        }
        binding.connectionDot.background.setTint(
            ContextCompat.getColor(requireContext(), colorRes)
        )
        binding.connectionLabel.text = label
    }

    override fun onResume() {
        super.onResume()
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
    }

    override fun onPause() {
        super.onPause()
        @Suppress("DEPRECATION")
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
