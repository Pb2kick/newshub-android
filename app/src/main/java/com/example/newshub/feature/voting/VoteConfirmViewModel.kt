package com.example.newshub.feature.voting

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newshub.BackendService
import com.example.newshub.R
import com.example.newshub.UiErrorMapper
import com.example.newshub.core.session.SessionStore
import com.example.newshub.network.ApiResult
import kotlinx.coroutines.launch

data class VoteConfirmUiState(
    val isSubmitting: Boolean = false,
    val receiptId: String? = null,
    val message: String? = null,
    val messageRes: Int? = null
)

class VoteConfirmViewModel(
    private val sessionStore: SessionStore,
    private val backendService: BackendService = BackendService()
) : ViewModel() {

    private val _uiState = MutableLiveData(VoteConfirmUiState())
    val uiState: LiveData<VoteConfirmUiState> = _uiState

    fun submitVote(electionId: String, candidateId: String) {
        val userId = sessionStore.getUserId()
        val accessToken = sessionStore.getAccessToken()
        if (userId.isNullOrBlank() || accessToken.isNullOrBlank()) {
            _uiState.value = _uiState.value?.copy(messageRes = R.string.supabase_session_missing)
            return
        }

        _uiState.value = _uiState.value?.copy(isSubmitting = true)
        viewModelScope.launch {
            when (val result = backendService.castVote(electionId, candidateId, userId, accessToken)) {
                is ApiResult.Success -> {
                    _uiState.value = VoteConfirmUiState(
                        isSubmitting = false,
                        receiptId = result.data.receiptId,
                        message = result.data.message
                    )
                }

                is ApiResult.Failure -> {
                    _uiState.value = VoteConfirmUiState(
                        isSubmitting = false,
                        messageRes = UiErrorMapper.toMessageRes(result.error)
                    )
                }
            }
        }
    }

    fun consumeMessageRes() {
        _uiState.value = _uiState.value?.copy(messageRes = null)
    }
}

