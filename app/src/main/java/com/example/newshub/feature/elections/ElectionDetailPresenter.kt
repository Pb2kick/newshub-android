package com.example.newshub.feature.elections

import com.example.newshub.BackendService
import com.example.newshub.ElectionRecord
import com.example.newshub.R
import com.example.newshub.UiErrorMapper
import com.example.newshub.network.ApiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ElectionDetailPresenter(
    private val backendService: BackendService = BackendService()
) : ElectionDetailContract.Presenter {

    private var view: ElectionDetailContract.View? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var election: ElectionRecord? = null

    override fun attach(view: ElectionDetailContract.View) {
        this.view = view
    }

    override fun detach() {
        view = null
        scope.cancel()
    }

    override fun onScreenStarted(electionId: String, snapshot: ElectionRecord?) {
        election = snapshot
        snapshot?.let { view?.renderElection(it) }

        if (electionId.isBlank()) return

        view?.showLoading(true)
        scope.launch {
            when (val result = backendService.fetchElection(electionId)) {
                is ApiResult.Success -> {
                    election = result.data
                    view?.renderElection(result.data)
                }
                is ApiResult.Failure -> {
                    if (snapshot == null) {
                        view?.showMessage(UiErrorMapper.toMessageRes(result.error))
                    }
                }
            }
            view?.showLoading(false)
        }
    }

    override fun onViewCandidatesClicked() {
        val current = election ?: return
        view?.navigateToCandidates(current.id, current.name)
    }

    override fun onVoteNowClicked() {
        onViewCandidatesClicked()
    }
}
