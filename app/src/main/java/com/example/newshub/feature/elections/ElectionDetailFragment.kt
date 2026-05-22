package com.example.newshub.feature.elections

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.newshub.ElectionRecord
import com.example.newshub.R
import com.example.newshub.databinding.FragmentElectionDetailBinding

class ElectionDetailFragment : Fragment(), ElectionDetailContract.View {

    private var _binding: FragmentElectionDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var presenter: ElectionDetailContract.Presenter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentElectionDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter = ElectionDetailPresenter()
        presenter.attach(this)

        val electionId = arguments?.getString("electionId").orEmpty()
        val snapshot = arguments?.toElectionSnapshot()

        binding.buttonBack.setOnClickListener { findNavController().popBackStack() }
        binding.buttonViewCandidates.setOnClickListener { presenter.onViewCandidatesClicked() }
        binding.buttonVoteNow.setOnClickListener { presenter.onVoteNowClicked() }

        presenter.onScreenStarted(electionId, snapshot)
    }

    override fun onDestroyView() {
        presenter.detach()
        super.onDestroyView()
        _binding = null
    }

    override fun showLoading(isLoading: Boolean) {
        binding.progressElection.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun showMessage(messageRes: Int) {
        Toast.makeText(requireContext(), getString(messageRes), Toast.LENGTH_SHORT).show()
    }

    override fun renderElection(election: ElectionRecord) {
        binding.textElectionName.text = election.name
        binding.textRegion.text = election.region.ifBlank { getString(R.string.election_region_unknown) }
        binding.textDateRange.text = listOf(election.startDate, election.endDate)
            .filter { it.isNotBlank() }
            .joinToString(" – ")
        binding.textDescription.text = election.description
        binding.textCandidateCount.text = getString(
            R.string.election_candidate_count,
            election.candidateCount
        )
        binding.textStatus.text = election.status
        binding.imageElection.load(election.imageUrl) {
            crossfade(true)
            placeholder(R.drawable.bg_home_feature_placeholder)
            error(R.drawable.bg_home_feature_placeholder)
        }

        val isActive = election.status.equals("ACTIVE", ignoreCase = true) ||
            election.status.equals("OPEN", ignoreCase = true)
        binding.buttonVoteNow.visibility = if (isActive) View.VISIBLE else View.GONE
    }

    override fun navigateToCandidates(electionId: String, electionName: String) {
        val args = Bundle().apply {
            putString("electionId", electionId)
            putString("electionName", electionName)
        }
        findNavController().navigate(R.id.action_electionDetailFragment_to_candidatesFragment, args)
    }

    private fun Bundle.toElectionSnapshot(): ElectionRecord? {
        val id = getString("electionId").orEmpty()
        val name = getString("electionName").orEmpty()
        if (id.isBlank() && name.isBlank()) return null
        return ElectionRecord(
            id = id,
            name = name,
            status = getString("electionStatus").orEmpty(),
            startDate = getString("electionStartDate").orEmpty(),
            endDate = getString("electionEndDate").orEmpty(),
            description = getString("electionDescription").orEmpty(),
            region = getString("electionRegion").orEmpty(),
            imageUrl = getString("electionImageUrl"),
            candidateCount = getInt("electionCandidateCount", 0)
        )
    }
}
