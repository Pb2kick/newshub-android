package com.example.newshub.network

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface SupabaseStorageApi {

    @POST
    suspend fun uploadObject(
        @Url url: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("x-upsert") upsert: String = "true",
        @Body body: RequestBody
    ): Response<ResponseBody>
}

