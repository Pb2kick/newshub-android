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

enum class ElectionScopeFilter {
    ALL,
    NATIONAL,
    STATE,
    LOCAL
}

data class ElectionsUiState(
    val isLoading: Boolean = false,
    val allItems: List<ElectionRecord> = emptyList(),
    val items: List<ElectionRecord> = emptyList(),
    val scopeFilter: ElectionScopeFilter = ElectionScopeFilter.ALL,
    val searchQuery: String = "",
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
                    val current = _uiState.value ?: ElectionsUiState()
                    val filtered = applyFilters(result.data, current.scopeFilter, current.searchQuery)
                    _uiState.value = current.copy(
                        isLoading = false,
                        allItems = result.data,
                        items = filtered,
                        emptyMessageRes = if (filtered.isEmpty()) R.string.elections_empty else null
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

    fun setScopeFilter(filter: ElectionScopeFilter) {
        val current = _uiState.value ?: return
        val filtered = applyFilters(current.allItems, filter, current.searchQuery)
        _uiState.value = current.copy(
            scopeFilter = filter,
            items = filtered,
            emptyMessageRes = if (filtered.isEmpty()) R.string.elections_empty else null
        )
    }

    fun setSearchQuery(query: String) {
        val current = _uiState.value ?: return
        val filtered = applyFilters(current.allItems, current.scopeFilter, query)
        _uiState.value = current.copy(
            searchQuery = query,
            items = filtered,
            emptyMessageRes = if (filtered.isEmpty()) R.string.elections_empty else null
        )
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value?.copy(messageRes = null)
    }

    private fun applyFilters(
        items: List<ElectionRecord>,
        scopeFilter: ElectionScopeFilter,
        searchQuery: String
    ): List<ElectionRecord> {
        val query = searchQuery.trim().lowercase()
        return items.filter { election ->
            val region = election.region.ifBlank { "national" }.lowercase()
            val matchesScope = when (scopeFilter) {
                ElectionScopeFilter.ALL -> true
                ElectionScopeFilter.NATIONAL -> region.contains("national")
                ElectionScopeFilter.STATE -> region.contains("state")
                ElectionScopeFilter.LOCAL -> region.contains("local") || region.contains("district")
            }
            val matchesQuery = query.isBlank() ||
                election.name.lowercase().contains(query) ||
                region.contains(query) ||
                election.status.lowercase().contains(query)
            matchesScope && matchesQuery
        }
    }
}
