package com.example.newshub.feature.auth.login

interface LoginContract {
    interface View {
        fun showLoading(isLoading: Boolean)
        fun showMessage(messageRes: Int)
        fun saveSession(userId: String, accessToken: String)
        fun navigateToHome()
        fun navigateToRegister()
    }

    interface Presenter {
        fun attach(view: View)
        fun detach()
        fun onForgotPasswordClicked()
        fun onRegisterClicked()
        fun onSignInClicked(email: String, password: String)
    }
}

