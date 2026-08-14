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

    fun put(
        path: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
    ): Result<String> = Result.failure(UnsupportedOperationException("PUT no soportado"))

    fun putMultipart(
        path: String,
        fieldName: String,
        filename: String,
        mimeType: String,
        bytes: ByteArray,
        headers: Map<String, String> = emptyMap(),
    ): Result<String> = Result.failure(UnsupportedOperationException("multipart no soportado"))

    fun postMultipart(
        path: String,
        fieldName: String,
        filename: String,
        mimeType: String,
        bytes: ByteArray,
        headers: Map<String, String> = emptyMap(),
    ): Result<String> = Result.failure(UnsupportedOperationException("multipart no soportado"))

    /**
     * Public endpoints deliberately do not reuse the authenticated transport.
     * The concrete HTTP client overrides these methods to omit the session token
     * and refresh flow; the defaults keep lightweight test clients simple.
     */
    fun getPublic(
        path: String,
        query: Map<String, String> = emptyMap(),
    ): Result<String> = get(path, query)

    fun postPublic(
        path: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
    ): Result<String> = post(path, body, headers)
}

class RemoteClientNotConfiguredException(
    message: String,
) : IllegalStateException(message)
