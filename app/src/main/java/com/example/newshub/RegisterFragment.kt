package com.example.newshub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.newshub.databinding.FragmentRegisterBinding
import com.example.newshub.network.ApiResult
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val supabaseService = SupabaseService()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textBackSignin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        binding.buttonCreateAccount.setOnClickListener {
            register()
        }
    }

    private fun register() {
        if (!supabaseService.isConfigured) {
            showToast(R.string.supabase_not_configured)
            return
        }

        val fullName = binding.editFullName.text?.toString()?.trim().orEmpty()
        val email = binding.editRegisterEmail.text?.toString()?.trim().orEmpty()
        val password = binding.editRegisterPassword.text?.toString().orEmpty()
        val confirmPassword = binding.editConfirmPassword.text?.toString().orEmpty()

        if (fullName.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            showToast(R.string.auth_register_required)
            return
        }

        if (password != confirmPassword) {
            showToast(R.string.profile_password_mismatch)
            return
        }

        if (password.length < 8) {
            showToast(R.string.profile_password_too_short)
            return
        }

        if (!binding.checkboxAttestation.isChecked) {
            showToast(R.string.auth_attestation_required)
            return
        }

        val names = fullName.split(" ").filter { it.isNotBlank() }
        val firstName = names.firstOrNull().orEmpty()
        val lastName = names.drop(1).joinToString(" ")

        binding.buttonCreateAccount.isEnabled = false
        binding.progressRegister.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = supabaseService.signUpWithPassword(email, password)
            binding.buttonCreateAccount.isEnabled = true
            binding.progressRegister.visibility = View.GONE

            when (result) {
                is ApiResult.Success -> {
                    val session = result.data
                    if (session != null) {
                        SessionPrefs.saveSession(requireContext(), session.userId, session.accessToken)
                        val profileSync = supabaseService.upsertProfile(
                            userId = session.userId,
                            firstName = firstName,
                            lastName = lastName,
                            avatarUrl = null,
                            email = email,
                            accessToken = session.accessToken
                        )
                        if (profileSync is ApiResult.Failure) {
                            showToast(UiErrorMapper.toMessageRes(profileSync.error))
                            return@launch
                        }

                        showToast(R.string.auth_register_success)
                        findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
                    } else {
                        showToast(R.string.auth_verify_email_required)
                        findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                    }
                }

                is ApiResult.Failure -> {
                    val fallback = if (result.error.statusCode == 400) {
                        R.string.auth_register_failed
                    } else {
                        UiErrorMapper.toMessageRes(result.error)
                    }
                    showToast(fallback)
                }
            }
        }
    }

    private fun showToast(messageRes: Int) {
        Toast.makeText(requireContext(), getString(messageRes), Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
