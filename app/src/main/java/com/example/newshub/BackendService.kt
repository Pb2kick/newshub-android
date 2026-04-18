package com.example.newshub

import com.example.newshub.network.ApiClient
import com.example.newshub.network.ApiFailure
import com.example.newshub.network.ApiFailureType
import com.example.newshub.network.ApiResult
import com.example.newshub.network.ErrorMapper
import com.example.newshub.network.VoteRequest
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

data class NewsArticle(
    val id: String,
    val title: String,
    val summary: String,
    val source: String,
    val author: String,
    val category: String,
    val publishedAt: String,
    val readTime: String,
    val imageUrl: String?,
    val articleUrl: String?
)

data class NewsArticleDetail(
    val title: String,
    val source: String,
    val publishedAt: String,
    val content: String,
    val articleUrl: String?
)

data class ElectionRecord(
    val id: String,
    val name: String,
    val status: String,
    val startDate: String,
    val endDate: String,
    val description: String
)

data class CandidateRecord(
    val id: String,
    val electionId: String,
    val fullName: String,
    val party: String,
    val platform: String,
    val photoUrl: String?
)

data class VoteReceipt(
    val receiptId: String,
    val status: String,
    val message: String
)

class BackendService {

    private val backendBaseUrl = BuildConfig.BACKEND_BASE_URL.trim().trimEnd('/')
    private val api by lazy { ApiClient.backendApi(backendBaseUrl) }

    val isConfigured: Boolean
        get() = backendBaseUrl.isNotBlank()

    suspend fun fetchNews(lat: Double?, lng: Double?, location: String?): ApiResult<List<NewsArticle>> = withContext(Dispatchers.IO) {
        runApiCall {
            val response = api.fetchNews(lat = lat, lng = lng, location = location)
            if (!response.isSuccessful) return@runApiCall ApiResult.Failure(mapFailure(response))

            val payload = response.body()
            val rows = payload.asFlexibleArray("data", "items", "news", "articles")
            val articles = rows.mapNotNull { row ->
                val title = row.optString("title", "headline")
                if (title.isBlank()) return@mapNotNull null
                NewsArticle(
                    id = row.optString("id", "newsId", "uuid").ifBlank { title.hashCode().toString() },
                    title = title,
                    summary = row.optString("summary", "description", "snippet", "excerpt", "content"),
                    source = row.optString("source", "author", "publisher", "sourceType").ifBlank { "NewsHub" },
                    author = row.optString("author", "publisher", "source").ifBlank { "NewsHub" },
                    category = row.optString("category", "section", "topic").ifBlank { "Top Stories" },
                    publishedAt = row.optString("publishedAt", "published_at", "date"),
                    readTime = row.optString("readTime", "read_time", "readingTime").ifBlank { "3 min read" },
                    imageUrl = row.optNullableString("imageUrl", "image_url", "thumbnail", "image"),
                    articleUrl = row.optNullableString("url", "articleUrl", "link", "sourceUrl")
                )
            }
            ApiResult.Success(articles)
        }
    }

    suspend fun fetchArticleContent(url: String): ApiResult<NewsArticleDetail> = withContext(Dispatchers.IO) {
        runApiCall {
            val response = api.fetchNewsContent(url)
            if (!response.isSuccessful) return@runApiCall ApiResult.Failure(mapFailure(response))

            val obj = response.body().asObjectOrNull()
                ?: return@runApiCall ApiResult.Failure(ApiFailure(ApiFailureType.Unknown, detail = "Empty article content"))

            ApiResult.Success(
                NewsArticleDetail(
                    title = obj.optString("title", "headline").ifBlank { "News Details" },
                    source = obj.optString("source", "author", "publisher").ifBlank { "NewsHub" },
                    publishedAt = obj.optString("publishedAt", "published_at", "date"),
                    content = obj.optString("content", "html", "body", "text").ifBlank { "No article content available." },
                    articleUrl = obj.optNullableString("url", "articleUrl", "link") ?: url
                )
            )
        }
    }

    suspend fun fetchElections(): ApiResult<List<ElectionRecord>> = withContext(Dispatchers.IO) {
        runApiCall {
            val response = api.fetchElections()
            if (!response.isSuccessful) return@runApiCall ApiResult.Failure(mapFailure(response))

            val payload = response.body()
            val rows = payload.asFlexibleArray("data", "items", "elections")
            val elections = rows.mapNotNull { row ->
                val id = row.optString("id", "electionId", "uuid")
                val name = row.optString("name", "title")
                if (id.isBlank() || name.isBlank()) return@mapNotNull null
                ElectionRecord(
                    id = id,
                    name = name,
                    status = row.optString("status", "state").ifBlank { "OPEN" },
                    startDate = row.optString("startDate", "start_date"),
                    endDate = row.optString("endDate", "end_date"),
                    description = row.optString("description", "details")
                )
            }
            ApiResult.Success(elections)
        }
    }

