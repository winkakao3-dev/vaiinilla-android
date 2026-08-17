package com.vaiinilla.app.core.network

import android.util.Log
import com.vaiinilla.app.core.auth.ActiveSessionRefresher
import com.vaiinilla.app.core.config.AppEnvironment
import com.vaiinilla.app.core.security.SecureSessionStore
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpVaiinillaApiClient
    @Inject
    constructor(
        environment: AppEnvironment,
        private val sessionStore: SecureSessionStore,
        private val responseParser: ApiResponseParser,
        private val sessionRefresher: ActiveSessionRefresher,
    ) : VaiinillaApiClient {
        override val baseUrl: String = environment.apiBaseUrl

        override fun get(
            path: String,
            query: Map<String, String>,
        ): Result<String> = execute(method = "GET", path = path, query = query)

        override fun post(
            path: String,
            body: String,
            headers: Map<String, String>,
        ): Result<String> = execute(method = "POST", path = path, body = body, headers = headers)

        override fun deleteWithBearer(
            bearer: String,
            path: String,
            body: String?,
            headers: Map<String, String>,
        ): Result<String> =
            execute(
                method = "DELETE",
                path = path,
                body = body,
                headers = headers,
                accessToken = bearer,
                allowSessionRefresh = false,
            )

        override fun deleteWithBearerExpecting200(
            bearer: String,
            path: String,
            body: String?,
            headers: Map<String, String>,
        ): Result<String> =
            execute(
                method = "DELETE",
                path = path,
                body = body,
                headers = headers,
                accessToken = bearer,
                allowSessionRefresh = false,
                expectedStatus = 200,
            )

        override fun put(
            path: String,
            body: String,
            headers: Map<String, String>,
        ): Result<String> = execute(method = "PUT", path = path, body = body, headers = headers)

        override fun putMultipart(
            path: String,
            fieldName: String,
            filename: String,
            mimeType: String,
            bytes: ByteArray,
            headers: Map<String, String>,
        ): Result<String> =
            runCatching {
                sendMultipartOnce(
                    method = "PUT",
                    path = path,
                    fieldName = fieldName,
                    filename = filename,
                    mimeType = mimeType,
                    bytes = bytes,
                    headers = headers,
                    allowSessionRefresh = true,
                )
            }

        override fun postMultipart(
            path: String,
            fieldName: String,
            filename: String,
            mimeType: String,
            bytes: ByteArray,
            headers: Map<String, String>,
        ): Result<String> =
            runCatching {
                sendMultipartOnce(
                    method = "POST",
                    path = path,
                    fieldName = fieldName,
                    filename = filename,
                    mimeType = mimeType,
                    bytes = bytes,
                    headers = headers,
                    allowSessionRefresh = true,
                )
            }

        override fun getPublic(
            path: String,
            query: Map<String, String>,
        ): Result<String> =
            execute(
                method = "GET",
                path = path,
                query = query,
                requireAuth = false,
                allowSessionRefresh = false,
            )

        override fun postPublic(
            path: String,
            body: String,
            headers: Map<String, String>,
        ): Result<String> =
            execute(
                method = "POST",
                path = path,
                body = body,
                headers = headers,
                requireAuth = false,
                allowSessionRefresh = false,
            )

        fun postWithAccessToken(
            accessToken: String,
            path: String,
            body: String,
            headers: Map<String, String> = emptyMap(),
        ): Result<String> =
            execute(
                method = "POST",
                path = path,
                body = body,
                headers = headers,
                accessToken = accessToken,
            )

        fun postWithBearer(
            bearer: String,
            path: String,
            body: String? = null,
            headers: Map<String, String> = emptyMap(),
        ): Result<String> =
            execute(
                method = "POST",
                path = path,
                body = body,
                headers = headers,
                accessToken = bearer,
                allowSessionRefresh = false,
            )

        fun getWithBearer(
            bearer: String,
            path: String,
            query: Map<String, String> = emptyMap(),
        ): Result<String> =
            execute(
                method = "GET",
                path = path,
                query = query,
                accessToken = bearer,
                allowSessionRefresh = false,
            )

        private fun execute(
            method: String,
            path: String,
            query: Map<String, String> = emptyMap(),
            body: String? = null,
            headers: Map<String, String> = emptyMap(),
            accessToken: String? = null,
            requireAuth: Boolean = true,
            allowSessionRefresh: Boolean = true,
            expectedStatus: Int? = null,
        ): Result<String> =
            runCatching {
                executeOnce(
                    method = method,
                    path = path,
                    query = query,
                    body = body,
                    headers = headers,
                    accessToken = accessToken,
                    requireAuth = requireAuth,
                    allowSessionRefresh = allowSessionRefresh,
                    expectedStatus = expectedStatus,
                )
            }

        private fun executeOnce(
            method: String,
            path: String,
            query: Map<String, String>,
            body: String?,
            headers: Map<String, String>,
            accessToken: String?,
            requireAuth: Boolean,
            allowSessionRefresh: Boolean,
            expectedStatus: Int?,
        ): String {
            val token =
                accessToken?.takeIf { it.isNotBlank() }
                    ?: sessionStore.readAccessToken()?.takeIf { it.isNotBlank() }
            if (requireAuth && token.isNullOrBlank()) {
                throw MissingAccessTokenException()
            }

            val connection = openConnection(method, path, query)
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            if (!token.isNullOrBlank()) {
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            headers.forEach { (name, value) ->
                connection.setRequestProperty(name, value)
            }

            if (body != null) {
                val payload = body.toByteArray(Charsets.UTF_8)
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.doOutput = true
                connection.requestMethod = method
                connection.setFixedLengthStreamingMode(payload.size)
                connection.outputStream.use { stream ->
                    stream.write(payload)
                }
            }

            val status = connection.responseCode
            val retryAfterSeconds = connection.getHeaderField("Retry-After")?.trim()?.toLongOrNull()
            val raw = readBody(connection, status)
            Log.w(TAG, "$method $path -> $status")
            if (status == expectedStatus || (expectedStatus == null && status in 200..299)) {
                return raw
            }

            val error = responseParser.parseError(raw, status, retryAfterSeconds)
            if (
                allowSessionRefresh &&
                error.httpStatus == 401 &&
                error.code.equals("UNAUTHENTICATED", ignoreCase = true)
            ) {
                sessionRefresher.refreshActiveSession().getOrThrow()
                return executeOnce(
                    method = method,
                    path = path,
                    query = query,
                    body = body,
                    headers = headers,
                    accessToken = null,
                    requireAuth = true,
                    allowSessionRefresh = false,
                    expectedStatus = expectedStatus,
                )
            }
            throw error
        }

        private fun sendMultipartOnce(
            method: String,
            path: String,
            fieldName: String,
            filename: String,
            mimeType: String,
            bytes: ByteArray,
            headers: Map<String, String>,
            allowSessionRefresh: Boolean,
        ): String {
            val token = sessionStore.readAccessToken()?.takeIf { it.isNotBlank() }
            if (token.isNullOrBlank()) throw MissingAccessTokenException()
            val boundary = "----VaiinillaForm${System.nanoTime()}"
            val safeName = filename.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "producto.jpg" }
            val prelude =
                buildString {
                    append("--").append(boundary).append("\r\n")
                    append("Content-Disposition: form-data; name=\"")
                    append(fieldName)
                    append("\"; filename=\"")
                    append(safeName)
                    append("\"\r\n")
                    append("Content-Type: ").append(mimeType).append("\r\n\r\n")
                }.toByteArray(Charsets.UTF_8)
            val closing = "\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8)
            val connection = (URL(buildUrl(path, emptyMap())).openConnection() as HttpURLConnection)
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = MULTIPART_READ_TIMEOUT_MS
            connection.instanceFollowRedirects = false
            connection.doOutput = true
            connection.requestMethod = method
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
            connection.setFixedLengthStreamingMode(prelude.size + bytes.size + closing.size)
            try {
                connection.outputStream.use { stream ->
                    stream.write(prelude)
                    stream.write(bytes)
                    stream.write(closing)
                }
                val status = connection.responseCode
                val retryAfterSeconds = connection.getHeaderField("Retry-After")?.trim()?.toLongOrNull()
                val raw = readBody(connection, status)
                Log.w(
                    TAG,
                    "$method-MULTIPART $path (${bytes.size} bytes) -> $status",
                )
                if (status in 200..299) return raw
                val error = responseParser.parseError(raw, status, retryAfterSeconds)
                if (
                    allowSessionRefresh &&
                    error.httpStatus == 401 &&
                    error.code.equals("UNAUTHENTICATED", ignoreCase = true)
                ) {
                    sessionRefresher.refreshActiveSession().getOrThrow()
                    return sendMultipartOnce(
                        method = method,
                        path = path,
                        fieldName = fieldName,
                        filename = filename,
                        mimeType = mimeType,
                        bytes = bytes,
                        headers = headers,
                        allowSessionRefresh = false,
                    )
                }
                throw error
            } finally {
                connection.disconnect()
            }
        }

        private fun openConnection(
            method: String,
            path: String,
            query: Map<String, String>,
        ): HttpURLConnection {
            val url = URL(buildUrl(path, query))
            return (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                requestMethod = method
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
            }
        }

        private fun buildUrl(
            path: String,
            query: Map<String, String>,
        ): String {
            val normalizedBase = baseUrl.trimEnd('/') + '/'
            val normalizedPath = path.trimStart('/')
            val baseWithPath = normalizedBase + normalizedPath
            if (query.isEmpty()) return baseWithPath

            val queryString =
                query.entries.joinToString("&") { (key, value) ->
                    "${URLEncoder.encode(key, Charsets.UTF_8.name())}=" +
                        URLEncoder.encode(value, Charsets.UTF_8.name())
                }
            return "$baseWithPath?$queryString"
        }

        private fun readBody(
            connection: HttpURLConnection,
            status: Int,
        ): String {
            val stream =
                if (status in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream ?: connection.inputStream
                } ?: return ""
            return stream.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
            }
        }

        private companion object {
            const val TAG = "VaiinillaHttp"
            const val CONNECT_TIMEOUT_MS = 15_000
            const val READ_TIMEOUT_MS = 20_000
            const val MULTIPART_READ_TIMEOUT_MS = 60_000
        }
    }
