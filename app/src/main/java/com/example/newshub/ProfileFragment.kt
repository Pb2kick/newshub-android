package com.example.newshub

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import coil.load
import coil.request.CachePolicy
import com.example.newshub.databinding.FragmentProfileBinding
import com.example.newshub.network.ApiFailureType
import com.example.newshub.network.ApiResult
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val supabaseService = SupabaseService()
    private var avatarUrl: String? = null
    private var userEmail: String? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            binding.imageProfileAvatar.load(uri)
            uploadAvatarToSupabase(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showAvatarPlaceholder()

        val placeholderClick = View.OnClickListener {
            Toast.makeText(requireContext(), getString(R.string.home_placeholder_action), Toast.LENGTH_SHORT).show()
        }

        binding.textBackHome.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_homeFragment)
        }
        binding.navNews.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_homeFragment)
        }
        binding.navElections.setOnClickListener(placeholderClick)
        binding.navAlerts.setOnClickListener(placeholderClick)
        binding.buttonLogout.setOnClickListener {
            SessionPrefs.clear(requireContext())
            navigateToLogin()
        }
        binding.textDevRegister.setOnClickListener(placeholderClick)
        binding.textDevSignin.setOnClickListener(placeholderClick)

        binding.buttonUploadPhoto.setOnClickListener {
            if (!supabaseService.isConfigured) {
                showToast(R.string.supabase_not_configured)
                return@setOnClickListener
            }
            pickImage.launch("image/*")
        }

        val nameWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                updateFullName()
            }
        }

        binding.editFirstName.addTextChangedListener(nameWatcher)
        binding.editLastName.addTextChangedListener(nameWatcher)
        updateFullName()

        if (supabaseService.isConfigured) {
            fetchAndBindProfile()
        }

        binding.buttonSaveName.setOnClickListener {
            val firstName = binding.editFirstName.text?.toString()?.trim().orEmpty()
            val lastName = binding.editLastName.text?.toString()?.trim().orEmpty()

            if (firstName.isBlank() || lastName.isBlank()) {
                showToast(R.string.profile_name_required)
                return@setOnClickListener
            }

            val userId = getSupabaseUserId()
            if (userId.isNullOrBlank()) {
                showToast(R.string.supabase_session_missing)
                return@setOnClickListener
            }

            setLoading(true)
            lifecycleScope.launch {
                val result = supabaseService.upsertProfile(
                    userId = userId,
                    firstName = firstName,
                    lastName = lastName,
                    avatarUrl = avatarUrl,
                    email = userEmail,
                    accessToken = getSupabaseAccessToken()
                )
                setLoading(false)

                when (result) {
                    is ApiResult.Success -> showToast(R.string.profile_name_saved)
                    is ApiResult.Failure -> handleFailure(result)
                }
            }
        }

        binding.buttonUpdatePassword.setOnClickListener {
            val currentPassword = binding.editCurrentPassword.text?.toString().orEmpty()
            val newPassword = binding.editNewPassword.text?.toString().orEmpty()
            val confirmPassword = binding.editConfirmNewPassword.text?.toString().orEmpty()

            if (currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
                showToast(R.string.profile_password_required)
                return@setOnClickListener
            }

            if (newPassword.length < 8) {
                showToast(R.string.profile_password_too_short)
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                showToast(R.string.profile_password_mismatch)
                return@setOnClickListener
            }

            val accessToken = getSupabaseAccessToken()
            if (!supabaseService.isConfigured || accessToken.isNullOrBlank()) {
                showToast(R.string.supabase_session_missing)
                return@setOnClickListener
            }

            setLoading(true)
            lifecycleScope.launch {
                when (val result = supabaseService.updatePassword(newPassword, accessToken)) {
                    is ApiResult.Success -> {
                        binding.editCurrentPassword.text?.clear()
                        binding.editNewPassword.text?.clear()
                        binding.editConfirmNewPassword.text?.clear()
                        showToast(R.string.profile_password_updated)
                    }

                    is ApiResult.Failure -> handleFailure(result)
                }
                setLoading(false)
            }
        }
    }

    private fun fetchAndBindProfile() {
        val sessionUserId = getSupabaseUserId()
        val accessToken = getSupabaseAccessToken()
        if (sessionUserId.isNullOrBlank() || accessToken.isNullOrBlank()) {
            showToast(R.string.supabase_session_missing)
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            val authResult = supabaseService.fetchAuthUser(accessToken)
            if (authResult is ApiResult.Failure) {
                setLoading(false)
                handleFailure(authResult)
                return@launch
            }

            val authUser = (authResult as ApiResult.Success).data
            val effectiveUserId = authUser.userId.ifBlank { sessionUserId }
            if (effectiveUserId != sessionUserId) {
                SessionPrefs.saveSession(requireContext(), effectiveUserId, accessToken)
            }

            userEmail = authUser.email
            when (val profileResult = supabaseService.fetchProfile(effectiveUserId, accessToken)) {
                is ApiResult.Success -> {
                    val resolved = resolveProfile(profileResult.data, authUser)
                    if (resolved != null) {
                        bindProfile(resolved)
                        if (profileResult.data == null && resolved.firstName.isNotBlank() && resolved.lastName.isNotBlank()) {
                            supabaseService.upsertProfile(
                                userId = effectiveUserId,
                                firstName = resolved.firstName,
                                lastName = resolved.lastName,
                                avatarUrl = resolved.avatarUrl,
                                email = userEmail,
                                accessToken = accessToken
                            )
                        }
                    }
                }

                is ApiResult.Failure -> handleFailure(profileResult)
            }
            setLoading(false)
        }
    }

    private fun resolveProfile(tableProfile: ProfileRecord?, authUser: AuthUserRecord): ProfileRecord? {
        if (tableProfile != null) {
            val enriched = mergeWithAuthFallback(tableProfile, authUser)
            return if (hasAnyProfileData(enriched)) enriched else null
        }

        val inferred = inferNames(authUser.firstName, authUser.lastName, authUser.fullName)
        return ProfileRecord(
            firstName = inferred.first,
            lastName = inferred.second,
            fullName = listOf(inferred.first, inferred.second).filter { it.isNotBlank() }.joinToString(" "),
            avatarUrl = authUser.avatarUrl
        ).takeIf { hasAnyProfileData(it) }
    }

    private fun mergeWithAuthFallback(tableProfile: ProfileRecord, authUser: AuthUserRecord): ProfileRecord {
        val inferred = inferNames(authUser.firstName, authUser.lastName, authUser.fullName)
        val firstName = tableProfile.firstName.ifBlank { inferred.first }
        val lastName = tableProfile.lastName.ifBlank { inferred.second }
        val fullName = tableProfile.fullName.ifBlank {
            listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
        }

        return ProfileRecord(
            firstName = firstName,
            lastName = lastName,
            fullName = fullName,
            avatarUrl = tableProfile.avatarUrl ?: authUser.avatarUrl
        )
    }

    private fun bindProfile(profile: ProfileRecord) {
        binding.editFirstName.setText(profile.firstName)
        binding.editLastName.setText(profile.lastName)
        avatarUrl = normalizeAvatarUrl(profile.avatarUrl)
        loadAvatar(avatarUrl)
        updateFullName()
        showToast(R.string.profile_loaded)
    }

    private fun inferNames(firstName: String, lastName: String, fullName: String): Pair<String, String> {
        if (firstName.isNotBlank() || lastName.isNotBlank()) {
            return firstName to lastName
        }

        val parts = fullName.trim().split(" ").filter { it.isNotBlank() }
        if (parts.isEmpty()) return "" to ""
        return parts.first() to parts.drop(1).joinToString(" ")
    }

    private fun hasAnyProfileData(profile: ProfileRecord): Boolean {
        return profile.firstName.isNotBlank() || profile.lastName.isNotBlank() || !profile.avatarUrl.isNullOrBlank()
    }

    private fun uploadAvatarToSupabase(uri: Uri) {
        val userId = getSupabaseUserId()
        if (userId.isNullOrBlank()) {
            showToast(R.string.supabase_session_missing)
            return
        }

        val mimeType = requireContext().contentResolver.getType(uri) ?: "image/jpeg"
        val bytes = requireContext().contentResolver.openInputStream(uri)?.use { it.readBytes() }
        if (bytes == null) {
            showToast(R.string.profile_picture_update_failed)
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            when (val uploadResult = supabaseService.uploadAvatar(
                userId = userId,
                bytes = bytes,
                mimeType = mimeType,
                accessToken = getSupabaseAccessToken()
            )) {
                is ApiResult.Success -> {
                    val uploadedUrl = uploadResult.data
                    avatarUrl = uploadedUrl
                    loadAvatar(uploadedUrl)

                    val firstName = binding.editFirstName.text?.toString()?.trim().orEmpty()
                    val lastName = binding.editLastName.text?.toString()?.trim().orEmpty()
                    if (firstName.isNotBlank() && lastName.isNotBlank()) {
                        val upsertResult = supabaseService.upsertProfile(
                            userId = userId,
                            firstName = firstName,
                            lastName = lastName,
                            avatarUrl = uploadedUrl,
                            email = userEmail,
                            accessToken = getSupabaseAccessToken()
                        )
                        if (upsertResult is ApiResult.Failure) {
                            handleFailure(upsertResult)
                            return@launch
                        }
                    }
                    showToast(R.string.profile_picture_updated)
                }

                is ApiResult.Failure -> handleFailure(uploadResult)
            }
            setLoading(false)
        }
    }

    private fun handleFailure(result: ApiResult.Failure) {
        if (result.error.type == ApiFailureType.Unauthorized || result.error.statusCode == 401) {
            SessionPrefs.clear(requireContext())
            showToast(R.string.error_unauthorized)
            navigateToLogin()
            return
        }

        showToast(UiErrorMapper.toMessageRes(result.error))
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressProfile.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonSaveName.isEnabled = !isLoading
        binding.buttonUploadPhoto.isEnabled = !isLoading
        binding.buttonUpdatePassword.isEnabled = !isLoading
    }

    private fun navigateToLogin() {
        val options = NavOptions.Builder()
            .setPopUpTo(R.id.nav_graph, true)
            .build()
        findNavController().navigate(R.id.loginFragment, null, options)
    }

    private fun getSupabaseUserId(): String? {
        return SessionPrefs.getUserId(requireContext())
    }

    private fun getSupabaseAccessToken(): String? {
        return SessionPrefs.getAccessToken(requireContext())
    }

    private fun showToast(messageRes: Int) {
        Toast.makeText(requireContext(), getString(messageRes), Toast.LENGTH_SHORT).show()
    }

    private fun updateFullName() {
        val firstName = binding.editFirstName.text?.toString()?.trim().orEmpty()
        val lastName = binding.editLastName.text?.toString()?.trim().orEmpty()
        val fullName = listOf(firstName, lastName)
            .filter { it.isNotBlank() }
            .joinToString(" ")
        binding.editFullName.setText(fullName)
    }

    private fun loadAvatar(rawAvatarUrl: String?) {
        val resolved = normalizeAvatarUrl(rawAvatarUrl)
        if (resolved.isNullOrBlank()) {
            showAvatarPlaceholder()
            return
        }

        binding.imageProfileAvatar.apply {
            // Remove icon-only styling before rendering a real photo.
            imageTintList = null
            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            setPadding(0, 0, 0, 0)
        }

        binding.imageProfileAvatar.load(resolved) {
            crossfade(true)
            memoryCachePolicy(CachePolicy.ENABLED)
            diskCachePolicy(CachePolicy.ENABLED)
            listener(
                onError = { _, _ ->
                    showAvatarPlaceholder()
                }
            )
        }
    }

    private fun showAvatarPlaceholder() {
        binding.imageProfileAvatar.apply {
            imageTintList = android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.profile_avatar_icon)
            )
            scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
            setPadding(28.dpToPx(), 28.dpToPx(), 28.dpToPx(), 28.dpToPx())
            setImageResource(android.R.drawable.ic_menu_myplaces)
        }
    }

    private fun normalizeAvatarUrl(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isBlank()) return null

        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value
        }

        val bucket = BuildConfig.SUPABASE_PROFILE_BUCKET.trim('/')
        val cleaned = value
            .removePrefix("storage/v1/object/public/")
            .removePrefix("/storage/v1/object/public/")
            .removePrefix("public/")
            .removePrefix("$bucket/")
            .trim('/')

        return if (cleaned.isBlank()) null else {
            "${BuildConfig.SUPABASE_URL}/storage/v1/object/public/$bucket/$cleaned"
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
