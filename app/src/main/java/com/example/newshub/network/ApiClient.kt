package com.example.newshub.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    fun authApi(baseUrl: String): SupabaseAuthApi {
        return retrofit(baseUrl).create(SupabaseAuthApi::class.java)
    }

    fun restApi(baseUrl: String): SupabaseRestApi {
        return retrofit(baseUrl).create(SupabaseRestApi::class.java)
    }

    fun storageApi(baseUrl: String): SupabaseStorageApi {
        return retrofit(baseUrl).create(SupabaseStorageApi::class.java)
    }

    private fun retrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(withTrailingSlash(baseUrl))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun withTrailingSlash(value: String): String {
        return if (value.endsWith("/")) value else "$value/"
    }
}

