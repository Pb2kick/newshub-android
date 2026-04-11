package com.example.newshub.feature.auth.register

import com.example.newshub.R
import com.example.newshub.UiErrorMapper
import com.example.newshub.feature.auth.data.AuthRepository
import com.example.newshub.feature.auth.data.SupabaseAuthRepository
import com.example.newshub.feature.profile.data.ProfileRepository
import com.example.newshub.feature.profile.data.SupabaseProfileRepository
import com.example.newshub.network.ApiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class RegisterPresenter(
    private val authRepository: AuthRepository = SupabaseAuthRepository(),
    private val profileRepository: ProfileRepository = SupabaseProfileRepository()
) : RegisterContract.Presenter {

    private var view: RegisterContract.View? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun attach(view: RegisterContract.View) {
        this.view = view
    }

    override fun detach() {
        view = null
        scope.cancel()
    }

    override fun onBackToSignInClicked() {
        view?.navigateToLogin()
    }

    override fun onCreateAccountClicked(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String,
        isAttested: Boolean
    ) {
        if (!authRepository.isConfigured) {
            view?.showMessage(R.string.supabase_not_configured)
            return
        }

        if (fullName.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            view?.showMessage(R.string.auth_register_required)
            return
        }

        if (password != confirmPassword) {
            view?.showMessage(R.string.profile_password_mismatch)
            return
        }

        if (password.length < 8) {
            view?.showMessage(R.string.profile_password_too_short)
            return
        }

        if (!isAttested) {
            view?.showMessage(R.string.auth_attestation_required)
            return
        }

        val names = fullName.split(" ").filter { it.isNotBlank() }
        val firstName = names.firstOrNull().orEmpty()
        val lastName = names.drop(1).joinToString(" ")

        view?.showLoading(true)
        scope.launch {
            when (val result = authRepository.signUp(email.trim(), password, firstName, lastName)) {
                is ApiResult.Success -> {
                    view?.showLoading(false)
                    val session = result.data
                    if (session != null) {
                        view?.saveSession(session.userId, session.accessToken)
                        val profileSync = profileRepository.upsertProfile(
                            userId = session.userId,
                            firstName = firstName,
                            lastName = lastName,
                            avatarUrl = null,
                            email = email.trim(),
                            accessToken = session.accessToken
                        )
                        if (profileSync is ApiResult.Failure) {
                            view?.showMessage(UiErrorMapper.toMessageRes(profileSync.error))
                            return@launch
                        }

                        view?.showMessage(R.string.auth_register_success)
                        view?.navigateToHome()
                    } else {
                        view?.showMessage(R.string.auth_verify_email_required)
                        view?.navigateToLogin()
                    }
                }

                is ApiResult.Failure -> {
                    view?.showLoading(false)
                    val fallback = if (result.error.statusCode == 400) {
                        R.string.auth_register_failed
                    } else {
                        UiErrorMapper.toMessageRes(result.error)
                    }
                    view?.showMessage(fallback)
                }
            }
        }
    }
}

