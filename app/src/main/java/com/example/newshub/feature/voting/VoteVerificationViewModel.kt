package com.example.newshub.feature.voting

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

data class VoteVerificationUiState(
    val isLoading: Boolean = false,
    val status: String = "",
    val message: String = "",
    val messageRes: Int? = null
)

class VoteVerificationViewModel(
    private val sessionStore: SessionStore,
    private val supabaseService: SupabaseService = SupabaseService()
) : ViewModel() {

    private val _uiState = MutableLiveData(VoteVerificationUiState())
    val uiState: LiveData<VoteVerificationUiState> = _uiState

    fun verify(receiptId: String, defaultMessage: String) {
        val accessToken = sessionStore.getAccessToken()
        if (accessToken.isNullOrBlank()) {
            _uiState.value = VoteVerificationUiState(messageRes = R.string.supabase_session_missing)
            return
        }

        _uiState.value = VoteVerificationUiState(isLoading = true, message = defaultMessage)
        viewModelScope.launch {
            when (val result = supabaseService.verifyVoteReceipt(receiptId, accessToken)) {
                is ApiResult.Success -> {
                    _uiState.value = VoteVerificationUiState(
                        isLoading = false,
                        status = result.data.status,
                        message = result.data.message
                    )
                }

                is ApiResult.Failure -> {
                    _uiState.value = VoteVerificationUiState(
                        isLoading = false,
                        status = getFallbackStatus(defaultMessage),
                        message = defaultMessage,
                        messageRes = UiErrorMapper.toMessageRes(result.error)
                    )
                }
            }
        }
    }

    private fun getFallbackStatus(defaultMessage: String): String {
        return if (defaultMessage.isBlank()) "SUBMITTED" else "PENDING_VERIFICATION"
    }

    fun consumeMessageRes() {
        _uiState.value = _uiState.value?.copy(messageRes = null)
    }
}
