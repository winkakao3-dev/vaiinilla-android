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

    fun postWithoutBody(
        path: String,
        headers: Map<String, String> = emptyMap(),
    ): Result<String> = Result.failure(UnsupportedOperationException("POST sin body no soportado"))

    fun delete(
        path: String,
        body: String? = null,
        headers: Map<String, String> = emptyMap(),
    ): Result<String> = Result.failure(UnsupportedOperationException("DELETE no soportado"))

    /** Sends an explicit bearer without reading or refreshing the Vaiinilla context JWT. */
    fun deleteWithBearer(
        bearer: String,
        path: String,
        body: String? = null,
        headers: Map<String, String> = emptyMap(),
    ): Result<String> = delete(path, body, headers)

    /** Account deletion has a strict 200-success contract. */
    fun deleteWithBearerExpecting200(
        bearer: String,
        path: String,
        body: String? = null,
        headers: Map<String, String> = emptyMap(),
    ): Result<String> = deleteWithBearer(bearer, path, body, headers)

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
