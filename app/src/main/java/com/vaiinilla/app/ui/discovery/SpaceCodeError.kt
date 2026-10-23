package com.vaiinilla.app.ui.discovery

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.toUserFacingMessage

/**
 * Lo que se dice cuando un código o QR de mesa no abre nada. El servidor distingue un código que el negocio
 * desactivó (422 `SPACE_NOT_ACTIVE`) de uno que no existe (404 `ESTABLISHMENT_NOT_FOUND`, "No se pudo resolver
 * el espacio."), y a cada uno se le dice en claro qué hacer.
 */
internal object SpaceCodeError {
    const val NOT_ACTIVE = "Este código no está activo. Escanea el QR de tu mesa o pide el código en el lugar."
    const val NOT_FOUND = "No encontramos ese código. Revisa que esté bien escrito o escanea el QR de tu mesa."

    fun message(error: Throwable?): String {
        val api = generateSequence(error) { it.cause }.filterIsInstance<ApiClientException>().firstOrNull()
        return when (api?.code) {
            "SPACE_NOT_ACTIVE" -> NOT_ACTIVE
            "ESTABLISHMENT_NOT_FOUND" -> NOT_FOUND
            else -> error.toUserFacingMessage("No se pudo abrir ese código.")
        }
    }
}
