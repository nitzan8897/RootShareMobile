package com.example.rootsharemobile.ui.screens.chat

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rootsharemobile.databinding.FragmentChatRoomBinding
import com.example.rootsharemobile.ui.screens.chat.adapter.MessageAdapter
import kotlinx.coroutines.launch

class ChatRoomFragment : Fragment() {

    private var _binding: FragmentChatRoomBinding? = null
    private val binding get() = _binding!!

    private val chatViewModel: ChatViewModel by activityViewModels()

    private lateinit var messageAdapter: MessageAdapter
    private var chatId: String = ""

    var onBackClick: (() -> Unit)? = null
    var onNavigateToChat: ((chatId: String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatRoomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupInputBar()
        setupToolbar()
        observeViewModel()

        // Join room
        if (chatId.isNotEmpty()) {
            chatViewModel.joinRoom(chatId)
            updateParticipantName()
        }
    }

    fun setChatId(id: String) {
        chatId = id
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter { senderId ->
            chatViewModel.createNewChat(senderId) { newChatId ->
                if (newChatId != null) onNavigateToChat?.invoke(newChatId)
            }
        }
        binding.messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
    }

    private fun setupToolbar() {
        binding.backButton.setOnClickListener {
            onBackClick?.invoke()
        }
        binding.groupInfoTapArea.setOnClickListener {
            if (chatId.isNotEmpty() && chatViewModel.isGroupChat(chatId)) {
                showGroupInfo()
            }
        }
    }

    private fun showGroupInfo() {
        val dialog = GroupInfoDialogFragment.newInstance(chatId)
        dialog.onNavigateToChat = { newChatId -> onNavigateToChat?.invoke(newChatId) }
        dialog.onGroupLeft = { onBackClick?.invoke() }
        dialog.onGroupDeleted = { onBackClick?.invoke() }
        dialog.show(childFragmentManager, "GroupInfoDialog")
    }

    private fun setupInputBar() {
        binding.messageInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrBlank()
                binding.sendButton.isEnabled = hasText
                binding.sendButton.alpha = if (hasText) 1.0f else 0.5f
                chatViewModel.sendTyping(hasText)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.sendButton.setOnClickListener { performSend() }
    }

    private fun performSend() {
        val content = binding.messageInput.text.toString()
        if (content.isNotBlank()) {
            chatViewModel.sendMessage(content)
            binding.messageInput.text.clear()
            chatViewModel.sendTyping(false)
        }
    }

    private fun updateParticipantName() {
        val name = chatViewModel.getParticipantName(chatId)
        binding.participantName.text = name
        binding.avatarInitial.text = name.take(1).uppercase()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe messages
                launch {
                    chatViewModel.messages.collect { messages ->
                        messageAdapter.submitList(messages) {
                            if (messages.isNotEmpty()) {
                                binding.messagesRecyclerView.post {
                                    binding.messagesRecyclerView.scrollToPosition(messages.size - 1)
                                }
                            }
                        }
                    }
                }

                // Observe typing indicator
                launch {
                    chatViewModel.isTyping.collect { isTyping ->
                        binding.typingIndicator.isVisible = isTyping
                    }
                }

                // Observe chats to update participant name when loaded
                launch {
                    chatViewModel.chats.collect {
                        if (chatId.isNotEmpty()) {
                            updateParticipantName()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        chatViewModel.leaveRoom()
        _binding = null
    }
}
