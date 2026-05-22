package com.example.newshub.feature.elections

import com.example.newshub.ElectionRecord

interface ElectionDetailContract {
    interface View {
        fun showLoading(isLoading: Boolean)
        fun showMessage(messageRes: Int)
        fun renderElection(election: ElectionRecord)
        fun navigateToCandidates(electionId: String, electionName: String)
    }

    interface Presenter {
        fun attach(view: View)
        fun detach()
        fun onScreenStarted(electionId: String, snapshot: ElectionRecord?)
        fun onViewCandidatesClicked()
        fun onVoteNowClicked()
    }
}
