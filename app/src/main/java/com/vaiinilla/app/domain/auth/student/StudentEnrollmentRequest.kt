package com.vaiinilla.app.domain.auth.student

data class StudentEnrollmentRequest(
    val nombre: String,
    val terminosVersion: String,
    val privacidadVersion: String,
)
