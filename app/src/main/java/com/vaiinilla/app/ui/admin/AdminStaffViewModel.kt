package com.vaiinilla.app.ui.admin

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.RetryKeys
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.admin.InvitationState
import com.vaiinilla.app.domain.admin.StaffInvitation
import com.vaiinilla.app.domain.admin.StaffMembership
import com.vaiinilla.app.domain.admin.StaffRole
import com.vaiinilla.app.domain.admin.StaffRules
import com.vaiinilla.app.domain.repository.AdminStaffRepository
import com.vaiinilla.app.domain.repository.CatalogRepositoryException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AdminStaffUiState(
    /** null = todas. */
    val filter: InvitationState? = null,
    val invitations: List<StaffInvitation> = emptyList(),
    val cursor: String? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val memberships: List<StaffMembership> = emptyList(),
    /** Los accesos no cargaron: se dice y se reintenta, en vez de decir que no hay nadie (en vivo, T07). */
    val membershipsError: String? = null,
    val saving: Boolean = false,
    val notice: String? = null,
    val noticeIsError: Boolean = false,
    /**
     * Por qué no salió la última invitación. No se borra solo como los avisos: se queda en la hoja, con el correo y
     * el rol escritos, hasta reintentar o cerrarla (en vivo, T07: un 500 del servidor no se veía).
     */
    val inviteError: String? = null,
)

/** Personal e invitaciones, como en la web. */
@HiltViewModel
class AdminStaffViewModel
    @Inject
    constructor(
        private val repository: AdminStaffRepository,
    ) : ViewModel() {
        private val _uiState = mutableStateOf(AdminStaffUiState())

        /** Un reintento tras perder la respuesta reusa la llave: el servidor no lo hace dos veces. */
        private val retryKeys = RetryKeys()
        val uiState: State<AdminStaffUiState> = _uiState
        private var noticeJob: Job? = null
        private var loadJob: Job? = null
        private var scope: String? = null

        fun onVisible(scope: String) {
            if (scope != this.scope) {
                this.scope = scope
                _uiState.value = AdminStaffUiState()
            }
            refresh()
        }

        fun refresh() {
            load(append = false)
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { repository.memberships() }
                _uiState.value =
                    result.fold(
                        onSuccess = { _uiState.value.copy(memberships = it, membershipsError = null) },
                        onFailure = {
                            _uiState.value.copy(
                                membershipsError = it.toUserFacingMessage("No se pudo cargar quién tiene acceso."),
                            )
                        },
                    )
            }
        }

        fun selectFilter(filter: InvitationState?) {
            if (filter == _uiState.value.filter) return
            _uiState.value = _uiState.value.copy(filter = filter, invitations = emptyList(), cursor = null)
            load(append = false)
        }

        fun loadMore() {
            if (_uiState.value.cursor != null && !_uiState.value.loading) load(append = true)
        }

        private fun load(append: Boolean) {
            val filter = _uiState.value.filter
            val cursor = if (append) _uiState.value.cursor else null
            loadJob?.cancel()
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            loadJob =
                viewModelScope.launch {
                    val result = withContext(Dispatchers.IO) { repository.invitations(filter, cursor) }
                    if (_uiState.value.filter != filter) return@launch
                    _uiState.value =
                        result.fold(
                            onSuccess = { page ->
                                val previous = if (append) _uiState.value.invitations else emptyList()
                                _uiState.value.copy(
                                    invitations = previous + page.invitations,
                                    cursor = page.cursor,
                                    loading = false,
                                )
                            },
                            onFailure = {
                                _uiState.value.copy(
                                    loading = false,
                                    error = it.toUserFacingMessage("No se pudieron cargar las invitaciones."),
                                )
                            },
                        )
                }
        }

        /** Manda la invitación; [onDone] cierra la hoja cuando el servidor la aceptó. */
        fun invite(
            email: String,
            role: StaffRole,
            onDone: () -> Unit,
        ) {
            val normalized =
                StaffRules.email(email)
                    ?: return run { _uiState.value = _uiState.value.copy(inviteError = "Escribe un correo válido.") }
            if (_uiState.value.saving) return
            _uiState.value = _uiState.value.copy(inviteError = null)
            mutate(
                success = "Invitación creada y correo enviado.",
                onDone = onDone,
                onError = { error -> _uiState.value = _uiState.value.copy(inviteError = inviteErrorMessage(error)) },
            ) {
                retryKeys
                    .withKey("invite", normalized, role) { key -> repository.invite(normalized, role, key) }
                    .map { }
            }
        }

        /** Al cerrar la hoja de invitar, su error ya no aplica. */
        fun clearInviteError() {
            _uiState.value = _uiState.value.copy(inviteError = null)
        }

        fun resend(invitation: StaffInvitation) =
            mutate("Invitación reenviada a ${invitation.email}.") {
                retryKeys.withKey("resend", invitation.id) { repository.resend(invitation.id, it) }.map { }
            }

        fun revoke(invitation: StaffInvitation) =
            mutate("Invitación revocada.") {
                retryKeys.withKey("revoke", invitation.id) { repository.revoke(invitation.id, it) }.map { }
            }

        fun changeRole(
            membership: StaffMembership,
            role: StaffRole,
            onDone: () -> Unit,
        ) = mutate("Rol actualizado.", onDone) {
            retryKeys.withKey("role", membership.id, role) { repository.changeRole(membership.id, role, it) }.map { }
        }

        fun deactivate(
            membership: StaffMembership,
            onDone: () -> Unit,
        ) = mutate("Acceso desactivado.", onDone) {
            retryKeys.withKey("deactivate", membership.id) { repository.deactivate(membership.id, it) }
        }

        private fun mutate(
            success: String,
            onDone: () -> Unit = {},
            onError: ((Throwable) -> Unit)? = null,
            call: () -> Result<Unit>,
        ) {
            if (_uiState.value.saving) return
            _uiState.value = _uiState.value.copy(saving = true)
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { call() }
                _uiState.value = _uiState.value.copy(saving = false)
                result.fold(
                    onSuccess = {
                        showNotice(success, isError = false)
                        onDone()
                        refresh()
                    },
                    onFailure = { error ->
                        if (onError != null) onError(error) else showNotice(staffErrorMessage(error), isError = true)
                    },
                )
            }
        }

        fun dismissNotice() {
            noticeJob?.cancel()
            _uiState.value = _uiState.value.copy(notice = null)
        }

        private fun showNotice(
            text: String,
            isError: Boolean,
        ) {
            noticeJob?.cancel()
            _uiState.value = _uiState.value.copy(notice = text, noticeIsError = isError)
            noticeJob =
                viewModelScope.launch {
                    delay(NOTICE_MS)
                    _uiState.value = _uiState.value.copy(notice = null)
                }
        }

        private companion object {
            const val NOTICE_MS = 3_500L
        }
    }

