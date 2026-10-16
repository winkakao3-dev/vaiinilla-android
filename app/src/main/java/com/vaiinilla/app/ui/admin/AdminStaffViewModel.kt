package com.vaiinilla.app.ui.admin

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import java.util.UUID
import javax.inject.Inject

data class AdminStaffUiState(
    /** null = todas. */
    val filter: InvitationState? = null,
    val invitations: List<StaffInvitation> = emptyList(),
    val cursor: String? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val memberships: List<StaffMembership> = emptyList(),
    val saving: Boolean = false,
    val notice: String? = null,
    val noticeIsError: Boolean = false,
)

/** Personal e invitaciones, como en la web. */
@HiltViewModel
class AdminStaffViewModel
    @Inject
    constructor(
        private val repository: AdminStaffRepository,
    ) : ViewModel() {
        private val _uiState = mutableStateOf(AdminStaffUiState())
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
                withContext(Dispatchers.IO) { repository.memberships() }.onSuccess {
                    _uiState.value = _uiState.value.copy(memberships = it)
                }
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
            val normalized = StaffRules.email(email) ?: return showNotice("Escribe un correo válido.", isError = true)
            mutate("Invitación creada y correo enviado.", onDone) {
                repository.invite(normalized, role, UUID.randomUUID().toString()).map { }
            }
        }

        fun resend(invitation: StaffInvitation) =
            mutate("Invitación reenviada a ${invitation.email}.") {
                repository.resend(invitation.id, UUID.randomUUID().toString()).map { }
            }

        fun revoke(invitation: StaffInvitation) =
            mutate("Invitación revocada.") {
                repository.revoke(invitation.id, UUID.randomUUID().toString()).map { }
            }

        fun changeRole(
            membership: StaffMembership,
            role: StaffRole,
            onDone: () -> Unit,
        ) = mutate("Rol actualizado.", onDone) {
            repository.changeRole(membership.id, role, UUID.randomUUID().toString()).map { }
        }

        fun deactivate(
            membership: StaffMembership,
            onDone: () -> Unit,
        ) = mutate("Acceso desactivado.", onDone) {
            repository.deactivate(membership.id, UUID.randomUUID().toString())
        }

        private fun mutate(
            success: String,
            onDone: () -> Unit = {},
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
                    onFailure = { showNotice(staffErrorMessage(it), isError = true) },
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
