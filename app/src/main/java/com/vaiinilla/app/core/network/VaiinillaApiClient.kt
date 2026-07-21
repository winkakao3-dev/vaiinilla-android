package com.vaiinilla.app.core.network

interface VaiinillaApiClient {
    val baseUrl: String

    fun get(path: String): Result<String>
}

class EmptyVaiinillaApiClient(
    override val baseUrl: String,
) : VaiinillaApiClient {
    override fun get(path: String): Result<String> = Result.failure(
        RemoteClientNotConfiguredException(
            "El cliente remoto está preparado pero no implementado. " +
                "Se requiere OpenAPI aprobado antes de conectar $path.",
        ),
    )
}

class RemoteClientNotConfiguredException(message: String) : IllegalStateException(message)
