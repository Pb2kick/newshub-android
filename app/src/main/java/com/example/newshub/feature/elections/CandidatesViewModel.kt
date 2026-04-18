package com.example.newshub.feature.elections

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newshub.BackendService
import com.example.newshub.CandidateRecord
import com.example.newshub.R
import com.example.newshub.UiErrorMapper
import com.example.newshub.network.ApiResult
import kotlinx.coroutines.launch

data class CandidatesUiState(
    val isLoading: Boolean = false,
    val items: List<CandidateRecord> = emptyList(),
    val emptyMessageRes: Int? = null,
    val messageRes: Int? = null
)

class CandidatesViewModel(
    private val backendService: BackendService = BackendService()
) : ViewModel() {

    private val _uiState = MutableLiveData(CandidatesUiState())
    val uiState: LiveData<CandidatesUiState> = _uiState

    fun load(electionId: String) {
        _uiState.value = _uiState.value?.copy(isLoading = true)
        viewModelScope.launch {
            when (val result = backendService.fetchCandidates(electionId)) {
                is ApiResult.Success -> {
                    _uiState.value = CandidatesUiState(
                        isLoading = false,
                        items = result.data,
                        emptyMessageRes = if (result.data.isEmpty()) R.string.candidates_empty else null
                    )
                }

                is ApiResult.Failure -> {
                    _uiState.value = CandidatesUiState(
                        isLoading = false,
                        items = emptyList(),
                        emptyMessageRes = R.string.candidates_empty,
                        messageRes = UiErrorMapper.toMessageRes(result.error)
                    )
                }
            }
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value?.copy(messageRes = null)
    }
}

