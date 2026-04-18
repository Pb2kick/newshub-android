package com.example.newshub.feature.voting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.newshub.R
import com.example.newshub.core.session.AndroidSessionStore
import com.example.newshub.databinding.FragmentVoteConfirmBinding

class VoteConfirmFragment : Fragment() {

    private var _binding: FragmentVoteConfirmBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VoteConfirmViewModel by viewModels {
        VoteConfirmViewModelFactory(AndroidSessionStore(requireContext().applicationContext))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVoteConfirmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val electionId = arguments?.getString("electionId").orEmpty()
        val electionName = arguments?.getString("electionName").orEmpty()
        val candidateId = arguments?.getString("candidateId").orEmpty()
        val candidateName = arguments?.getString("candidateName").orEmpty()

        binding.textElectionName.text = electionName
        binding.textCandidateName.text = candidateName

        binding.buttonBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.buttonConfirmVote.setOnClickListener {
            viewModel.submitVote(electionId = electionId, candidateId = candidateId)
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.progressSubmit.visibility = if (state.isSubmitting) View.VISIBLE else View.GONE
            binding.buttonConfirmVote.isEnabled = !state.isSubmitting
            state.messageRes?.let {
                Toast.makeText(requireContext(), getString(it), Toast.LENGTH_SHORT).show()
                viewModel.consumeMessageRes()
            }
            if (!state.receiptId.isNullOrBlank()) {
                val args = Bundle().apply {
                    putString("receiptId", state.receiptId)
                    putString("voteMessage", state.message)
                }
                findNavController().navigate(R.id.action_voteConfirmFragment_to_voteVerificationFragment, args)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

