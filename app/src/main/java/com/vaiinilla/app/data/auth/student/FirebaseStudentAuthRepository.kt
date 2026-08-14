package com.vaiinilla.app.data.auth.student

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserProfileChangeRequest
import com.vaiinilla.app.core.security.SecureSessionStore
import com.vaiinilla.app.domain.auth.student.StudentAuthRepository
import com.vaiinilla.app.domain.auth.student.StudentAuthSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
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

        override fun peekSession(): StudentAuthSession? = auth.currentUser?.toSession()

        override fun isReadyForCheckout(establishmentId: String?): Boolean {
            val session = peekSession() ?: return false
            return session.emailVerified
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
                    throw IllegalStateException(firebaseAuthUserMessage(error))
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
                }
            }

        override suspend fun signOut() {
            withContext(Dispatchers.IO) {
                auth.signOut()
                preferences.clear()
                sessionStore.clear()
            }
        }

        private fun com.google.firebase.auth.FirebaseUser.toSession(): StudentAuthSession =
            StudentAuthSession(
                uid = uid,
                email = email.orEmpty(),
                displayName = displayName.orEmpty().ifBlank { email.orEmpty() },
                emailVerified = isEmailVerified,
            )
    }
