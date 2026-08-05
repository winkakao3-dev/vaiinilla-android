package com.vaiinilla.app.ui.auth.student

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.auth.VaiinillaJwtRefreshCoordinator
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class StudentAuthViewModel
    @Inject
    constructor(
        private val authRepository: StudentAuthRepository,
        private val enrollmentRepository: StudentEnrollmentRepository,
        private val guestSessionStore: GuestSessionStore,
        private val sessionStore: SecureSessionStore,
        private val contextoExchange: ContextoExchanger,
        private val refreshCoordinator: VaiinillaJwtRefreshCoordinator,
        private val preferences: StudentAuthPreferences,
        private val remoteAccessEmailApi: AccessEmailApi,
    ) : ViewModel() {
        private val _state = mutableStateOf(StudentAuthUiState())
        val state: State<StudentAuthUiState> = _state

        init {
            refreshGuestVenue()
        }

        fun refreshGuestVenue() {
            val venue = guestSessionStore.readVenue()
            _state.value =
                _state.value.copy(
                    guestVenue = venue,
                    clientIdLabel = venue?.establishment?.clientIdLabel ?: "Identificador",
                    clientIdRequired = venue?.establishment?.clientIdRequired == true,
                    enrollmentComplete =
                        authRepository.isReadyForCheckout(venue?.establishment?.id),
                    session = authRepository.peekSession(),
                )
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

        fun clearError() {
            _state.value = _state.value.copy(errorMessage = null)
        }

        fun register(onSuccess: () -> Unit) {
            val current = _state.value
            validateRegistration(current)?.let { error ->
                _state.value = current.copy(errorMessage = error)
                return
            }
            _state.value = current.copy(loading = true, errorMessage = null, emailExistsSuggestion = false)
            viewModelScope.launch {
                authRepository.signUp(current.email, current.password, current.name).fold(
                    onSuccess = { session ->
                        val verificationResult = sendVerificationEmail()
                        _state.value =
                            _state.value.copy(
                                loading = false,
                                session = session,
                                verificationSent = verificationResult.isSuccess,
                                errorMessage = verificationResult.exceptionOrNull()?.message,
                            )
                        onSuccess()
                    },
                    onFailure = { error ->
                        val emailExists = error is StudentAuthEmailExistsException
                        _state.value =
                            _state.value.copy(
                                loading = false,
                                errorMessage = error.message,
                                emailExistsSuggestion = emailExists,
                            )
                    },
                )
            }
        }

        fun login(onSuccess: (Boolean) -> Unit) {
            val current = _state.value
            if (current.email.isBlank() || current.password.isBlank()) {
                _state.value = current.copy(errorMessage = "Ingresa correo y contraseña.")
                return
            }
            if (current.clientIdRequired && current.contextualId.isBlank()) {
                _state.value =
                    current.copy(
                        errorMessage = "Ingresa tu ${current.clientIdLabel.lowercase()}.",
                    )
                return
            }
            _state.value = current.copy(loading = true, errorMessage = null)
            viewModelScope.launch {
                authRepository.signIn(current.email, current.password).fold(
                    onSuccess = { session ->
                        _state.value =
                            _state.value.copy(
                                loading = false,
                                session = session,
                            )
                        if (session.emailVerified) {
                            completeEnrollment(
                                onSuccess = { onSuccess(true) },
                                onNeedsVerify = { onSuccess(false) },
                            )
                        } else {
                            onSuccess(false)
                        }
                    },
                    onFailure = { error ->
                        _state.value =
                            _state.value.copy(
                                loading = false,
                                errorMessage = error.message,
                            )
                    },
                )
            }
        }

        fun resendVerification() {
            _state.value = _state.value.copy(loading = true, errorMessage = null)
            viewModelScope.launch {
                sendVerificationEmail().fold(
                    onSuccess = {
                        _state.value =
                            _state.value.copy(
                                loading = false,
                                verificationSent = true,
                            )
                    },
                    onFailure = { error ->
                        _state.value =
                            _state.value.copy(
                                loading = false,
                                errorMessage = error.message,
                            )
                    },
                )
            }
        }

        fun checkVerification(onVerified: () -> Unit) {
            _state.value = _state.value.copy(loading = true, errorMessage = null)
            viewModelScope.launch {
                authRepository.reloadSession().fold(
                    onSuccess = { session ->
                        _state.value = _state.value.copy(loading = false, session = session)
                        if (session?.emailVerified == true) {
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
                                errorMessage = error.message,
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
            _state.value = _state.value.copy(loading = true, errorMessage = null)
            viewModelScope.launch {
                sendPasswordResetEmail(email).fold(
                    onSuccess = {
                        _state.value =
                            _state.value.copy(
                                loading = false,
                                passwordResetSent = true,
                            )
                        onSent()
                    },
                    onFailure = { error ->
                        _state.value =
                            _state.value.copy(
                                loading = false,
                                errorMessage = error.message,
                            )
                    },
                )
            }
        }

        fun completeEnrollment(
            onSuccess: () -> Unit,
            onNeedsVerify: () -> Unit = {},
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
            if (venue.establishment.clientIdRequired && current.contextualId.isBlank()) {
                _state.value =
                    current.copy(
                        errorMessage = "Ingresa tu ${venue.establishment.clientIdLabel.lowercase()}.",
                    )
                return
            }
            _state.value = current.copy(loading = true, errorMessage = null)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        runCatching {
                            val firebaseToken =
                                authRepository.getIdToken(forceRefresh = true).getOrThrow()
                            enrollmentRepository
                                .enroll(
                                    StudentEnrollmentRequest(
                                        nombre = session.displayName.ifBlank { current.name },
                                        terminosVersion = "2026-07",
                                        privacidadVersion = "2026-07",
                                    ),
                                    firebaseIdToken = firebaseToken,
                                ).getOrThrow()
                            val contexto =
                                contextoExchange.exchange(
                                    firebaseIdToken = firebaseToken,
                                    establecimientoSlug = venue.establishment.slug,
                                    establecimientoId = venue.establishment.id,
                                    identificadorCliente = current.contextualId.trim().ifBlank { null },
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
                                                    identificadorCliente = current.contextualId.trim().ifBlank { null },
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
                            )
                        onSuccess()
                    },
                    onFailure = { error ->
                        _state.value =
                            _state.value.copy(
                                loading = false,
                                errorMessage = error.message,
                            )
                    },
                )
            }
        }

        private suspend fun sendVerificationEmail(): Result<Unit> =
            authRepository.getIdToken(forceRefresh = true).fold(
                onSuccess = { firebaseIdToken -> remoteAccessEmailApi.sendVerification(firebaseIdToken) },
                onFailure = { Result.failure(it) },
            )

        private suspend fun sendPasswordResetEmail(email: String): Result<Unit> =
            remoteAccessEmailApi.sendRecovery(email)

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
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        runCatching {
                            val firebaseToken = authRepository.getIdToken(forceRefresh = true).getOrThrow()
                            enrollmentRepository
                                .enroll(
                                    StudentEnrollmentRequest(
                                        nombre = session.displayName.ifBlank { state.name },
                                        terminosVersion = "2026-07",
                                        privacidadVersion = "2026-07",
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
                            )
                        onSuccess()
                    },
                    onFailure = { error ->
                        _state.value =
                            _state.value.copy(
                                loading = false,
                                errorMessage = error.message,
                            )
                    },
                )
            }
        }

        private fun validateRegistration(state: StudentAuthUiState): String? {
            if (state.name.isBlank()) return "Ingresa tu nombre."
            if (state.email.isBlank()) return "Ingresa tu correo."
            if (state.password.length < 6) return "La contraseña debe tener al menos 6 caracteres."
            if (!state.termsAccepted) return "Debes aceptar los términos y condiciones."
            if (state.clientIdRequired && state.contextualId.isBlank()) {
                return "Ingresa tu ${state.clientIdLabel.lowercase()}."
            }
            return null
        }
    }
