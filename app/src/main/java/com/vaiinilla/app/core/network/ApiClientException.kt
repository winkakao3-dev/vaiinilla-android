package com.vaiinilla.app.core.network

import kotlinx.serialization.json.JsonElement

class ApiClientException(
    val code: String,
    message: String,
    val httpStatus: Int,
    val retryAfterSeconds: Long? = null,
    /** `error.details` del sobre (p. ej. `intentos_restantes` de un PIN equivocado); null si no vino. */
    val details: JsonElement? = null,
) : IllegalStateException(message)

class MissingAccessTokenException :
    IllegalStateException(
        "Falta el contexto operativo. Inicia sesión y selecciona un establecimiento para continuar.",
    )
