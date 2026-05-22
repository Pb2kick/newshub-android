package com.example.newshub.feature.news

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newshub.BackendService
import com.example.newshub.NewsArticle
import com.example.newshub.NewsArticleDetail
import com.example.newshub.R
import com.example.newshub.UiErrorMapper
import com.example.newshub.network.ApiResult
import kotlinx.coroutines.launch

data class NewsDetailUiState(
    val isLoading: Boolean = false,
    val detail: NewsArticleDetail? = null,
    val relatedArticles: List<NewsArticle> = emptyList(),
    val messageRes: Int? = null
)

class NewsDetailViewModel(
    private val backendService: BackendService = BackendService()
) : ViewModel() {

    private val _uiState = MutableLiveData(NewsDetailUiState())
    val uiState: LiveData<NewsDetailUiState> = _uiState

    fun loadArticle(url: String?, fallbackTitle: String, fallbackSource: String, fallbackPublishedAt: String, fallbackSummary: String) {
        if (url.isNullOrBlank()) {
            _uiState.value = NewsDetailUiState(
                isLoading = false,
                detail = NewsArticleDetail(
                    title = fallbackTitle,
                    source = fallbackSource,
                    publishedAt = fallbackPublishedAt,
                    content = fallbackSummary.ifBlank { "No article content available." },
                    articleUrl = null
                )
            )
            return
        }

        _uiState.value = _uiState.value?.copy(isLoading = true)
        viewModelScope.launch {
            when (val result = backendService.fetchArticleContent(url)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value?.copy(isLoading = false, detail = result.data)
                }

                is ApiResult.Failure -> {
                    _uiState.value = NewsDetailUiState(
                        detail = NewsArticleDetail(
                            title = fallbackTitle,
                            source = fallbackSource,
                            publishedAt = fallbackPublishedAt,
                            content = fallbackSummary.ifBlank { "No article content available." },
                            articleUrl = url
                        ),
                        messageRes = UiErrorMapper.toMessageRes(result.error)
                    )
                }
            }
        }
    }

    fun loadRelated(category: String, excludeArticleId: String) {
        if (category.isBlank()) return
        viewModelScope.launch {
            when (
                val result = backendService.fetchNews(
                    lat = null,
                    lng = null,
                    scope = "National",
                    country = java.util.Locale.getDefault().displayCountry.ifBlank { "Philippines" },
                    area = null,
                    page = 0,
                    size = 4
                )
            ) {
                is ApiResult.Success -> {
                    val filtered = result.data.articles.filter { it.id != excludeArticleId }
                    _uiState.value = _uiState.value?.copy(relatedArticles = filtered)
                }
                is ApiResult.Failure -> Unit
            }
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value?.copy(messageRes = null)
    }
}
