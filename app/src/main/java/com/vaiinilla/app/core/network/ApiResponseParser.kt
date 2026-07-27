package com.vaiinilla.app.core.network

import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiResponseParser
    @Inject
    constructor() {
        private val json =
            Json {
                ignoreUnknownKeys = true
                explicitNulls = true
                isLenient = false
            }

        fun parseError(
            raw: String,
            httpStatus: Int,
        ): ApiClientException {
            val envelope = runCatching { json.decodeFromString<ErrorEnvelopeDto>(raw) }.getOrNull()
            val error = envelope?.error
            return ApiClientException(
                code = error?.code ?: "HTTP_$httpStatus",
                message = error?.message ?: "La API respondió con código $httpStatus.",
                httpStatus = httpStatus,
            )
        }
    }
