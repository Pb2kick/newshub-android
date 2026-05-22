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
    val messageRes: Int? = null
)

class CommentsViewModel(
    private val sessionStore: SessionStore,
    private val supabaseService: SupabaseService = SupabaseService()
) : ViewModel() {

    private val _uiState = MutableLiveData(CommentsUiState())
    val uiState: LiveData<CommentsUiState> = _uiState

    private var articleId: String = ""

    fun load(articleId: String) {
        this.articleId = articleId
        if (articleId.isBlank()) return
        val token = sessionStore.getAccessToken() ?: return

        _uiState.value = _uiState.value?.copy(isLoading = true)
        viewModelScope.launch {
            when (val result = supabaseService.fetchComments(articleId, token)) {
                is ApiResult.Success -> {
                    _uiState.value = CommentsUiState(isLoading = false, comments = result.data)
                }
                is ApiResult.Failure -> {
                    _uiState.value = CommentsUiState(
                        isLoading = false,
                        messageRes = UiErrorMapper.toMessageRes(result.error)
                    )
                }
            }
        }
    }

    fun postComment(content: String, displayName: String) {
        val trimmed = content.trim()
        if (trimmed.isBlank() || articleId.isBlank()) return

        val userId = sessionStore.getUserId()
        val token = sessionStore.getAccessToken()
        if (userId.isNullOrBlank() || token.isNullOrBlank()) {
            _uiState.value = _uiState.value?.copy(messageRes = R.string.error_unauthorized)
            return
        }

        val resolvedName = displayName.ifBlank { "NewsHub User" }

        viewModelScope.launch {
            when (
                val result = supabaseService.postComment(
                    articleId = articleId,
                    userId = userId,
                    displayName = resolvedName,
                    content = trimmed,
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
