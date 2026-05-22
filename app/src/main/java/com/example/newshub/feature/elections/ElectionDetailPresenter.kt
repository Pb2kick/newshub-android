package com.example.newshub.feature.elections

import com.example.newshub.SupabaseService
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
    private val supabaseService: SupabaseService = SupabaseService()
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
            when (val result = supabaseService.fetchElection(electionId)) {
                is ApiResult.Success -> {
                    val resolved = result.data ?: snapshot
                    election = resolved
                    resolved?.let { view?.renderElection(it) }
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
