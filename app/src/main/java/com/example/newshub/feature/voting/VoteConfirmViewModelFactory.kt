package com.example.newshub.feature.voting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.newshub.BackendService
import com.example.newshub.core.session.SessionStore

class VoteConfirmViewModelFactory(
    private val sessionStore: SessionStore,
    private val backendService: BackendService = BackendService()
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VoteConfirmViewModel::class.java)) {
            return VoteConfirmViewModel(sessionStore, backendService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

