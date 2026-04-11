package com.example.newshub.feature.auth.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.newshub.R
import com.example.newshub.SessionPrefs
import com.example.newshub.databinding.FragmentLoginBinding

class LoginFragment : Fragment(), LoginContract.View {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var presenter: LoginContract.Presenter

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
        presenter = LoginPresenter()
        presenter.attach(this)

        binding.buttonSignIn.setOnClickListener {
            presenter.onSignInClicked(
                email = binding.editEmail.text?.toString().orEmpty(),
                password = binding.editPassword.text?.toString().orEmpty()
            )
        }

        binding.textForgot.setOnClickListener {
            presenter.onForgotPasswordClicked()
        }

        binding.textGoRegister.setOnClickListener {
            presenter.onRegisterClicked()
        }
    }

    override fun showLoading(isLoading: Boolean) {
        binding.buttonSignIn.isEnabled = !isLoading
        binding.progressLogin.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun showMessage(messageRes: Int) {
        Toast.makeText(requireContext(), getString(messageRes), Toast.LENGTH_SHORT).show()
    }

    override fun saveSession(userId: String, accessToken: String) {
        SessionPrefs.saveSession(requireContext(), userId, accessToken)
    }

    override fun navigateToHome() {
        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
    }

    override fun navigateToRegister() {
        findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detach()
        _binding = null
    }
}
