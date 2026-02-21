package com.example.rootsharemobile.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.rootsharemobile.data.local.TokenManager
import com.example.rootsharemobile.data.local.db.AppDatabase
import com.example.rootsharemobile.data.local.db.entity.PlantEntity
import com.example.rootsharemobile.data.local.db.entity.PostEntity
import com.example.rootsharemobile.data.model.CreatePostRequest
import com.example.rootsharemobile.data.model.PostType
import com.example.rootsharemobile.data.model.UpdatePostRequest
import com.example.rootsharemobile.data.repository.PostRepository
import kotlinx.coroutines.launch

class MyPostsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val tokenManager = TokenManager(application)
    private val postRepository = PostRepository(database.postDao())

    private val _currentUserId = MutableLiveData<String>()

    sealed class PostsUiState {
        object Idle : PostsUiState()
        object Loading : PostsUiState()
        object Success : PostsUiState()
        data class Error(val message: String) : PostsUiState()
    }

    sealed class OperationState {
        object Idle : OperationState()
        object Loading : OperationState()
        object Success : OperationState()
        data class Error(val message: String) : OperationState()
    }

    private val _uiState = MutableLiveData<PostsUiState>(PostsUiState.Idle)
    val uiState: LiveData<PostsUiState> = _uiState

    private val _operationState = MutableLiveData<OperationState>(OperationState.Idle)
    val operationState: LiveData<OperationState> = _operationState

    private val _snackMessage = MutableLiveData<String?>(null)
    val snackMessage: LiveData<String?> = _snackMessage

    val userPosts: LiveData<List<PostEntity>> =
        _currentUserId.switchMap { userId ->
            postRepository.observeUserPosts(userId)
        }

    /** User's plants for the "link to plant" dropdown in the post form. */
    val userPlants: LiveData<List<PlantEntity>> =
        _currentUserId.switchMap { userId ->
            database.plantDao().observeUserPlants(userId)
        }

    /** Set the current user ID without triggering a network fetch. Used by HomeFragment. */
    suspend fun initUserId() {
        val user = tokenManager.getUser()
        val userId = user?.id ?: ""
        if (_currentUserId.value != userId) {
            _currentUserId.value = userId
        }
    }

    fun loadPosts(token: String) {
        if (_uiState.value == PostsUiState.Loading) return
        _uiState.value = PostsUiState.Loading
        viewModelScope.launch {
            val user = tokenManager.getUser()
            val userId = user?.id ?: ""
            if (_currentUserId.value != userId) {
                _currentUserId.value = userId
            }
            val result = postRepository.fetchAndStorePosts(token)
            _uiState.value = result.fold(
                onSuccess = { PostsUiState.Success },
                onFailure = { PostsUiState.Error(it.message ?: "Failed to load posts.") }
            )
        }
    }

    fun refresh(token: String) {
        _uiState.value = PostsUiState.Loading
        viewModelScope.launch {
            val user = tokenManager.getUser()
            val userId = user?.id ?: ""
            if (_currentUserId.value != userId) {
                _currentUserId.value = userId
            }
            val result = postRepository.fetchAndStorePosts(token)
            _uiState.value = result.fold(
                onSuccess = { PostsUiState.Success },
                onFailure = { PostsUiState.Error(it.message ?: "Failed to load posts.") }
            )
        }
    }

    fun createPost(
        token: String,
        content: String,
        type: PostType,
        plantId: String?,
        images: List<String>
    ) {
        _operationState.value = OperationState.Loading
        viewModelScope.launch {
            val request = CreatePostRequest(
                plantId = plantId,
                type = type,
                content = content,
                images = images.ifEmpty { null }
            )
            val result = postRepository.createPost(token, request)
            result.fold(
                onSuccess = {
                    _operationState.value = OperationState.Success
                    _snackMessage.value = "Post created!"
                },
                onFailure = {
                    _operationState.value = OperationState.Error(it.message ?: "Could not create post.")
                }
            )
        }
    }

    fun updatePost(
        token: String,
        postId: String,
        content: String,
        type: PostType? = null,
        plantId: String? = null,
        images: List<String>? = null
    ) {
        _operationState.value = OperationState.Loading
        viewModelScope.launch {
            val request = UpdatePostRequest(
                content = content,
                type = type,
                plantId = plantId,
                images = images
            )
            val result = postRepository.updatePost(token, postId, request)
            result.fold(
                onSuccess = {
                    _operationState.value = OperationState.Success
                    _snackMessage.value = "Post updated."
                },
                onFailure = {
                    _operationState.value = OperationState.Error(it.message ?: "Could not update post.")
                }
            )
        }
    }

    fun deletePost(token: String, postId: String) {
        _operationState.value = OperationState.Loading
        viewModelScope.launch {
            val result = postRepository.deletePost(token, postId)
            result.fold(
                onSuccess = {
                    _operationState.value = OperationState.Success
                    _snackMessage.value = "Post deleted."
                },
                onFailure = {
                    _operationState.value = OperationState.Error(it.message ?: "Could not delete post.")
                }
            )
        }
    }

    fun clearSnackMessage() {
        _snackMessage.value = null
    }

    fun resetOperationState() {
        _operationState.value = OperationState.Idle
    }
}
