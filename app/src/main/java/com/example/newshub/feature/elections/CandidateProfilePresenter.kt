package com.example.newshub.feature.elections

import com.example.newshub.BackendService
import com.example.newshub.CandidateRecord
import com.example.newshub.UiErrorMapper
import com.example.newshub.network.ApiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class CandidateProfilePresenter(
    private val backendService: BackendService = BackendService()
) : CandidateProfileContract.Presenter {

    private var view: CandidateProfileContract.View? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var candidate: CandidateRecord? = null
    private var electionName: String = ""

    override fun attach(view: CandidateProfileContract.View) {
        this.view = view
    }

    override fun detach() {
        view = null
        scope.cancel()
    }

    override fun onScreenStarted(candidateId: String, snapshot: CandidateRecord?, electionName: String) {
        this.electionName = electionName
        candidate = snapshot
        snapshot?.let { view?.renderCandidate(it, electionName) }

        if (candidateId.isBlank()) return

        view?.showLoading(true)
        scope.launch {
            when (val result = backendService.fetchCandidate(candidateId)) {
                is ApiResult.Success -> {
                    candidate = result.data
                    view?.renderCandidate(result.data, electionName)
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

    override fun onVoteClicked() {
        val current = candidate ?: return
        view?.navigateToVoteConfirm(
            electionId = current.electionId,
            electionName = electionName,
            candidateId = current.id,
            candidateName = current.fullName
        )
    }
}
