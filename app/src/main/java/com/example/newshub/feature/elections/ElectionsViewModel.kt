package com.example.newshub.feature.elections

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newshub.BackendService
import com.example.newshub.ElectionRecord
import com.example.newshub.R
import com.example.newshub.UiErrorMapper
import com.example.newshub.network.ApiResult
import kotlinx.coroutines.launch

data class ElectionsUiState(
    val isLoading: Boolean = false,
    val items: List<ElectionRecord> = emptyList(),
    val emptyMessageRes: Int? = null,
    val messageRes: Int? = null
)

class ElectionsViewModel(
    private val backendService: BackendService = BackendService()
) : ViewModel() {

    private val _uiState = MutableLiveData(ElectionsUiState())
    val uiState: LiveData<ElectionsUiState> = _uiState

    fun load() {
        _uiState.value = _uiState.value?.copy(isLoading = true)
        viewModelScope.launch {
            when (val result = backendService.fetchElections()) {
                is ApiResult.Success -> {
                    _uiState.value = ElectionsUiState(
                        isLoading = false,
                        items = result.data,
                        emptyMessageRes = if (result.data.isEmpty()) R.string.elections_empty else null
                    )
                }

                is ApiResult.Failure -> {
                    _uiState.value = ElectionsUiState(
                        isLoading = false,
                        items = emptyList(),
                        emptyMessageRes = R.string.elections_empty,
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

