package com.vaiinilla.app.data.auth.student

import com.vaiinilla.app.domain.auth.student.StudentAuthMfaChallenge

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

class StudentAuthMfaRequiredException(
    val challenge: StudentAuthMfaChallenge,
) : IllegalStateException("Se requiere un segundo factor de autenticación.")

class StudentAuthMfaUnavailableException(
    message: String = "Esta cuenta requiere un método de autenticación que Vaiinilla todavía no soporta.",
) : IllegalStateException(message)

class StudentAuthMfaChallengeExpiredException(
    message: String = "El desafío de autenticación expiró. Inicia el proceso nuevamente.",
) : IllegalStateException(message)
