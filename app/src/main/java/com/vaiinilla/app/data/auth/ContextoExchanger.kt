package com.vaiinilla.app.data.auth

fun interface ContextoExchanger {
    fun exchange(
        firebaseIdToken: String,
        membresiaId: String,
    ): SesionesContextoDataDto
}
