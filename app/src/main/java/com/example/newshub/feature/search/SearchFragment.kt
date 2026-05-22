package com.example.newshub.feature.search

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
import com.example.newshub.core.location.NewsLocationStore
import com.example.newshub.databinding.FragmentSearchBinding
import com.example.newshub.toDetailBundle
import com.example.newshub.toProfileBundle
import java.util.Locale

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels()
    private lateinit var adapter: SearchSectionAdapter

    private var scope = "Local"
    private var country = ""
    private var area = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadSavedLocation()

        adapter = SearchSectionAdapter(
            onArticleClick = { row ->
                findNavController().navigate(
                    R.id.action_searchFragment_to_newsDetailFragment,
                    row.article.toDetailBundle()
                )
            },
            onElectionClick = { row ->
                findNavController().navigate(
                    R.id.action_searchFragment_to_electionDetailFragment,
                    row.election.toDetailBundle()
                )
            },
            onCandidateClick = { row ->
                findNavController().navigate(
                    R.id.action_searchFragment_to_candidateProfileFragment,
                    row.candidate.toProfileBundle(electionName = "")
                )
            }
        )
        binding.recyclerResults.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerResults.adapter = adapter

        binding.buttonBack.setOnClickListener { findNavController().popBackStack() }

        binding.inputSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                runSearch(s?.toString().orEmpty())
            }
        })

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.progressSearch.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            adapter.submitList(state.rows)
            state.messageRes?.let {
                Toast.makeText(requireContext(), getString(it), Toast.LENGTH_SHORT).show()
                viewModel.consumeMessage()
            }
        }

        val existing = binding.inputSearch.text?.toString().orEmpty()
        if (existing.length >= 2) {
            runSearch(existing)
        }
    }

    override fun onResume() {
        super.onResume()
        loadSavedLocation()
        val query = binding.inputSearch.text?.toString().orEmpty()
        if (query.length >= 2) {
            runSearch(query)
        }
    }

    private fun loadSavedLocation() {
        val saved = NewsLocationStore(requireContext()).load()
        scope = saved.scope.ifBlank { "Local" }
        country = saved.country.ifBlank {
            Locale.getDefault().displayCountry.takeIf { it.isNotBlank() } ?: "Philippines"
        }
        area = saved.area
    }

    private fun runSearch(query: String) {
        viewModel.search(
            query = query,
            scope = scope,
            country = country,
            area = area
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
