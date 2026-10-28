package com.vaiinilla.app.ui.screens.admin

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.admin.InvitationState
import com.vaiinilla.app.domain.admin.StaffInvitation
import com.vaiinilla.app.domain.admin.StaffMembership
import com.vaiinilla.app.domain.admin.StaffRole
import com.vaiinilla.app.domain.time.BusinessTime
import com.vaiinilla.app.ui.admin.AdminStaffViewModel
import com.vaiinilla.app.ui.screens.ActionPill
import com.vaiinilla.app.ui.screens.ChipButton
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.OperationalField

private sealed interface StaffSheet {
    data object Invite : StaffSheet

    data class Resend(
        val invitation: StaffInvitation,
    ) : StaffSheet

    data class Revoke(
        val invitation: StaffInvitation,
    ) : StaffSheet

    data class EditRole(
        val membership: StaffMembership,
    ) : StaffSheet

    data class Deactivate(
        val membership: StaffMembership,
    ) : StaffSheet
}

/** Personal e invitaciones: quién entra al negocio y con qué rol. */
@Composable
internal fun AdminStaffSection(
    viewModel: AdminStaffViewModel,
    colors: OperationalColors,
    readOnly: Boolean,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState
    var sheet by remember { mutableStateOf<StaffSheet?>(null) }
    val close = { sheet = null }

    AdminSectionPage(
        title = "Personal",
        colors = colors,
        notice = state.notice,
        noticeIsError = state.noticeIsError,
        onBack = onBack,
    ) {
        if (!readOnly) {
            item {
                ActionPill(label = "Invitar a alguien", enabled = !state.saving, loading = false, colors = colors) {
                    sheet = StaffSheet.Invite
                }
            }
        }
        item { AdminSectionTitle("Accesos activos", colors, Modifier.padding(top = 4.dp)) }
        if (state.memberships.isEmpty()) {
            item { AdminHint("Aún no hay personal con acceso.", colors) }
        }
        items(state.memberships, key = { "m${it.id}" }) { membership ->
            AdminCard(colors) {
                AdminValueRow(
                    label = membership.name.ifBlank { membership.email },
                    value = membership.role?.label ?: membership.roleWire,
                    colors = colors,
                    detail = membership.email.takeIf { membership.name.isNotBlank() },
                )
                if (!readOnly) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChipButton(label = "Cambiar rol", enabled = !state.saving, colors = colors) {
                            sheet = StaffSheet.EditRole(membership)
                        }
                        ChipButton(label = "Desactivar", enabled = !state.saving, colors = colors) {
                            sheet = StaffSheet.Deactivate(membership)
                        }
                    }
                }
            }
        }
        item { AdminSectionTitle("Invitaciones", colors, Modifier.padding(top = 8.dp)) }
        item {
            val filters = listOf<InvitationState?>(null) + InvitationState.entries
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                filters.forEach { filter ->
                    ChipButton(
                        label = filter?.label?.let { "${it}s" } ?: "Todas",
                        enabled = true,
                        colors = colors,
                        selected = filter == state.filter,
                    ) { viewModel.selectFilter(filter) }
                }
            }
        }
        val error = state.error
        if (error != null && state.invitations.isEmpty()) {
            item { AdminLoadError(error, colors, viewModel::refresh) }
        } else if (!state.loading && state.invitations.isEmpty()) {
            item { AdminHint("No hay invitaciones con este filtro.", colors) }
        }
        items(state.invitations, key = { "i${it.id}" }) { invitation ->
            InvitationCard(invitation, colors, readOnly, state.saving, onResend = {
                sheet =
                    StaffSheet.Resend(
                        it,
                    )
            }, onRevoke = {
                sheet = StaffSheet.Revoke(it)
            })
        }
        if (state.loading) {
            item {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = colors.textPrimary,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        } else if (state.cursor != null) {
            item {
                ActionPill(
                    label = "Ver más invitaciones",
                    enabled = true,
                    loading = false,
                    colors = colors,
                    primary = false,
                ) {
                    viewModel.loadMore()
                }
            }
        }
    }

    when (val current = sheet) {
        StaffSheet.Invite ->
            InviteSheet(colors, state.saving, state.notice.takeIf { state.noticeIsError }, close) { email, role ->
                viewModel.invite(email, role, close)
            }
        is StaffSheet.Resend ->
            AdminConfirmSheet(
                title = "Reenviar a ${current.invitation.email}",
                message = "El enlace anterior será reemplazado y se enviará una invitación nueva por correo.",
                confirmLabel = "Reenviar invitación",
                colors = colors,
                busy = state.saving,
                onDismiss = close,
            ) {
                viewModel.resend(current.invitation)
                close()
            }
        is StaffSheet.Revoke ->
            AdminConfirmSheet(
                title = "Revocar invitación",
                message = "El enlace de ${current.invitation.email} dejará de servir de inmediato.",
                confirmLabel = "Revocar",
                colors = colors,
                busy = state.saving,
                onDismiss = close,
            ) {
                viewModel.revoke(current.invitation)
                close()
            }
        is StaffSheet.EditRole ->
            RoleSheet(current.membership, colors, state.saving, state.notice.takeIf { state.noticeIsError }, close) {
                viewModel.changeRole(current.membership, it, close)
            }
        is StaffSheet.Deactivate ->
            AdminConfirmSheet(
                title = "Desactivar acceso",
                message = "${current.membership.name.ifBlank {
                    current.membership.email
                }} dejará de poder entrar a este negocio.",
                confirmLabel = "Desactivar acceso",
                colors = colors,
                busy = state.saving,
                onDismiss = close,
            ) { viewModel.deactivate(current.membership, close) }
        null -> Unit
    }
}

