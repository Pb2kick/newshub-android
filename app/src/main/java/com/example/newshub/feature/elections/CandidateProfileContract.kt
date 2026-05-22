package com.example.newshub.feature.elections

import com.example.newshub.CandidateRecord

interface CandidateProfileContract {
    interface View {
        fun showLoading(isLoading: Boolean)
        fun showMessage(messageRes: Int)
        fun renderCandidate(candidate: CandidateRecord, electionName: String)
        fun navigateToVoteConfirm(
            electionId: String,
            electionName: String,
            candidateId: String,
            candidateName: String
        )
    }

    interface Presenter {
        fun attach(view: View)
        fun detach()
        fun onScreenStarted(candidateId: String, snapshot: CandidateRecord?, electionName: String)
        fun onVoteClicked()
    }
}
