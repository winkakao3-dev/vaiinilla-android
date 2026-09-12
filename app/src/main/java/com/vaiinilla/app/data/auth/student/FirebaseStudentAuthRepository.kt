package com.vaiinilla.app.data.auth.student

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthMultiFactorException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.MultiFactorResolver
import com.google.firebase.auth.TotpMultiFactorGenerator
import com.google.firebase.auth.UserProfileChangeRequest
import com.vaiinilla.app.core.security.SecureSessionStore
import com.vaiinilla.app.domain.auth.student.StudentAuthMfaChallenge
import com.vaiinilla.app.domain.auth.student.StudentAuthMfaFactor
import com.vaiinilla.app.domain.auth.student.StudentAuthMfaOperation
import com.vaiinilla.app.domain.auth.student.StudentAuthMfaResolution
import com.vaiinilla.app.domain.auth.student.StudentAuthRepository
import com.vaiinilla.app.domain.auth.student.StudentAuthSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseStudentAuthRepository
    @Inject
    constructor(
        private val sessionStore: SecureSessionStore,
        private val preferences: StudentAuthPreferences,
    ) : StudentAuthRepository {
        private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
        private val pendingMfa = ConcurrentHashMap<String, PendingMfa>()

        override fun peekSession(): StudentAuthSession? = auth.currentUser?.toSession()

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
            withContext(Dispatchers.IO) {
                runCatching {
                    preferences.clear()
                    val result =
                        auth.createUserWithEmailAndPassword(email.trim().lowercase(), password).await()
                    val user =
                        result.user
                            ?: throw IllegalStateException("No se pudo crear la cuenta.")
                    sessionStore.clear()
                    user
                        .updateProfile(
                            UserProfileChangeRequest
                                .Builder()
                                .setDisplayName(displayName.trim())
                                .build(),
                        ).await()
                    user.toSession()
                }.recoverCatching { error ->
                    if (error is StudentAuthMfaRequiredException || error is StudentAuthMfaUnavailableException) {
                        throw error
                    }
                    if (error is FirebaseAuthException && error.errorCode == "ERROR_EMAIL_ALREADY_IN_USE") {
                        throw StudentAuthEmailExistsException()
                    }
                    throw IllegalStateException(firebaseAuthUserMessage(error))
                }
            }

        override suspend fun signIn(
            email: String,
            password: String,
        ): Result<StudentAuthSession> =
            withContext(Dispatchers.IO) {
                runCatching {
                    auth.signInWithEmailAndPassword(email.trim().lowercase(), password).await()
                    val user = auth.currentUser ?: throw IllegalStateException("No se pudo iniciar sesión.")
                    user.reload().await()
                    sessionStore.clear()
                    user.toSession()
                }.recoverCatching { error ->
                    if (error is FirebaseAuthMultiFactorException) {
                        throw createMfaRequiredException(
                            resolver = error.resolver,
                            operation = StudentAuthMfaOperation.LOGIN,
                            expectedUid = null,
                        )
                    }
                    if (error is StudentAuthMfaRequiredException || error is StudentAuthMfaUnavailableException) {
                        throw error
                    }
                    throw IllegalStateException(firebaseAuthUserMessage(error))
                }
            }

        override suspend fun signInWithGoogleIdToken(idToken: String): Result<StudentAuthSession> =
            withContext(Dispatchers.IO) {
                runCatching {
                    require(idToken.isNotBlank()) { "Token de Google inválido." }
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(credential).await()
                    val user = auth.currentUser ?: throw IllegalStateException("No se pudo iniciar sesión con Google.")
                    sessionStore.clear()
                    user.toSession()
                }.recoverCatching { error ->
                    throw IllegalStateException(firebaseAuthUserMessage(error))
                }
            }

        override suspend fun sendEmailVerification(): Result<Unit> =
            withContext(Dispatchers.IO) {
                runCatching {
                    val user = auth.currentUser ?: throw IllegalStateException("No hay sesión activa.")
                    user.sendEmailVerification().await()
                    Unit
                }
            }

        override suspend fun reloadSession(): Result<StudentAuthSession?> =
            withContext(Dispatchers.IO) {
                runCatching {
                    val user = auth.currentUser ?: return@runCatching null
                    user.reload().await()
                    user.toSession()
                }
            }

        override suspend fun getIdToken(forceRefresh: Boolean): Result<String> =
            withContext(Dispatchers.IO) {
                runCatching {
                    auth.currentUser
                        ?.getIdToken(forceRefresh)
                        ?.await()
                        ?.token
                        ?.takeIf { it.isNotBlank() }
                        ?: throw IllegalStateException("No se pudo obtener el ID token de Firebase.")
                }.recoverCatching { error ->
                    if (error is FirebaseAuthException && error.errorCode == "ERROR_USER_NOT_FOUND") {
                        throw StudentAuthUserNotFoundException()
                    }
                    throw error
                }
            }

        override suspend fun reauthenticateWithPassword(password: String): Result<Unit> =
            withContext(Dispatchers.IO) {
                runCatching {
                    require(password.isNotEmpty()) { "Ingresa tu contraseña para continuar." }
                    val user = auth.currentUser ?: throw IllegalStateException("No hay una sesión activa.")
                    val email =
                        user.email?.trim()?.takeIf { it.isNotEmpty() }
                            ?: throw StudentAuthProviderNotSupportedException()
                    val hasPasswordProvider =
                        user.providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID }
                    if (!hasPasswordProvider) throw StudentAuthProviderNotSupportedException()
                    val hasEnrolledMfa = user.multiFactor.enrolledFactors.isNotEmpty()
                    if (hasEnrolledMfa) {
                        try {
                            val result = auth.signInWithEmailAndPassword(email, password).await()
                            val signedInUser =
                                result.user
                                    ?: auth.currentUser
                                    ?: throw IllegalStateException("No se pudo reautenticar la cuenta.")
                            if (signedInUser.uid != user.uid) {
                                auth.signOut()
                                throw IllegalStateException("La cuenta reautenticada no coincide con la sesión actual.")
                            }
                            signedInUser.reload().await()
                        } catch (error: FirebaseAuthMultiFactorException) {
                            throw createMfaRequiredException(
                                resolver = error.resolver,
                                operation = StudentAuthMfaOperation.REAUTHENTICATION,
                                expectedUid = user.uid,
                            )
                        }
                    } else {
                        user.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()
                    }
                    Unit
                }.recoverCatching { error ->
                    if (
                        error is StudentAuthMfaRequiredException ||
                        error is StudentAuthMfaUnavailableException ||
                        error is StudentAuthMfaChallengeExpiredException ||
                        error is StudentAuthProviderNotSupportedException
                    ) {
                        throw error
                    }
                    if (error is FirebaseAuthException && error.errorCode == "ERROR_USER_NOT_FOUND") {
                        throw StudentAuthUserNotFoundException()
                    }
                    throw IllegalStateException(firebaseAuthUserMessage(error))
                }
            }

        override suspend fun resolveMfa(
            challengeId: String,
            factorUid: String,
            code: String,
        ): Result<StudentAuthMfaResolution> =
            withContext(Dispatchers.IO) {
                val pending =
                    pendingMfa[challengeId]
                        ?: return@withContext Result.failure(StudentAuthMfaChallengeExpiredException())
                runCatching {
                    val normalizedCode = code.trim()
                    require(normalizedCode.length == 6 && normalizedCode.all(Char::isDigit)) {
                        "El código debe tener 6 dígitos."
                    }
                    require(pending.challenge.factors.any { it.uid == factorUid }) {
                        "El método de autenticación seleccionado no está disponible."
                    }
                    val assertion =
                        TotpMultiFactorGenerator.getAssertionForSignIn(
                            factorUid,
                            normalizedCode,
                        )
                    val result = pending.resolver.resolveSignIn(assertion).await()
                    val user =
                        result.user
                            ?: auth.currentUser
                            ?: throw IllegalStateException("No se pudo completar la autenticación.")
                    pending.expectedUid?.let { expectedUid ->
                        if (user.uid != expectedUid) {
                            auth.signOut()
                            throw IllegalStateException("La cuenta reautenticada no coincide con la sesión actual.")
                        }
                    }
                    user.reload().await()
                    pendingMfa.remove(challengeId)
                    if (pending.challenge.operation == StudentAuthMfaOperation.LOGIN) {
                        sessionStore.clear()
                    }
                    StudentAuthMfaResolution(
                        operation = pending.challenge.operation,
                        session = user.toSession(),
                    )
                }.recoverCatching { error ->
                    if (error is FirebaseAuthException && error.isMfaChallengeExpired()) {
                        pendingMfa.remove(challengeId)
                        throw StudentAuthMfaChallengeExpiredException()
                    }
                    if (error is StudentAuthMfaChallengeExpiredException) throw error
                    throw IllegalStateException(firebaseAuthUserMessage(error))
                }
            }

        override fun cancelMfa(challengeId: String) {
            pendingMfa.remove(challengeId)
        }

        override suspend fun signOut() {
            withContext(Dispatchers.IO) {
                pendingMfa.clear()
                auth.signOut()
                preferences.clear()
                sessionStore.clear()
            }
        }

        private fun createMfaRequiredException(
            resolver: MultiFactorResolver,
            operation: StudentAuthMfaOperation,
            expectedUid: String?,
        ): StudentAuthMfaRequiredException {
            val factors =
                resolver.hints
                    .filter { it.factorId == TotpMultiFactorGenerator.FACTOR_ID }
                    .map { hint -> StudentAuthMfaFactor(uid = hint.uid, displayName = hint.displayName) }
            if (factors.isEmpty()) throw StudentAuthMfaUnavailableException()
            val challenge =
                StudentAuthMfaChallenge(
                    id = UUID.randomUUID().toString(),
                    operation = operation,
                    factors = factors,
                )
            pendingMfa[challenge.id] = PendingMfa(challenge, resolver, expectedUid)
            return StudentAuthMfaRequiredException(challenge)
        }

        private fun FirebaseAuthException.isMfaChallengeExpired(): Boolean =
            errorCode in
                setOf(
                    "ERROR_CODE_EXPIRED",
                    "ERROR_INVALID_MFA_PENDING_CREDENTIAL",
                    "ERROR_INVALID_MFA_SESSION",
                )

        private data class PendingMfa(
            val challenge: StudentAuthMfaChallenge,
            val resolver: MultiFactorResolver,
            val expectedUid: String?,
        )

        private fun com.google.firebase.auth.FirebaseUser.toSession(): StudentAuthSession =
            StudentAuthSession(
                uid = uid,
                email = email.orEmpty(),
                displayName = displayName.orEmpty().ifBlank { email.orEmpty() },
                emailVerified = isEmailVerified,
            )
    }
