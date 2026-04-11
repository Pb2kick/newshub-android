package com.example.newshub.feature.home.data

import com.example.newshub.AuthUserRecord
import com.example.newshub.feature.auth.data.AuthRepository
import com.example.newshub.feature.auth.data.SupabaseAuthRepository
import com.example.newshub.network.ApiResult

interface HomeRepository {
    suspend fun fetchAuthUser(accessToken: String): ApiResult<AuthUserRecord>
}

class SupabaseHomeRepository(
    private val authRepository: AuthRepository = SupabaseAuthRepository()
) : HomeRepository {
    override suspend fun fetchAuthUser(accessToken: String): ApiResult<AuthUserRecord> {
        return authRepository.fetchAuthUser(accessToken)
    }
}

