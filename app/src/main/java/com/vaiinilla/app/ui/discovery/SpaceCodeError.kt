package com.vaiinilla.app.ui.discovery

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.toUserFacingMessage

/**
 * Lo que se dice cuando un código o QR de mesa no abre nada. El servidor responde igual a un código que no
 * existe y a uno que el negocio desactivó ("No se pudo resolver el espacio."), así que se dice en claro qué hacer.
 */
internal object SpaceCodeError {
    const val NOT_ACTIVE = "Este código no está activo. Escanea el QR de tu mesa o pide el código en el lugar."

    fun message(error: Throwable?): String {
        val api = generateSequence(error) { it.cause }.filterIsInstance<ApiClientException>().firstOrNull()
        if (api?.code == "ESTABLISHMENT_NOT_FOUND") return NOT_ACTIVE
        return error.toUserFacingMessage("No se pudo abrir ese código.")
    }
}
