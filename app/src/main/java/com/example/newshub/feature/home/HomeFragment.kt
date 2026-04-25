package com.example.newshub.feature.home

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.newshub.NewsArticle
import com.example.newshub.R
import com.example.newshub.core.session.AndroidSessionStore
import com.example.newshub.databinding.FragmentHomeBinding
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
            val bundle = Bundle().apply {
                putString("articleUrl", article.articleUrl)
                putString("articleTitle", article.title)
                putString("articleSource", article.source)
                putString("articlePublishedAt", article.publishedAt)
                putString("articleSummary", article.summary)
                putString("articleCategory", article.category)
                putString("articleAuthor", article.author)
                putString("articleReadTime", article.readTime)
                putString("articleImage", article.imageUrl)
            }
            findNavController().navigate(R.id.action_homeFragment_to_newsDetailFragment, bundle)
        }
        binding.recyclerNews.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerNews.adapter = adapter

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
            binding.textEmpty.visibility = if (state.items.drop(1).isEmpty()) View.VISIBLE else View.GONE
            state.emptyMessageRes?.let { binding.textEmpty.setText(it) }
            binding.textLocation.text = getString(R.string.news_location_format, state.locationLabel)
            state.messageRes?.let {
                Toast.makeText(requireContext(), getString(it), Toast.LENGTH_SHORT).show()
                viewModel.consumeMessage()
            }
        }

        binding.buttonRefresh.setOnClickListener {
            if (hasLocationPermission()) {
                resolveLastKnownLocation()
            }
            refreshNews()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun refreshNews() {
        val locationLabel = resolveLocationLabel()
        binding.textLocation.text = getString(R.string.news_location_format, locationLabel)
        viewModel.loadNews(locationLabel = locationLabel, lat = lastLat, lng = lastLng)
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
                // Fallback to locale label when reverse geocoding is unavailable.
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
            val bundle = Bundle().apply {
                putString("articleUrl", article.articleUrl)
                putString("articleTitle", article.title)
                putString("articleSource", article.source)
                putString("articlePublishedAt", article.publishedAt)
                putString("articleSummary", article.summary)
                putString("articleCategory", article.category)
                putString("articleAuthor", article.author)
                putString("articleReadTime", article.readTime)
                putString("articleImage", article.imageUrl)
            }
            findNavController().navigate(R.id.action_homeFragment_to_newsDetailFragment, bundle)
        }
    }
}
