package com.example.newshub.feature.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newshub.BackendService
import com.example.newshub.NewsArticle
import com.example.newshub.R
import com.example.newshub.UiErrorMapper
import com.example.newshub.network.ApiResult
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val items: List<NewsArticle> = emptyList(),
    val hasMore: Boolean = true,
    val emptyMessageRes: Int? = null,
    val messageRes: Int? = null,
    val locationLabel: String = "",
    val category: String = "Top Stories",
    val scope: String = "Local"
)

class HomeViewModel(
    private val backendService: BackendService = BackendService()
) : ViewModel() {

    private val _uiState = MutableLiveData(HomeUiState())
    val uiState: LiveData<HomeUiState> = _uiState

    private var currentPage = 0
    private var lastLat: Double? = null
    private var lastLng: Double? = null
    private var lastLocationLabel = ""
    private var lastCountry = ""
    private var lastArea = ""

    fun loadNews(
        locationLabel: String,
        lat: Double?,
        lng: Double?,
        category: String,
        scope: String,
        country: String,
        area: String
    ) {
        currentPage = 0
        lastLat = lat
        lastLng = lng
        lastLocationLabel = locationLabel
        lastCountry = country
        lastArea = area
        _uiState.value = _uiState.value?.copy(
            isLoading = true,
            isLoadingMore = false,
            locationLabel = locationLabel,
            category = category,
            scope = scope,
            hasMore = true
        )
        fetchPage(replace = true)
    }

    fun loadMore() {
        val state = _uiState.value ?: return
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        currentPage += 1
        _uiState.value = state.copy(isLoadingMore = true)
        fetchPage(replace = false)
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value?.copy(messageRes = null)
    }

    private fun fetchPage(replace: Boolean) {
        val state = _uiState.value ?: return
        viewModelScope.launch {
            when (
                val result = backendService.fetchNews(
                    lat = lastLat,
                    lng = lastLng,
                    scope = state.scope,
                    country = lastCountry.takeIf { it.isNotBlank() },
                    area = lastArea.takeIf { it.isNotBlank() },
                    page = currentPage
                )
            ) {
                is ApiResult.Success -> {
                    val merged = if (replace) {
                        result.data.articles
                    } else {
                        state.items + result.data.articles
                    }
                    _uiState.value = state.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        items = merged,
                        hasMore = result.data.hasMore,
                        emptyMessageRes = if (merged.isEmpty()) R.string.news_empty else null,
                        messageRes = null,
                        locationLabel = lastLocationLabel
                    )
                }

                is ApiResult.Failure -> {
                    _uiState.value = state.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        items = if (replace) emptyList() else state.items,
                        emptyMessageRes = if (replace) R.string.news_load_failed else state.emptyMessageRes,
                        messageRes = UiErrorMapper.toMessageRes(result.error),
                        locationLabel = lastLocationLabel
                    )
                }
            }
        }
    }
}
