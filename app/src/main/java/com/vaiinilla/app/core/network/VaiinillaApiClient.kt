package com.vaiinilla.app.core.network

interface VaiinillaApiClient {
    val baseUrl: String

    fun get(
        path: String,
        query: Map<String, String> = emptyMap(),
    ): Result<String>

    fun post(
        path: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
    ): Result<String>
}

class RemoteClientNotConfiguredException(
    message: String,
) : IllegalStateException(message)
