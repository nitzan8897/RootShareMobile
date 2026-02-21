package com.example.rootsharemobile.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rootsharemobile.data.local.db.AppDatabase
import com.example.rootsharemobile.data.model.Comment
import com.example.rootsharemobile.data.repository.CommentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CommentsViewModel(application: Application) : AndroidViewModel(application) {

    private val commentRepository = CommentRepository()
    private val postDao = AppDatabase.getInstance(application).postDao()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Signals the BottomSheet to clear the input field after a successful post.
    private val _commentPosted = MutableStateFlow(false)
    val commentPosted: StateFlow<Boolean> = _commentPosted.asStateFlow()

    fun loadComments(token: String, postId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = commentRepository.getComments(token, postId)
            _isLoading.value = false
            result.fold(
                onSuccess = { _comments.value = it },
                onFailure = { _error.value = it.message }
            )
        }
    }

    fun addComment(token: String, postId: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            val previousCount = _comments.value.size
            val result = commentRepository.createComment(token, postId, content)
            // Always reload from GET — even when POST reports failure, the server may
            // have saved the comment and dropped the connection before responding.
            val freshComments = commentRepository.getComments(token, postId)
                .getOrNull()
            if (freshComments != null) {
                _comments.value = freshComments
            }
            val commentAppearedInList = (freshComments?.size ?: 0) > previousCount
            if (result.isSuccess || commentAppearedInList) {
                postDao.incrementCommentsCount(postId)
                _commentPosted.value = true
            } else {
                _error.value = result.exceptionOrNull()?.message
            }
            _isLoading.value = false
        }
    }

    fun clearCommentPosted() { _commentPosted.value = false }
    fun clearError() { _error.value = null }
    fun clearComments() { _comments.value = emptyList() }
}
