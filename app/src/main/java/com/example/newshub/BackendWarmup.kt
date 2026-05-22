package com.example.newshub

import com.example.newshub.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Pings the Render backend on app start so the first user-facing request is less likely to time out.
 */
object BackendWarmup {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun ping() {
        val baseUrl = BuildConfig.BACKEND_BASE_URL.trim().trimEnd('/')
        if (baseUrl.isBlank()) return
        scope.launch {
            runCatching {
                ApiClient.backendApi(baseUrl).fetchNews(page = 0, size = 1)
            }
        }
    }
}
