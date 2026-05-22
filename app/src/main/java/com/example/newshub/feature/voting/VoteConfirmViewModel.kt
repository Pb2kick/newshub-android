package com.example.newshub.feature.voting

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newshub.R
import com.example.newshub.SupabaseService
import com.example.newshub.UiErrorMapper
import com.example.newshub.core.session.SessionStore
import com.example.newshub.feature.auth.data.AuthRepository
import com.example.newshub.feature.auth.data.SupabaseAuthRepository
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
    private val supabaseService: SupabaseService = SupabaseService(),
    private val authRepository: AuthRepository = SupabaseAuthRepository()
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
            val email = when (val authResult = authRepository.fetchAuthUser(accessToken)) {
                is ApiResult.Success -> authResult.data.email
                is ApiResult.Failure -> null
            }

            when (val result = supabaseService.castVote(
                electionId = electionId,
                candidateId = candidateId,
                authUserId = userId,
                email = email,
                accessToken = accessToken
            )) {
                is ApiResult.Success -> {
                    val payload = result.data
                    if (payload.success) {
                        _uiState.value = VoteConfirmUiState(
                            isSubmitting = false,
                            receiptId = payload.receiptId,
                            message = payload.message
                        )
                    } else {
                        _uiState.value = VoteConfirmUiState(
                            isSubmitting = false,
                            message = payload.message.ifBlank { null },
                            messageRes = voteReasonMessage(payload.reason)
                        )
                    }
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

    private fun voteReasonMessage(reason: String): Int {
        return when (reason.uppercase()) {
            "ALREADY_VOTED" -> R.string.vote_error_already_voted
            "NOT_VERIFIED" -> R.string.vote_error_not_verified
            "NOT_ACTIVE" -> R.string.vote_error_not_active
            "PROFILE_MISSING" -> R.string.vote_error_profile_missing
            else -> R.string.vote_error_generic
        }
    }
}
