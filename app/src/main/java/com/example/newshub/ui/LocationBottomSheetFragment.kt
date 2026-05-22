package com.example.newshub.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newshub.R
import com.example.newshub.core.location.NewsLocationContext
import com.example.newshub.core.location.NewsLocationStore
import com.example.newshub.databinding.BottomSheetLocationBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlin.coroutines.resume

class LocationBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetLocationBinding? = null
    private val binding get() = _binding!!
    private val httpClient = OkHttpClient()
    private var searchJob: Job? = null
    private lateinit var suggestionAdapter: SuggestionAdapter

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) applyCurrentLocation() else {
            Toast.makeText(requireContext(), R.string.location_permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val store = NewsLocationStore(requireContext())
        val saved = store.load()
        if (saved.label.isNotBlank()) {
            binding.inputManualLocation.setText(saved.label)
        }

        suggestionAdapter = SuggestionAdapter { suggestion ->
            applyManualLocation(suggestion)
        }
        binding.recyclerSuggestions.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSuggestions.adapter = suggestionAdapter

        binding.buttonClose.setOnClickListener { dismiss() }
        binding.cardUseCurrent.setOnClickListener { requestCurrentLocation() }
        binding.buttonApplyManual.setOnClickListener {
            val query = binding.inputManualLocation.text?.toString().orEmpty().trim()
            if (query.isBlank()) {
                Toast.makeText(requireContext(), R.string.location_manual_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            resolveManualQuery(query)
        }

        binding.inputManualLocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString().orEmpty().trim()
                if (query.length < 2) {
                    binding.recyclerSuggestions.visibility = View.GONE
                    suggestionAdapter.submit(emptyList())
                    return
                }
                searchJob?.cancel()
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(300)
                    loadSuggestions(query)
                }
            }
        })
    }

    private fun requestCurrentLocation() {
        if (hasLocationPermission()) {
            applyCurrentLocation()
        } else {
            requestPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun applyCurrentLocation() {
        binding.progressLocation.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val resolved = withContext(Dispatchers.IO) { readLastKnownLocation() }
            binding.progressLocation.visibility = View.GONE
            if (resolved == null) {
                Toast.makeText(requireContext(), R.string.location_unavailable, Toast.LENGTH_SHORT).show()
                return@launch
            }
            publishLocation(resolved)
        }
    }

    @SuppressLint("MissingPermission")
    private fun readLastKnownLocation(): NewsLocationContext? {
        val manager = requireContext().getSystemService(LocationManager::class.java) ?: return null
        var bestTime = Long.MIN_VALUE
        var lat: Double? = null
        var lng: Double? = null
        for (provider in manager.getProviders(true)) {
            val location = runCatching { manager.getLastKnownLocation(provider) }.getOrNull() ?: continue
            if (location.time >= bestTime) {
                bestTime = location.time
                lat = location.latitude
                lng = location.longitude
            }
        }
        val latitude = lat ?: return null
        val longitude = lng ?: return null
        return geocode(latitude, longitude)
    }

    private fun geocode(lat: Double, lng: Double): NewsLocationContext? {
        return try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // We use a blocking approach for simplicity in this background context
                // but technically we should use the callback version for API 33+
                // However, since we are already in withContext(Dispatchers.IO), 
                // the deprecated version is still functional and easier to use synchronously.
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()
            } ?: return null

            val locality = address.locality.orEmpty()
            val admin = address.adminArea.orEmpty()
            val country = address.countryName.orEmpty().ifBlank { Locale.getDefault().displayCountry }
            val area = locality.ifBlank { admin }.ifBlank { country }
            val label = listOf(locality, admin, country).filter { it.isNotBlank() }.distinct().joinToString(", ")
            NewsLocationContext(
                label = label.ifBlank { country },
                area = area,
                country = country,
                scope = "Local",
                latitude = lat,
                longitude = lng
            )
        } catch (_: Throwable) {
            null
        }
    }

    private fun resolveManualQuery(query: String) {
        binding.progressLocation.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val suggestion = withContext(Dispatchers.IO) { geocodeQuery(query) }
            binding.progressLocation.visibility = View.GONE
            if (suggestion != null) {
                applyManualLocation(suggestion)
            } else {
                val fallbackCountry = Locale.getDefault().displayCountry.ifBlank { "Philippines" }
                applyManualLocation(
                    NewsLocationContext(
                        label = query,
                        area = query,
                        country = fallbackCountry,
                        scope = "Local",
                        latitude = null,
                        longitude = null
                    )
                )
            }
        }
    }

    private suspend fun loadSuggestions(query: String) {
        binding.progressLocation.visibility = View.VISIBLE
        val results = withContext(Dispatchers.IO) { fetchNominatimSuggestions(query) }
        binding.progressLocation.visibility = View.GONE
        suggestionAdapter.submit(results)
        binding.recyclerSuggestions.visibility = if (results.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun fetchNominatimSuggestions(query: String): List<NewsLocationContext> {
        return try {
            val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
            val url = "https://nominatim.openstreetmap.org/search?q=$encoded&format=jsonv2&addressdetails=1&limit=6"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "NewsHub-Android/1.0")
                .build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string().orEmpty()
            val array = JsonParser.parseString(body).asJsonArray
            parseNominatim(array)
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun geocodeQuery(query: String): NewsLocationContext? {
        return fetchNominatimSuggestions(query).firstOrNull()
    }

    private fun parseNominatim(array: JsonArray): List<NewsLocationContext> {
        val results = mutableListOf<NewsLocationContext>()
        for (element in array) {
            val obj = element.asJsonObject
            val display = obj.get("display_name")?.asString.orEmpty()
            if (display.isBlank()) continue
            val address = obj.getAsJsonObject("address")
            val city = address?.get("city")?.asString
                ?: address?.get("town")?.asString
                ?: address?.get("village")?.asString
                ?: address?.get("state")?.asString
                ?: ""
            val country = address?.get("country")?.asString.orEmpty()
            val lat = obj.get("lat")?.asString?.toDoubleOrNull()
            val lng = obj.get("lon")?.asString?.toDoubleOrNull()
            results += NewsLocationContext(
                label = display,
                area = city.ifBlank { display.substringBefore(",") },
                country = country.ifBlank { Locale.getDefault().displayCountry },
                scope = "Local",
                latitude = lat,
                longitude = lng
            )
        }
        return results.distinctBy { it.label }.take(6)
    }

    private fun applyManualLocation(context: NewsLocationContext) {
        publishLocation(context)
    }

    private fun publishLocation(context: NewsLocationContext) {
        NewsLocationStore(requireContext()).save(context)
        setFragmentResult(
            REQUEST_KEY,
            bundleOf(BUNDLE_LOCATION to context)
        )
        dismiss()
    }

    override fun onDestroyView() {
        searchJob?.cancel()
        super.onDestroyView()
        _binding = null
    }

    private class SuggestionAdapter(
        private val onSelect: (NewsLocationContext) -> Unit
    ) : RecyclerView.Adapter<SuggestionAdapter.Holder>() {

        private val items = mutableListOf<NewsLocationContext>()

        fun submit(list: List<NewsLocationContext>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            return Holder(view)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(items[position], onSelect)
        }

        override fun getItemCount(): Int = items.size

        class Holder(view: View) : RecyclerView.ViewHolder(view) {
            private val text = view.findViewById<android.widget.TextView>(android.R.id.text1)

            fun bind(item: NewsLocationContext, onSelect: (NewsLocationContext) -> Unit) {
                text.text = item.label
                itemView.setOnClickListener { onSelect(item) }
            }
        }
    }

    companion object {
        const val REQUEST_KEY = "location_sheet_result"
        const val BUNDLE_LOCATION = "location_context"

        fun newInstance(): LocationBottomSheetFragment = LocationBottomSheetFragment()
    }
}
