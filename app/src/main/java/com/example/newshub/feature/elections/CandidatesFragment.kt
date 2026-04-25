package com.example.newshub.feature.elections

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.newshub.R
import com.example.newshub.databinding.FragmentCandidatesBinding

class CandidatesFragment : Fragment() {

    private var _binding: FragmentCandidatesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CandidatesViewModel by viewModels()
    private lateinit var adapter: CandidatesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCandidatesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val electionId = arguments?.getString("electionId").orEmpty()
        val electionName = arguments?.getString("electionName").orEmpty()
        binding.textTitle.text = if (electionName.isBlank()) getString(R.string.candidates_title) else electionName

        adapter = CandidatesAdapter { candidate ->
            val args = Bundle().apply {
                putString("electionId", electionId)
                putString("electionName", electionName)
                putString("candidateId", candidate.id)
                putString("candidateName", candidate.fullName)
            }
            findNavController().navigate(R.id.action_candidatesFragment_to_voteConfirmFragment, args)
        }
        binding.recyclerCandidates.adapter = adapter

        binding.buttonBack.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.buttonMenu.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.home_placeholder_action), Toast.LENGTH_SHORT).show()
        }
        binding.buttonRefresh.setOnClickListener {
            if (electionId.isNotBlank()) {
                viewModel.load(electionId)
            }
        }
        binding.buttonProfileShortcut.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }

        binding.navNews.setOnClickListener {
            findNavController().navigate(R.id.homeFragment)
        }
        binding.navElections.setOnClickListener { }
        binding.navProfile.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.progressCandidates.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            adapter.submitList(state.items)
            binding.textEmpty.visibility = if (state.items.isEmpty()) View.VISIBLE else View.GONE
            state.emptyMessageRes?.let { binding.textEmpty.setText(it) }
            state.messageRes?.let {
                Toast.makeText(requireContext(), getString(it), Toast.LENGTH_SHORT).show()
                viewModel.consumeMessage()
            }
        }

        if (electionId.isNotBlank()) {
            viewModel.load(electionId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

