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
import com.example.newshub.databinding.FragmentVoteVerificationBinding

class VoteVerificationFragment : Fragment() {

    private var _binding: FragmentVoteVerificationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VoteVerificationViewModel by viewModels {
        VoteVerificationViewModelFactory(AndroidSessionStore(requireContext().applicationContext))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVoteVerificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val receiptId = arguments?.getString("receiptId").orEmpty()
        val voteMessage = arguments?.getString("voteMessage").orEmpty()

        binding.textReceiptValue.text = receiptId
        binding.buttonDone.setOnClickListener {
            findNavController().navigate(R.id.homeFragment)
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.progressVerify.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            binding.textStatusValue.text = state.status
            binding.textMessageValue.text = state.message
            state.messageRes?.let {
                Toast.makeText(requireContext(), getString(it), Toast.LENGTH_SHORT).show()
                viewModel.consumeMessageRes()
            }
        }

        viewModel.verify(receiptId = receiptId, defaultMessage = voteMessage)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

