package com.vaiinilla.app.ui.auth.student

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.auth.VaiinillaJwtRefreshCoordinator
import com.vaiinilla.app.core.config.AppEnvironment
import com.vaiinilla.app.core.config.DataSourceMode
import com.vaiinilla.app.core.security.SecureSessionStore
import com.vaiinilla.app.data.auth.ContextoExchanger
import com.vaiinilla.app.data.auth.student.FixtureStudentAuthRepository
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
        private val environment: AppEnvironment,
        private val fixtureAuthRepository: FixtureStudentAuthRepository,
    ) : ViewModel() {
        private val _state = mutableStateOf(StudentAuthUiState())
        val state: State<StudentAuthUiState> = _state

        init {
            refreshGuestVenue()
            _state.value =
                _state.value.copy(
                    session = authRepository.peekSession(),
                    enrollmentComplete = authRepository.isReadyForCheckout(),
                )
        }

        fun refreshGuestVenue() {
            val venue = guestSessionStore.readVenue()
            _state.value =
                _state.value.copy(
                    guestVenue = venue,
                    clientIdLabel = venue?.establishment?.clientIdLabel ?: "Identificador",
                    clientIdRequired = venue?.establishment?.clientIdRequired == true,
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
            _state.value = _state.value.copy(termsAccepted = accepted, errorMessage = null)
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
                        logTermsAcceptance(current)
                        authRepository.sendEmailVerification()
                        _state.value =
                            _state.value.copy(
                                loading = false,
                                session = session,
                                verificationSent = true,
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
                authRepository.sendEmailVerification().fold(
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
                if (environment.dataSourceMode == DataSourceMode.MOCK) {
                    fixtureAuthRepository.markCurrentEmailVerified()
                }
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
                authRepository.sendPasswordReset(email).fold(
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
                _state.value = current.copy(errorMessage = "No encontramos el establecimiento del pedido.")
                return
            }
            _state.value = current.copy(loading = true, errorMessage = null)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        runCatching {
                            val firebaseToken =
                                authRepository.getIdToken(forceRefresh = true).getOrThrow()
                            val enrollment =
                                enrollmentRepository
                                    .enroll(
                                        StudentEnrollmentRequest(
                                            establecimientoId = venue.establishment.id,
                                            nombre = session.displayName.ifBlank { current.name },
                                            identificadorContextual =
                                                current.contextualId.trim().ifBlank { null },
                                            aceptacionTerminosEn = Instant.now(),
                                        ),
                                        firebaseIdToken = firebaseToken,
                                    ).getOrThrow()
                            val contexto =
                                contextoExchange.exchange(
                                    firebaseIdToken = firebaseToken,
                                    membresiaId = enrollment.membresiaId,
                                )
                            sessionStore.saveAccessToken(contexto.accessToken)
                            refreshCoordinator.startSession(
                                OperationalRole.CLIENT,
                                contexto.expiresIn,
                            )
                            if (environment.dataSourceMode == DataSourceMode.MOCK) {
                                fixtureAuthRepository.completeMockEnrollment(contexto.accessToken)
                            } else {
                                preferences.enrollmentComplete = true
                            }
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

        fun isReadyForCheckout(): Boolean = authRepository.isReadyForCheckout()

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

        private fun logTermsAcceptance(state: StudentAuthUiState) {
            if (environment.dataSourceMode == DataSourceMode.MOCK) {
                Log.i(TAG, "MOCK terms accepted at ${Instant.now()} for ${state.email}")
            }
        }

        private companion object {
            const val TAG = "StudentAuth"
        }
    }
