package com.vaiinilla.app.ui.mode

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.auth.VaiinillaJwtRefreshCoordinator
import com.vaiinilla.app.core.network.toUserFacingMessage
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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharedFlow
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
        private var returnJob: Job? = null
        private val returnCallbacks = mutableListOf<() -> Unit>()
        private var lastModesUid: String? = null

        /** Las credenciales de la cuenta dejaron de valer: la app debe cerrar la sesión y pedir entrar de nuevo. */
        val sessionExpired: SharedFlow<Unit> get() = refreshCoordinator.sessionExpired

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
                                    errorMessage = error.toUserFacingMessage("Invitación no disponible."),
                                )
                        },
                    )
                }
        }

        /** [onRefreshed] recibe `false` cuando la consulta falló: los modos que hay no son los de la cuenta. */
        fun refreshModes(
            force: Boolean = false,
            onRefreshed: (loaded: Boolean) -> Unit = {},
        ) {
            val session = authRepository.peekSession()
            if (session == null) {
                lastModesUid = null
                _state.value = _state.value.copy(session = null, modes = emptyList(), loading = false)
                onRefreshed(true)
                return
            }
            if (
                !force &&
                lastModesUid == session.uid &&
                _state.value.modes.isNotEmpty() &&
                modesJob?.isActive != true
            ) {
                _state.value = _state.value.copy(session = session, loading = false)
                onRefreshed(true)
                return
            }
            if (modesJob?.isActive == true && !force) {
                _state.value = _state.value.copy(session = session)
                onRefreshed(true)
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
                            onRefreshed(true)
                        },
                        onFailure = { error ->
                            _state.value =
                                _state.value.copy(
                                    loading = false,
                                    errorMessage = error.toUserFacingMessage(),
                                )
                            onRefreshed(false)
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
                    // La invitación es de un solo uso: si el servidor la aceptó, la app termina de registrarlo.
                    withContext(Dispatchers.IO + NonCancellable) { repository.acceptInvitation(token, session) }.fold(
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
                                    errorMessage = error.toUserFacingMessage("No se pudo aceptar la invitación."),
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
                            // El token del modo nuevo y su contexto se guardan juntos aunque cancelen la corrutina.
                            withContext(NonCancellable) {
                                // Espera a que termine un refresh del modo anterior: fuera del hilo principal.
                                withContext(Dispatchers.IO) { activateContext(mode, session, context) }
                                _state.value =
                                    _state.value.copy(
                                        loading = false,
                                        session = session,
                                        activeContext = context,
                                        message = "Modo ${mode.role.label} activo.",
                                    )
                            }
                            onActivated()
                        },
                        onFailure = { error ->
                            _state.value =
                                _state.value.copy(
                                    loading = false,
                                    errorMessage = error.toUserFacingMessage("No se pudo activar este modo."),
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
                viewModelScope.launch(Dispatchers.IO) {
                    refreshCoordinator.clearSession()
                    sessionStore.clear()
                }
                _state.value =
                    _state.value.copy(
                        loading = false,
                        activeContext = null,
                        errorMessage = "No hay un acceso Alumno disponible para esta cuenta.",
                    )
                return
            }
            // Dos avisos seguidos (o el temporizador de 30 s) piden lo mismo: la segunda llamada se suma
            // a la que va en curso en vez de cancelarla y perder su destino.
            returnCallbacks += onReturned
            if (returnJob?.isActive == true) return
            actionJob?.cancel()
            _state.value = _state.value.copy(loading = true, errorMessage = null, message = null)
            returnJob =
                viewModelScope.launch {
                    withContext(Dispatchers.IO) { repository.activateMode(clientMode, session) }.fold(
                        onSuccess = { context ->
                            withContext(NonCancellable) {
                                withContext(Dispatchers.IO) { activateContext(clientMode, session, context) }
                                _state.value =
                                    _state.value.copy(
                                        loading = false,
                                        activeContext = null,
                                        message = "Regresaste al modo Alumno.",
                                        errorMessage = null,
                                    )
                            }
                            val callbacks = returnCallbacks.toList()
                            returnCallbacks.clear()
                            callbacks.forEach { it() }
                        },
                        onFailure = { error ->
                            returnCallbacks.clear()
                            _state.value =
                                _state.value.copy(
                                    loading = false,
                                    errorMessage = error.toUserFacingMessage("No se pudo volver al modo Alumno."),
                                )
                        },
                    )
                }
        }

        /** Un QR o NFC de mesa pidió entrar como cliente: se deja el modo de personal sin aviso de error. */
        fun leaveStaffModeForLink(onLeft: () -> Unit) {
            if (_state.value.activeContext == null) return onLeft()
            if (_state.value.modes.any { it.role == OperationalRole.CLIENT }) {
                returnToClient(onLeft)
                return
            }
            viewModelScope.launch(Dispatchers.IO) {
                refreshCoordinator.clearSession()
                sessionStore.clear()
            }
            _state.value = _state.value.copy(loading = false, activeContext = null, errorMessage = null)
            onLeft()
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

        fun resetAfterSignOut() {
            invitationJob?.cancel()
            modesJob?.cancel()
            actionJob?.cancel()
            returnJob?.cancel()
            returnCallbacks.clear()
            lastModesUid = null
            refreshCoordinator.clearSession()
            _state.value = AuthorizedAccessUiState()
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
                viewModelScope.launch(Dispatchers.IO) {
                    refreshCoordinator.clearSession()
                    sessionStore.clear()
                }
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
            refreshCoordinator.startSession(
                role = context.role,
                expiresInSeconds = context.expiresIn,
                saveToken = { sessionStore.saveAccessToken(context.accessToken) },
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
