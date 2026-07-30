package com.vaiinilla.app.domain.auth.student

data class StudentAuthSession(
    val uid: String,
    val email: String,
    val displayName: String,
    val emailVerified: Boolean,
)
