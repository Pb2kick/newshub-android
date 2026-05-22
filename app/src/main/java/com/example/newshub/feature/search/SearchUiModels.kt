package com.example.newshub.feature.search

import com.example.newshub.CandidateRecord
import com.example.newshub.ElectionRecord
import com.example.newshub.NewsArticle

sealed class SearchRow {
    data class Header(val title: String) : SearchRow()
    data class Article(val article: NewsArticle) : SearchRow()
    data class Election(val election: ElectionRecord) : SearchRow()
    data class Candidate(val candidate: CandidateRecord) : SearchRow()
}

data class SearchUiState(
    val isLoading: Boolean = false,
    val rows: List<SearchRow> = emptyList(),
    val messageRes: Int? = null
)
