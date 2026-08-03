package com.vaiinilla.app.domain.auth.student

interface StudentAuthRepository {
    fun peekSession(): StudentAuthSession?

    /** Ready when verified + enrolled for [establishmentId] (or any venue if null). */
    fun isReadyForCheckout(establishmentId: String? = null): Boolean

    suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
    ): Result<StudentAuthSession>

    suspend fun signIn(
        email: String,
        password: String,
    ): Result<StudentAuthSession>

    suspend fun sendEmailVerification(): Result<Unit>

    suspend fun reloadSession(): Result<StudentAuthSession?>

    suspend fun sendPasswordReset(email: String): Result<Unit>

    suspend fun getIdToken(forceRefresh: Boolean = false): Result<String>

    suspend fun signOut()
}
