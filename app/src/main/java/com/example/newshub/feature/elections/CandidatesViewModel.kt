package com.example.newshub.feature.elections

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newshub.SupabaseService
import com.example.newshub.CandidateRecord
import com.example.newshub.R
import com.example.newshub.UiErrorMapper
import com.example.newshub.network.ApiResult
import kotlinx.coroutines.launch

data class CandidatesUiState(
    val isLoading: Boolean = false,
    val allItems: List<CandidateRecord> = emptyList(),
    val items: List<CandidateRecord> = emptyList(),
    val searchQuery: String = "",
    val emptyMessageRes: Int? = null,
    val messageRes: Int? = null
)

class CandidatesViewModel(
    private val supabaseService: SupabaseService = SupabaseService()
) : ViewModel() {

    private val _uiState = MutableLiveData(CandidatesUiState())
    val uiState: LiveData<CandidatesUiState> = _uiState

    fun load(electionId: String) {
        _uiState.value = _uiState.value?.copy(isLoading = true)
        viewModelScope.launch {
            when (val result = supabaseService.fetchCandidatesByElection(electionId)) {
                is ApiResult.Success -> {
                    val current = _uiState.value ?: CandidatesUiState()
                    val filtered = applySearch(result.data, current.searchQuery)
                    _uiState.value = current.copy(
                        isLoading = false,
                        allItems = result.data,
                        items = filtered,
                        emptyMessageRes = if (filtered.isEmpty()) R.string.candidates_empty else null
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

    fun setSearchQuery(query: String) {
        val current = _uiState.value ?: return
        val filtered = applySearch(current.allItems, query)
        _uiState.value = current.copy(
            searchQuery = query,
            items = filtered,
            emptyMessageRes = if (filtered.isEmpty()) R.string.candidates_empty else null
        )
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value?.copy(messageRes = null)
    }

    private fun applySearch(items: List<CandidateRecord>, query: String): List<CandidateRecord> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return items
        return items.filter {
            it.fullName.lowercase().contains(q) || it.party.lowercase().contains(q)
        }
    }
}
