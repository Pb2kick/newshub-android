package com.example.newshub.feature.elections

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.load
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
        binding.topBarInclude.buttonSearch.setOnClickListener {
            findNavController().navigate(R.id.searchFragment)
        }
        binding.topBarInclude.buttonLocation.setOnClickListener { }
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
        binding.buttonVote.text = getString(R.string.vote_for_candidate_named, candidate.fullName)

        val hasPosition = candidate.position.isNotBlank()
        binding.textPosition.visibility = if (hasPosition) View.VISIBLE else View.GONE
        if (hasPosition) {
            binding.textPosition.text = candidate.position
        }

        val hasParty = candidate.party.isNotBlank()
        binding.textParty.visibility = if (hasParty) View.VISIBLE else View.GONE
        if (hasParty) {
            binding.textParty.text = candidate.party
        }

        val hasPlatform = candidate.platform.isNotBlank()
        binding.layoutAboutHeader.visibility = if (hasPlatform) View.VISIBLE else View.GONE
        binding.textBio.visibility = if (hasPlatform) View.VISIBLE else View.GONE
        if (hasPlatform) {
            binding.textBio.text = candidate.platform
        }

        val hasEducation = candidate.education.isNotBlank()
        binding.rowStatEducation.visibility = if (hasEducation) View.VISIBLE else View.GONE
        binding.cardQuickStats.visibility = if (hasEducation) View.VISIBLE else View.GONE
        if (hasEducation) {
            binding.textStatEducation.text = candidate.education
        }

        if (hasParty) {
            binding.layoutEndorsementsHeader.visibility = View.VISIBLE
            binding.textEndorsements.visibility = View.VISIBLE
            binding.textEndorsements.text = "• ${candidate.party}"
        } else {
            binding.layoutEndorsementsHeader.visibility = View.GONE
            binding.textEndorsements.visibility = View.GONE
        }

        binding.imagePhoto.load(candidate.photoUrl) {
            crossfade(true)
            placeholder(R.drawable.bg_home_feature_placeholder)
            error(R.drawable.bg_home_feature_placeholder)
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
