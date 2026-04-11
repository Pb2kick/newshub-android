package com.example.newshub.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.newshub.R
import com.example.newshub.core.session.AndroidSessionStore
import com.example.newshub.databinding.FragmentHomeBinding

class HomeFragment : Fragment(), HomeContract.View {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var presenter: HomeContract.Presenter

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
        presenter = HomePresenter(AndroidSessionStore(requireContext().applicationContext))
        presenter.attach(this)

        presenter.onScreenStarted()

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
            presenter.onProfileClicked()
        }
    }

    override fun showLoading(isLoading: Boolean) {
        binding.progressDashboard.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun showMessage(messageRes: Int) {
        Toast.makeText(requireContext(), getString(messageRes), Toast.LENGTH_SHORT).show()
    }

    override fun navigateToLogin() {
        val options = NavOptions.Builder()
            .setPopUpTo(R.id.nav_graph, true)
            .build()
        findNavController().navigate(R.id.loginFragment, null, options)
    }

    override fun navigateToProfile() {
        findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detach()
        _binding = null
    }
}
