package com.vaiinilla.app.ui.mode

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.config.AppEnvironment
import com.vaiinilla.app.core.config.DataSourceMode
import com.vaiinilla.app.core.security.SecureSessionStore
import com.vaiinilla.app.data.auth.student.FixtureStudentAuthRepository
import com.vaiinilla.app.data.mode.FixtureAuthorizedAccessRepository
import com.vaiinilla.app.domain.auth.student.StudentAuthRepository
import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.model.OperationalRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthorizedAccessViewModel
    @Inject
    constructor(
        private val repository: FixtureAuthorizedAccessRepository,
        private val authRepository: StudentAuthRepository,
        private val fixtureAuthRepository: FixtureStudentAuthRepository,
        private val environment: AppEnvironment,
        private val sessionStore: SecureSessionStore,
    ) : ViewModel() {
        private val _state = mutableStateOf(AuthorizedAccessUiState())
        val state: State<AuthorizedAccessUiState> = _state

        private var invitationJob: Job? = null
        private var modesJob: Job? = null
        private var actionJob: Job? = null
        private var lastModesUid: String? = null

        fun openInvitation(token: String) {
            val normalized = token.trim()
            if (normalized.isEmpty()) {
                _state.value =
                    _state.value.copy(
                        loading = false,
                        errorMessage = "No hay una invitación seleccionada.",
                    )
                return
            }
            val current = _state.value
            if (
                current.invitationToken == normalized &&
                (current.loading || current.invitation != null) &&
                current.errorMessage == null
            ) {
                _state.value = current.copy(session = authRepository.peekSession())
                return
            }
            invitationJob?.cancel()
            _state.value =
                current.copy(
                    loading = true,
                    invitationToken = normalized,
                    invitation = null,
                    errorMessage = null,
                    message = null,
                )
            if (environment.dataSourceMode != DataSourceMode.MOCK) {
                _state.value =
                    _state.value.copy(
                        loading = false,
                        errorMessage = "Las invitaciones estarán disponibles cuando el backend esté listo.",
                    )
                return
            }
            invitationJob =
                viewModelScope.launch {
                    repository.invitation(normalized).fold(
                        onSuccess = { invitation ->
                            _state.value =
                                _state.value.copy(
                                    loading = false,
                                    invitation = invitation,
                                    session = authRepository.peekSession(),
                                )
                        },
                        onFailure = { error ->
                            _state.value =
                                _state.value.copy(
                                    loading = false,
                                    errorMessage = error.message ?: "Invitación no disponible.",
                                )
                        },
                    )
                }
        }

        fun refreshModes(
            force: Boolean = false,
            onRefreshed: () -> Unit = {},
        ) {
            if (!isMockEnabled()) {
                onRefreshed()
                return
            }
            val session = authRepository.peekSession()
            if (session == null) {
                lastModesUid = null
                _state.value = _state.value.copy(session = null, modes = emptyList(), loading = false)
                onRefreshed()
                return
            }
            if (
                !force &&
                lastModesUid == session.uid &&
                _state.value.modes.isNotEmpty() &&
                modesJob?.isActive != true
            ) {
                _state.value = _state.value.copy(session = session, loading = false)
                onRefreshed()
                return
            }
            if (modesJob?.isActive == true && !force) {
                _state.value = _state.value.copy(session = session)
                onRefreshed()
                return
            }
            modesJob?.cancel()
            _state.value =
                _state.value.copy(
                    session = session,
                    errorMessage = null,
                    loading = true,
                )
            modesJob =
                viewModelScope.launch {
                    repository.authorizedModes(session).fold(
                        onSuccess = { modes ->
                            lastModesUid = session.uid
                            _state.value =
                                _state.value.copy(
                                    modes = modes,
                                    loading = false,
                                )
                            onRefreshed()
                        },
                        onFailure = { error ->
                            _state.value =
                                _state.value.copy(
                                    loading = false,
                                    errorMessage = error.message,
                                )
                            onRefreshed()
                        },
                    )
                }
        }

        fun acceptInvitation(onAccepted: () -> Unit = {}) {
            if (!isMockEnabled()) return
            val token = _state.value.invitationToken
            val session = authRepository.peekSession()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(errorMessage = "No hay una invitación seleccionada.")
                return
            }
            if (session == null) {
                _state.value = _state.value.copy(errorMessage = "Inicia sesión para aceptar la invitación.")
                return
            }
            actionJob?.cancel()
            _state.value = _state.value.copy(loading = true, errorMessage = null, message = null)
            actionJob =
                viewModelScope.launch {
                    repository.acceptInvitation(token, session).fold(
                        onSuccess = {
                            _state.value =
                                _state.value.copy(
                                    session = session,
                                    message = "Acceso autorizado. Elige cómo entrar.",
                                )
                            refreshModes(force = true) {
                                _state.value = _state.value.copy(loading = false)
                                onAccepted()
                            }
                        },
                        onFailure = { error ->
                            _state.value =
                                _state.value.copy(
                                    loading = false,
                                    errorMessage = error.message ?: "No se pudo aceptar la invitación.",
                                )
                        },
                    )
                }
        }

        fun activateMode(
            mode: AuthorizedMode,
            onActivated: () -> Unit = {},
        ) {
            if (!isMockEnabled()) return
            val session = authRepository.peekSession()
            if (session == null) {
                _state.value = _state.value.copy(errorMessage = "Inicia sesión para cambiar de modo.")
                return
            }
            actionJob?.cancel()
            _state.value = _state.value.copy(loading = true, errorMessage = null, message = null)
            actionJob =
                viewModelScope.launch {
                    repository.activateMode(mode, session).fold(
                        onSuccess = { context ->
                            sessionStore.saveAccessToken(context.accessToken)
                            _state.value =
                                _state.value.copy(
                                    loading = false,
                                    session = session,
                                    activeContext = context,
                                    message = "Modo ${mode.role.label} activo.",
                                )
                            onActivated()
                        },
                        onFailure = { error ->
                            _state.value =
                                _state.value.copy(
                                    loading = false,
                                    errorMessage = error.message ?: "No se pudo activar este modo.",
                                )
                        },
                    )
                }
        }

        fun returnToClient(onReturned: () -> Unit = {}) {
            val session = authRepository.peekSession()
            if (session != null) {
                sessionStore.saveAccessToken("mock-vaiinilla-${session.uid}-cliente")
            }
            _state.value =
                _state.value.copy(
                    activeContext = null,
                    message = "Regresaste al modo Alumno.",
                    errorMessage = null,
                )
            onReturned()
        }

        /**
         * Simula una revocación externa (admin/backend) del modo activo.
         * El invitado no puede disparar esto desde la UI de modos.
         */
        fun simulateExternalRevocation(onForcedToClient: () -> Unit = {}) {
            if (!isMockEnabled()) return
            val session = authRepository.peekSession()
            val active = _state.value.activeContext
            if (session == null || active == null) {
                _state.value =
                    _state.value.copy(
                        errorMessage = "No hay un modo operativo activo para revocar.",
                    )
                return
            }
            val mode =
                AuthorizedMode(
                    role = active.role,
                    establishmentId = active.establishmentId,
                    establishmentName = active.establishmentName,
                    membershipId = active.membershipId,
                )
            actionJob?.cancel()
            _state.value = _state.value.copy(loading = true, errorMessage = null, message = null)
            actionJob =
                viewModelScope.launch {
                    repository.revokeMode(mode, session).fold(
                        onSuccess = {
                            refreshModes(force = true) {
                                enforceActiveModeStillAuthorized(
                                    message = "Tu acceso operativo fue revocado. Regresaste al modo Alumno.",
                                    onForcedToClient = onForcedToClient,
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

        fun prepareMockGallerySession() {
            if (environment.dataSourceMode == DataSourceMode.MOCK) {
                fixtureAuthRepository.ensureMockVerifiedAccount()
                refreshModes()
            }
        }

        fun prepareMockGalleryModes() {
            if (environment.dataSourceMode != DataSourceMode.MOCK) return
            val session = fixtureAuthRepository.ensureMockVerifiedAccount()
            repository.seedGalleryModes(session)
            refreshModes()
        }

        /**
         * Demo MOCK: activa Caja y luego aplica una revocación externa → Alumno.
         */
        fun prepareMockExternalRevocationScenario(onForcedToClient: () -> Unit = {}) {
            if (environment.dataSourceMode != DataSourceMode.MOCK) return
            val session = fixtureAuthRepository.ensureMockVerifiedAccount()
            repository.seedGalleryModes(session)
            actionJob?.cancel()
            _state.value =
                _state.value.copy(
                    session = session,
                    loading = true,
                    errorMessage = null,
                    message = null,
                )
            actionJob =
                viewModelScope.launch {
                    val modes = repository.authorizedModes(session).getOrElse { emptyList() }
                    val cashier = modes.firstOrNull { it.role == OperationalRole.CASHIER }
                    if (cashier == null) {
                        _state.value =
                            _state.value.copy(
                                loading = false,
                                modes = modes,
                                errorMessage = "No hay un modo Caja para simular la revocación.",
                            )
                        return@launch
                    }
                    repository.activateMode(cashier, session).fold(
                        onSuccess = { context ->
                            sessionStore.saveAccessToken(context.accessToken)
                            _state.value =
                                _state.value.copy(
                                    modes = modes,
                                    activeContext = context,
                                    session = session,
                                )
                            repository.revokeMode(cashier, session)
                            refreshModes(force = true) {
                                enforceActiveModeStillAuthorized(
                                    message = "Tu acceso operativo fue revocado. Regresaste al modo Alumno.",
                                    onForcedToClient = onForcedToClient,
                                )
                            }
                        },
                        onFailure = { error ->
                            _state.value =
                                _state.value.copy(
                                    loading = false,
                                    errorMessage = error.message ?: "No se pudo preparar la revocación externa.",
                                )
                        },
                    )
                }
        }

        fun syncAuthorizedAccessOrReturnToClient(onForcedToClient: () -> Unit = {}) {
            if (!isMockEnabled()) return
            refreshModes(force = true) {
                enforceActiveModeStillAuthorized(onForcedToClient = onForcedToClient)
            }
        }

        fun refreshCurrentSession() {
            if (!isMockEnabled()) return
            _state.value = _state.value.copy(session = authRepository.peekSession())
        }

        fun resetFixtures() {
            if (!isMockEnabled()) return
            invitationJob?.cancel()
            modesJob?.cancel()
            actionJob?.cancel()
            lastModesUid = null
            repository.reset()
            _state.value = AuthorizedAccessUiState(session = authRepository.peekSession())
        }

        fun clearFeedback() {
            _state.value = _state.value.copy(errorMessage = null, message = null)
        }

        private fun enforceActiveModeStillAuthorized(
            message: String = "Tu acceso operativo fue revocado. Regresaste al modo Alumno.",
            onForcedToClient: () -> Unit = {},
        ) {
            val active = _state.value.activeContext
            if (active == null) {
                _state.value = _state.value.copy(loading = false)
                return
            }
            val stillAuthorized =
                _state.value.modes.any {
                    it.role == active.role && it.establishmentId == active.establishmentId
                }
            if (stillAuthorized) {
                _state.value = _state.value.copy(loading = false)
                return
            }
            val session = authRepository.peekSession()
            if (session != null) {
                sessionStore.saveAccessToken("mock-vaiinilla-${session.uid}-cliente")
            }
            _state.value =
                _state.value.copy(
                    loading = false,
                    activeContext = null,
                    message = message,
                    errorMessage = null,
                )
            onForcedToClient()
        }

        private fun isMockEnabled(): Boolean {
            if (environment.dataSourceMode == DataSourceMode.MOCK) return true
            _state.value =
                _state.value.copy(
                    errorMessage = "Los accesos autorizados estarán disponibles cuando el backend esté listo.",
                )
            return false
        }
    }
