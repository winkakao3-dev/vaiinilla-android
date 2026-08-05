package com.vaiinilla.app.core.network

class ApiClientException(
    val code: String,
    message: String,
    val httpStatus: Int,
) : IllegalStateException(message)

class MissingAccessTokenException :
    IllegalStateException(
        "Falta el contexto operativo. Inicia sesión y selecciona un establecimiento para continuar.",
    )
