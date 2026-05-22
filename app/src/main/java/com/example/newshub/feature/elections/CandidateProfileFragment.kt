package com.example.newshub.feature.elections

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.example.newshub.CandidateRecord
import com.example.newshub.R
import com.example.newshub.databinding.FragmentCandidateProfileBinding
import com.example.newshub.toVoteBundle

class CandidateProfileFragment : Fragment(), CandidateProfileContract.View {

    private var _binding: FragmentCandidateProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var presenter: CandidateProfileContract.Presenter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCandidateProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter = CandidateProfilePresenter()
        presenter.attach(this)

        val candidateId = arguments?.getString("candidateId").orEmpty()
        val electionName = arguments?.getString("electionName").orEmpty()
        val snapshot = arguments?.toCandidateSnapshot()

        binding.buttonBack.setOnClickListener { findNavController().popBackStack() }
        binding.buttonVote.setOnClickListener { presenter.onVoteClicked() }

        presenter.onScreenStarted(candidateId, snapshot, electionName)
    }

    override fun onDestroyView() {
        presenter.detach()
        super.onDestroyView()
        _binding = null
    }

    override fun showLoading(isLoading: Boolean) {
        binding.progressCandidate.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun showMessage(messageRes: Int) {
        Toast.makeText(requireContext(), getString(messageRes), Toast.LENGTH_SHORT).show()
    }

    override fun renderCandidate(candidate: CandidateRecord, electionName: String) {
        binding.textCandidateName.text = candidate.fullName
        binding.textPosition.text = candidate.position.ifBlank { getString(R.string.candidate_position_unknown) }
        binding.textParty.text = candidate.party
        binding.textBio.text = candidate.platform
        binding.textEducation.text = candidate.education.ifBlank { getString(R.string.candidate_education_unknown) }
        binding.imagePhoto.load(candidate.photoUrl) {
            crossfade(true)
            transformations(CircleCropTransformation())
            placeholder(R.drawable.bg_home_chip_muted)
            error(R.drawable.bg_home_chip_muted)
        }
    }

    override fun navigateToVoteConfirm(
        electionId: String,
        electionName: String,
        candidateId: String,
        candidateName: String
    ) {
        val record = CandidateRecord(
            id = candidateId,
            electionId = electionId,
            fullName = candidateName,
            party = "",
            platform = "",
            photoUrl = null
        )
        findNavController().navigate(
            R.id.action_candidateProfileFragment_to_voteConfirmFragment,
            record.toVoteBundle(electionName)
        )
    }

    private fun Bundle.toCandidateSnapshot(): CandidateRecord? {
        val id = getString("candidateId").orEmpty()
        if (id.isBlank()) return null
        return CandidateRecord(
            id = id,
            electionId = getString("electionId").orEmpty(),
            fullName = getString("candidateName").orEmpty(),
            party = getString("candidateParty").orEmpty(),
            platform = getString("candidatePlatform").orEmpty(),
            photoUrl = getString("candidatePhotoUrl"),
            position = getString("candidatePosition").orEmpty(),
            education = getString("candidateEducation").orEmpty()
        )
    }
}
