package com.example.newshub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.newshub.databinding.FragmentLoginBinding
import com.example.newshub.network.ApiResult
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val supabaseService = SupabaseService()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSignIn.setOnClickListener {
            signIn()
        }

        binding.textForgot.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.forgot_password_placeholder), Toast.LENGTH_SHORT).show()
        }

        binding.textGoRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    private fun signIn() {
        if (!supabaseService.isConfigured) {
            showToast(R.string.supabase_not_configured)
            return
        }

        val email = binding.editEmail.text?.toString()?.trim().orEmpty()
        val password = binding.editPassword.text?.toString().orEmpty()

        if (email.isBlank() || password.isBlank()) {
            showToast(R.string.auth_login_required)
            return
        }

        binding.buttonSignIn.isEnabled = false
        binding.progressLogin.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = supabaseService.signInWithPassword(email, password)
            binding.buttonSignIn.isEnabled = true
            binding.progressLogin.visibility = View.GONE

            when (result) {
                is ApiResult.Success -> {
                    val session = result.data
                    SessionPrefs.saveSession(requireContext(), session.userId, session.accessToken)
                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                }

                is ApiResult.Failure -> {
                    val fallback = if (result.error.statusCode == 400 || result.error.statusCode == 401) {
                        R.string.auth_login_failed
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
