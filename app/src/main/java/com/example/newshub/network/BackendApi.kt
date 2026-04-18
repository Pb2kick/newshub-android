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
        @Query("location") location: String? = null
    ): Response<JsonElement>

    @GET("api/news/content")
    suspend fun fetchNewsContent(
        @Query("url") url: String
    ): Response<JsonElement>

    @GET("api/elections")
    suspend fun fetchElections(): Response<JsonElement>

    @GET("api/elections/{electionId}/candidates")
    suspend fun fetchCandidates(
        @Path("electionId") electionId: String
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

