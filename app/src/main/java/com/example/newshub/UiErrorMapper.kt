package com.example.newshub

import com.example.newshub.network.ApiFailure
import com.example.newshub.network.ApiFailureType

object UiErrorMapper {

    fun toMessageRes(error: ApiFailure): Int {
        return when (error.type) {
            ApiFailureType.BadRequest -> R.string.error_bad_request
            ApiFailureType.Unauthorized -> R.string.error_unauthorized
            ApiFailureType.Server -> R.string.error_server
            ApiFailureType.Network -> R.string.error_network
            ApiFailureType.Configuration -> R.string.error_configuration
            ApiFailureType.Unknown -> R.string.error_unknown
        }
    }
}

