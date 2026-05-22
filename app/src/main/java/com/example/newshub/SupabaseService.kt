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

    // FIX: do not query users.user_id or users.id with auth UUID — causes 403/400 on NewsHub schema
    private val fallbackUserIdColumns = emptyList<String>()
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

    suspend fun fetchProfile(
        userId: String,
        accessToken: String?,
        email: String? = null
    ): ApiResult<ProfileRecord?> = withContext(Dispatchers.IO) {
        runApiCall {
            val authColumn = BuildConfig.SUPABASE_PROFILE_USER_ID_COLUMN
            when (val byAuth = fetchProfileByColumn(userId, authColumn, accessToken)) {
                is ApiResult.Success -> {
                    if (byAuth.data != null) return@runApiCall ApiResult.Success(byAuth.data)
                }
                is ApiResult.Failure -> {
                    if (!isRecoverableProfileLookupFailure(byAuth.error)) {
                        return@runApiCall byAuth
                    }
                }
            }

            val trimmedEmail = email?.trim().orEmpty()
            if (trimmedEmail.isNotBlank()) {
                when (val byEmail = fetchProfileByColumn(trimmedEmail, "email", accessToken)) {
                    is ApiResult.Success -> {
                        if (byEmail.data != null) return@runApiCall ApiResult.Success(byEmail.data)
                    }
                    is ApiResult.Failure -> {
                        if (!isRecoverableProfileLookupFailure(byEmail.error)) {
                            return@runApiCall byEmail
                        }
                    }
                }
            }

            ApiResult.Success(null)
        }
    }

    private fun isRecoverableProfileLookupFailure(error: ApiFailure): Boolean {
        val detail = error.detail.orEmpty()
        if (detail.contains("column", ignoreCase = true)) return true
        if (error.statusCode == 403) return true
        if (error.statusCode == 400) {
            return detail.contains("uuid", ignoreCase = true) ||
                detail.contains("invalid input", ignoreCase = true)
        }
        return false
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
        val photoColumn = BuildConfig.SUPABASE_PROFILE_PHOTO_COLUMN
        // PostgREST returns 400 if any column in `select` does not exist; do not bundle fallback names here.
        val selectColumns = listOf("first_name", "last_name", "full_name", photoColumn).joinToString(",")

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
        val avatarUrl = firstRow.optString(photoColumn, *fallbackPhotoColumns.toTypedArray())
            .ifBlank { null }

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
                    body = mapOf("is_read" to true)
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
                    body = mapOf("is_read" to true)
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
                val photoColumn = BuildConfig.SUPABASE_PROFILE_PHOTO_COLUMN
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
        avatarUrl: String?,
        accessToken: String
    ): ApiResult<Unit> = withContext(Dispatchers.IO) {
        runApiCall {
            val photoColumn = BuildConfig.SUPABASE_PROFILE_PHOTO_COLUMN
            val body = mutableMapOf<String, Any?>(
                "article_id" to articleId,
                "user_id" to userId,
                "display_name" to displayName,
                "content" to content.trim()
            )
            if (!avatarUrl.isNullOrBlank()) {
                body[photoColumn] = avatarUrl
            }
            val response = restApi.insertRow(
                url = "$supabaseUrl/rest/v1/article_comments",
                apiKey = supabaseAnonKey,
                authorization = bearer(accessToken),
                body = body
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

    suspend fun fetchElections(accessToken: String? = null): ApiResult<List<ElectionRecord>> =
        withContext(Dispatchers.IO) {
            runApiCall {
                val url = "$supabaseUrl/rest/v1/elections" +
                    "?select=id,name,region,start_time,end_time,status,image_url,is_deleted" +
                    "&is_deleted=eq.false&order=start_time.asc"
                val response = restApi.fetchRows(url, supabaseAnonKey, bearer(accessToken))
                if (!response.isSuccessful) return@runApiCall ApiResult.Failure(mapFailure(response))
                val elections = response.body().orEmpty().mapNotNull { mapElectionRow(it) }
                val counts = elections.map { election ->
                    countCandidatesForElection(election.id, accessToken)
                }
                ApiResult.Success(
                    elections.mapIndexed { index, election ->
                        election.copy(candidateCount = counts[index])
                    }
                )
            }
        }

    suspend fun fetchElection(electionId: String, accessToken: String? = null): ApiResult<ElectionRecord?> =
        withContext(Dispatchers.IO) {
            runApiCall {
                val encodedId = urlEncode(com.example.newshub.core.RestIdNormalizer.normalize(electionId))
                val url = "$supabaseUrl/rest/v1/elections" +
                    "?select=id,name,region,start_time,end_time,status,image_url,is_deleted" +
                    "&id=eq.$encodedId&is_deleted=eq.false&limit=1"
                val response = restApi.fetchRows(url, supabaseAnonKey, bearer(accessToken))
                if (!response.isSuccessful) return@runApiCall ApiResult.Failure(mapFailure(response))
                val row = response.body().orEmpty().firstOrNull() ?: return@runApiCall ApiResult.Success(null)
                val election = mapElectionRow(row) ?: return@runApiCall ApiResult.Success(null)
                ApiResult.Success(election.copy(candidateCount = countCandidatesForElection(election.id, accessToken)))
            }
        }

    suspend fun fetchCandidatesByElection(
        electionId: String,
        accessToken: String? = null
    ): ApiResult<List<CandidateRecord>> = withContext(Dispatchers.IO) {
        runApiCall {
            val normalizedId = com.example.newshub.core.RestIdNormalizer.normalize(electionId)
            val encodedElectionId = urlEncode(normalizedId)
            val url = "$supabaseUrl/rest/v1/candidates" +
                "?select=id,election_id,name,position,party,photo_url,education,experience,bio,display_order" +
                "&election_id=eq.$encodedElectionId&order=display_order.asc"
            val response = restApi.fetchRows(url, supabaseAnonKey, bearer(accessToken))
            if (!response.isSuccessful) return@runApiCall ApiResult.Failure(mapFailure(response))
            ApiResult.Success(response.body().orEmpty().mapNotNull { mapCandidateRow(it, normalizedId) })
        }
    }

    suspend fun fetchCandidate(candidateId: String, accessToken: String? = null): ApiResult<CandidateRecord?> =
        withContext(Dispatchers.IO) {
            runApiCall {
                val encodedId = urlEncode(com.example.newshub.core.RestIdNormalizer.normalize(candidateId))
                val url = "$supabaseUrl/rest/v1/candidates" +
                    "?select=id,election_id,name,position,party,photo_url,education,experience,bio,display_order" +
                    "&id=eq.$encodedId&limit=1"
                val response = restApi.fetchRows(url, supabaseAnonKey, bearer(accessToken))
                if (!response.isSuccessful) return@runApiCall ApiResult.Failure(mapFailure(response))
                val row = response.body().orEmpty().firstOrNull()
                    ?: return@runApiCall ApiResult.Success(null)
                ApiResult.Success(mapCandidateRow(row, row.optString("election_id", "electionId")))
            }
        }

    suspend fun searchElections(query: String, accessToken: String? = null): ApiResult<List<ElectionRecord>> =
        withContext(Dispatchers.IO) {
            when (val all = fetchElections(accessToken)) {
                is ApiResult.Failure -> all
                is ApiResult.Success -> {
                    val q = query.trim().lowercase()
                    if (q.isBlank()) return@withContext all
                    ApiResult.Success(
                        all.data.filter {
                            it.name.lowercase().contains(q) || it.region.lowercase().contains(q)
                        }
                    )
                }
            }
        }

    suspend fun searchCandidates(query: String, accessToken: String? = null): ApiResult<List<CandidateRecord>> =
        withContext(Dispatchers.IO) {
            runApiCall {
                val url = "$supabaseUrl/rest/v1/candidates" +
                    "?select=id,election_id,name,position,party,photo_url,education,experience,bio,display_order" +
                    "&order=display_order.asc&limit=50"
                val response = restApi.fetchRows(url, supabaseAnonKey, bearer(accessToken))
                if (!response.isSuccessful) return@runApiCall ApiResult.Failure(mapFailure(response))
                val q = query.trim().lowercase()
                val items = response.body().orEmpty().mapNotNull { mapCandidateRow(it, "") }
                ApiResult.Success(
                    if (q.isBlank()) items
                    else items.filter {
                        it.fullName.lowercase().contains(q) || it.party.lowercase().contains(q)
                    }
                )
            }
        }

    suspend fun resolveNumericUserId(
        authUserId: String,
        email: String?,
        accessToken: String
    ): ApiResult<String?> = withContext(Dispatchers.IO) {
        runApiCall {
            val encodedAuthId = urlEncode(authUserId)
            val byAuthUrl = "$supabaseUrl/rest/v1/${BuildConfig.SUPABASE_PROFILE_TABLE}" +
                "?auth_user_id=eq.$encodedAuthId&select=id&limit=1"
            val byAuth = restApi.fetchRows(byAuthUrl, supabaseAnonKey, bearer(accessToken))
            if (byAuth.isSuccessful) {
                val id = byAuth.body().orEmpty().firstOrNull()?.optString("id")
                if (!id.isNullOrBlank()) return@runApiCall ApiResult.Success(id)
            }
            val trimmedEmail = email?.trim().orEmpty()
            if (trimmedEmail.isNotBlank()) {
                val encodedEmail = urlEncode(trimmedEmail)
                val byEmailUrl = "$supabaseUrl/rest/v1/${BuildConfig.SUPABASE_PROFILE_TABLE}" +
                    "?email=eq.$encodedEmail&select=id&limit=1"
                val byEmail = restApi.fetchRows(byEmailUrl, supabaseAnonKey, bearer(accessToken))
                if (byEmail.isSuccessful) {
                    val id = byEmail.body().orEmpty().firstOrNull()?.optString("id")
                    if (!id.isNullOrBlank()) return@runApiCall ApiResult.Success(id)
                }
            }
            ApiResult.Success(null)
        }
    }

    suspend fun castVote(
        electionId: String,
        candidateId: String,
        authUserId: String,
        email: String?,
        accessToken: String
    ): ApiResult<VoteCastResult> = withContext(Dispatchers.IO) {
        runApiCall {
            when (val numericResult = resolveNumericUserId(authUserId, email, accessToken)) {
                is ApiResult.Failure -> return@runApiCall ApiResult.Failure(numericResult.error)
                is ApiResult.Success -> {
                    val numericUserId = numericResult.data
                    if (numericUserId.isNullOrBlank()) {
                        return@runApiCall ApiResult.Success(
                            VoteCastResult(
                                success = false,
                                reason = "PROFILE_MISSING",
                                message = "Complete your profile before voting."
                            )
                        )
                    }
                    castVoteWithNumericUser(
                        electionId = electionId,
                        candidateId = candidateId,
                        numericUserId = numericUserId,
                        authUserId = authUserId,
                        accessToken = accessToken
                    )
                }
            }
        }
    }

    suspend fun verifyVoteReceipt(receiptId: String, accessToken: String): ApiResult<VoteReceipt> =
        withContext(Dispatchers.IO) {
            runApiCall {
                val encodedId = urlEncode(receiptId)
                val url = "$supabaseUrl/rest/v1/votes?id=eq.$encodedId&select=id,voted_at&limit=1"
                val response = restApi.fetchRows(url, supabaseAnonKey, bearer(accessToken))
                if (!response.isSuccessful) return@runApiCall ApiResult.Failure(mapFailure(response))
                val found = response.body().orEmpty().isNotEmpty()
                if (!found) {
                    return@runApiCall ApiResult.Failure(
                        ApiFailure(ApiFailureType.NotFound, detail = "Vote receipt not found")
                    )
                }
                ApiResult.Success(
                    VoteReceipt(
                        receiptId = receiptId,
                        status = "VERIFIED",
                        message = "Your vote was recorded successfully."
                    )
                )
            }
        }

    private suspend fun castVoteWithNumericUser(
        electionId: String,
        candidateId: String,
        numericUserId: String,
        authUserId: String,
        accessToken: String
    ): ApiResult<VoteCastResult> {
        val encodedElectionId = urlEncode(electionId)
        val encodedNumericUserId = urlEncode(numericUserId)
        val encodedAuthUserId = urlEncode(authUserId)

        val existingVoteUrl = "$supabaseUrl/rest/v1/votes" +
            "?user_id=eq.$encodedNumericUserId&election_id=eq.$encodedElectionId&select=id&limit=1"
        val existingVote = restApi.fetchRows(existingVoteUrl, supabaseAnonKey, bearer(accessToken))
        if (!existingVote.isSuccessful) {
            return ApiResult.Failure(mapFailure(existingVote))
        }
        if (existingVote.body().orEmpty().isNotEmpty()) {
            return ApiResult.Success(
                VoteCastResult(success = false, reason = "ALREADY_VOTED", message = "You already voted in this election.")
            )
        }

        val electionUrl = "$supabaseUrl/rest/v1/elections?id=eq.$encodedElectionId&select=status&limit=1"
        val electionResponse = restApi.fetchRows(electionUrl, supabaseAnonKey, bearer(accessToken))
        if (!electionResponse.isSuccessful) {
            return ApiResult.Failure(mapFailure(electionResponse))
        }
        val electionStatus = electionResponse.body().orEmpty().firstOrNull()
            ?.optString("status")
            .orEmpty()
            .uppercase()
        if (electionStatus != "ACTIVE") {
            return ApiResult.Success(
                VoteCastResult(success = false, reason = "NOT_ACTIVE", message = "This election is not open for voting.")
            )
        }

        val verificationUrl = "$supabaseUrl/rest/v1/verifications" +
            "?user_id=eq.$encodedAuthUserId&select=status&order=submitted_at.desc&limit=1"
        val verificationResponse = restApi.fetchRows(verificationUrl, supabaseAnonKey, bearer(accessToken))
        if (!verificationResponse.isSuccessful) {
            return ApiResult.Failure(mapFailure(verificationResponse))
        }
        val verificationStatus = verificationResponse.body().orEmpty().firstOrNull()
            ?.optString("status")
            .orEmpty()
            .uppercase()
        if (verificationStatus != "APPROVED") {
            return ApiResult.Success(
                VoteCastResult(
                    success = false,
                    reason = "NOT_VERIFIED",
                    message = "Voter verification must be approved before voting."
                )
            )
        }

        val insertResponse = restApi.insertRowReturning(
            url = "$supabaseUrl/rest/v1/votes",
            apiKey = supabaseAnonKey,
            authorization = bearer(accessToken),
            body = mapOf(
                "user_id" to numericUserId,
                "candidate_id" to candidateId,
                "election_id" to electionId,
                "voted_at" to java.time.Instant.now().toString()
            )
        )
        if (!insertResponse.isSuccessful) {
            return ApiResult.Failure(mapFailure(insertResponse))
        }
        val voteId = insertResponse.body().orEmpty().firstOrNull()?.optString("id").orEmpty()
        return ApiResult.Success(
            VoteCastResult(
                success = true,
                receiptId = voteId,
                message = "Vote submitted successfully."
            )
        )
    }

    private suspend fun countCandidatesForElection(electionId: String, accessToken: String?): Int {
        val encodedElectionId = urlEncode(electionId)
        val url = "$supabaseUrl/rest/v1/candidates" +
            "?election_id=eq.$encodedElectionId&select=id"
        val response = restApi.fetchRows(url, supabaseAnonKey, bearer(accessToken))
        return if (response.isSuccessful) response.body().orEmpty().size else 0
    }

    private fun mapElectionRow(row: Map<String, Any?>): ElectionRecord? {
        val id = row.optString("id")
        val name = row.optString("name")
        if (id.isBlank() || name.isBlank()) return null
        val deleted = row.optBoolean("is_deleted")
        if (deleted) return null
        return ElectionRecord(
            id = id,
            name = name,
            status = row.optString("status").ifBlank { "UPCOMING" },
            startDate = formatElectionDate(row.optString("start_time", "start_time")),
            endDate = formatElectionDate(row.optString("end_time", "end_time")),
            description = row.optString("description", "details"),
            region = row.optString("region").ifBlank { "National" },
            imageUrl = row.optNullableString("image_url", "imageUrl"),
            candidateCount = 0
        )
    }

    private fun mapCandidateRow(row: Map<String, Any?>, defaultElectionId: String): CandidateRecord? {
        val id = row.optString("id")
        val name = row.optString("name", "full_name", "fullName")
        if (id.isBlank() || name.isBlank()) return null
        val electionId = row.optString("election_id", "electionId").ifBlank { defaultElectionId }
        val platform = row.optString("bio", "platform", "experience")
        return CandidateRecord(
            id = id,
            electionId = electionId,
            fullName = name,
            party = row.optString("party", "affiliation"),
            platform = platform,
            photoUrl = row.optNullableString("photo_url", "photoUrl"),
            position = row.optString("position", "office", "role"),
            education = row.optString("education", "school")
        )
    }

    private fun Map<String, Any?>.optNullableString(vararg keys: String): String? {
        val value = optString(*keys)
        return value.ifBlank { null }
    }

    private fun formatElectionDate(raw: String): String {
        if (raw.isBlank()) return "TBD"
        return runCatching {
            val instant = java.time.Instant.parse(raw)
            java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy", java.util.Locale.ENGLISH)
                .withZone(java.time.ZoneId.systemDefault())
                .format(instant)
        }.getOrElse { raw }
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
        val photoColumn = BuildConfig.SUPABASE_PROFILE_PHOTO_COLUMN
        return CommentItem(
            id = id,
            articleId = row.optString("article_id", "articleId"),
            userId = row.optString("user_id", "userId"),
            displayName = row.optString("display_name", "displayName"),
            avatarUrl = row.optString(photoColumn, *fallbackPhotoColumns.toTypedArray()).ifBlank { null },
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

    suspend fun submitReport(
        targetType: String,
        targetId: String,
        targetLabel: String?,
        reason: String,
        details: String?,
        reporterUserId: String,
        accessToken: String
    ): ApiResult<Unit> = withContext(Dispatchers.IO) {
        runApiCall {
            val response = restApi.insertRow(
                url = "$supabaseUrl/rest/v1/reports",
                apiKey = supabaseAnonKey,
                authorization = bearer(accessToken),
                body = mapOf(
                    "reporter_user_id" to reporterUserId,
                    "target_type" to targetType,
                    "target_id" to targetId,
                    "target_label" to targetLabel,
                    "reason" to reason,
                    "details" to details?.trim()?.ifBlank { null },
                    "status" to "PENDING"
                )
            )
            if (!response.isSuccessful) return@runApiCall ApiResult.Failure(mapFailure(response))
            ApiResult.Success(Unit)
        }
    }

    private fun Map<String, Any?>.optString(vararg keys: String): String {
        for (key in keys) {
            val value = this[key]
            when (value) {
                is String -> if (value.isNotBlank()) return value
                is Number -> return formatRestNumber(value)
                is Boolean -> return value.toString()
            }
        }
        return ""
    }

    private fun formatRestNumber(value: Number): String {
        val asDouble = value.toDouble()
        val asLong = asDouble.toLong()
        return if (asDouble == asLong.toDouble()) asLong.toString() else value.toString()
    }
}
