package com.example.newshub.feature.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newshub.BackendService
import com.example.newshub.SupabaseService
import com.example.newshub.R
import com.example.newshub.network.ApiResult
import kotlinx.coroutines.launch

class SearchViewModel(
    private val backendService: BackendService = BackendService(),
    private val supabaseService: SupabaseService = SupabaseService()
) : ViewModel() {

    private val _uiState = MutableLiveData(SearchUiState())
    val uiState: LiveData<SearchUiState> = _uiState

    fun search(query: String, scope: String, country: String, area: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _uiState.value = SearchUiState()
            return
        }

        _uiState.value = _uiState.value?.copy(isLoading = true)
        viewModelScope.launch {
            val articles = when (
                val result = backendService.searchNews(
                    query = trimmed,
                    scope = scope,
                    country = country.takeIf { it.isNotBlank() },
                    area = area.takeIf { it.isNotBlank() }
                )
            ) {
                is ApiResult.Success -> result.data
                is ApiResult.Failure -> emptyList()
            }
            val elections = when (val result = supabaseService.searchElections(trimmed)) {
                is ApiResult.Success -> result.data
                is ApiResult.Failure -> emptyList()
            }
            val candidates = when (val result = supabaseService.searchCandidates(trimmed)) {
                is ApiResult.Success -> result.data
                is ApiResult.Failure -> emptyList()
            }

            val rows = buildList {
                if (articles.isNotEmpty()) {
                    add(SearchRow.Header("ARTICLES"))
                    articles.forEach { add(SearchRow.Article(it)) }
                }
                if (elections.isNotEmpty()) {
                    add(SearchRow.Header("ELECTIONS"))
                    elections.forEach { add(SearchRow.Election(it)) }
                }
                if (candidates.isNotEmpty()) {
                    add(SearchRow.Header("CANDIDATES"))
                    candidates.forEach { add(SearchRow.Candidate(it)) }
                }
            }

            _uiState.value = SearchUiState(
                isLoading = false,
                rows = rows,
                messageRes = if (rows.isEmpty()) R.string.search_no_results else null
            )
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value?.copy(messageRes = null)
    }
}
