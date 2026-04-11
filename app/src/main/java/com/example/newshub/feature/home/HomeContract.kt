package com.example.newshub.feature.home

interface HomeContract {
    interface View {
        fun showLoading(isLoading: Boolean)
        fun showMessage(messageRes: Int)
        fun navigateToLogin()
        fun navigateToProfile()
    }

    interface Presenter {
        fun attach(view: View)
        fun detach()
        fun onScreenStarted()
        fun onProfileClicked()
    }
}

