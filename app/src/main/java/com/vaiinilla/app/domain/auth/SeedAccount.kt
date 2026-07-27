package com.vaiinilla.app.domain.auth

import com.vaiinilla.app.domain.model.OperationalRole

data class SeedAccount(
    val role: OperationalRole,
    val email: String,
    val membresiaId: String,
    val password: String,
)
