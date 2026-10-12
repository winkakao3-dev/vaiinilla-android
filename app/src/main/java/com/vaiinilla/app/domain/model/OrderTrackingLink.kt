package com.vaiinilla.app.domain.model

/** Enlace de seguimiento de un pedido de invitado: token largo y aleatorio (mismo formato que el backend). */
object OrderTrackingLink {
    private val token = Regex("[A-Za-z0-9_-]{43}")

    fun isValidToken(value: String): Boolean = token.matches(value)
}
