package com.example.newshub.network

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Failure(val error: ApiFailure) : ApiResult<Nothing>()
}

enum class ApiFailureType {
    BadRequest,
    Unauthorized,
    NotFound,
    Server,
    Network,
    Configuration,
    Unknown
}

data class ApiFailure(
    val type: ApiFailureType,
    val statusCode: Int? = null,
    val detail: String? = null
)

