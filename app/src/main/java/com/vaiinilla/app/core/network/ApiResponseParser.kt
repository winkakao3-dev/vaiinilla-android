package com.vaiinilla.app.core.network

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiResponseParser
    @Inject
    constructor() {
        private val json =
            ApiJson.plain

        fun parseError(
            raw: String,
            httpStatus: Int,
            retryAfterSeconds: Long? = null,
        ): ApiClientException {
            val envelope = runCatching { json.decodeFromString<ErrorEnvelopeDto>(raw) }.getOrNull()
            val error = envelope?.error
            return ApiClientException(
                code = error?.code ?: "HTTP_$httpStatus",
                message = error?.message ?: "La API respondió con código $httpStatus.",
                httpStatus = httpStatus,
                retryAfterSeconds = retryAfterSeconds,
                details = error?.details,
            )
        }
    }
