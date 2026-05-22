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
import com.example.newshub.R
import com.example.newshub.databinding.FragmentElectionsBinding
import com.example.newshub.toDetailBundle
import com.example.newshub.ui.BottomNavHelper

class ElectionsFragment : Fragment() {

    private var _binding: FragmentElectionsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ElectionsViewModel by viewModels()
    private lateinit var adapter: ElectionsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentElectionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ElectionsAdapter { election ->
            findNavController().navigate(
                R.id.action_electionsFragment_to_electionDetailFragment,
                election.toDetailBundle()
            )
        }
        binding.recyclerElections.adapter = adapter

        binding.topBarInclude.buttonSearch.setOnClickListener {
            findNavController().navigate(R.id.searchFragment)
        }
        binding.topBarInclude.buttonLocation.setOnClickListener {
            viewModel.load()
        }
        binding.buttonFilter.setOnClickListener {
            binding.chipGroupScope.check(R.id.chip_filter_all)
            viewModel.setScopeFilter(ElectionScopeFilter.ALL)
            binding.inputSearchElections.text?.clear()
        }

        binding.chipGroupScope.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val filter = when (checkedId) {
                R.id.chip_filter_national -> ElectionScopeFilter.NATIONAL
                R.id.chip_filter_state -> ElectionScopeFilter.STATE
                R.id.chip_filter_local -> ElectionScopeFilter.LOCAL
                else -> ElectionScopeFilter.ALL
            }
            viewModel.setScopeFilter(filter)
        }

        binding.inputSearchElections.addTextChangedListener(object : TextWatcher {
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
        binding.bottomNavBar.navElections.setOnClickListener { }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.progressElections.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            adapter.submitList(state.items)
            binding.textEmpty.visibility = if (state.items.isEmpty() && !state.isLoading) View.VISIBLE else View.GONE
            state.emptyMessageRes?.let { binding.textEmpty.setText(it) }
            state.messageRes?.let {
                Toast.makeText(requireContext(), getString(it), Toast.LENGTH_SHORT).show()
                viewModel.consumeMessage()
            }
        }

        viewModel.load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
