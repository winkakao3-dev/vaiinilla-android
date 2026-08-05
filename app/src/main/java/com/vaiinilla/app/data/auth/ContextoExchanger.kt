package com.vaiinilla.app.data.auth

fun interface ContextoExchanger {
    suspend fun exchange(
        firebaseIdToken: String,
        establecimientoSlug: String,
        establecimientoId: String,
        identificadorCliente: String?,
    ): SesionesContextoDataDto
}
