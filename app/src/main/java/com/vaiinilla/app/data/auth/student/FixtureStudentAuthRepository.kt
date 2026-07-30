package com.vaiinilla.app.data.auth.student

import com.vaiinilla.app.core.security.SecureSessionStore
import com.vaiinilla.app.domain.auth.student.StudentAuthRepository
import com.vaiinilla.app.domain.auth.student.StudentAuthSession
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FixtureStudentAuthRepository
    @Inject
    constructor(
        private val sessionStore: SecureSessionStore,
        private val preferences: StudentAuthPreferences,
    ) : StudentAuthRepository {
        private val accounts = ConcurrentHashMap<String, FixtureAccount>()
        private var currentUid: String? = null

        override fun peekSession(): StudentAuthSession? {
            val uid = currentUid ?: return null
            return accounts[uid]?.toSession()
        }

        override fun isReadyForCheckout(establishmentId: String?): Boolean {
            val session = peekSession() ?: return false
            return session.emailVerified &&
                preferences.isEnrolledFor(establishmentId) &&
                !sessionStore.readAccessToken().isNullOrBlank()
        }

        override suspend fun signUp(
            email: String,
            password: String,
            displayName: String,
        ): Result<StudentAuthSession> =
            runCatching {
                val normalized = email.trim().lowercase()
                if (accounts.values.any { it.email == normalized }) {
                    throw StudentAuthEmailExistsException()
                }
                val uid = UUID.randomUUID().toString()
                val account =
                    FixtureAccount(
                        uid = uid,
                        email = normalized,
                        password = password,
                        displayName = displayName.trim(),
                        emailVerified = false,
                    )
                accounts[uid] = account
                currentUid = uid
                preferences.clear()
                account.toSession()
            }

        override suspend fun signIn(
            email: String,
            password: String,
        ): Result<StudentAuthSession> =
            runCatching {
                val normalized = email.trim().lowercase()
                val account =
                    accounts.values.firstOrNull { it.email == normalized && it.password == password }
                        ?: throw IllegalStateException("Correo o contraseña incorrectos.")
                currentUid = account.uid
                account.toSession()
            }

        override suspend fun sendEmailVerification(): Result<Unit> =
            runCatching {
                val uid = currentUid ?: throw IllegalStateException("No hay sesión activa.")
                val account = accounts[uid] ?: throw IllegalStateException("No hay sesión activa.")
                accounts[uid] = account.copy(verificationSent = true)
            }

        override suspend fun reloadSession(): Result<StudentAuthSession?> =
            runCatching {
                val uid = currentUid ?: return@runCatching null
                accounts[uid]?.toSession()
            }

        /** MOCK helper: marks the current account as verified (simulates clicking email link). */
        fun markCurrentEmailVerified() {
            val uid = currentUid ?: return
            val account = accounts[uid] ?: return
            accounts[uid] = account.copy(emailVerified = true)
        }

        override suspend fun sendPasswordReset(email: String): Result<Unit> =
            runCatching {
                val normalized = email.trim().lowercase()
                if (accounts.values.none { it.email == normalized }) {
                    throw IllegalStateException("No encontramos una cuenta con ese correo.")
                }
            }

        override suspend fun getIdToken(forceRefresh: Boolean): Result<String> =
            runCatching {
                val uid = currentUid ?: throw IllegalStateException("No hay sesión activa.")
                "mock-firebase-token-$uid"
            }

        override suspend fun signOut() {
            currentUid = null
            preferences.clear()
            sessionStore.clear()
        }

        fun completeMockEnrollment(
            accessToken: String,
            establishmentId: String,
        ) {
            sessionStore.saveAccessToken(accessToken)
            preferences.markEnrolled(establishmentId)
        }

        private data class FixtureAccount(
            val uid: String,
            val email: String,
            val password: String,
            val displayName: String,
            val emailVerified: Boolean,
            val verificationSent: Boolean = false,
        ) {
            fun toSession(): StudentAuthSession =
                StudentAuthSession(
                    uid = uid,
                    email = email,
                    displayName = displayName,
                    emailVerified = emailVerified,
                )
        }
    }
