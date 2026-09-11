package com.vaiinilla.app.data.auth.student

import com.google.firebase.auth.FirebaseAuthException

fun firebaseAuthUserMessage(error: Throwable): String {
    val code = (error as? FirebaseAuthException)?.errorCode.orEmpty()
    return when (code) {
        "ERROR_EMAIL_ALREADY_IN_USE" ->
            "Ya existe una cuenta con este correo. Inicia sesión."
        "ERROR_INVALID_EMAIL" ->
            "El correo no es válido."
        "ERROR_WEAK_PASSWORD" ->
            "La contraseña no cumple los requisitos."
        "ERROR_TOO_MANY_REQUESTS" ->
            "Se hicieron demasiados intentos. Espera antes de intentarlo nuevamente."
        "ERROR_NETWORK_REQUEST_FAILED" ->
            "No fue posible conectarse. Revisa tu conexión."
        "ERROR_INVALID_VERIFICATION_CODE" ->
            "El código de tu aplicación autenticadora no es válido. Revisa el código actual e inténtalo de nuevo."
        "ERROR_CODE_EXPIRED" ->
            "El código expiró. Abre tu aplicación autenticadora y usa el código actual."
        "ERROR_INVALID_MFA_PENDING_CREDENTIAL",
        "ERROR_INVALID_MFA_SESSION",
        ->
            "El desafío de autenticación expiró. Inicia el proceso nuevamente."
        "ERROR_INVALID_CREDENTIAL",
        "ERROR_WRONG_PASSWORD",
        "ERROR_USER_NOT_FOUND",
        ->
            "Correo o contraseña incorrectos."
        else ->
            error.message?.takeIf { it.isNotBlank() } ?: "No se pudo completar la autenticación."
    }
}
