package com.example.newshub.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException

/**
 * Retries idempotent GET-style failures when Render (or similar) is cold-starting.
 */
class RetryInterceptor(
    private val maxRetries: Int = 2,
    private val initialDelayMs: Long = 2_000L
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 0
        while (true) {
            try {
                return chain.proceed(chain.request())
            } catch (error: IOException) {
                if (!shouldRetry(error) || attempt >= maxRetries) {
                    throw error
                }
                attempt++
                Thread.sleep(initialDelayMs * attempt)
            }
        }
    }

    private fun shouldRetry(error: IOException): Boolean {
        return error is SocketTimeoutException || error is ConnectException
    }
}
