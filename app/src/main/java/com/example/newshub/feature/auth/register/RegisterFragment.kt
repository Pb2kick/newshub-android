package com.example.newshub.feature.auth.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.newshub.R
import com.example.newshub.SessionPrefs
import com.example.newshub.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment(), RegisterContract.View {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private lateinit var presenter: RegisterContract.Presenter

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
        presenter = RegisterPresenter()
        presenter.attach(this)

        binding.textBackSignin.setOnClickListener {
            presenter.onBackToSignInClicked()
        }

        binding.buttonCreateAccount.setOnClickListener {
            presenter.onCreateAccountClicked(
                fullName = binding.editFullName.text?.toString().orEmpty(),
                email = binding.editRegisterEmail.text?.toString().orEmpty(),
                password = binding.editRegisterPassword.text?.toString().orEmpty(),
                confirmPassword = binding.editConfirmPassword.text?.toString().orEmpty(),
                isAttested = binding.checkboxAttestation.isChecked
            )
        }
    }

    override fun showLoading(isLoading: Boolean) {
        binding.buttonCreateAccount.isEnabled = !isLoading
        binding.progressRegister.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun showMessage(messageRes: Int) {
        Toast.makeText(requireContext(), getString(messageRes), Toast.LENGTH_SHORT).show()
    }

    override fun saveSession(userId: String, accessToken: String) {
        SessionPrefs.saveSession(requireContext(), userId, accessToken)
    }

    override fun navigateToLogin() {
        findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
    }

    override fun navigateToHome() {
        findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detach()
        _binding = null
    }
}
