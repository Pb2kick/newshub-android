package com.example.newshub.feature.news

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newshub.R
import com.example.newshub.SupabaseService
import com.example.newshub.UiErrorMapper
import com.example.newshub.core.session.SessionStore
import com.example.newshub.network.ApiResult
import kotlinx.coroutines.launch

data class CommentsUiState(
    val isLoading: Boolean = false,
    val comments: List<CommentItem> = emptyList(),
    val messageRes: Int? = null,
    val currentUserAvatar: String? = null,
    val currentUserFullName: String? = null
)

class CommentsViewModel(
    private val sessionStore: SessionStore,
    private val supabaseService: SupabaseService = SupabaseService()
) : ViewModel() {

    private val _uiState = MutableLiveData(CommentsUiState())
    val uiState: LiveData<CommentsUiState> = _uiState

    private var articleId: String = ""

    init {
        refreshCurrentUserProfile()
    }

    private fun refreshCurrentUserProfile() {
        val userId = sessionStore.getUserId()
        val token = sessionStore.getAccessToken()
        if (userId != null && token != null) {
            viewModelScope.launch {
                val profileResult = supabaseService.fetchProfile(userId, token)
                if (profileResult is ApiResult.Success) {
                    _uiState.value = _uiState.value?.copy(
                        currentUserAvatar = profileResult.data?.avatarUrl,
                        currentUserFullName = profileResult.data?.fullName?.ifBlank { null }
                    )
                }
            }
        }
    }

    fun load(articleId: String) {
        this.articleId = articleId
        if (articleId.isBlank()) return
        val token = sessionStore.getAccessToken() ?: return

        _uiState.value = _uiState.value?.copy(isLoading = true)
        viewModelScope.launch {
            if (_uiState.value?.currentUserFullName == null) {
                refreshCurrentUserProfile()
            }

            when (val result = supabaseService.fetchComments(articleId, token)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value?.copy(isLoading = false, comments = result.data)
                }
                is ApiResult.Failure -> {
                    _uiState.value = _uiState.value?.copy(
                        isLoading = false,
                        messageRes = UiErrorMapper.toMessageRes(result.error)
                    )
                }
            }
        }
    }

    fun postComment(content: String) {
        val trimmed = content.trim()
        if (trimmed.isBlank() || articleId.isBlank()) return

        val userId = sessionStore.getUserId()
        val token = sessionStore.getAccessToken()
        if (userId.isNullOrBlank() || token.isNullOrBlank()) {
            _uiState.value = _uiState.value?.copy(messageRes = R.string.error_unauthorized)
            return
        }

        val displayName = _uiState.value?.currentUserFullName ?: "NewsHub User"
        val avatarUrl = _uiState.value?.currentUserAvatar

        viewModelScope.launch {
            when (
                val result = supabaseService.postComment(
                    articleId = articleId,
                    userId = userId,
                    displayName = displayName,
                    content = trimmed,
                    avatarUrl = avatarUrl,
                    accessToken = token
                )
            ) {
                is ApiResult.Success -> load(articleId)
                is ApiResult.Failure -> {
                    _uiState.value = _uiState.value?.copy(
                        messageRes = UiErrorMapper.toMessageRes(result.error)
                    )
                }
            }
        }
    }

    fun deleteComment(commentId: String) {
        val token = sessionStore.getAccessToken() ?: return
        val userId = sessionStore.getUserId()
        val comment = _uiState.value?.comments?.find { it.id == commentId }
        if (comment == null || userId == null || comment.userId != userId) return

        viewModelScope.launch {
            when (val result = supabaseService.deleteComment(commentId, token)) {
                is ApiResult.Success -> load(articleId)
                is ApiResult.Failure -> {
                    _uiState.value = _uiState.value?.copy(
                        messageRes = UiErrorMapper.toMessageRes(result.error)
                    )
                }
            }
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value?.copy(messageRes = null)
    }

    fun currentUserId(): String? = sessionStore.getUserId()
}
