package com.example.newshub.feature.home

import com.example.newshub.R
import com.example.newshub.UiErrorMapper
import com.example.newshub.core.session.SessionStore
import com.example.newshub.feature.home.data.HomeRepository
import com.example.newshub.feature.home.data.SupabaseHomeRepository
import com.example.newshub.network.ApiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class HomePresenter(
    private val sessionStore: SessionStore,
    private val homeRepository: HomeRepository = SupabaseHomeRepository()
) : HomeContract.Presenter {

    private var view: HomeContract.View? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun attach(view: HomeContract.View) {
        this.view = view
    }

    override fun detach() {
        view = null
        scope.cancel()
    }

    override fun onScreenStarted() {
        val accessToken = sessionStore.getAccessToken()
        if (accessToken.isNullOrBlank()) {
            view?.navigateToLogin()
            return
        }

        view?.showLoading(true)
        scope.launch {
            when (val result = homeRepository.fetchAuthUser(accessToken)) {
                is ApiResult.Success -> {
                    view?.showLoading(false)
                    view?.showMessage(R.string.home_dashboard_ready)
                }

                is ApiResult.Failure -> {
                    view?.showLoading(false)
                    if (result.error.statusCode == 401) {
                        sessionStore.clear()
                        view?.showMessage(R.string.error_unauthorized)
                        view?.navigateToLogin()
                    } else {
                        view?.showMessage(UiErrorMapper.toMessageRes(result.error))
                    }
                }
            }
        }
    }

    override fun onProfileClicked() {
        view?.navigateToProfile()
    }
}

