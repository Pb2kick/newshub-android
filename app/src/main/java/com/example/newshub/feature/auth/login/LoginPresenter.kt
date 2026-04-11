package com.example.newshub.feature.auth.login

import com.example.newshub.R
import com.example.newshub.UiErrorMapper
import com.example.newshub.feature.auth.data.AuthRepository
import com.example.newshub.feature.auth.data.SupabaseAuthRepository
import com.example.newshub.network.ApiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LoginPresenter(
    private val authRepository: AuthRepository = SupabaseAuthRepository()
) : LoginContract.Presenter {

    private var view: LoginContract.View? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun attach(view: LoginContract.View) {
        this.view = view
    }

    override fun detach() {
        view = null
        scope.cancel()
    }

    override fun onForgotPasswordClicked() {
        view?.showMessage(R.string.forgot_password_placeholder)
    }

    override fun onRegisterClicked() {
        view?.navigateToRegister()
    }

    override fun onSignInClicked(email: String, password: String) {
        if (!authRepository.isConfigured) {
            view?.showMessage(R.string.supabase_not_configured)
            return
        }

        if (email.isBlank() || password.isBlank()) {
            view?.showMessage(R.string.auth_login_required)
            return
        }

        view?.showLoading(true)
        scope.launch {
            when (val result = authRepository.signIn(email.trim(), password)) {
                is ApiResult.Success -> {
                    view?.showLoading(false)
                    view?.saveSession(result.data.userId, result.data.accessToken)
                    view?.navigateToHome()
                }

                is ApiResult.Failure -> {
                    view?.showLoading(false)
                    val fallback = if (result.error.statusCode == 400 || result.error.statusCode == 401) {
                        R.string.auth_login_failed
                    } else {
                        UiErrorMapper.toMessageRes(result.error)
                    }
                    view?.showMessage(fallback)
                }
            }
        }
    }
}

