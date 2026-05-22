package com.example.newshub.feature.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.newshub.core.session.SessionStore

class CommentsViewModelFactory(
    private val sessionStore: SessionStore
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CommentsViewModel::class.java)) {
            return CommentsViewModel(sessionStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
