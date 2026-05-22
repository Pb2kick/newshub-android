package com.example.newshub.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.example.newshub.R
import com.example.newshub.SupabaseService
import com.example.newshub.core.session.AndroidSessionStore
import com.example.newshub.databinding.BottomSheetReportBinding
import com.example.newshub.network.ApiResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReportBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetReportBinding? = null
    private val binding get() = _binding!!
    private val supabaseService = SupabaseService()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonClose.setOnClickListener { dismiss() }
        binding.buttonSubmit.setOnClickListener { submitReport() }
    }

    private fun submitReport() {
        val session = AndroidSessionStore(requireContext().applicationContext)
        val userId = session.getUserId()
        val token = session.getAccessToken()
        if (userId.isNullOrBlank() || token.isNullOrBlank()) {
            Toast.makeText(requireContext(), R.string.report_sign_in_required, Toast.LENGTH_SHORT).show()
            return
        }

        val targetType = requireArguments().getString(ARG_TARGET_TYPE).orEmpty()
        val targetId = requireArguments().getString(ARG_TARGET_ID).orEmpty()
        val targetLabel = requireArguments().getString(ARG_TARGET_LABEL)
        if (targetType.isBlank() || targetId.isBlank()) {
            dismiss()
            return
        }

        val reason = when (binding.groupReason.checkedRadioButtonId) {
            R.id.reason_spam -> getString(R.string.report_reason_spam)
            R.id.reason_harassment -> getString(R.string.report_reason_harassment)
            R.id.reason_misinformation -> getString(R.string.report_reason_misinformation)
            else -> getString(R.string.report_reason_other)
        }
        val details = binding.inputDetails.text?.toString()

        binding.buttonSubmit.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                supabaseService.submitReport(
                    targetType = targetType,
                    targetId = targetId,
                    targetLabel = targetLabel,
                    reason = reason,
                    details = details,
                    reporterUserId = userId,
                    accessToken = token
                )
            }
            binding.buttonSubmit.isEnabled = true
            if (result is ApiResult.Success) {
                Toast.makeText(requireContext(), R.string.report_success, Toast.LENGTH_SHORT).show()
                dismiss()
            } else {
                Toast.makeText(requireContext(), R.string.report_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TARGET_TYPE = "target_type"
        private const val ARG_TARGET_ID = "target_id"
        private const val ARG_TARGET_LABEL = "target_label"

        fun newInstance(
            targetType: String,
            targetId: String,
            targetLabel: String?
        ): ReportBottomSheetFragment {
            return ReportBottomSheetFragment().apply {
                arguments = bundleOf(
                    ARG_TARGET_TYPE to targetType,
                    ARG_TARGET_ID to targetId,
                    ARG_TARGET_LABEL to targetLabel
                )
            }
        }
    }
}
