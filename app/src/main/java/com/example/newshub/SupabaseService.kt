package com.example.newshub

import com.example.newshub.network.ApiClient
import com.example.newshub.network.ApiFailure
import com.example.newshub.network.ApiFailureType
import com.example.newshub.network.ApiResult
import com.example.newshub.network.ErrorMapper
import com.example.newshub.network.model.AuthTokenRequest
import com.example.newshub.network.model.ChangePasswordRequest
import com.example.newshub.network.model.ProfileUpsertRequest
import com.example.newshub.network.model.SignUpRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class ProfileRecord(
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val avatarUrl: String?
)

data class AuthSession(
    val userId: String,
    val accessToken: String
)

data class AuthUserRecord(
    val userId: String,
    val email: String?,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val avatarUrl: String?
)

class SupabaseService {

    private val authApi by lazy { ApiClient.authApi(BuildConfig.SUPABASE_URL) }
    private val restApi by lazy { ApiClient.restApi(BuildConfig.SUPABASE_URL) }
    private val storageApi by lazy { ApiClient.storageApi(BuildConfig.SUPABASE_URL) }

    val isConfigured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    suspend fun signInWithPassword(email: String, password: String): ApiResult<AuthSession> = withContext(Dispatchers.IO) {
        runApiCall {
            val response = authApi.signInWithPassword(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                request = AuthTokenRequest(email = email, password = password)
            )

            if (!response.isSuccessful) {
                return@runApiCall ApiResult.Failure(mapFailure(response))
            }

            val payload = response.body()
            val accessToken = payload?.accessToken.orEmpty()
            val userId = payload?.user?.id.orEmpty()

            if (accessToken.isBlank() || userId.isBlank()) {
                return@runApiCall ApiResult.Failure(ApiFailure(ApiFailureType.Unknown, detail = "Invalid session payload"))
            }

            ApiResult.Success(AuthSession(userId = userId, accessToken = accessToken))
        }
    }

    suspend fun signUpWithPassword(email: String, password: String): ApiResult<AuthSession?> = withContext(Dispatchers.IO) {
        runApiCall {
            val signUpData = mapOf("full_name" to "")
            val response = authApi.signUpWithPassword(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                request = SignUpRequest(email = email, password = password, data = signUpData)
            )

            if (!response.isSuccessful) {
                return@runApiCall ApiResult.Failure(mapFailure(response))
            }

            val payload = response.body()
            val accessToken = payload?.accessToken.orEmpty()
            val userId = payload?.user?.id.orEmpty()

            if (accessToken.isBlank() || userId.isBlank()) {
                return@runApiCall ApiResult.Success(null)
            }

            ApiResult.Success(AuthSession(userId = userId, accessToken = accessToken))
        }
    }

    suspend fun fetchProfile(userId: String, accessToken: String?): ApiResult<ProfileRecord?> = withContext(Dispatchers.IO) {
        runApiCall {
            val encodedUserId = urlEncode(userId)
            val userIdColumn = BuildConfig.SUPABASE_PROFILE_USER_ID_COLUMN
            val photoColumn = BuildConfig.SUPABASE_PROFILE_PHOTO_COLUMN
            val url = "${BuildConfig.SUPABASE_URL}/rest/v1/${BuildConfig.SUPABASE_PROFILE_TABLE}" +
                "?$userIdColumn=eq.$encodedUserId&select=first_name,last_name,full_name,$photoColumn&limit=1"

            val response = restApi.fetchRows(
                url = url,
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                authorization = bearer(accessToken)
            )

            if (!response.isSuccessful) {
                return@runApiCall ApiResult.Failure(mapFailure(response))
            }

            val firstRow = response.body().orEmpty().firstOrNull() ?: return@runApiCall ApiResult.Success(null)
            val firstName = firstRow.firstName.orEmpty()
            val lastName = firstRow.lastName.orEmpty()
            val fullName = firstRow.fullName.orEmpty().ifBlank {
                listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
            }
            val avatarUrl = firstRow.profilePhotoUrl

            ApiResult.Success(
                ProfileRecord(
                    firstName = firstName,
                    lastName = lastName,
                    fullName = fullName,
                    avatarUrl = avatarUrl
                )
            )
        }
    }

