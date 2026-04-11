package com.example.newshub.feature.profile.data

import com.example.newshub.ProfileRecord
import com.example.newshub.SupabaseService
import com.example.newshub.network.ApiResult

interface ProfileRepository {
    val isConfigured: Boolean

    suspend fun fetchProfile(userId: String, accessToken: String): ApiResult<ProfileRecord?>

    suspend fun upsertProfile(
        userId: String,
        firstName: String,
        lastName: String,
        avatarUrl: String?,
        email: String?,
        accessToken: String?
    ): ApiResult<Unit>

    suspend fun uploadAvatar(
        userId: String,
        bytes: ByteArray,
        mimeType: String,
        accessToken: String?
    ): ApiResult<String>
}

class SupabaseProfileRepository(
    private val service: SupabaseService = SupabaseService()
) : ProfileRepository {

    override val isConfigured: Boolean
        get() = service.isConfigured

    override suspend fun fetchProfile(userId: String, accessToken: String): ApiResult<ProfileRecord?> {
        return service.fetchProfile(userId, accessToken)
    }

    override suspend fun upsertProfile(
        userId: String,
        firstName: String,
        lastName: String,
        avatarUrl: String?,
        email: String?,
        accessToken: String?
    ): ApiResult<Unit> {
        return service.upsertProfile(userId, firstName, lastName, avatarUrl, email, accessToken)
    }

    override suspend fun uploadAvatar(
        userId: String,
        bytes: ByteArray,
        mimeType: String,
        accessToken: String?
    ): ApiResult<String> {
        return service.uploadAvatar(userId, bytes, mimeType, accessToken)
    }
}