/** Lo que dice la hoja de invitar cuando falla; un error del servidor dice que no salió el correo. */
internal fun inviteErrorMessage(error: Throwable): String {
    val serverDown =
        generateSequence(error) { it.cause }.any { it is ApiClientException && it.httpStatus in 500..599 }
    return if (serverDown) {
        "No se pudo enviar la invitación: el servidor falló al mandar el correo. Tus datos siguen aquí; " +
            "intenta de nuevo en unos minutos."
    } else {
        staffErrorMessage(error)
    }
}

/** Los rechazos del backend dichos para quien administra. */
internal fun staffErrorMessage(error: Throwable): String =
    when ((error as? CatalogRepositoryException)?.code) {
        "INVITATION_PENDING_EXISTS" -> "Ya hay una invitación pendiente para ese correo y rol."
        "INVITATION_STATE_CONFLICT" -> "La invitación ya cambió de estado. Actualiza la lista."
        "MEMBERSHIP_SELF_PROTECTION" -> "No puedes cambiar ni desactivar el acceso que estás usando."
        "LAST_ADMIN_REQUIRED" -> "El negocio debe conservar al menos una persona en Administración."
        "MEMBERSHIP_ROLE_EXISTS" -> "Esa persona ya tiene ese rol."
        "RATE_LIMITED" -> "Enviaste muchas invitaciones. Espera un rato e intenta de nuevo."
        else -> error.toUserFacingMessage("No se pudo guardar.")
    }
