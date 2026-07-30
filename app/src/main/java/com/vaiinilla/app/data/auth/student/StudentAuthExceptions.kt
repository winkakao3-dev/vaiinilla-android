package com.vaiinilla.app.data.auth.student

class StudentAuthEmailExistsException(
    message: String = "Ya existe una cuenta con este correo.",
) : IllegalStateException(message)

class StudentEnrollmentUnavailableException(
    message: String = "Alta de cliente no disponible en el servidor. Dependencia backend pendiente.",
) : IllegalStateException(message)
