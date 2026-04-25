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
import com.example.newshub.databinding.FragmentElectionsBinding

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
            val args = Bundle().apply {
                putString("electionId", election.id)
                putString("electionName", election.name)
            }
            findNavController().navigate(R.id.action_electionsFragment_to_candidatesFragment, args)
        }
        binding.recyclerElections.adapter = adapter

        binding.buttonMenu.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.home_placeholder_action), Toast.LENGTH_SHORT).show()
        }
        binding.buttonRefresh.setOnClickListener {
            viewModel.load()
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
            binding.progressElections.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            adapter.submitList(state.items)
            binding.textEmpty.visibility = if (state.items.isEmpty()) View.VISIBLE else View.GONE
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

