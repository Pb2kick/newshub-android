package com.example.newshub.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.newshub.core.session.SessionStore

class NotificationsViewModelFactory(
    private val sessionStore: SessionStore
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationsViewModel::class.java)) {
            return NotificationsViewModel(sessionStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
