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
    val authorImageUrl: String?,
    val category: String,
    val publishedAt: String,
    val readTime: String,
    val imageUrl: String?,
    val articleUrl: String?
)

data class NewsPageResult(
    val articles: List<NewsArticle>,
    val hasMore: Boolean,
    val page: Int
)

data class NewsArticleDetail(
    val title: String,
    val source: String,
    val author: String?,
    val authorImageUrl: String?,
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
    val description: String,
    val region: String = "",
    val imageUrl: String? = null,
    val candidateCount: Int = 0
)

data class CandidateRecord(
    val id: String,
    val electionId: String,
    val fullName: String,
    val party: String,
    val platform: String,
    val photoUrl: String?,
    val position: String = "",
    val education: String = ""
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

    suspend fun fetchNews(
        lat: Double? = null,
        lng: Double? = null,
        scope: String = "Local",
        country: String? = null,
        area: String? = null,
        page: Int = 0,
        size: Int = 4
    ): ApiResult<NewsPageResult> = withContext(Dispatchers.IO) {
        runApiCall {
            val response = api.fetchNews(
                lat = lat,
                lng = lng,
                location = scope,
                country = country,
                area = area,
                page = page,
                size = size
            )
            if (!response.isSuccessful) return@runApiCall ApiResult.Failure(mapFailure(response))
            ApiResult.Success(parseNewsPage(response.body(), page))
        }
    }

    suspend fun searchNews(
        query: String,
        scope: String?,
        country: String? = null,
        area: String? = null
    ): ApiResult<List<NewsArticle>> = withContext(Dispatchers.IO) {
        runApiCall {
            val response = api.searchNews(
                query = query,
                location = scope,
                country = country,
                area = area
            )
            if (!response.isSuccessful) return@runApiCall ApiResult.Failure(mapFailure(response))
            ApiResult.Success(parseArticles(response.body()))
        }
    }

    suspend fun searchElections(query: String): ApiResult<List<ElectionRecord>> = withContext(Dispatchers.IO) {
        runApiCall {
            val response = api.searchElections(query = query)
            if (!response.isSuccessful) return@runApiCall ApiResult.Failure(mapFailure(response))
            ApiResult.Success(parseElections(response.body()))
        }
    }

    suspend fun searchCandidates(query: String): ApiResult<List<CandidateRecord>> = withContext(Dispatchers.IO) {
        runApiCall {
            val response = api.searchCandidates(query = query)
            if (!response.isSuccessful) return@runApiCall ApiResult.Failure(mapFailure(response))
            ApiResult.Success(parseCandidates(response.body(), defaultElectionId = ""))
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
                    title = obj.optString("title", "headline", "name", "title").ifBlank { "" },
                    source = obj.optString("source", "publisher", "site", "sourceName").ifBlank { "NewsHub" },
                    author = obj.optNullableString("author", "writer", "byline", "creator"),
                    authorImageUrl = obj.optNullableString("authorImageUrl", "author_image", "publisher_logo", "source_icon", "authorPhoto"),
                    publishedAt = obj.optString("publishedAt", "published_at", "date", "created_at"),
                    content = obj.optString("content", "html", "body", "text", "description").ifBlank { "No article content available." },
                    articleUrl = obj.optNullableString("url", "articleUrl", "link", "sourceUrl") ?: url
                )
            )
        }
    }

    suspend fun fetchElections(): ApiResult<List<ElectionRecord>> = withContext(Dispatchers.IO) {
        runApiCall {
            val response = api.fetchElections()
            if (!response.isSuccessful) return@runApiCall ApiResult.Failure(mapFailure(response))
            ApiResult.Success(parseElections(response.body()))
        }
    }

    suspend fun fetchElection(electionId: String): ApiResult<ElectionRecord> = withContext(Dispatchers.IO) {
        runApiCall {
            val response = api.fetchElection(electionId)
            if (!response.isSuccessful) return@runApiCall ApiResult.Failure(mapFailure(response))
            val obj = response.body().asObjectOrNull()
                ?: response.body().asFlexibleArray("data").firstOrNull()
            if (obj == null) {
                return@runApiCall ApiResult.Failure(ApiFailure(ApiFailureType.Unknown, detail = "Election not found"))
            }
            parseElection(obj)?.let { ApiResult.Success(it) }
                ?: ApiResult.Failure(ApiFailure(ApiFailureType.Unknown, detail = "Election not found"))
        }
    }

    suspend fun fetchCandidates(electionId: String): ApiResult<List<CandidateRecord>> = withContext(Dispatchers.IO) {
        runApiCall {
            val response = api.fetchCandidates(electionId)
            if (!response.isSuccessful) return@runApiCall ApiResult.Failure(mapFailure(response))
            ApiResult.Success(parseCandidates(response.body(), defaultElectionId = electionId))
        }
    }

    suspend fun fetchCandidate(candidateId: String): ApiResult<CandidateRecord> = withContext(Dispatchers.IO) {
        runApiCall {
            val response = api.fetchCandidate(candidateId)
            if (!response.isSuccessful) return@runApiCall ApiResult.Failure(mapFailure(response))
            val obj = response.body().asObjectOrNull()
                ?: response.body().asFlexibleArray("data").firstOrNull()
            if (obj == null) {
                return@runApiCall ApiResult.Failure(ApiFailure(ApiFailureType.Unknown, detail = "Candidate not found"))
            }
            parseCandidate(obj, "")?.let { ApiResult.Success(it) }
                ?: ApiResult.Failure(ApiFailure(ApiFailureType.Unknown, detail = "Candidate not found"))
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

    private fun parseNewsPage(payload: JsonElement?, page: Int): NewsPageResult {
        val root = payload.asObjectOrNull()
        val hasMore = root?.optBoolean("hasMore", "has_more") ?: false
        val resolvedPage = root?.optInt("page")?.takeIf { it >= 0 } ?: page
        val articles = parseArticles(payload)
        return NewsPageResult(articles = articles, hasMore = hasMore, page = resolvedPage)
    }

    private fun parseArticles(payload: JsonElement?): List<NewsArticle> {
        val rows = payload.asFlexibleArray("data", "items", "news", "articles")
        return rows.mapNotNull { parseArticle(it) }
    }

    private fun parseArticle(row: JsonObject): NewsArticle? {
        val title = row.optString("title", "headline", "name", "text")
        if (title.isBlank()) return null
        return NewsArticle(
            id = row.optString("id", "newsId", "uuid").ifBlank { title.hashCode().toString() },
            title = title,
            summary = row.optString("summary", "description", "snippet", "excerpt", "content"),
            source = row.optString("source", "publisher", "site", "sourceName").ifBlank { "NewsHub" },
            author = row.optString("author", "publisher", "source", "creator").ifBlank { "NewsHub" },
            authorImageUrl = row.optNullableString("authorImageUrl", "author_image", "publisher_logo", "source_icon", "authorPhoto"),
            category = row.optString("category", "section", "topic", "tag").ifBlank { "Top Stories" },
            publishedAt = row.optString("publishedAt", "published_at", "date", "created_at"),
            readTime = row.optString("readTime", "read_time", "readingTime").ifBlank { "3 min read" },
            imageUrl = row.optNullableString("imageUrl", "image_url", "thumbnail", "image"),
            articleUrl = row.optNullableString("url", "articleUrl", "link", "sourceUrl")
        )
    }

    private fun parseElections(payload: JsonElement?): List<ElectionRecord> {
        return payload.asFlexibleArray("data", "items", "elections").mapNotNull { parseElection(it) }
    }

    private fun parseElection(row: JsonObject): ElectionRecord? {
        val id = row.optString("id", "electionId", "uuid")
        val name = row.optString("name", "title")
        if (id.isBlank() || name.isBlank()) return null
        return ElectionRecord(
            id = id,
            name = name,
            status = row.optString("status", "state").ifBlank { "OPEN" },
            startDate = row.optString("startDate", "start_date"),
            endDate = row.optString("endDate", "end_date"),
            description = row.optString("description", "details"),
            region = row.optString("region", "location"),
            imageUrl = row.optNullableString("imageUrl", "image_url", "image"),
            candidateCount = row.optInt("candidateCount", "candidate_count", "candidatesCount")
        )
    }

    private fun parseCandidates(payload: JsonElement?, defaultElectionId: String): List<CandidateRecord> {
        return payload.asFlexibleArray("data", "items", "candidates").mapNotNull {
            parseCandidate(it, defaultElectionId)
        }
    }

    private fun parseCandidate(row: JsonObject, defaultElectionId: String): CandidateRecord? {
        val id = row.optString("id", "candidateId", "uuid")
        val fullName = row.optString("fullName", "name", "candidate_name")
        if (id.isBlank() || fullName.isBlank()) return null
        return CandidateRecord(
            id = id,
            electionId = row.optString("electionId", "election_id").ifBlank { defaultElectionId },
            fullName = fullName,
            party = row.optString("party", "affiliation"),
            platform = row.optString("platform", "bio", "manifesto"),
            photoUrl = row.optNullableString("photoUrl", "photo_url", "avatar_url"),
            position = row.optString("position", "office", "role"),
            education = row.optString("education", "school")
        )
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

private fun JsonObject.optInt(vararg keys: String): Int {
    for (key in keys) {
        val value = get(key) ?: continue
        if (!value.isJsonNull) {
            return when {
                value.isJsonPrimitive && value.asJsonPrimitive.isNumber -> value.asInt
                value.isJsonPrimitive -> value.asString.toIntOrNull() ?: 0
                else -> 0
            }
        }
    }
    return 0
}

private fun JsonObject.optBoolean(vararg keys: String): Boolean {
    for (key in keys) {
        val value = get(key) ?: continue
        if (!value.isJsonNull) {
            return when {
                value.isJsonPrimitive && value.asJsonPrimitive.isBoolean -> value.asBoolean
                value.isJsonPrimitive -> value.asString.equals("true", ignoreCase = true)
                else -> false
            }
        }
    }
    return false
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
