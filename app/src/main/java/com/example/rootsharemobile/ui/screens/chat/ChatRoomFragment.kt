package com.example.rootsharemobile.ui.screens.chat

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rootsharemobile.R
import com.example.rootsharemobile.databinding.FragmentChatRoomBinding
import com.example.rootsharemobile.ui.screens.chat.adapter.MessageAdapter
import kotlinx.coroutines.launch

class ChatRoomFragment : Fragment() {

    private var _binding: FragmentChatRoomBinding? = null
    private val binding get() = _binding!!

    private val chatViewModel: ChatViewModel by activityViewModels()
    private val args: ChatRoomFragmentArgs by navArgs()

    private lateinit var messageAdapter: MessageAdapter

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

        chatViewModel.joinRoom(args.chatId)
        updateParticipantName()
    }

    override fun onStart() {
        super.onStart()
        chatViewModel.setInChatRoom(true)
    }

    override fun onStop() {
        super.onStop()
        chatViewModel.setInChatRoom(false)
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter { senderId ->
            chatViewModel.createNewChat(senderId) { newChatId ->
                if (newChatId != null) {
                    navigateToChat(newChatId)
                }
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
            // Pop back to the chat list (removes all chat room entries from stack)
            findNavController().popBackStack(R.id.chatListFragment, false)
        }
        binding.groupInfoTapArea.setOnClickListener {
            showGroupInfo()
        }
    }

    private fun showGroupInfo() {
        val dialog = GroupInfoDialogFragment.newInstance(args.chatId)
        dialog.onNavigateToChat = { newChatId ->
            navigateToChat(newChatId)
        }
        dialog.onGroupLeft = {
            findNavController().popBackStack(R.id.chatListFragment, false)
        }
        dialog.onGroupDeleted = {
            findNavController().popBackStack(R.id.chatListFragment, false)
        }
        dialog.show(childFragmentManager, "GroupInfoDialog")
    }

    private fun navigateToChat(chatId: String) {
        findNavController().navigate(
            ChatRoomFragmentDirections.actionChatRoomFragmentToChatRoomFragment(chatId)
        )
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
        val name = chatViewModel.getParticipantName(args.chatId)
        binding.participantName.text = name
        binding.avatarInitial.text = name.take(1).uppercase()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
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

                launch {
                    chatViewModel.isTyping.collect { isTyping ->
                        binding.typingIndicator.isVisible = isTyping
                    }
                }

                launch {
                    chatViewModel.chats.collect {
                        updateParticipantName()
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onResume() {
        super.onResume()
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        chatViewModel.leaveRoom()
        _binding = null
    }
}
