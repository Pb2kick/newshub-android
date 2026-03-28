package com.example.newshub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.newshub.databinding.FragmentHomeBinding
import com.example.newshub.network.ApiResult
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val supabaseService = SupabaseService()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        validateDashboardSession()

        val placeholderClick = View.OnClickListener {
            Toast.makeText(requireContext(), getString(R.string.home_placeholder_action), Toast.LENGTH_SHORT).show()
        }

        binding.buttonSearch.setOnClickListener(placeholderClick)
        binding.cardFeature.setOnClickListener(placeholderClick)
        binding.cardArticle1.setOnClickListener(placeholderClick)
        binding.cardArticle2.setOnClickListener(placeholderClick)
        binding.textRead1.setOnClickListener(placeholderClick)
        binding.textRead2.setOnClickListener(placeholderClick)
        binding.tabAll.setOnClickListener(placeholderClick)
        binding.tabPolitics.setOnClickListener(placeholderClick)
        binding.navNews.setOnClickListener(placeholderClick)
        binding.navElections.setOnClickListener(placeholderClick)
        binding.navAlerts.setOnClickListener(placeholderClick)
        binding.navProfile.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
        }
    }

    private fun validateDashboardSession() {
        val accessToken = SessionPrefs.getAccessToken(requireContext())
        if (accessToken.isNullOrBlank()) {
            navigateToLogin()
            return
        }

        binding.progressDashboard.visibility = View.VISIBLE
        lifecycleScope.launch {
            when (val result = supabaseService.fetchAuthUser(accessToken)) {
                is ApiResult.Success -> {
                    binding.progressDashboard.visibility = View.GONE
                    Toast.makeText(requireContext(), getString(R.string.home_dashboard_ready), Toast.LENGTH_SHORT).show()
                }

                is ApiResult.Failure -> {
                    binding.progressDashboard.visibility = View.GONE
                    if (result.error.statusCode == 401) {
                        SessionPrefs.clear(requireContext())
                        showToast(R.string.error_unauthorized)
                        navigateToLogin()
                    } else {
                        showToast(UiErrorMapper.toMessageRes(result.error))
                    }
                }
            }
        }
    }

    private fun navigateToLogin() {
        val options = NavOptions.Builder()
            .setPopUpTo(R.id.nav_graph, true)
            .build()
        findNavController().navigate(R.id.loginFragment, null, options)
    }

    private fun showToast(messageRes: Int) {
        Toast.makeText(requireContext(), getString(messageRes), Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
