package com.example.newshub.network

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorMapper {

    fun fromStatusCode(statusCode: Int, detail: String? = null): ApiFailure {
        return when (statusCode) {
            400 -> ApiFailure(ApiFailureType.BadRequest, statusCode, detail)
            401 -> ApiFailure(ApiFailureType.Unauthorized, statusCode, detail)
            in 500..599 -> ApiFailure(ApiFailureType.Server, statusCode, detail)
            else -> ApiFailure(ApiFailureType.Unknown, statusCode, detail)
        }
    }

    fun fromThrowable(throwable: Throwable): ApiFailure {
        return when (throwable) {
            is ConnectException -> ApiFailure(ApiFailureType.Configuration, detail = throwable.message)
            is SocketTimeoutException -> ApiFailure(ApiFailureType.Configuration, detail = throwable.message)
            is UnknownHostException -> ApiFailure(ApiFailureType.Configuration, detail = throwable.message)
            is IOException -> ApiFailure(ApiFailureType.Network, detail = throwable.message)
            is IllegalArgumentException -> ApiFailure(ApiFailureType.Configuration, detail = throwable.message)
            else -> ApiFailure(ApiFailureType.Unknown, detail = throwable.message)
        }
    }
}

