package com.vaiinilla.app.domain.auth.student

enum class StudentAuthMfaOperation {
    LOGIN,
    REAUTHENTICATION,
}

data class StudentAuthMfaFactor(
    val uid: String,
    val displayName: String?,
)

data class StudentAuthMfaChallenge(
    val id: String,
    val operation: StudentAuthMfaOperation,
    val factors: List<StudentAuthMfaFactor>,
)

data class StudentAuthMfaResolution(
    val operation: StudentAuthMfaOperation,
    val session: StudentAuthSession?,
)
