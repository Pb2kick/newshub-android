package com.example.newshub.feature.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newshub.BackendService
import com.example.newshub.NewsArticle
import com.example.newshub.R
import com.example.newshub.UiErrorMapper
import com.example.newshub.core.session.SessionStore
import com.example.newshub.network.ApiResult
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val items: List<NewsArticle> = emptyList(),
    val emptyMessageRes: Int? = null,
    val messageRes: Int? = null,
    val locationLabel: String = ""
)

class HomeViewModel(
    private val sessionStore: SessionStore,
    private val backendService: BackendService = BackendService()
) : ViewModel() {

    private val _uiState = MutableLiveData(HomeUiState())
    val uiState: LiveData<HomeUiState> = _uiState

    fun loadNews(locationLabel: String, lat: Double? = null, lng: Double? = null) {
        val accessToken = sessionStore.getAccessToken()
        if (accessToken.isNullOrBlank()) {
            _uiState.value = _uiState.value?.copy(messageRes = R.string.error_unauthorized)
            return
        }

        _uiState.value = _uiState.value?.copy(isLoading = true, locationLabel = locationLabel)
        viewModelScope.launch {
            when (val result = backendService.fetchNews(lat = lat, lng = lng, location = locationLabel)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value?.copy(
                        isLoading = false,
                        items = result.data,
                        emptyMessageRes = if (result.data.isEmpty()) R.string.news_empty else null,
                        messageRes = null,
                        locationLabel = locationLabel
                    )
                }

                is ApiResult.Failure -> {
                    _uiState.value = _uiState.value?.copy(
                        isLoading = false,
                        items = emptyList(),
                        emptyMessageRes = R.string.news_empty,
                        messageRes = UiErrorMapper.toMessageRes(result.error),
                        locationLabel = locationLabel
                    )
                }
            }
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value?.copy(messageRes = null)
    }
}

