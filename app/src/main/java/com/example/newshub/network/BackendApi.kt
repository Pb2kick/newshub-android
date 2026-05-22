package com.example.newshub.network

import com.google.gson.JsonElement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class VoteRequest(
    val electionId: String,
    val candidateId: String,
    val userId: String
)

interface BackendApi {

    @GET("api/news")
    suspend fun fetchNews(
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("location") location: String? = null,
        @Query("country") country: String? = null,
        @Query("area") area: String? = null,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null
    ): Response<JsonElement>

    @GET("api/news/search")
    suspend fun searchNews(
        @Query("q") query: String,
        @Query("location") location: String? = null,
        @Query("country") country: String? = null,
        @Query("area") area: String? = null,
        @Query("limit") limit: Int = 25
    ): Response<JsonElement>

    @GET("api/news/content")
    suspend fun fetchNewsContent(
        @Query("url") url: String
    ): Response<JsonElement>

    @GET("api/elections")
    suspend fun fetchElections(): Response<JsonElement>

    @GET("api/elections/search")
    suspend fun searchElections(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5
    ): Response<JsonElement>

    @GET("api/elections/{electionId}")
    suspend fun fetchElection(
        @Path("electionId") electionId: String
    ): Response<JsonElement>

    @GET("api/elections/{electionId}/candidates")
    suspend fun fetchCandidates(
        @Path("electionId") electionId: String
    ): Response<JsonElement>

    @GET("api/candidates/search")
    suspend fun searchCandidates(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5
    ): Response<JsonElement>

    @GET("api/candidates/{candidateId}")
    suspend fun fetchCandidate(
        @Path("candidateId") candidateId: String
    ): Response<JsonElement>

    @POST("api/votes")
    suspend fun castVote(
        @Header("Authorization") authorization: String,
        @Body request: VoteRequest
    ): Response<JsonElement>

    @GET("api/votes/verify/{receiptId}")
    suspend fun verifyVote(
        @Path("receiptId") receiptId: String,
        @Header("Authorization") authorization: String
    ): Response<JsonElement>
}