@Composable
private fun InvitationCard(
    invitation: StaffInvitation,
    colors: OperationalColors,
    readOnly: Boolean,
    saving: Boolean,
    onResend: (StaffInvitation) -> Unit,
    onRevoke: (StaffInvitation) -> Unit,
) {
    val state = invitation.state
    AdminCard(colors) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    invitation.email,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(
                        invitation.role?.label,
                        adminShortDate(invitation.createdAt)?.let { "creada $it" },
                    ).joinToString(" · "),
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                )
            }
            Text(
                state?.label ?: invitation.stateWire,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color =
                    if (state == InvitationState.PENDING ||
                        state == InvitationState.ACCEPTED
                    ) {
                        colors.textPrimary
                    } else {
                        colors.textSecondary
                    },
            )
        }
        if (state == InvitationState.PENDING) {
            adminShortDate(invitation.expiresAt)?.let { AdminHint("Vence $it", colors) }
        }
        if (!readOnly && state != null && (state.canResend || state == InvitationState.PENDING)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.canResend) {
                    ChipButton(
                        label = "Reenviar",
                        enabled = !saving,
                        colors = colors,
                    ) { onResend(invitation) }
                }
                if (state ==
                    InvitationState.PENDING
                ) {
                    ChipButton(label = "Revocar", enabled = !saving, colors = colors) { onRevoke(invitation) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InviteSheet(
    colors: OperationalColors,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSend: (String, StaffRole) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(StaffRole.CASHIER) }
    AdminSheet(title = "Nueva invitación", colors = colors, busy = saving, onDismiss = onDismiss) {
        AdminHint("El enlace será de un solo uso, vencerá en 72 horas y solo sirve para el rol que elijas.", colors)
        OperationalField(
            value = email,
            onValueChange = { email = it.trim() },
            label = "Correo de la persona",
            colors = colors,
            placeholder = "nombre@correo.com",
            enabled = !saving,
            keyboardType = KeyboardType.Email,
        )
        Text("Rol", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StaffRole.entries.forEach { option ->
                ChipButton(label = option.label, enabled = !saving, colors = colors, selected = option == role) {
                    role =
                        option
                }
            }
        }
        AdminSheetError(error)
        ActionPill(label = "Enviar invitación", enabled = email.contains('@'), loading = saving, colors = colors) {
            onSend(email, role)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoleSheet(
    membership: StaffMembership,
    colors: OperationalColors,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (StaffRole) -> Unit,
) {
    var role by remember { mutableStateOf(membership.role ?: StaffRole.CASHIER) }
    AdminSheet(
        title = "Rol de ${membership.name.ifBlank { membership.email }}",
        colors = colors,
        busy = saving,
        onDismiss = onDismiss,
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StaffRole.entries.forEach { option ->
                ChipButton(label = option.label, enabled = !saving, colors = colors, selected = option == role) {
                    role =
                        option
                }
            }
        }
        AdminSheetError(error)
        ActionPill(
            label = "Guardar rol",
            enabled = role != membership.role,
            loading = saving,
            colors = colors,
        ) { onSave(role) }
    }
}

/** "16 oct" de una fecha ISO del backend; null si no se puede leer. */
internal fun adminShortDate(iso: String): String? =
    runCatching {
        java.time.format.DateTimeFormatter
            .ofPattern("d MMM", java.util.Locale.forLanguageTag("es-MX"))
            .format(
                java.time.Instant
                    .parse(iso)
                    .atZone(BusinessTime.zone),
            )
    }.getOrNull()
