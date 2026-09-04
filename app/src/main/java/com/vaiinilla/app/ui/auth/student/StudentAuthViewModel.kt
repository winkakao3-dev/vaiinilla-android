package com.vaiinilla.app.ui.auth.student

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.auth.StudentSessionCleanup
import com.vaiinilla.app.core.auth.VaiinillaJwtRefreshCoordinator
import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.core.security.SecureSessionStore
import com.vaiinilla.app.data.auth.ContextoExchanger
import com.vaiinilla.app.data.auth.student.AccessEmailApi
import com.vaiinilla.app.data.auth.student.StudentAuthEmailExistsException
import com.vaiinilla.app.data.auth.student.StudentAuthPreferences
import com.vaiinilla.app.data.guest.GuestSessionStore
import com.vaiinilla.app.domain.auth.student.StudentAuthRepository
import com.vaiinilla.app.domain.auth.student.StudentEnrollmentRepository
import com.vaiinilla.app.domain.auth.student.StudentEnrollmentRequest
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.repository.AuthorizedAccessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

private class MissingClientIdentifierException(
    label: String,
) : IllegalStateException("Ingresa tu ${label.lowercase()} para continuar.")

@HiltViewModel
class StudentAuthViewModel
    @Inject
    constructor(
        private val authRepository: StudentAuthRepository,
        private val enrollmentRepository: StudentEnrollmentRepository,
        private val authorizedAccessRepository: AuthorizedAccessRepository,
        private val guestSessionStore: GuestSessionStore,
        private val sessionStore: SecureSessionStore,
        private val contextoExchange: ContextoExchanger,
        private val refreshCoordinator: VaiinillaJwtRefreshCoordinator,
        private val preferences: StudentAuthPreferences,
        private val remoteAccessEmailApi: AccessEmailApi,
        private val sessionCleanup: StudentSessionCleanup,
    ) : ViewModel() {
        private val _state = mutableStateOf(StudentAuthUiState())
        val state: State<StudentAuthUiState> = _state
        private val activeJobs = mutableSetOf<Job>()
        private var contextBootstrapJob: Job? = null

        init {
            refreshGuestVenue()
            restoreRemoteContextIfNeeded()
            loadLegalDocuments()
        }

        fun refreshGuestVenue() {
            val venue = guestSessionStore.readVenue()
            val current = _state.value
            val venueChanged = current.guestVenue?.establishment?.id != venue?.establishment?.id
            _state.value =
                current.copy(
                    guestVenue = venue,
                    clientIdLabel = venue?.establishment?.clientIdLabel ?: "Identificador",
                    clientIdRequired = venue?.establishment?.clientIdRequired == true,
                    contextualId = if (venueChanged) "" else current.contextualId,
                    enrollmentComplete =
                        authRepository.isReadyForCheckout(venue?.establishment?.id),
                    session = authRepository.peekSession(),
                )
        }

        fun signOut(onDone: () -> Unit = {}) {
            cancelActiveJobs()
            launchTracked {
                withContext(Dispatchers.IO) { sessionCleanup.clear() }
                _state.value = StudentAuthUiState()
                refreshGuestVenue()
                onDone()
            }
        }

        fun markSessionCleared(noticeMessage: String? = null) {
            cancelActiveJobs()
            _state.value = StudentAuthUiState(noticeMessage = noticeMessage)
            refreshGuestVenue()
        }

        /** Rehydrates the short-lived Railway client context after process restart. */
        private fun restoreRemoteContextIfNeeded() {
            val session = authRepository.peekSession() ?: return
            if (!session.emailVerified) return
            val venue = guestSessionStore.readVenue() ?: return
            if (!preferences.isEnrolledFor(venue.establishment.id)) return

            // A persisted JWT cannot refresh after a process restart until this coordinator
            // is rebuilt. Never let checkout trust a token without its refresh callback.
            sessionStore.clear()
            refreshCoordinator.clearSession()
            _state.value = _state.value.copy(enrollmentComplete = false)

            contextBootstrapJob?.cancel()
            contextBootstrapJob =
                launchTracked {
                    val result =
                        withContext(Dispatchers.IO) {
                            runCatching {
                                val clientIdLabel = venue.establishment.clientIdLabel.lowercase()
                                val clientIdentifier =
                                    if (venue.establishment.clientIdRequired) {
                                        authorizedAccessRepository
                                            .authorizedModes(session)
                                            .getOrThrow()
                                            .firstOrNull { mode ->
                                                mode.role == OperationalRole.CLIENT &&
                                                    mode.establishmentId == venue.establishment.id
                                            }?.clientIdentifier
                                            ?.trim()
                                            ?.takeIf { it.isNotBlank() }
                                            ?: throw IllegalStateException(
                                                "No encontramos tu $clientIdLabel vinculada. " +
                                                    "Complétala para continuar.",
                                            )
                                    } else {
                                        null
                                    }
                                val firebaseToken = authRepository.getIdToken(forceRefresh = true).getOrThrow()
                                val contexto =
                                    contextoExchange.exchange(
                                        firebaseIdToken = firebaseToken,
                                        establecimientoSlug = venue.establishment.slug,
                                        establecimientoId = venue.establishment.id,
                                        identificadorCliente = clientIdentifier,
                                    )
                                sessionStore.saveAccessToken(contexto.accessToken)
                                refreshCoordinator.startSession(
                                    OperationalRole.CLIENT,
                                    contexto.expiresIn,
                                    refresh = {
                                        kotlinx.coroutines.runBlocking {
                                            runCatching {
                                                val refreshedFirebaseToken =
                                                    authRepository.getIdToken(forceRefresh = true).getOrThrow()
                                                val refreshedContext =
                                                    contextoExchange.exchange(
                                                        firebaseIdToken = refreshedFirebaseToken,
                                                        establecimientoSlug = venue.establishment.slug,
                                                        establecimientoId = venue.establishment.id,
                                                        identificadorCliente = clientIdentifier,
                                                    )
                                                sessionStore.saveAccessToken(refreshedContext.accessToken)
                                            }
                                        }
                                    },
                                )
                                contexto
                            }
                        }
                    result.fold(
                        onSuccess = {
                            _state.value =
                                _state.value.copy(
                                    enrollmentComplete = true,
                                    errorMessage = null,
                                )
                        },
                        onFailure = { error ->
                            _state.value =
                                _state.value.copy(
                                    enrollmentComplete = false,
                                    errorMessage = error.toUserFacingMessage(),
                                )
                        },
                    )
                }
        }

        fun updateName(value: String) {
            _state.value = _state.value.copy(name = value, errorMessage = null)
        }

        fun updateEmail(value: String) {
            _state.value =
                _state.value.copy(
                    email = value,
                    errorMessage = null,
                    emailExistsSuggestion = false,
                )
        }

        fun updatePassword(value: String) {
            _state.value = _state.value.copy(password = value, errorMessage = null)
        }

        fun updatePasswordConfirm(value: String) {
            _state.value = _state.value.copy(passwordConfirm = value, errorMessage = null)
        }

        fun updateContextualId(value: String) {
            _state.value = _state.value.copy(contextualId = value, errorMessage = null)
        }

        fun updateTermsAccepted(accepted: Boolean) {
            _state.value =
                _state.value.copy(
                    termsAccepted = accepted,
                    termsAcceptedAt = if (accepted) Instant.now() else null,
                    errorMessage = null,
                )
        }

        fun updatePrivacyAccepted(accepted: Boolean) {
            _state.value = _state.value.copy(privacyAccepted = accepted, errorMessage = null)
        }

        fun clearError() {
            _state.value = _state.value.copy(errorMessage = null)
        }

        fun register(onSuccess: () -> Unit) {
            val current = _state.value
            if (current.loading) return
            validateRegistration(current)?.let { error ->
                _state.value = current.copy(errorMessage = error)
                return
            }
            _state.value = current.copy(loading = true, errorMessage = null, emailExistsSuggestion = false)
            launchTracked {
                val email = current.email.trim().lowercase()
                _state.value = _state.value.copy(email = email)
                authRepository.signUp(email, current.password, current.name).fold(
                    onSuccess = { session ->
                        val verificationResult = sendVerificationEmail()
                        _state.value =
                            _state.value.copy(
                                loading = false,
                                session = session,
                                verificationSent = verificationResult.isSuccess,
                                errorMessage = verificationResult.exceptionOrNull()?.toUserFacingMessage(),
                            )
                        onSuccess()
                    },
                    onFailure = { error ->
                        val emailExists = error is StudentAuthEmailExistsException
                        _state.value =
                            _state.value.copy(
                                loading = false,
                                errorMessage = error.toUserFacingMessage(),
                                emailExistsSuggestion = emailExists,
                            )
                    },
                )
            }
        }

        fun login(onSuccess: (Boolean) -> Unit) {
            signIn(bootstrapClientContext = true, onSuccess = onSuccess)
        }

        /**
         * Authenticates the Firebase identity without assuming that it is a client account.
         * Launch login uses this path so staff roles can be resolved from `sesiones/accesos`
         * before any client enrollment/context bootstrap is attempted.
         */
        fun loginIdentity(onSuccess: (Boolean) -> Unit) {
            signIn(bootstrapClientContext = false, onSuccess = onSuccess)
        }

        private fun signIn(
            bootstrapClientContext: Boolean,
            onSuccess: (Boolean) -> Unit,
        ) {
            val current = _state.value
            if (current.email.isBlank() || current.password.isBlank()) {
                _state.value = current.copy(errorMessage = "Ingresa correo y contraseña.")
                return
            }
            if (
                bootstrapClientContext &&
                current.clientIdRequired &&
                current.contextualId.isBlank()
            ) {
                _state.value =
                    current.copy(
                        errorMessage = "Ingresa tu ${current.clientIdLabel.lowercase()}.",
                    )
                return
            }
            _state.value = current.copy(loading = true, errorMessage = null)
            launchTracked {
                val email = current.email.trim().lowercase()
                _state.value = _state.value.copy(email = email)
                authRepository.signIn(email, current.password).fold(
                    onSuccess = { session ->
                        _state.value =
                            _state.value.copy(
                                loading = false,
                                session = session,
                            )
                        if (!session.emailVerified) {
                            onSuccess(false)
                        } else if (bootstrapClientContext) {
                            completeEnrollment(
                                onSuccess = { onSuccess(true) },
                                onNeedsVerify = { onSuccess(false) },
                            )
                        } else {
                            onSuccess(true)
                        }
                    },
                    onFailure = { error ->
                        _state.value =
                            _state.value.copy(
                                loading = false,
                                errorMessage = error.toUserFacingMessage(),
                            )
                    },
                )
            }
        }

        fun resendVerification() {
            val current = _state.value
            if (current.loading) return
            if (current.resendLockedUntilMs > System.currentTimeMillis()) return
            _state.value = current.copy(loading = true, errorMessage = null)
            launchTracked {
                sendVerificationEmail().fold(
                    onSuccess = {
                        _state.value =
                            _state.value.copy(
                                loading = false,
                                verificationSent = true,
                            )
                        lockResend(60, errorMessage = null)
                    },
                    onFailure = { error ->
                        val limited = error as? ApiClientException
                        val rateLimited =
                            limited != null &&
                                (
                                    limited.httpStatus == 429 ||
                                        limited.code.equals("RATE_LIMITED", ignoreCase = true)
                                )
                        if (rateLimited) {
                            lockResend(limited.retryAfterSeconds ?: 60)
                        } else {
                            _state.value =
                                _state.value.copy(
                                    loading = false,
                                    errorMessage = error.toUserFacingMessage(),
                                )
                        }
                    },
                )
            }
        }

        fun checkVerification(onVerified: () -> Unit) {
            if (_state.value.loading) return
            _state.value = _state.value.copy(loading = true, errorMessage = null)
            launchTracked {
                authRepository.reloadSession().fold(
                    onSuccess = { session ->
                        authRepository.getIdToken(forceRefresh = true).fold(
                            onSuccess = {
                                val verified = authRepository.peekSession()
                                _state.value = _state.value.copy(loading = false, session = verified ?: session)
                                if (verified?.emailVerified == true) {
                                    completeEnrollment(
                                        onSuccess = onVerified,
                                        onNeedsVerify = {},
                                    )
                                } else {
                                    _state.value =
                                        _state.value.copy(
                                            errorMessage = "Aún no hemos confirmado tu correo. Revisa tu bandeja.",
                                        )
                                }
                            },
                            onFailure = { error ->
                                _state.value =
                                    _state.value.copy(
                                        loading = false,
                                        session = session,
                                        errorMessage = error.toUserFacingMessage(),
                                    )
                            },
                        )
                    },
                    onFailure = { error ->
                        _state.value =
                            _state.value.copy(
                                loading = false,
                                errorMessage = error.toUserFacingMessage(),
                            )
                    },
                )
            }
        }

        fun sendPasswordReset(onSent: () -> Unit = {}) {
            val email = _state.value.email.trim()
            if (email.isBlank()) {
                _state.value = _state.value.copy(errorMessage = "Ingresa tu correo.")
                return
            }
            if (_state.value.resendLockedUntilMs > System.currentTimeMillis()) return
            _state.value = _state.value.copy(loading = true, errorMessage = null)
            launchTracked {
                sendPasswordResetEmail(email).fold(
                    onSuccess = {
                        _state.value =
                            _state.value.copy(
                                loading = false,
                                passwordResetSent = true,
                            )
                        lockResend(60, errorMessage = null)
                        onSent()
                    },
                    onFailure = { error ->
                        val limited = error as? ApiClientException
                        val rateLimited =
                            limited != null &&
                                (
                                    limited.httpStatus == 429 ||
                                        limited.code.equals("RATE_LIMITED", ignoreCase = true)
                                )
                        if (rateLimited) {
                            lockResend(limited.retryAfterSeconds ?: 60)
                        } else {
                            _state.value =
                                _state.value.copy(
                                    loading = false,
                                    errorMessage = error.toUserFacingMessage(),
                                )
                        }
                    },
                )
            }
        }

        fun completeEnrollment(
            onSuccess: () -> Unit,
            onNeedsVerify: () -> Unit = {},
            onNeedsContextualId: () -> Unit = {},
        ) {
            val current = _state.value
            val session = authRepository.peekSession()
            if (session == null) {
                _state.value = current.copy(errorMessage = "No hay sesión activa.")
                return
            }
            if (!session.emailVerified) {
                onNeedsVerify()
                return
            }
            val venue = current.guestVenue ?: guestSessionStore.readVenue()
            if (venue == null) {
                enrollIdentityOnly(current, session, onSuccess)
                return
            }
            _state.value = current.copy(loading = true, errorMessage = null)
            contextBootstrapJob?.cancel()
            contextBootstrapJob =
                launchTracked {
                    val result =
                        withContext(Dispatchers.IO) {
                            runCatching {
                                val firebaseToken =
                                    authRepository.getIdToken(forceRefresh = true).getOrThrow()
                                enrollmentRepository
                                    .enroll(
                                        StudentEnrollmentRequest(
                                            nombre = session.displayName.ifBlank { current.name },
                                            terminosVersion = current.termsVersion,
                                            privacidadVersion = current.privacyVersion,
                                        ),
                                        firebaseIdToken = firebaseToken,
                                    ).getOrThrow()
                                val explicitClientIdentifier = current.contextualId.trim().ifBlank { null }
                                val linkedClientIdentifier =
                                    if (venue.establishment.clientIdRequired && explicitClientIdentifier == null) {
                                        authorizedAccessRepository
                                            .authorizedModes(session)
                                            .getOrThrow()
                                            .firstOrNull { mode ->
                                                mode.role == OperationalRole.CLIENT &&
                                                    mode.establishmentId == venue.establishment.id
                                            }?.clientIdentifier
                                            ?.trim()
                                            ?.takeIf { it.isNotBlank() }
                                    } else {
                                        null
                                    }
                                val clientIdentifier = explicitClientIdentifier ?: linkedClientIdentifier
                                if (venue.establishment.clientIdRequired && clientIdentifier == null) {
                                    throw MissingClientIdentifierException(venue.establishment.clientIdLabel)
                                }
                                val contexto =
                                    contextoExchange.exchange(
                                        firebaseIdToken = firebaseToken,
                                        establecimientoSlug = venue.establishment.slug,
                                        establecimientoId = venue.establishment.id,
                                        identificadorCliente = clientIdentifier,
                                    )
                                sessionStore.saveAccessToken(contexto.accessToken)
                                refreshCoordinator.startSession(
                                    OperationalRole.CLIENT,
                                    contexto.expiresIn,
                                    refresh = {
                                        kotlinx.coroutines.runBlocking {
                                            runCatching {
                                                val refreshedFirebaseToken =
                                                    authRepository.getIdToken(forceRefresh = true).getOrThrow()
                                                val refreshedContext =
                                                    contextoExchange.exchange(
                                                        firebaseIdToken = refreshedFirebaseToken,
                                                        establecimientoSlug = venue.establishment.slug,
                                                        establecimientoId = venue.establishment.id,
                                                        identificadorCliente = clientIdentifier,
                                                    )
                                                sessionStore.saveAccessToken(refreshedContext.accessToken)
                                            }
                                        }
                                    },
                                )
                                preferences.markEnrolled(venue.establishment.id)
                                contexto
                            }
                        }
                    result.fold(
                        onSuccess = {
                            _state.value =
                                _state.value.copy(
                                    loading = false,
                                    enrollmentComplete = true,
                                    errorMessage = null,
                                )
                            onSuccess()
                        },
                        onFailure = { error ->
                            if (error is MissingClientIdentifierException) {
                                _state.value =
                                    _state.value.copy(
                                        loading = false,
                                        errorMessage = error.message,
                                    )
                                onNeedsContextualId()
                            } else if (error is ApiClientException &&
                                error.code.equals("EMAIL_NOT_VERIFIED", ignoreCase = true)
                            ) {
                                onNeedsVerify()
                                _state.value =
                                    _state.value.copy(
                                        loading = false,
                                        errorMessage = "Vuelve a verificar tu correo.",
                                    )
                            } else {
                                _state.value =
                                    _state.value.copy(
                                        loading = false,
                                        errorMessage = error.toUserFacingMessage(),
                                    )
                            }
                        },
                    )
                }
        }

        private suspend fun sendVerificationEmail(): Result<Unit> =
            authRepository.getIdToken(forceRefresh = true).fold(
                onSuccess = { firebaseIdToken ->
                    remoteAccessEmailApi.sendVerification(firebaseIdToken)
                },
                onFailure = { Result.failure(it) },
            )

        private suspend fun sendPasswordResetEmail(email: String): Result<Unit> =
            remoteAccessEmailApi.sendRecovery(email)

        fun ensureVenueContext(
            onReady: () -> Unit,
            onNeedsAuth: () -> Unit,
        ) {
            refreshGuestVenue()
            val session = authRepository.peekSession()
            val venue = _state.value.guestVenue
            if (session == null || !session.emailVerified || venue == null) {
                onNeedsAuth()
                return
            }
            if (authRepository.isReadyForCheckout(venue.establishment.id)) {
                onReady()
                return
            }
            completeEnrollment(
                onSuccess = onReady,
                onNeedsVerify = onNeedsAuth,
                onNeedsContextualId = onNeedsAuth,
            )
        }

        fun isReadyForCheckout(): Boolean {
            val venue = _state.value.guestVenue ?: guestSessionStore.readVenue()
            return authRepository.isReadyForCheckout(venue?.establishment?.id)
        }

        private fun enrollIdentityOnly(
            state: StudentAuthUiState,
            session: com.vaiinilla.app.domain.auth.student.StudentAuthSession,
            onSuccess: () -> Unit,
        ) {
            _state.value = state.copy(loading = true, errorMessage = null)
            launchTracked {
                val result =
                    withContext(Dispatchers.IO) {
                        runCatching {
                            val firebaseToken = authRepository.getIdToken(forceRefresh = true).getOrThrow()
                            enrollmentRepository
                                .enroll(
                                    StudentEnrollmentRequest(
                                        nombre = session.displayName.ifBlank { state.name },
                                        terminosVersion = state.termsVersion,
                                        privacidadVersion = state.privacyVersion,
                                    ),
                                    firebaseIdToken = firebaseToken,
                                ).getOrThrow()
                        }
                    }
                result.fold(
                    onSuccess = {
                        _state.value =
                            _state.value.copy(
                                loading = false,
                                session = session,
                                identityEnrollmentComplete = true,
                                errorMessage = null,
                            )
                        onSuccess()
                    },
                    onFailure = { error ->
                        _state.value =
                            _state.value.copy(
                                loading = false,
                                errorMessage = error.toUserFacingMessage(),
                            )
                    },
                )
            }
        }

        private fun validateRegistration(state: StudentAuthUiState): String? {
            if (state.name.isBlank()) return "Ingresa tu nombre."
            if (state.email.isBlank()) return "Ingresa tu correo."
            if (state.password.length < 6) return "La contraseña debe tener al menos 6 caracteres."
            if (state.password != state.passwordConfirm) return "Las contraseñas no coinciden."
            if (!state.termsAccepted) return "Debes aceptar los términos y condiciones."
            if (!state.privacyAccepted) return "Debes aceptar el aviso de privacidad."
            if (state.clientIdRequired && state.contextualId.isBlank()) {
                return "Ingresa tu ${state.clientIdLabel.lowercase()}."
            }
            return null
        }

        private fun loadLegalDocuments() {
            launchTracked {
                remoteAccessEmailApi.currentLegal().onSuccess { legal ->
                    _state.value =
                        _state.value.copy(
                            termsVersion = legal.termsVersion,
                            termsUrl = legal.termsUrl,
                            privacyVersion = legal.privacyVersion,
                            privacyUrl = legal.privacyUrl,
                        )
                }
            }
        }

        private fun lockResend(
            seconds: Long,
            errorMessage: String? = "Espera ${seconds.coerceAtLeast(1)} s para reenviar el correo.",
        ) {
            val wait = seconds.coerceAtLeast(1)
            val until = System.currentTimeMillis() + wait * 1000
            _state.value =
                _state.value.copy(
                    loading = false,
                    resendLockedUntilMs = until,
                    errorMessage = errorMessage ?: _state.value.errorMessage,
                )
            launchTracked {
                delay(wait * 1000)
                if (_state.value.resendLockedUntilMs == until) {
                    _state.value = _state.value.copy(resendLockedUntilMs = 0L)
                }
            }
        }

        private fun cancelActiveJobs() {
            activeJobs.toList().forEach(Job::cancel)
            activeJobs.clear()
            contextBootstrapJob = null
        }

        private fun launchTracked(block: suspend () -> Unit): Job {
            lateinit var job: Job
            job =
                viewModelScope.launch(start = CoroutineStart.LAZY) {
                    try {
                        block()
                    } finally {
                        activeJobs.remove(job)
                    }
                }
            activeJobs += job
            job.start()
            return job
        }
    }
