package com.example.newshub.feature.auth.data

import com.example.newshub.AuthSession
import com.example.newshub.AuthUserRecord
import com.example.newshub.SupabaseService
import com.example.newshub.network.ApiResult

interface AuthRepository {
    val isConfigured: Boolean

    suspend fun signIn(email: String, password: String): ApiResult<AuthSession>

    suspend fun signUp(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ): ApiResult<AuthSession?>

    suspend fun fetchAuthUser(accessToken: String): ApiResult<AuthUserRecord>

    suspend fun updatePassword(newPassword: String, accessToken: String): ApiResult<Unit>
}

class SupabaseAuthRepository(
    private val service: SupabaseService = SupabaseService()
) : AuthRepository {

    override val isConfigured: Boolean
        get() = service.isConfigured

    override suspend fun signIn(email: String, password: String): ApiResult<AuthSession> {
        return service.signInWithPassword(email, password)
    }

    override suspend fun signUp(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ): ApiResult<AuthSession?> {
        return service.signUpWithPassword(email, password, firstName, lastName)
    }

    override suspend fun fetchAuthUser(accessToken: String): ApiResult<AuthUserRecord> {
        return service.fetchAuthUser(accessToken)
    }

    override suspend fun updatePassword(newPassword: String, accessToken: String): ApiResult<Unit> {
        return service.updatePassword(newPassword, accessToken)
    }
}

