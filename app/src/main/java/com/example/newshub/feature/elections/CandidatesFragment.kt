package com.example.newshub.feature.elections

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.newshub.R
import com.example.newshub.databinding.FragmentCandidatesBinding
import com.example.newshub.toProfileBundle
import com.example.newshub.core.RestIdNormalizer
import com.example.newshub.ui.BottomNavHelper
import com.example.newshub.ui.LocationNavHelper

class CandidatesFragment : Fragment() {

    private var _binding: FragmentCandidatesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CandidatesViewModel by viewModels()
    private lateinit var adapter: CandidatesAdapter
    private var electionId: String = ""
    private var electionName: String = ""

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

        electionId = RestIdNormalizer.normalize(arguments?.getString("electionId").orEmpty())
        electionName = arguments?.getString("electionName").orEmpty()
        binding.textTitle.text = if (electionName.isBlank()) {
            getString(R.string.candidates_title)
        } else {
            "$electionName Candidates"
        }

        adapter = CandidatesAdapter { candidate ->
            findNavController().navigate(
                R.id.action_candidatesFragment_to_candidateProfileFragment,
                candidate.toProfileBundle(electionName)
            )
        }
        binding.recyclerCandidates.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerCandidates.adapter = adapter

        binding.buttonBack.setOnClickListener { findNavController().popBackStack() }
        binding.topBarInclude.buttonSearch.setOnClickListener {
            findNavController().navigate(R.id.searchFragment)
        }
        LocationNavHelper.wirePin(this, binding.topBarInclude) { }
        binding.buttonVoteNow.setOnClickListener {
            val first = viewModel.uiState.value?.items?.firstOrNull()
            if (first != null) {
                findNavController().navigate(
                    R.id.action_candidatesFragment_to_candidateProfileFragment,
                    first.toProfileBundle(electionName)
                )
            } else {
                Toast.makeText(requireContext(), R.string.candidates_empty, Toast.LENGTH_SHORT).show()
            }
        }
        binding.buttonSort.setOnClickListener {
            val sorted = viewModel.uiState.value?.items?.sortedBy { it.fullName }.orEmpty()
            adapter.submitList(sorted)
        }

        binding.inputSearchCandidates.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchQuery(s?.toString().orEmpty())
            }
        })

        BottomNavHelper.wire(
            fragment = this,
            navHome = binding.bottomNavBar.navHome,
            navElections = binding.bottomNavBar.navElections,
            navAlerts = binding.bottomNavBar.navAlerts,
            navProfile = binding.bottomNavBar.navProfile
        )

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
