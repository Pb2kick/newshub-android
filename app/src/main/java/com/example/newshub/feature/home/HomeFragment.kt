package com.example.newshub.feature.home

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.newshub.NewsArticle
import com.example.newshub.R
import com.example.newshub.SupabaseService
import com.example.newshub.core.session.AndroidSessionStore
import com.example.newshub.databinding.FragmentHomeBinding
import com.example.newshub.network.ApiResult
import com.example.newshub.toDetailBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(AndroidSessionStore(requireContext().applicationContext))
    }
    private lateinit var adapter: NewsAdapter
    private var lastLat: Double? = null
    private var lastLng: Double? = null
    private var selectedCategory = "Top Stories"
    private var selectedScope = "Local"

    private val supabaseService = SupabaseService()
    private val notificationPollHandler = Handler(Looper.getMainLooper())
    private val notificationPollRunnable = object : Runnable {
        override fun run() {
            refreshUnreadBadge()
            notificationPollHandler.postDelayed(this, 60_000L)
        }
    }

    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            resolveLastKnownLocation()
            refreshNews()
        } else {
            refreshNews()
        }
    }

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
        adapter = NewsAdapter { article ->
            findNavController().navigate(
                R.id.action_homeFragment_to_newsDetailFragment,
                article.toDetailBundle()
            )
        }
        binding.recyclerNews.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerNews.adapter = adapter

        setupCategoryChips()
        setupScopeToggle()

        if (hasLocationPermission()) {
            resolveLastKnownLocation()
        } else {
            requestLocationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        refreshNews()

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.progressDashboard.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            val featured = state.items.firstOrNull()
            bindFeaturedArticle(featured)
            adapter.submitList(state.items.drop(1))
            binding.textEmpty.visibility = if (state.items.drop(1).isEmpty() && !state.isLoading) {
                View.VISIBLE
            } else {
                View.GONE
            }
            state.emptyMessageRes?.let { binding.textEmpty.setText(it) }
            binding.textLocation.text = getString(R.string.news_location_format, state.locationLabel)

            val showSeeMore = state.hasMore && !state.isLoading
            binding.buttonSeeMore.visibility = if (showSeeMore) View.VISIBLE else View.GONE
            binding.buttonSeeMore.isEnabled = !state.isLoadingMore
            binding.progressSeeMore.visibility = if (state.isLoadingMore) View.VISIBLE else View.GONE
            binding.buttonSeeMore.text = if (state.isLoadingMore) "" else getString(R.string.see_more)

            state.messageRes?.let {
                Toast.makeText(requireContext(), getString(it), Toast.LENGTH_SHORT).show()
                viewModel.consumeMessage()
            }
        }

        binding.buttonSeeMore.setOnClickListener { viewModel.loadMore() }
        binding.buttonRefresh.setOnClickListener {
            if (hasLocationPermission()) {
                resolveLastKnownLocation()
            }
            refreshNews()
        }
        binding.buttonSearch.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_searchFragment)
        }
        binding.buttonNotifications.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_notificationsFragment)
        }
        binding.buttonProfileShortcut.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
        }
        binding.buttonMenu.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.home_placeholder_action), Toast.LENGTH_SHORT).show()
        }
        binding.navNews.setOnClickListener { }
        binding.navElections.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_electionsFragment)
        }
        binding.navProfile.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUnreadBadge()
        notificationPollHandler.postDelayed(notificationPollRunnable, 60_000L)
    }

    override fun onPause() {
        notificationPollHandler.removeCallbacks(notificationPollRunnable)
        super.onPause()
    }

    override fun onDestroyView() {
        notificationPollHandler.removeCallbacks(notificationPollRunnable)
        super.onDestroyView()
        _binding = null
    }

    private fun setupCategoryChips() {
        val categoryByChipId = mapOf(
            R.id.chip_top_stories to "Top Stories",
            R.id.chip_politics to "Politics",
            R.id.chip_economy to "Economy",
            R.id.chip_technology to "Technology",
            R.id.chip_elections_category to "Elections"
        )
        binding.chipGroupCategory.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            selectedCategory = categoryByChipId[checkedId] ?: "Top Stories"
            refreshNews()
        }
    }

    private fun setupScopeToggle() {
        binding.toggleScope.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            selectedScope = when (checkedId) {
                R.id.button_scope_national -> "National"
                R.id.button_scope_international -> "International"
                else -> "Local"
            }
            refreshNews()
        }
    }

    private fun refreshNews() {
        val locationLabel = resolveLocationLabel()
        binding.textLocation.text = getString(R.string.news_location_format, locationLabel)
        viewModel.loadNews(
            locationLabel = locationLabel,
            lat = lastLat,
            lng = lastLng,
            category = selectedCategory,
            scope = selectedScope
        )
    }

    private fun refreshUnreadBadge() {
        val sessionStore = AndroidSessionStore(requireContext().applicationContext)
        val userId = sessionStore.getUserId()
        val token = sessionStore.getAccessToken()
        if (userId.isNullOrBlank() || token.isNullOrBlank() || !supabaseService.isConfigured) {
            binding.badgeNotifications.visibility = View.GONE
            return
        }
        lifecycleScope.launch {
            val count = withContext(Dispatchers.IO) {
                when (val result = supabaseService.fetchNotifications(userId, token)) {
                    is ApiResult.Success -> result.data.count { !it.isRead }
                    is ApiResult.Failure -> 0
                }
            }
            if (_binding == null) return@launch
            if (count > 0) {
                binding.badgeNotifications.visibility = View.VISIBLE
                binding.badgeNotifications.text = if (count > 9) "9+" else count.toString()
            } else {
                binding.badgeNotifications.visibility = View.GONE
            }
        }
    }

    private fun resolveLocationLabel(): String {
        val lat = lastLat
        val lng = lastLng
        if (lat != null && lng != null) {
            try {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                val address = addresses?.firstOrNull()
                val locality = address?.locality.orEmpty()
                val admin = address?.adminArea.orEmpty()
                val country = address?.countryName.orEmpty()

                return listOf(locality, admin, country)
                    .filter { it.isNotBlank() }
                    .distinct()
                    .joinToString(", ")
                    .ifBlank { country.ifBlank { "Global" } }
            } catch (_: Throwable) {
                // Fallback when reverse geocoding is unavailable.
            }
        }

        return Locale.getDefault().displayCountry.takeIf { it.isNotBlank() } ?: "Global"
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun resolveLastKnownLocation() {
        val hasFine = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return

        val manager = requireContext().getSystemService(LocationManager::class.java) ?: return
        val providers = manager.getProviders(true)
        var bestTime = Long.MIN_VALUE
        for (provider in providers) {
            val location = runCatching { manager.getLastKnownLocation(provider) }.getOrNull() ?: continue
            if (location.time >= bestTime) {
                bestTime = location.time
                lastLat = location.latitude
                lastLng = location.longitude
            }
        }
    }

    private fun bindFeaturedArticle(article: NewsArticle?) {
        if (article == null) {
            binding.cardFeature.visibility = View.GONE
            return
        }

        binding.cardFeature.visibility = View.VISIBLE
        binding.imageFeature.load(article.imageUrl) {
            crossfade(true)
            placeholder(R.drawable.bg_home_feature_placeholder)
            error(R.drawable.bg_home_feature_placeholder)
        }
        binding.textFeatureCategory.text = article.category
        binding.textFeatureTitle.text = article.title
        binding.textFeatureSummary.text = article.summary
        binding.textFeatureMeta.text = listOf(article.author, article.publishedAt)
            .filter { it.isNotBlank() }
            .joinToString(" • ")
        binding.cardFeature.setOnClickListener {
            findNavController().navigate(
                R.id.action_homeFragment_to_newsDetailFragment,
                article.toDetailBundle()
            )
        }
    }
}
