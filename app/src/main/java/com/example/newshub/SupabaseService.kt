package com.example.newshub

import com.example.newshub.network.ApiClient
import com.example.newshub.network.ApiFailure
import com.example.newshub.network.ApiFailureType
import com.example.newshub.network.ApiResult
import com.example.newshub.network.ErrorMapper
import com.example.newshub.feature.news.CommentItem
import com.example.newshub.feature.notifications.NotificationItem
import com.example.newshub.feature.profile.VerificationStatus
import com.example.newshub.network.model.AuthTokenRequest
import com.example.newshub.network.model.ChangePasswordRequest
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

    private val fallbackUserIdColumns = listOf("auth_user_id", "user_id", "id")
    private val fallbackPhotoColumns = listOf("profile_photo_url", "avatar_url", "photo_url", "picture")

    private val supabaseUrl = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
    private val supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY.trim()

    private val authApi by lazy { ApiClient.authApi(supabaseUrl) }
    private val restApi by lazy { ApiClient.restApi(supabaseUrl) }
    private val storageApi by lazy { ApiClient.storageApi(supabaseUrl) }

    val isConfigured: Boolean
        get() = supabaseUrl.isNotBlank() && supabaseAnonKey.isNotBlank()

    suspend fun signInWithPassword(email: String, password: String): ApiResult<AuthSession> = withContext(Dispatchers.IO) {
        runApiCall {
            val response = authApi.signInWithPassword(
                apiKey = supabaseAnonKey,
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

    suspend fun signUpWithPassword(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ): ApiResult<AuthSession?> = withContext(Dispatchers.IO) {
        runApiCall {
            val fullName = listOf(firstName.trim(), lastName.trim()).filter { it.isNotBlank() }.joinToString(" ")
            val signUpData = mapOf(
                "first_name" to firstName.trim(),
                "last_name" to lastName.trim(),
                "full_name" to fullName
            )
            val response = authApi.signUpWithPassword(
                apiKey = supabaseAnonKey,
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
            val requestedUserIdColumn = BuildConfig.SUPABASE_PROFILE_USER_ID_COLUMN
            val lookupColumns = linkedSetOf(requestedUserIdColumn).apply {
                addAll(fallbackUserIdColumns)
            }

            for (candidateColumn in lookupColumns) {
                val queryResult = fetchProfileByColumn(userId, candidateColumn, accessToken)

                when (queryResult) {
                    is ApiResult.Success -> {
                        if (queryResult.data != null) {
                            return@runApiCall ApiResult.Success(queryResult.data)
                        }
                    }

                    is ApiResult.Failure -> {
                        if (!queryResult.error.detail.orEmpty().contains("column", ignoreCase = true)) {
                            return@runApiCall queryResult
                        }
                    }
                }
            }

            ApiResult.Success(null)
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
            val resolvedFullName = "$firstName $lastName".trim()
            val photoColumn = BuildConfig.SUPABASE_PROFILE_PHOTO_COLUMN
            val patchPayload = mutableMapOf<String, Any?>(
                "first_name" to firstName,
                "last_name" to lastName,
                "full_name" to resolvedFullName
            )
            patchPayload[photoColumn] = avatarUrl

            val patchResponse = restApi.patchRows(
                url = "$supabaseUrl/rest/v1/${BuildConfig.SUPABASE_PROFILE_TABLE}?$userIdColumn=eq.$encodedUserId",
                apiKey = supabaseAnonKey,
                authorization = bearer(accessToken),
                body = patchPayload
            )

            if (!patchResponse.isSuccessful) {
                return@runApiCall ApiResult.Failure(mapFailure(patchResponse))
            }

            if (patchResponse.body().orEmpty().isNotEmpty()) {
                return@runApiCall ApiResult.Success(Unit)
            }

            val insertPayload = mutableMapOf<String, Any?>(
                userIdColumn to userId,
                "first_name" to firstName,
                "last_name" to lastName,
                "full_name" to resolvedFullName,
                "email" to email
            )
            insertPayload[photoColumn] = avatarUrl

            val insertResponse = restApi.insertRow(
                url = "$supabaseUrl/rest/v1/${BuildConfig.SUPABASE_PROFILE_TABLE}",
                apiKey = supabaseAnonKey,
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
            val uploadUrl = "$supabaseUrl/storage/v1/object/${BuildConfig.SUPABASE_PROFILE_BUCKET}/$encodedPath"

            val uploadResponse = storageApi.uploadObject(
                url = uploadUrl,
                apiKey = supabaseAnonKey,
                authorization = bearer(accessToken),
                body = bytes.toRequestBody(mimeType.toMediaType())
            )

            if (!uploadResponse.isSuccessful) {
                return@runApiCall ApiResult.Failure(mapFailure(uploadResponse))
            }

            ApiResult.Success("$supabaseUrl/storage/v1/object/public/${BuildConfig.SUPABASE_PROFILE_BUCKET}/$objectPath")
        }
    }

    suspend fun updatePassword(newPassword: String, accessToken: String): ApiResult<Unit> = withContext(Dispatchers.IO) {
        runApiCall {
            val response = authApi.updatePassword(
                apiKey = supabaseAnonKey,
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
                apiKey = supabaseAnonKey,
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
            "Bearer $supabaseAnonKey"
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

    private suspend fun fetchProfileByColumn(
        userId: String,
        userIdColumn: String,
        accessToken: String?
    ): ApiResult<ProfileRecord?> {
        val encodedUserId = urlEncode(userId)
        val photoColumns = linkedSetOf(BuildConfig.SUPABASE_PROFILE_PHOTO_COLUMN).apply {
            addAll(fallbackPhotoColumns)
        }
        val selectColumns = linkedSetOf("first_name", "last_name", "full_name").apply {
            addAll(photoColumns)
        }.joinToString(",")

        val url = "$supabaseUrl/rest/v1/${BuildConfig.SUPABASE_PROFILE_TABLE}" +
            "?$userIdColumn=eq.$encodedUserId&select=$selectColumns&limit=1"

        val response = restApi.fetchRows(
            url = url,
            apiKey = supabaseAnonKey,
            authorization = bearer(accessToken)
        )

        if (!response.isSuccessful) {
            return ApiResult.Failure(mapFailure(response))
        }

        val firstRow = response.body().orEmpty().firstOrNull() ?: return ApiResult.Success(null)
        val firstName = firstRow.optString("first_name")
        val lastName = firstRow.optString("last_name")
        val fullName = firstRow.optString("full_name").ifBlank {
            listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
        }
        val avatarUrl = firstRow.optString(*photoColumns.toTypedArray()).ifBlank { "" }.ifBlank { null }

        return ApiResult.Success(
            ProfileRecord(
                firstName = firstName,
                lastName = lastName,
                fullName = fullName,
                avatarUrl = avatarUrl
            )
        )
    }


    suspend fun fetchNotifications(userId: String, accessToken: String): ApiResult<List<NotificationItem>> =
        withContext(Dispatchers.IO) {
            runApiCall {
                val encodedUserId = urlEncode(userId)
                val url = "$supabaseUrl/rest/v1/notifications" +
                    "?recipient_user_id=eq.$encodedUserId&order=created_at.desc&limit=50"
                val response = restApi.fetchRows(url, supabaseAnonKey, bearer(accessToken))
                if (!response.isSuccessful) return@runApiCall ApiResult.Failure(mapFailure(response))
                val items = response.body().orEmpty().mapNotNull { mapNotificationRow(it) }
                ApiResult.Success(items)
            }
        }

    suspend fun markNotificationRead(notificationId: String, accessToken: String): ApiResult<Unit> =
        withContext(Dispatchers.IO) {
            runApiCall {
                val encodedId = urlEncode(notificationId)
                val response = restApi.patchRows(
                    url = "$supabaseUrl/rest/v1/notifications?id=eq.$encodedId",
                    apiKey = supabaseAnonKey,
                    authorization = bearer(accessToken),
                    body = mapOf("read" to true)
                )
                if (!response.isSuccessful) return@runApiCall ApiResult.Failure(mapFailure(response))
                ApiResult.Success(Unit)
            }
        }

    suspend fun markAllNotificationsRead(userId: String, accessToken: String): ApiResult<Unit> =
        withContext(Dispatchers.IO) {
            runApiCall {
                val encodedUserId = urlEncode(userId)
                val response = restApi.patchRows(
                    url = "$supabaseUrl/rest/v1/notifications?recipient_user_id=eq.$encodedUserId",
                    apiKey = supabaseAnonKey,
                    authorization = bearer(accessToken),
                    body = mapOf("read" to true)
                )
                if (!response.isSuccessful) return@runApiCall ApiResult.Failure(mapFailure(response))
                ApiResult.Success(Unit)
            }
        }

    suspend fun deleteNotification(notificationId: String, accessToken: String): ApiResult<Unit> =
        withContext(Dispatchers.IO) {
            runApiCall {
                val encodedId = urlEncode(notificationId)
                val response = restApi.deleteRow(
                    url = "$supabaseUrl/rest/v1/notifications?id=eq.$encodedId",
                    apiKey = supabaseAnonKey,
                    authorization = bearer(accessToken)
                )
                if (!response.isSuccessful) return@runApiCall ApiResult.Failure(mapFailure(response))
                ApiResult.Success(Unit)
            }
        }

    suspend fun fetchComments(articleId: String, accessToken: String): ApiResult<List<CommentItem>> =
        withContext(Dispatchers.IO) {
            runApiCall {
                val encodedArticleId = urlEncode(articleId)
                val url = "$supabaseUrl/rest/v1/article_comments" +
                    "?article_id=eq.$encodedArticleId&order=created_at.asc&limit=50"
                val response = restApi.fetchRows(url, supabaseAnonKey, bearer(accessToken))
                if (!response.isSuccessful) return@runApiCall ApiResult.Failure(mapFailure(response))
                val items = response.body().orEmpty().mapNotNull { mapCommentRow(it) }
                ApiResult.Success(items)
            }
        }

    suspend fun postComment(
        articleId: String,
        userId: String,
        displayName: String,
        content: String,
        accessToken: String
    ): ApiResult<Unit> = withContext(Dispatchers.IO) {
        runApiCall {
            val response = restApi.insertRow(
                url = "$supabaseUrl/rest/v1/article_comments",
                apiKey = supabaseAnonKey,
                authorization = bearer(accessToken),
                body = mapOf(
                    "article_id" to articleId,
                    "user_id" to userId,
                    "display_name" to displayName,
                    "content" to content.trim()
                )
            )
            if (!response.isSuccessful) return@runApiCall ApiResult.Failure(mapFailure(response))
            ApiResult.Success(Unit)
        }
    }

    suspend fun deleteComment(commentId: String, accessToken: String): ApiResult<Unit> =
        withContext(Dispatchers.IO) {
            runApiCall {
                val encodedId = urlEncode(commentId)
                val response = restApi.deleteRow(
                    url = "$supabaseUrl/rest/v1/article_comments?id=eq.$encodedId",
                    apiKey = supabaseAnonKey,
                    authorization = bearer(accessToken)
                )
                if (!response.isSuccessful) return@runApiCall ApiResult.Failure(mapFailure(response))
                ApiResult.Success(Unit)
            }
        }

    suspend fun fetchLatestVerification(userId: String, accessToken: String): ApiResult<VerificationStatus> =
        withContext(Dispatchers.IO) {
            runApiCall {
                val encodedUserId = urlEncode(userId)
                val url = "$supabaseUrl/rest/v1/verifications" +
                    "?user_id=eq.$encodedUserId&order=submitted_at.desc&limit=1"
                val response = restApi.fetchRows(url, supabaseAnonKey, bearer(accessToken))
                if (!response.isSuccessful) return@runApiCall ApiResult.Failure(mapFailure(response))
                val row = response.body().orEmpty().firstOrNull()
                    ?: return@runApiCall ApiResult.Success(VerificationStatus.NotSubmitted)
                ApiResult.Success(mapVerificationStatus(row))
            }
        }

    private fun mapNotificationRow(row: Map<String, Any?>): NotificationItem? {
        val id = row.optString("id")
        if (id.isBlank()) return null
        return NotificationItem(
            id = id,
            type = row.optString("type"),
            actorName = row.optString("actor_name", "actorName"),
            articleTitle = row.optString("article_title", "articleTitle").ifBlank { null },
            rejectionReason = row.optString("rejection_reason", "rejectionReason").ifBlank { null },
            createdAt = row.optString("created_at", "createdAt"),
            isRead = row.optBoolean("read", "is_read")
        )
    }

    private fun mapCommentRow(row: Map<String, Any?>): CommentItem? {
        val id = row.optString("id")
        if (id.isBlank()) return null
        val parent = row.optString("parent_id", "parentId").ifBlank { null }
        return CommentItem(
            id = id,
            articleId = row.optString("article_id", "articleId"),
            userId = row.optString("user_id", "userId"),
            displayName = row.optString("display_name", "displayName"),
            content = row.optString("content"),
            parentId = parent,
            createdAt = row.optString("created_at", "createdAt")
        )
    }

    private fun mapVerificationStatus(row: Map<String, Any?>): VerificationStatus {
        return when (row.optString("status").uppercase()) {
            "APPROVED", "VERIFIED" -> VerificationStatus.Verified
            "PENDING" -> VerificationStatus.Pending
            "REJECTED" -> VerificationStatus.Rejected(
                row.optString("rejection_reason", "rejectionReason").ifBlank { "No reason provided." }
            )
            else -> VerificationStatus.NotSubmitted
        }
    }

    private fun Map<String, Any?>.optBoolean(vararg keys: String): Boolean {
        for (key in keys) {
            when (val value = this[key]) {
                is Boolean -> return value
                is String -> return value.equals("true", ignoreCase = true)
                is Number -> return value.toInt() != 0
            }
        }
        return false
    }

    private fun Map<String, Any?>.optString(vararg keys: String): String {
        for (key in keys) {
            val value = this[key]
            when (value) {
                is String -> if (value.isNotBlank()) return value
                is Number -> return value.toString()
                is Boolean -> return value.toString()
            }
        }
        return ""
    }
}
