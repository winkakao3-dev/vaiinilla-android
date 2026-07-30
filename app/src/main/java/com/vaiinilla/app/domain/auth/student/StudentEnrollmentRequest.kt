package com.vaiinilla.app.domain.auth.student

import java.time.Instant

data class StudentEnrollmentRequest(
    val establecimientoId: String,
    val nombre: String,
    val identificadorContextual: String?,
    val aceptacionTerminosEn: Instant,
)
