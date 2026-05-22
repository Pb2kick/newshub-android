package com.example.newshub.feature.voting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.newshub.core.session.SessionStore

class VoteVerificationViewModelFactory(
    private val sessionStore: SessionStore
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VoteVerificationViewModel::class.java)) {
            return VoteVerificationViewModel(sessionStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

