package com.example.newshub.feature.profile

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.newshub.feature.profile.VerificationStatus
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import coil.load
import coil.request.CachePolicy
import com.example.newshub.BuildConfig
import com.example.newshub.R
import com.example.newshub.core.session.AndroidSessionStore
import com.example.newshub.databinding.FragmentProfileBinding
import androidx.core.content.ContextCompat

class ProfileFragment : Fragment(), ProfileContract.View {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var presenter: ProfileContract.Presenter
    private var isBindingName = false

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val mimeType = requireContext().contentResolver.getType(uri) ?: "image/jpeg"
            val bytes = requireContext().contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null) {
                showMessage(R.string.profile_picture_update_failed)
                return@registerForActivityResult
            }
            presenter.onAvatarSelected(bytes, mimeType)
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
        presenter = ProfilePresenter(AndroidSessionStore(requireContext().applicationContext))
        presenter.attach(this)

        showAvatarPlaceholder()
        setupListeners()
        presenter.onScreenStarted()
    }

    private fun setupListeners() {
        val placeholderClick = View.OnClickListener {
            showMessage(R.string.home_placeholder_action)
        }

        binding.buttonMenu.setOnClickListener(placeholderClick)
        binding.buttonRefresh.setOnClickListener { presenter.onScreenStarted() }
        binding.buttonProfileShortcut.setOnClickListener { }

        binding.navNews.setOnClickListener { presenter.onBackClicked() }
        binding.navElections.setOnClickListener {
            findNavController().navigate(R.id.electionsFragment)
        }
        binding.navProfile.setOnClickListener { }
        binding.buttonLogout.setOnClickListener { presenter.onLogoutClicked() }
        binding.textDevRegister.setOnClickListener(placeholderClick)
        binding.textDevSignin.setOnClickListener(placeholderClick)
        binding.buttonUploadPhoto.setOnClickListener { presenter.onUploadPhotoClicked() }

        val nameWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (!isBindingName) {
                    presenter.onNameChanged(
                        binding.editFirstName.text?.toString().orEmpty(),
                        binding.editLastName.text?.toString().orEmpty()
                    )
                }
            }
        }

        binding.editFirstName.addTextChangedListener(nameWatcher)
        binding.editLastName.addTextChangedListener(nameWatcher)

        binding.buttonSaveName.setOnClickListener {
            presenter.onSaveNameClicked(
                binding.editFirstName.text?.toString().orEmpty(),
                binding.editLastName.text?.toString().orEmpty()
            )
        }

        binding.buttonUpdatePassword.setOnClickListener {
            presenter.onUpdatePasswordClicked(
                currentPassword = binding.editCurrentPassword.text?.toString().orEmpty(),
                newPassword = binding.editNewPassword.text?.toString().orEmpty(),
                confirmPassword = binding.editConfirmNewPassword.text?.toString().orEmpty()
            )
        }

        binding.buttonSubmitVerification.setOnClickListener {
            presenter.navigateToVerification()
        }
    }

    override fun showLoading(isLoading: Boolean) {
        binding.progressProfile.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonSaveName.isEnabled = !isLoading
        binding.buttonUploadPhoto.isEnabled = !isLoading
        binding.buttonUpdatePassword.isEnabled = !isLoading
    }

    override fun showMessage(messageRes: Int) {
        Toast.makeText(requireContext(), getString(messageRes), Toast.LENGTH_SHORT).show()
    }

    override fun navigateToHome() {
        findNavController().navigate(R.id.action_profileFragment_to_homeFragment)
    }

    override fun navigateToLogin() {
        val options = NavOptions.Builder()
            .setPopUpTo(R.id.nav_graph, true)
            .build()
        findNavController().navigate(R.id.loginFragment, null, options)
    }

    override fun openImagePicker() {
        pickImage.launch("image/*")
    }

    override fun renderName(firstName: String, lastName: String, fullName: String) {
        isBindingName = true
        if (binding.editFirstName.text?.toString() != firstName) {
            binding.editFirstName.setText(firstName)
        }
        if (binding.editLastName.text?.toString() != lastName) {
            binding.editLastName.setText(lastName)
        }
        binding.editFullName.setText(fullName)
        isBindingName = false
    }

    override fun renderAvatar(avatarUrl: String?) {
        loadAvatar(avatarUrl)
    }

    override fun clearPasswordInputs() {
        binding.editCurrentPassword.text?.clear()
        binding.editNewPassword.text?.clear()
        binding.editConfirmNewPassword.text?.clear()
    }

    override fun showVerificationStatus(status: VerificationStatus) {
        binding.textVerificationReason.visibility = View.GONE
        binding.buttonSubmitVerification.visibility = View.GONE

        when (status) {
            VerificationStatus.Verified -> {
                binding.imageVerificationStatus.setImageResource(android.R.drawable.checkbox_on_background)
                binding.imageVerificationStatus.setColorFilter(
                    ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
                )
                binding.textVerificationStatus.text = getString(R.string.verification_verified)
            }
            VerificationStatus.Pending -> {
                binding.imageVerificationStatus.setImageResource(android.R.drawable.ic_menu_recent_history)
                binding.imageVerificationStatus.setColorFilter(
                    ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark)
                )
                binding.textVerificationStatus.text = getString(R.string.verification_pending)
            }
            is VerificationStatus.Rejected -> {
                binding.imageVerificationStatus.setImageResource(android.R.drawable.ic_delete)
                binding.imageVerificationStatus.setColorFilter(
                    ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
                )
                binding.textVerificationStatus.text = getString(R.string.verification_rejected)
                binding.textVerificationReason.text = status.reason
                binding.textVerificationReason.visibility = View.VISIBLE
            }
            VerificationStatus.NotSubmitted -> {
                binding.imageVerificationStatus.setImageResource(android.R.drawable.ic_menu_info_details)
                binding.imageVerificationStatus.setColorFilter(
                    ContextCompat.getColor(requireContext(), R.color.home_muted)
                )
                binding.textVerificationStatus.text = getString(R.string.verification_not_verified)
                binding.buttonSubmitVerification.visibility = View.VISIBLE
            }
        }
    }

    override fun navigateToVerification() {
        findNavController().navigate(R.id.action_profileFragment_to_voteVerificationFragment)
    }

    private fun loadAvatar(rawAvatarUrl: String?) {
        val resolved = normalizeAvatarUrl(rawAvatarUrl)
        if (resolved.isNullOrBlank()) {
            showAvatarPlaceholder()
            return
        }

        binding.imageProfileAvatar.apply {
            imageTintList = null
            scaleType = ImageView.ScaleType.CENTER_CROP
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
            imageTintList = ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.profile_avatar_icon)
            )
            scaleType = ImageView.ScaleType.CENTER_INSIDE
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
        presenter.detach()
        _binding = null
    }
}


