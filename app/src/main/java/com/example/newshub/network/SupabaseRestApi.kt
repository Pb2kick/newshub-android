package com.example.newshub.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Url

interface SupabaseRestApi {

    @GET
    suspend fun fetchRows(
        @Url url: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Accept") accept: String = "application/json"
    ): Response<List<Map<String, Any?>>>

    @PATCH
    suspend fun patchRows(
        @Url url: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Header("Prefer") prefer: String = "return=representation",
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<List<Map<String, Any?>>>

    @POST
    suspend fun insertRow(
        @Url url: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Header("Prefer") prefer: String = "return=minimal",
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<Unit>
}

