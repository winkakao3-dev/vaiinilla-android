package com.vaiinilla.app.core.network

class ApiClientException(
    val code: String,
    message: String,
    val httpStatus: Int,
) : IllegalStateException(message)

class MissingAccessTokenException :
    IllegalStateException(
        "Falta el token de acceso. Configura vaiinillaAccessToken en local.properties " +
            "o guárdalo con SecureSessionStore.",
    )
