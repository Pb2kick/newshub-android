package com.example.newshub.feature.auth.register

interface RegisterContract {
    interface View {
        fun showLoading(isLoading: Boolean)
        fun showMessage(messageRes: Int)
        fun saveSession(userId: String, accessToken: String)
        fun navigateToLogin()
        fun navigateToHome()
    }

    interface Presenter {
        fun attach(view: View)
        fun detach()
        fun onBackToSignInClicked()
        fun onCreateAccountClicked(
            fullName: String,
            email: String,
            password: String,
            confirmPassword: String,
            isAttested: Boolean
        )
    }
}

