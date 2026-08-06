package com.vaiinilla.app.ui.mode

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.auth.VaiinillaJwtRefreshCoordinator
import com.vaiinilla.app.core.security.SecureSessionStore
import com.vaiinilla.app.domain.auth.student.StudentAuthRepository
import com.vaiinilla.app.domain.auth.student.StudentAuthSession
import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.mode.AuthorizedModeContext
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.repository.AuthorizedAccessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AuthorizedAccessViewModel
    @Inject
    constructor(
        private val repository: AuthorizedAccessRepository,
        private val authRepository: StudentAuthRepository,
        private val sessionStore: SecureSessionStore,
        private val refreshCoordinator: VaiinillaJwtRefreshCoordinator,
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
            invitationJob =
                viewModelScope.launch {
                    withContext(Dispatchers.IO) { repository.invitation(normalized) }.fold(
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
                    withContext(Dispatchers.IO) { repository.authorizedModes(session) }.fold(
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
                    withContext(Dispatchers.IO) { repository.acceptInvitation(token, session) }.fold(
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
            val session = authRepository.peekSession()
            if (session == null) {
                _state.value = _state.value.copy(errorMessage = "Inicia sesión para cambiar de modo.")
                return
            }
            actionJob?.cancel()
            _state.value = _state.value.copy(loading = true, errorMessage = null, message = null)
            actionJob =
                viewModelScope.launch {
                    withContext(Dispatchers.IO) { repository.activateMode(mode, session) }.fold(
                        onSuccess = { context ->
                            activateContext(mode, session, context)
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
            if (session == null) {
                _state.value = _state.value.copy(errorMessage = "Inicia sesión para volver al modo Alumno.")
                return
            }
            val clientMode = _state.value.modes.firstOrNull { it.role == OperationalRole.CLIENT }
            if (clientMode == null) {
                sessionStore.clear()
                _state.value =
                    _state.value.copy(
                        loading = false,
                        activeContext = null,
                        errorMessage = "No hay un acceso Alumno disponible para esta cuenta.",
                    )
                return
            }
            actionJob?.cancel()
            _state.value = _state.value.copy(loading = true, errorMessage = null, message = null)
            actionJob =
                viewModelScope.launch {
                    withContext(Dispatchers.IO) { repository.activateMode(clientMode, session) }.fold(
                        onSuccess = { context ->
                            activateContext(clientMode, session, context)
                            _state.value =
                                _state.value.copy(
                                    loading = false,
                                    activeContext = null,
                                    message = "Regresaste al modo Alumno.",
                                    errorMessage = null,
                                )
                            onReturned()
                        },
                        onFailure = { error ->
                            _state.value =
                                _state.value.copy(
                                    loading = false,
                                    errorMessage = error.message ?: "No se pudo volver al modo Alumno.",
                                )
                        },
                    )
                }
        }

        fun syncAuthorizedAccessOrReturnToClient(onForcedToClient: () -> Unit = {}) {
            refreshModes(force = true) {
                enforceActiveModeStillAuthorized(onForcedToClient = onForcedToClient)
            }
        }

        fun refreshCurrentSession() {
            _state.value = _state.value.copy(session = authRepository.peekSession())
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
                    it.role == active.role &&
                        it.establishmentId == active.establishmentId &&
                        it.membershipId == active.membershipId
                }
            if (stillAuthorized) {
                _state.value = _state.value.copy(loading = false)
                return
            }
            val session = authRepository.peekSession()
            if (session == null || _state.value.modes.none { it.role == OperationalRole.CLIENT }) {
                sessionStore.clear()
                _state.value =
                    _state.value.copy(
                        loading = false,
                        activeContext = null,
                        errorMessage = "Tu acceso operativo cambió, pero no hay un contexto Alumno disponible.",
                    )
                return
            }
            returnToClient {
                _state.value = _state.value.copy(message = message)
                onForcedToClient()
            }
        }

        private fun activateContext(
            mode: AuthorizedMode,
            session: StudentAuthSession,
            context: AuthorizedModeContext,
        ) {
            sessionStore.saveAccessToken(context.accessToken)
            refreshCoordinator.startSession(
                role = context.role,
                expiresInSeconds = context.expiresIn,
                refresh = {
                    kotlinx.coroutines.runBlocking {
                        repository.activateMode(mode, session).fold(
                            onSuccess = { refreshed ->
                                sessionStore.saveAccessToken(refreshed.accessToken)
                                Result.success(Unit)
                            },
                            onFailure = { Result.failure(it) },
                        )
                    }
                },
            )
        }
    }
