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

    /** Supabase / auth — keep moderate timeouts. */
    private val defaultClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Render free tier can take 50s+ to wake; use long timeouts and retries.
     */
    private val backendClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(RetryInterceptor(maxRetries = 2, initialDelayMs = 3_000L))
        .addInterceptor(logging)
        .connectTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build()

    fun authApi(baseUrl: String): SupabaseAuthApi {
        return retrofit(baseUrl, defaultClient).create(SupabaseAuthApi::class.java)
    }

    fun restApi(baseUrl: String): SupabaseRestApi {
        return retrofit(baseUrl, defaultClient).create(SupabaseRestApi::class.java)
    }

    fun storageApi(baseUrl: String): SupabaseStorageApi {
        return retrofit(baseUrl, defaultClient).create(SupabaseStorageApi::class.java)
    }

    fun backendApi(baseUrl: String): BackendApi {
        return retrofit(baseUrl, backendClient).create(BackendApi::class.java)
    }

    private fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit {
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
