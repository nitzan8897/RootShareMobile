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
import com.example.rootsharemobile.databinding.DialogNewChatBinding
import com.example.rootsharemobile.ui.screens.chat.adapter.UserSearchAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NewChatDialogFragment : DialogFragment() {

    private var _binding: DialogNewChatBinding? = null
    private val binding get() = _binding!!

    private val chatViewModel: ChatViewModel by activityViewModels()

    private lateinit var userSearchAdapter: UserSearchAdapter
    private var searchJob: Job? = null

    var onChatCreated: ((chatId: String) -> Unit)? = null

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
        _binding = DialogNewChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearch()
        setupCloseButton()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        userSearchAdapter = UserSearchAdapter { user ->
            chatViewModel.createNewChat(user.id) { chatId ->
                if (chatId != null) {
                    onChatCreated?.invoke(chatId)
                    dismiss()
                }
            }
        }
        binding.searchResultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userSearchAdapter
        }
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim() ?: ""
                // Debounce search
                searchJob?.cancel()
                if (query.length < 2) {
                    chatViewModel.clearSearchResults()
                    binding.emptyText.isVisible = true
                    binding.noResultsText.isVisible = false
                    return
                }
                binding.emptyText.isVisible = false
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(300) // debounce
                    chatViewModel.searchUsers(query)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.searchInput.requestFocus()
    }

    private fun setupCloseButton() {
        binding.closeButton.setOnClickListener {
            chatViewModel.clearSearchResults()
            dismiss()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    chatViewModel.searchResults.collect { results ->
                        userSearchAdapter.submitList(results)
                        val query = binding.searchInput.text?.toString()?.trim() ?: ""
                        binding.noResultsText.isVisible =
                            results.isEmpty() && query.length >= 2 && !chatViewModel.isSearching.value
                        binding.emptyText.isVisible = results.isEmpty() && query.length < 2
                        binding.searchResultsRecyclerView.isVisible = results.isNotEmpty()
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
