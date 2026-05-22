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
import com.example.newshub.ui.BottomNavHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
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

            // FIX: 72dp padding let items scroll into the "See more" touch band; use 8dp when button visible
            val bottomPadding = if (showSeeMore) {
                resources.getDimensionPixelSize(R.dimen.home_list_padding_above_see_more)
            } else {
                resources.getDimensionPixelSize(R.dimen.home_list_padding_above_nav)
            }
            binding.recyclerNews.setPadding(
                binding.recyclerNews.paddingLeft,
                binding.recyclerNews.paddingTop,
                binding.recyclerNews.paddingRight,
                bottomPadding
            )

            state.messageRes?.let {
                Toast.makeText(requireContext(), getString(it), Toast.LENGTH_SHORT).show()
                viewModel.consumeMessage()
            }
        }

        binding.buttonSeeMore.setOnClickListener { viewModel.loadMore() }
        binding.topBarInclude.buttonSearch.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_searchFragment)
        }
        binding.topBarInclude.buttonLocation.setOnClickListener {
            if (hasLocationPermission()) {
                resolveLastKnownLocation()
            } else {
                requestLocationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            refreshNews()
        }
        BottomNavHelper.wire(
            fragment = this,
            navHome = binding.bottomNavBar.navHome,
            navElections = binding.bottomNavBar.navElections,
            navAlerts = binding.bottomNavBar.navAlerts,
            navProfile = binding.bottomNavBar.navProfile
        )
        binding.bottomNavBar.navElections.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_electionsFragment)
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
        val location = resolveLocationContext()
        binding.textLocation.text = getString(R.string.news_location_format, location.label)
        viewModel.loadNews(
            locationLabel = location.label,
            lat = lastLat,
            lng = lastLng,
            category = selectedCategory,
            scope = selectedScope,
            country = location.country,
            area = location.area
        )
    }

    private fun refreshUnreadBadge() {
        val sessionStore = AndroidSessionStore(requireContext().applicationContext)
        val userId = sessionStore.getUserId()
        val token = sessionStore.getAccessToken()
        if (userId.isNullOrBlank() || token.isNullOrBlank() || !supabaseService.isConfigured) {
            binding.bottomNavBar.badgeNavAlerts.visibility = View.GONE
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
            binding.bottomNavBar.badgeNavAlerts.visibility = if (count > 0) View.VISIBLE else View.GONE
        }
    }

    private data class LocationContext(
        val label: String,
        val country: String,
        val area: String
    )

    private fun resolveLocationContext(): LocationContext {
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
                val area = locality.ifBlank { admin }
                val label = listOf(locality, admin, country)
                    .filter { it.isNotBlank() }
                    .distinct()
                    .joinToString(", ")
                    .ifBlank { country.ifBlank { "Global" } }
                return LocationContext(
                    label = label,
                    country = country.ifBlank { Locale.getDefault().displayCountry },
                    area = area.ifBlank { admin.ifBlank { country } }
                )
            } catch (_: Throwable) {
                // Fallback when reverse geocoding is unavailable.
            }
        }

        val fallbackCountry = Locale.getDefault().displayCountry.takeIf { it.isNotBlank() } ?: "Philippines"
        return LocationContext(
            label = fallbackCountry,
            country = fallbackCountry,
            area = ""
        )
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
