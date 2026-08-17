package com.vaiinilla.app.data.auth.student

class StudentAuthEmailExistsException(
    message: String = "Ya existe una cuenta con este correo. Inicia sesión.",
) : IllegalStateException(message)

class StudentAuthUserNotFoundException : IllegalStateException()

class StudentAuthProviderNotSupportedException(
    message: String = "Este método de acceso todavía no puede reautenticar la cuenta.",
) : IllegalStateException(message)

class StudentEnrollmentUnavailableException(
    message: String = "Alta de cliente no disponible en el servidor. Dependencia backend pendiente.",
) : IllegalStateException(message)
