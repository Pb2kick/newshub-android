package com.example.newshub.network.model

import com.google.gson.annotations.SerializedName

data class AuthTokenRequest(
    val email: String,
    val password: String
)

data class SignUpRequest(
    val email: String,
    val password: String,
    val data: Map<String, String>? = null
)

data class AuthTokenResponse(
    @SerializedName("access_token") val accessToken: String?,
    val user: AuthUserDto?
)

data class AuthUserDto(
    val id: String?,
    val email: String?
)

data class ChangePasswordRequest(
    val password: String
)

data class SupabaseUserResponse(
    val id: String?,
    val email: String?,
    @SerializedName("user_metadata") val userMetadata: Map<String, Any?>?
)

