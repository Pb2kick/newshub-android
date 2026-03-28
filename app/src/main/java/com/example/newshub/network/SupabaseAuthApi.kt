package com.example.newshub.network

import com.example.newshub.network.model.AuthTokenRequest
import com.example.newshub.network.model.AuthTokenResponse
import com.example.newshub.network.model.ChangePasswordRequest
import com.example.newshub.network.model.SignUpRequest
import com.example.newshub.network.model.SupabaseUserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT

interface SupabaseAuthApi {

    @POST("auth/v1/token?grant_type=password")
    suspend fun signInWithPassword(
        @Header("apikey") apiKey: String,
        @Body request: AuthTokenRequest
    ): Response<AuthTokenResponse>

    @POST("auth/v1/signup")
    suspend fun signUpWithPassword(
        @Header("apikey") apiKey: String,
        @Body request: SignUpRequest
    ): Response<AuthTokenResponse>

    @GET("auth/v1/user")
    suspend fun getCurrentUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String
    ): Response<SupabaseUserResponse>

    @PUT("auth/v1/user")
    suspend fun updatePassword(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body request: ChangePasswordRequest
    ): Response<Unit>
}