    suspend fun fetchCandidates(electionId: String): ApiResult<List<CandidateRecord>> = withContext(Dispatchers.IO) {
        runApiCall {
            val response = api.fetchCandidates(electionId)
            if (!response.isSuccessful) return@runApiCall ApiResult.Failure(mapFailure(response))

            val payload = response.body()
            val rows = payload.asFlexibleArray("data", "items", "candidates")
            val candidates = rows.mapNotNull { row ->
                val id = row.optString("id", "candidateId", "uuid")
                val fullName = row.optString("fullName", "name", "candidate_name")
                if (id.isBlank() || fullName.isBlank()) return@mapNotNull null

                CandidateRecord(
                    id = id,
                    electionId = row.optString("electionId", "election_id").ifBlank { electionId },
                    fullName = fullName,
                    party = row.optString("party", "affiliation"),
                    platform = row.optString("platform", "bio", "manifesto"),
                    photoUrl = row.optNullableString("photoUrl", "photo_url", "avatar_url")
                )
            }
            ApiResult.Success(candidates)
        }
    }

    suspend fun castVote(
        electionId: String,
        candidateId: String,
        userId: String,
        accessToken: String
    ): ApiResult<VoteReceipt> = withContext(Dispatchers.IO) {
        runApiCall {
            val response = api.castVote(
                authorization = "Bearer $accessToken",
                request = VoteRequest(electionId = electionId, candidateId = candidateId, userId = userId)
            )
            if (!response.isSuccessful) return@runApiCall ApiResult.Failure(mapFailure(response))

            val payload = response.body().asObjectOrNull()
            val receiptId = payload?.optString("receiptId", "verificationId", "id").orEmpty()
            val status = payload?.optString("status", "voteStatus").orEmpty().ifBlank { "SUBMITTED" }
            val message = payload?.optString("message", "detail").orEmpty().ifBlank { "Vote submitted successfully." }

            ApiResult.Success(VoteReceipt(receiptId = receiptId, status = status, message = message))
        }
    }

    suspend fun verifyVote(receiptId: String, accessToken: String): ApiResult<VoteReceipt> = withContext(Dispatchers.IO) {
        runApiCall {
            val response = api.verifyVote(receiptId = receiptId, authorization = "Bearer $accessToken")
            if (!response.isSuccessful) return@runApiCall ApiResult.Failure(mapFailure(response))

            val payload = response.body().asObjectOrNull()
            val status = payload?.optString("status", "verificationStatus").orEmpty().ifBlank { "VERIFIED" }
            val message = payload?.optString("message", "detail").orEmpty().ifBlank { "Vote verification completed." }

            ApiResult.Success(VoteReceipt(receiptId = receiptId, status = status, message = message))
        }
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
}

private fun JsonObject.optString(vararg keys: String): String {
    for (key in keys) {
        val value = get(key) ?: continue
        if (!value.isJsonNull) {
            return value.asString.orEmpty().trim()
        }
    }
    return ""
}

private fun JsonObject.optNullableString(vararg keys: String): String? {
    val value = optString(*keys)
    return value.ifBlank { null }
}

private fun JsonElement?.asFlexibleArray(vararg keys: String): List<JsonObject> {
    if (this == null || this.isJsonNull) return emptyList()
    if (isJsonArray) return asJsonArray.asObjectList()
    if (!isJsonObject) return emptyList()

    val root = asJsonObject
    for (key in keys) {
        val candidate = root.get(key)
        if (candidate != null && candidate.isJsonArray) {
            return candidate.asJsonArray.asObjectList()
        }
    }

    return listOf(root)
}

private fun JsonElement?.asObjectOrNull(): JsonObject? {
    if (this == null || this.isJsonNull) return null
    if (!isJsonObject) return null
    return asJsonObject
}

private fun JsonArray.asObjectList(): List<JsonObject> {
    val list = ArrayList<JsonObject>(size())
    forEach { element ->
        if (element != null && element.isJsonObject) {
            list.add(element.asJsonObject)
        }
    }
    return list
}

