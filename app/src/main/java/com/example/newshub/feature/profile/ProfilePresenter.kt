package com.example.newshub.feature.profile

import com.example.newshub.AuthUserRecord
import com.example.newshub.ProfileRecord
import com.example.newshub.R
import com.example.newshub.UiErrorMapper
import com.example.newshub.core.session.SessionStore
import com.example.newshub.feature.auth.data.AuthRepository
import com.example.newshub.feature.auth.data.SupabaseAuthRepository
import com.example.newshub.SupabaseService
import com.example.newshub.feature.profile.data.ProfileRepository
import com.example.newshub.feature.profile.data.SupabaseProfileRepository
import kotlinx.coroutines.withContext
import com.example.newshub.network.ApiFailureType
import com.example.newshub.network.ApiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ProfilePresenter(
    private val sessionStore: SessionStore,
    private val authRepository: AuthRepository = SupabaseAuthRepository(),
    private val profileRepository: ProfileRepository = SupabaseProfileRepository(),
    private val supabaseService: SupabaseService = SupabaseService()
) : ProfileContract.Presenter {

    private var view: ProfileContract.View? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var avatarUrl: String? = null
    private var userEmail: String? = null
    private var firstName: String = ""
    private var lastName: String = ""

    override fun attach(view: ProfileContract.View) {
        this.view = view
    }

    override fun detach() {
        view = null
        scope.cancel()
    }

    override fun onScreenStarted() {
        onNameChanged("", "")

        if (!profileRepository.isConfigured) {
            view?.showMessage(R.string.supabase_not_configured)
            return
        }

        val sessionUserId = sessionStore.getUserId()
        val accessToken = sessionStore.getAccessToken()
        if (sessionUserId.isNullOrBlank() || accessToken.isNullOrBlank()) {
            view?.showMessage(R.string.supabase_session_missing)
            return
        }

        view?.showLoading(true)
        scope.launch {
            val authResult = authRepository.fetchAuthUser(accessToken)
            if (authResult is ApiResult.Failure) {
                view?.showLoading(false)
                handleFailure(authResult)
                return@launch
            }

            val authUser = (authResult as ApiResult.Success).data
            val effectiveUserId = authUser.userId.ifBlank { sessionUserId }
            if (effectiveUserId != sessionUserId) {
                sessionStore.saveSession(effectiveUserId, accessToken)
            }

            userEmail = authUser.email
            view?.renderAccountDetails(
                email = userEmail.orEmpty(),
                voterId = formatVoterId(effectiveUserId)
            )
            when (val profileResult = profileRepository.fetchProfile(effectiveUserId, accessToken)) {
                is ApiResult.Success -> {
                    val resolved = resolveProfile(profileResult.data, authUser)
                    if (resolved != null) {
                        bindProfile(resolved)
                        if (profileResult.data == null && resolved.firstName.isNotBlank() && resolved.lastName.isNotBlank()) {
                            profileRepository.upsertProfile(
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

            loadVerificationStatus(effectiveUserId, accessToken)
            view?.showLoading(false)
        }
    }

    private suspend fun loadVerificationStatus(userId: String, accessToken: String) {
        if (!supabaseService.isConfigured) {
            view?.showVerificationStatus(VerificationStatus.NotSubmitted)
            return
        }
        val result = withContext(Dispatchers.IO) {
            supabaseService.fetchLatestVerification(userId, accessToken)
        }
        when (result) {
            is ApiResult.Success -> view?.showVerificationStatus(result.data)
            is ApiResult.Failure -> view?.showVerificationStatus(VerificationStatus.NotSubmitted)
        }
    }

    override fun onBackClicked() {
        view?.navigateToHome()
    }

    override fun onLogoutClicked() {
        sessionStore.clear()
        view?.navigateToLogin()
    }

    override fun onUploadPhotoClicked() {
        if (!profileRepository.isConfigured) {
            view?.showMessage(R.string.supabase_not_configured)
            return
        }
        view?.openImagePicker()
    }

    override fun onAvatarSelected(bytes: ByteArray, mimeType: String) {
        val userId = sessionStore.getUserId()
        if (userId.isNullOrBlank()) {
            view?.showMessage(R.string.supabase_session_missing)
            return
        }

        view?.showLoading(true)
        scope.launch {
            when (val uploadResult = profileRepository.uploadAvatar(
                userId = userId,
                bytes = bytes,
                mimeType = mimeType,
                accessToken = sessionStore.getAccessToken()
            )) {
                is ApiResult.Success -> {
                    val uploadedUrl = uploadResult.data
                    avatarUrl = uploadedUrl
                    view?.renderAvatar(uploadedUrl)

                    if (firstName.isNotBlank() && lastName.isNotBlank()) {
                        val upsertResult = profileRepository.upsertProfile(
                            userId = userId,
                            firstName = firstName,
                            lastName = lastName,
                            avatarUrl = uploadedUrl,
                            email = userEmail,
                            accessToken = sessionStore.getAccessToken()
                        )
                        if (upsertResult is ApiResult.Failure) {
                            handleFailure(upsertResult)
                            view?.showLoading(false)
                            return@launch
                        }
                    }

                    view?.showMessage(R.string.profile_picture_updated)
                }

                is ApiResult.Failure -> handleFailure(uploadResult)
            }
            view?.showLoading(false)
        }
    }

    override fun onNameChanged(firstName: String, lastName: String) {
        this.firstName = firstName.trim()
        this.lastName = lastName.trim()
        val fullName = listOf(this.firstName, this.lastName)
            .filter { it.isNotBlank() }
            .joinToString(" ")
        view?.renderName(this.firstName, this.lastName, fullName)
    }

    override fun onSaveNameClicked(firstName: String, lastName: String) {
        val trimmedFirstName = firstName.trim()
        val trimmedLastName = lastName.trim()

        if (trimmedFirstName.isBlank() || trimmedLastName.isBlank()) {
            view?.showMessage(R.string.profile_name_required)
            return
        }

        val userId = sessionStore.getUserId()
        if (userId.isNullOrBlank()) {
            view?.showMessage(R.string.supabase_session_missing)
            return
        }

        view?.showLoading(true)
        scope.launch {
            val result = profileRepository.upsertProfile(
                userId = userId,
                firstName = trimmedFirstName,
                lastName = trimmedLastName,
                avatarUrl = avatarUrl,
                email = userEmail,
                accessToken = sessionStore.getAccessToken()
            )
            view?.showLoading(false)

            when (result) {
                is ApiResult.Success -> view?.showMessage(R.string.profile_name_saved)
                is ApiResult.Failure -> handleFailure(result)
            }
        }
    }

    override fun navigateToVerification() {
        view?.navigateToVerification()
    }

    override fun onUpdatePasswordClicked(currentPassword: String, newPassword: String, confirmPassword: String) {
        if (currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
            view?.showMessage(R.string.profile_password_required)
            return
        }

        if (newPassword.length < 8) {
            view?.showMessage(R.string.profile_password_too_short)
            return
        }

        if (newPassword != confirmPassword) {
            view?.showMessage(R.string.profile_password_mismatch)
            return
        }

        val accessToken = sessionStore.getAccessToken()
        if (!authRepository.isConfigured || accessToken.isNullOrBlank()) {
            view?.showMessage(R.string.supabase_session_missing)
            return
        }

        view?.showLoading(true)
        scope.launch {
            when (val result = authRepository.updatePassword(newPassword, accessToken)) {
                is ApiResult.Success -> {
                    view?.clearPasswordInputs()
                    view?.showMessage(R.string.profile_password_updated)
                }

                is ApiResult.Failure -> handleFailure(result)
            }
            view?.showLoading(false)
        }
    }

    private fun bindProfile(profile: ProfileRecord) {
        firstName = profile.firstName
        lastName = profile.lastName
        avatarUrl = profile.avatarUrl
        view?.renderName(profile.firstName, profile.lastName, profile.fullName)
        view?.renderAvatar(profile.avatarUrl)
        view?.showMessage(R.string.profile_loaded)
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

    private fun formatVoterId(userId: String): String {
        val trimmed = userId.trim()
        if (trimmed.isBlank()) return ""
        return if (trimmed.startsWith("VID", ignoreCase = true)) trimmed else "VID-$trimmed"
    }

    private fun handleFailure(result: ApiResult.Failure) {
        if (result.error.type == ApiFailureType.Unauthorized || result.error.statusCode == 401) {
            sessionStore.clear()
            view?.showMessage(R.string.error_unauthorized)
            view?.navigateToLogin()
            return
        }

        view?.showMessage(UiErrorMapper.toMessageRes(result.error))
    }
}


