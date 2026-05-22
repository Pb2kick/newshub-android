package com.example.newshub.feature.notifications

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

data class NotificationsUiState(
    val isLoading: Boolean = false,
    val allItems: List<NotificationItem> = emptyList(),
    val items: List<NotificationItem> = emptyList(),
    val unreadOnly: Boolean = false,
    val unreadCount: Int = 0,
    val messageRes: Int? = null
)

class NotificationsViewModel(
    private val sessionStore: SessionStore,
    private val supabaseService: SupabaseService = SupabaseService()
) : ViewModel() {

    private val _uiState = MutableLiveData(NotificationsUiState())
    val uiState: LiveData<NotificationsUiState> = _uiState

    fun refresh() {
        val userId = sessionStore.getUserId()
        val token = sessionStore.getAccessToken()
        if (userId.isNullOrBlank() || token.isNullOrBlank()) {
            _uiState.value = _uiState.value?.copy(messageRes = R.string.error_unauthorized)
            return
        }

        _uiState.value = _uiState.value?.copy(isLoading = true)
        viewModelScope.launch {
            when (val result = supabaseService.fetchNotifications(userId, token)) {
                is ApiResult.Success -> {
                    val current = _uiState.value ?: NotificationsUiState()
                    val unreadCount = result.data.count { !it.isRead }
                    val visible = applyUnreadFilter(result.data, current.unreadOnly)
                    _uiState.value = current.copy(
                        isLoading = false,
                        allItems = result.data,
                        items = visible,
                        unreadCount = unreadCount
                    )
                }
                is ApiResult.Failure -> {
                    _uiState.value = NotificationsUiState(
                        isLoading = false,
                        messageRes = UiErrorMapper.toMessageRes(result.error)
                    )
                }
            }
        }
    }

    fun setUnreadOnly(unreadOnly: Boolean) {
        val current = _uiState.value ?: return
        _uiState.value = current.copy(
            unreadOnly = unreadOnly,
            items = applyUnreadFilter(current.allItems, unreadOnly)
        )
    }

    fun markAllRead() {
        val userId = sessionStore.getUserId() ?: return
        val token = sessionStore.getAccessToken() ?: return
        viewModelScope.launch {
            when (val result = supabaseService.markAllNotificationsRead(userId, token)) {
                is ApiResult.Success -> refresh()
                is ApiResult.Failure -> showError(result)
            }
        }
    }

    fun deleteNotification(id: String) {
        val token = sessionStore.getAccessToken() ?: return
        viewModelScope.launch {
            when (val result = supabaseService.deleteNotification(id, token)) {
                is ApiResult.Success -> refresh()
                is ApiResult.Failure -> showError(result)
            }
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value?.copy(messageRes = null)
    }

    private fun applyUnreadFilter(
        items: List<NotificationItem>,
        unreadOnly: Boolean
    ): List<NotificationItem> {
        return if (unreadOnly) items.filter { !it.isRead } else items
    }

    private fun showError(result: ApiResult.Failure) {
        _uiState.value = _uiState.value?.copy(messageRes = UiErrorMapper.toMessageRes(result.error))
    }
}