    suspend fun upsertProfile(
        userId: String,
        firstName: String,
        lastName: String,
        avatarUrl: String?,
        email: String?,
        accessToken: String?
    ): ApiResult<Unit> = withContext(Dispatchers.IO) {
        runApiCall {
            val userIdColumn = BuildConfig.SUPABASE_PROFILE_USER_ID_COLUMN
            val encodedUserId = urlEncode(userId)

            val patchPayload = ProfileUpsertRequest(
                firstName = firstName,
                lastName = lastName,
                fullName = "$firstName $lastName".trim(),
                profilePhotoUrl = avatarUrl
            )

            val patchResponse = restApi.patchRows(
                url = "${BuildConfig.SUPABASE_URL}/rest/v1/${BuildConfig.SUPABASE_PROFILE_TABLE}?$userIdColumn=eq.$encodedUserId",
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                authorization = bearer(accessToken),
                body = patchPayload
            )

            if (!patchResponse.isSuccessful) {
                return@runApiCall ApiResult.Failure(mapFailure(patchResponse))
            }

            if (patchResponse.body().orEmpty().isNotEmpty()) {
                return@runApiCall ApiResult.Success(Unit)
            }

            val insertPayload = ProfileUpsertRequest(
                authUserId = userId,
                firstName = firstName,
                lastName = lastName,
                fullName = "$firstName $lastName".trim(),
                profilePhotoUrl = avatarUrl,
                email = email
            )

            val insertResponse = restApi.insertRow(
                url = "${BuildConfig.SUPABASE_URL}/rest/v1/${BuildConfig.SUPABASE_PROFILE_TABLE}",
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                authorization = bearer(accessToken),
                body = insertPayload
            )

            if (!insertResponse.isSuccessful) {
                return@runApiCall ApiResult.Failure(mapFailure(insertResponse))
            }

            ApiResult.Success(Unit)
        }
    }

    suspend fun uploadAvatar(
        userId: String,
        bytes: ByteArray,
        mimeType: String,
        accessToken: String?
    ): ApiResult<String> = withContext(Dispatchers.IO) {
        runApiCall {
            val fileExtension = if (mimeType.contains("png")) "png" else "jpg"
            val objectPath = "$userId/avatar.$fileExtension"
            val encodedPath = objectPath.split("/").joinToString("/") { urlEncode(it) }
            val uploadUrl = "${BuildConfig.SUPABASE_URL}/storage/v1/object/${BuildConfig.SUPABASE_PROFILE_BUCKET}/$encodedPath"

            val uploadResponse = storageApi.uploadObject(
                url = uploadUrl,
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                authorization = bearer(accessToken),
                body = bytes.toRequestBody(mimeType.toMediaType())
            )

            if (!uploadResponse.isSuccessful) {
                return@runApiCall ApiResult.Failure(mapFailure(uploadResponse))
            }

            ApiResult.Success("${BuildConfig.SUPABASE_URL}/storage/v1/object/public/${BuildConfig.SUPABASE_PROFILE_BUCKET}/$objectPath")
        }
    }

    suspend fun updatePassword(newPassword: String, accessToken: String): ApiResult<Unit> = withContext(Dispatchers.IO) {
        runApiCall {
            val response = authApi.updatePassword(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                authorization = "Bearer $accessToken",
                request = ChangePasswordRequest(password = newPassword)
            )

            if (!response.isSuccessful) {
                return@runApiCall ApiResult.Failure(mapFailure(response))
            }

            ApiResult.Success(Unit)
        }
    }

    suspend fun fetchAuthUser(accessToken: String): ApiResult<AuthUserRecord> = withContext(Dispatchers.IO) {
        runApiCall {
            val response = authApi.getCurrentUser(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                authorization = "Bearer $accessToken"
            )

            if (!response.isSuccessful) {
                return@runApiCall ApiResult.Failure(mapFailure(response))
            }

            val user = response.body()
                ?: return@runApiCall ApiResult.Failure(ApiFailure(ApiFailureType.Unknown, detail = "User payload missing"))

            val metadata = user.userMetadata.orEmpty()
            val fullName = metadata.optString("full_name", "name")
            var firstName = metadata.optString("first_name", "given_name")
            var lastName = metadata.optString("last_name", "family_name")
            val avatarUrl = metadata.optString("avatar_url", "picture", "profile_photo_url")

            if (firstName.isBlank() && lastName.isBlank() && fullName.isNotBlank()) {
                val parts = fullName.split(" ").filter { it.isNotBlank() }
                if (parts.isNotEmpty()) {
                    firstName = parts.first()
                    lastName = parts.drop(1).joinToString(" ")
                }
            }

            ApiResult.Success(
                AuthUserRecord(
                    userId = user.id.orEmpty(),
                    email = user.email,
                    firstName = firstName,
                    lastName = lastName,
                    fullName = fullName,
                    avatarUrl = avatarUrl
                )
            )
        }
    }

    private fun bearer(accessToken: String?): String {
        return if (accessToken.isNullOrBlank()) {
            "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
        } else {
            "Bearer $accessToken"
        }
    }

    private fun urlEncode(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
    }

    private suspend fun <T> runApiCall(block: suspend () -> ApiResult<T>): ApiResult<T> {
        return try {
            block()
        } catch (throwable: Throwable) {
            ApiResult.Failure(ErrorMapper.fromThrowable(throwable))
        }
    }

    private fun mapFailure(response: Response<*>): ApiFailure {
        val detail = runCatching { response.errorBody()?.string() }.getOrNull()
        return ErrorMapper.fromStatusCode(response.code(), detail)
    }


    private fun Map<String, Any?>.optString(vararg keys: String): String {
        for (key in keys) {
            val value = this[key]
            if (value is String && value.isNotBlank()) {
                return value
            }
        }
        return ""
    }
}
