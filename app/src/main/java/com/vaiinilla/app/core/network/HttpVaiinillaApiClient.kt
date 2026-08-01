package com.vaiinilla.app.core.network

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
            body: String,
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
        ): String {
            val token =
                accessToken?.takeIf { it.isNotBlank() }
                    ?: sessionStore.readAccessToken()?.takeIf { it.isNotBlank() }
            if (requireAuth && token.isNullOrBlank()) {
                throw MissingAccessTokenException()
            }

            val connection = openConnection(method, path, query)
            connection.setRequestProperty("Accept", "application/json")
            if (!token.isNullOrBlank()) {
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            headers.forEach { (name, value) ->
                connection.setRequestProperty(name, value)
            }

            if (body != null) {
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.doOutput = true
                connection.outputStream.use { stream ->
                    stream.write(body.toByteArray(Charsets.UTF_8))
                }
            }

            val status = connection.responseCode
            val raw = readBody(connection, status)
            if (status in 200..299) {
                return raw
            }

            val error = responseParser.parseError(raw, status)
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
                )
            }
            throw error
        }

        private fun openConnection(
            method: String,
            path: String,
            query: Map<String, String>,
        ): HttpURLConnection {
            val url = URL(buildUrl(path, query))
            return (url.openConnection() as HttpURLConnection).apply {
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
            const val CONNECT_TIMEOUT_MS = 15_000
            const val READ_TIMEOUT_MS = 20_000
        }
    }
