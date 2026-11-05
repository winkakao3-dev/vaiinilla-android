package com.vaiinilla.app.ui.screens.admin

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Forward
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.RoomService
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.admin.InvitationState
import com.vaiinilla.app.domain.admin.StaffInvitation
import com.vaiinilla.app.domain.admin.StaffMembership
import com.vaiinilla.app.domain.admin.StaffRole
import com.vaiinilla.app.domain.admin.StaffRules
import com.vaiinilla.app.domain.time.BusinessTime
import com.vaiinilla.app.ui.admin.AdminStaffUiState
import com.vaiinilla.app.ui.admin.AdminStaffViewModel
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.animateSelectionColor
import com.vaiinilla.app.ui.components.initialOf
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.theme.Yolk
import java.time.Duration
import java.time.Instant

private sealed interface StaffSheet {
    data object Invite : StaffSheet

    data class EditRole(
        val membership: StaffMembership,
    ) : StaffSheet

    data class Deactivate(
        val membership: StaffMembership,
    ) : StaffSheet
}

/**
 * Personal como el lienzo de Doop (marcos 05 y 52 a 54): sin nadie ni invitaciones, el primer paso es invitar; después
 * van los accesos activos con su rol y las invitaciones con su filtro, para reenviarlas o revocarlas.
 */
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
    val dialog = LocalAdminDialog.current
    val firstInvite =
        !readOnly &&
            state.filter == null &&
            state.memberships.isEmpty() &&
            state.invitations.isEmpty() &&
            !state.loading &&
            state.error == null &&
            state.membershipsError == null

    AdminSectionPage(
        title = "Personal",
        colors = colors,
        notice = state.notice,
        noticeIsError = state.noticeIsError,
        onBack = onBack,
        trailing = {
            if (!readOnly && !firstInvite) {
                AdminPrimaryButton(
                    title = "Invitar",
                    colors = colors,
                    fullWidth = false,
                    icon = Icons.Outlined.Email,
                    enabled = !state.saving,
                ) { sheet = StaffSheet.Invite }
            }
        },
    ) {
        item {
            Text(
                "Quién te ayuda a atender la tienda",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMuted,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        if (firstInvite) {
            item(key = "primera") {
                AdminPanel(colors, Modifier.adminRise(1).padding(top = 16.dp)) {
                    Text(
                        "Nueva invitación",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    InviteForm(colors, state, viewModel) {}
                }
            }
            item(key = "equipo") {
                Column(Modifier.adminRise(2)) {
                    AdminH2("Equipo", colors)
                    AdminPanel(colors, padding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)) {
                        AdminRow(
                            title = "Tú",
                            colors = colors,
                            detail = "Dueño · todos los puestos",
                            leading = { StaffAvatar("Tú", StaffRole.ADMIN, colors) },
                        ) { AdminBadge("Admin", colors, AdminBadgeTone.OK) }
                    }
                }
            }
            return@AdminSectionPage
        }
        item(key = "accesos") {
            Column(Modifier.adminRise(1)) {
                AdminCapsLabel(
                    "Accesos activos · ${state.memberships.size}",
                    colors,
                    Modifier.padding(top = 16.dp, bottom = 8.dp),
                )
                val membershipsError = state.membershipsError
                if (membershipsError != null && state.memberships.isEmpty()) {
                    AdminLoadError(membershipsError, colors, viewModel::refresh)
                } else if (state.memberships.isEmpty()) {
                    AdminPanel(colors) {
                        Text(
                            "Todavía nadie más tiene acceso. Cuando acepten la invitación, aparecen aquí.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textMuted,
                        )
                    }
                } else {
                    AdminPanel(colors, padding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)) {
                        state.memberships.forEachIndexed { index, membership ->
                            if (index > 0) AdminRowDivider(colors)
                            val name = membership.name.ifBlank { membership.email }
                            AdminRow(
                                title = name,
                                colors = colors,
                                detail = if (membership.name.isBlank()) membership.role?.duty else membership.email,
                                onClick = if (readOnly) null else ({ sheet = StaffSheet.EditRole(membership) }),
                                leading = { StaffAvatar(name, membership.role, colors) },
                            ) {
                                AdminBadge(
                                    membership.role?.short ?: membership.roleWire,
                                    colors,
                                    if (membership.role ==
                                        StaffRole.ADMIN
                                    ) {
                                        AdminBadgeTone.OK
                                    } else {
                                        AdminBadgeTone.NEUTRAL
                                    },
                                )
                                if (!readOnly) {
                                    Icon(
                                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = colors.textMuted,
                                    )
                                }
                            }
                        }
                    }
                }
                Box(Modifier.padding(top = 10.dp, start = 6.dp, end = 6.dp)) {
                    AdminHint(
                        "Administración ya incluye Caja, Mesero y Cocina. Un acceso desactivado deja de salir " +
                            "aquí: para devolvérselo, invítalo de nuevo.",
                        colors,
                    )
                }
            }
        }
        item(key = "filtros") {
            Column(Modifier.adminRise(2)) {
                AdminCapsLabel("Invitaciones", colors, Modifier.padding(top = 20.dp, bottom = 8.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    InvitationFilters.forEach { filter ->
                        val selected = state.filter == filter
                        val label =
                            when (filter) {
                                null -> "Todas"
                                InvitationState.EXPIRED -> "Vencidas"
                                else -> "${filter.label}s"
                            }
                        AdminFilterChip(
                            label = if (selected && !state.loading) "$label · ${state.invitations.size}" else label,
                            selected = selected,
                            colors = colors,
                            onClick = { viewModel.selectFilter(filter) },
                        )
                    }
                }
                Box(Modifier.height(10.dp))
            }
        }
        val error = state.error
        if (error != null && state.invitations.isEmpty()) {
            item { AdminLoadError(error, colors, viewModel::refresh) }
        } else if (!state.loading && state.invitations.isEmpty()) {
            item {
                AdminPanel(colors) {
                    AdminEmptyState(
                        icon = Icons.Outlined.Email,
                        title = "Sin invitaciones aquí",
                        message = "Cuando invites a alguien, su invitación sale aquí hasta que la acepte.",
                        colors = colors,
                    )
                }
            }
        }
        items(state.invitations, key = { "i${it.id}" }) { invitation ->
            InvitationCard(
                invitation = invitation,
                colors = colors,
                readOnly = readOnly,
                saving = state.saving,
                modifier = Modifier.animateItem().padding(bottom = 10.dp),
                onResend = {
                    dialog(
                        AdminDialogRequest(
                            title = "¿Reenviar a ${invitation.email}?",
                            message = "El enlace anterior deja de servir y le llega una invitación nueva por correo.",
                            confirm = "Reenviar",
                            icon = Icons.Outlined.Forward,
                            destructive = false,
                        ) {
                            viewModel.resend(invitation)
                            true
                        },
                    )
                },
                onRevoke = {
                    dialog(
                        AdminDialogRequest(
                            title = "¿Revocar la invitación?",
                            message = "El enlace que le llegó a ${invitation.email} deja de servir de inmediato.",
                            confirm = "Revocar",
                            icon = Icons.Outlined.Cancel,
                        ) {
                            viewModel.revoke(invitation)
                            true
                        },
                    )
                },
            )
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
                AdminSecondaryButton(
                    title = "Ver más invitaciones",
                    colors = colors,
                    outlined = true,
                ) { viewModel.loadMore() }
            }
        }
        item {
            Box(Modifier.padding(top = 12.dp, start = 6.dp, end = 6.dp)) {
                AdminHint("Las invitaciones duran 72 horas. Si vence, la reenvías y empieza de nuevo.", colors)
            }
        }
    }

    when (val current = sheet) {
        StaffSheet.Invite ->
            AdminSheet(title = "Nueva invitación", colors = colors, busy = state.saving, onDismiss = close) {
                InviteForm(colors, state, viewModel, close)
            }
        is StaffSheet.EditRole ->
            RoleSheet(
                current.membership,
                colors,
                state,
                onDismiss = close,
                onDeactivate = { sheet = StaffSheet.Deactivate(current.membership) },
            ) { viewModel.changeRole(current.membership, it, close) }
        is StaffSheet.Deactivate -> {
            val name = current.membership.name.ifBlank { current.membership.email }
            AdminConfirmSheet(
                title = "¿Desactivar el acceso de ${name.substringBefore(' ')}?",
                message =
                    "Deja de entrar a ${current.membership.role?.short ?: "tu negocio"} desde ahora. " +
                        "Lo que ya cobró se queda en el historial.",
                confirmLabel = "Desactivar acceso",
                colors = colors,
                busy = state.saving,
                onDismiss = close,
            ) { viewModel.deactivate(current.membership, close) }
        }
        null -> Unit
    }
}

private val InvitationFilters =
    listOf(InvitationState.PENDING, InvitationState.ACCEPTED, InvitationState.EXPIRED, InvitationState.REVOKED, null)

/** El nombre corto de las etiquetas. */
private val StaffRole.short: String
    get() =
        when (this) {
            StaffRole.CASHIER -> "Caja"
            StaffRole.KITCHEN -> "Cocina"
            StaffRole.WAITER -> "Mesero"
            StaffRole.ADMIN -> "Admin"
        }

/** Lo que hace quien tiene el rol. */
private val StaffRole.duty: String
    get() =
        when (this) {
            StaffRole.CASHIER -> "Cobra, abre y cierra la caja"
            StaffRole.KITCHEN -> "Ve y prepara los pedidos"
            StaffRole.WAITER -> "Toma pedidos y entrega en mesas"
            StaffRole.ADMIN -> "Todo, incluido el negocio y el personal"
        }

private val StaffRole.icon: ImageVector
    get() =
        when (this) {
            StaffRole.CASHIER -> Icons.Outlined.PointOfSale
            StaffRole.KITCHEN -> Icons.Outlined.Restaurant
            StaffRole.WAITER -> Icons.Outlined.RoomService
            StaffRole.ADMIN -> Icons.Outlined.Storefront
        }

/** La inicial en un círculo con el color del rol. */
@Composable
private fun StaffAvatar(
    name: String,
    role: StaffRole?,
    colors: OperationalColors,
    size: Dp = 40.dp,
) {
    val tint =
        when (role) {
            StaffRole.ADMIN -> colors.accentLime
            StaffRole.CASHIER -> Yolk
            StaffRole.KITCHEN -> Color(0xFF9ED3E0)
            StaffRole.WAITER -> Color(0xFFF4A9A3)
            null -> colors.textSecondary
        }
    Box(Modifier.size(size).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
        Text(
            initialOf(name),
            fontSize = (size.value * 0.42f).sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF1D1E1C),
        )
    }
}

@Composable
private fun InvitationCard(
    invitation: StaffInvitation,
    colors: OperationalColors,
    readOnly: Boolean,
    saving: Boolean,
    modifier: Modifier,
    onResend: () -> Unit,
    onRevoke: () -> Unit,
) {
    val state = invitation.state
    AdminPanel(colors, modifier) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(colors.textPrimary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Email,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    invitation.email,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(invitation.role?.label, invitationTiming(invitation)).joinToString(" · "),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMuted,
                )
            }
            AdminBadge(state?.label ?: invitation.stateWire, colors, invitationTone(state))
        }
        if (!readOnly && state != null && state.canResend) {
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                SmallButton("Reenviar", danger = false, enabled = !saving, colors = colors, onClick = onResend)
                if (state == InvitationState.PENDING) {
                    SmallButton("Revocar", danger = true, enabled = !saving, colors = colors, onClick = onRevoke)
                }
            }
        }
    }
}

@Composable
private fun SmallButton(
    title: String,
    danger: Boolean,
    enabled: Boolean,
    colors: OperationalColors,
    onClick: () -> Unit,
) {
    val tint = if (danger) colors.coralText else colors.textPrimary
    Box(
        Modifier
            .physicalPress(enabled = enabled, scale = PhysicalPressScale.Small, onClick = onClick)
            .height(34.dp)
            .clip(CircleShape)
            .border(1.dp, if (danger) colors.coralText.copy(alpha = 0.6f) else colors.cardBorder, CircleShape)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) { Text(title, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = tint) }
}

/** "vence en 68 h" mientras sigue pendiente; si no, cuándo se creó. */
private fun invitationTiming(invitation: StaffInvitation): String? {
    if (invitation.state == InvitationState.PENDING) {
        val expires = runCatching { Instant.parse(invitation.expiresAt) }.getOrNull()
        if (expires != null) {
            val left = Duration.between(Instant.now(), expires)
            if (left.toHours() >= 1) return "vence en ${left.toHours()} h"
            if (!left.isNegative) return "vence en menos de 1 h"
        }
    }
    return adminShortDate(invitation.createdAt)?.let { "creada $it" }
}

private fun invitationTone(state: InvitationState?): AdminBadgeTone =
    when (state) {
        InvitationState.PENDING -> AdminBadgeTone.WARN
        InvitationState.ACCEPTED -> AdminBadgeTone.OK
        InvitationState.REVOKED, InvitationState.EXPIRED -> AdminBadgeTone.DANGER
        else -> AdminBadgeTone.NEUTRAL
    }

/** Correo y rol de una invitación nueva (marco 05). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InviteForm(
    colors: OperationalColors,
    state: AdminStaffUiState,
    viewModel: AdminStaffViewModel,
    onSent: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(StaffRole.CASHIER) }
    val haptics = rememberVaiinillaHaptics()
    Column {
        AdminTextField(
            label = "Correo de la persona",
            value = email,
            onValueChange = { email = it.trim() },
            colors = colors,
            placeholder = "nombre@correo.com",
            keyboardType = KeyboardType.Email,
            enabled = !state.saving,
        )
        Text(
            "¿Qué va a hacer?",
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colors.textPrimary,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StaffRole.entries.forEach { option ->
                val selected = role == option
                val bg =
                    animateSelectionColor(
                        if (selected) colors.textPrimary else colors.cardBackground,
                        "rol-${option.wire}",
                    )
                Row(
                    Modifier
                        .physicalPress(scale = PhysicalPressScale.Small) {
                            haptics.selection()
                            role = option
                        }.semantics { this.selected = selected }
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(bg)
                        .border(1.dp, if (selected) colors.textPrimary else colors.cardBorder, CircleShape)
                        .padding(horizontal = 16.dp)
                        .animateContentSize(spring(stiffness = 600f)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (selected) {
                        Icon(
                            option.icon,
                            contentDescription = null,
                            tint = colors.background,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Text(
                        option.short,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (selected) colors.background else colors.textSecondary,
                    )
                }
            }
        }
        Box(Modifier.padding(top = 10.dp)) {
            AdminHint(
                "Le llega un correo de un solo uso que vence en 72 horas. Al aceptar, ve solo " +
                    "${if (role == StaffRole.ADMIN) "la administración" else role.short.lowercase()} de tu negocio.",
                colors,
            )
        }
        Box(Modifier.padding(top = 6.dp)) { AdminSheetError(state.notice.takeIf { state.noticeIsError }) }
        AdminPrimaryButton(
            title = "Enviar invitación",
            colors = colors,
            modifier = Modifier.padding(top = 14.dp).fillMaxWidth(),
            icon = Icons.Outlined.Email,
            busy = state.saving,
            enabled = StaffRules.email(email) != null,
        ) {
            viewModel.invite(email, role) {
                email = ""
                onSent()
            }
        }
    }
}

/** Cambiar el rol o quitar el acceso (marcos 53 y 54). */
@Composable
private fun RoleSheet(
    membership: StaffMembership,
    colors: OperationalColors,
    state: AdminStaffUiState,
    onDismiss: () -> Unit,
    onDeactivate: () -> Unit,
    onSave: (StaffRole) -> Unit,
) {
    var role by remember { mutableStateOf(membership.role ?: StaffRole.CASHIER) }
    val name = membership.name.ifBlank { membership.email }
    val haptics = rememberVaiinillaHaptics()
    AdminSheet(title = name, colors = colors, busy = state.saving, onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StaffAvatar(name, membership.role, colors, size = 52.dp)
            Text(
                listOfNotNull(
                    membership.email.takeIf { membership.name.isNotBlank() },
                    adminShortDate(membership.createdAt)?.let { "desde $it" },
                ).joinToString(" · "),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text("¿Qué va a hacer?", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
        StaffRole.entries.forEach { option ->
            val selected = role == option
            val border =
                animateSelectionColor(if (selected) colors.accentLime else Color.Transparent, "borde-${option.wire}")
            Row(
                Modifier
                    .fillMaxWidth()
                    .physicalPress {
                        haptics.selection()
                        role = option
                    }.semantics { this.selected = selected }
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.cardBackground)
                    .border(1.5.dp, border, RoundedCornerShape(20.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier
                        .size(
                            22.dp,
                        ).border(1.5.dp, if (selected) colors.accentLime else colors.textMuted, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) Box(Modifier.size(12.dp).clip(CircleShape).background(colors.accentLime))
                }
                Box(
                    Modifier
                        .size(
                            36.dp,
                        ).clip(RoundedCornerShape(10.dp))
                        .background(colors.textPrimary.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        option.icon,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        if (option ==
                            StaffRole.ADMIN
                        ) {
                            "Admin"
                        } else if (option == StaffRole.WAITER) {
                            "Mesero"
                        } else {
                            option.label
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textPrimary,
                    )
                    Text(option.duty, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = colors.textMuted)
                }
            }
        }
        AdminSheetError(state.notice.takeIf { state.noticeIsError })
        AdminPrimaryButton(
            title = "Guardar rol",
            colors = colors,
            modifier = Modifier.fillMaxWidth(),
            busy = state.saving,
            enabled = role != membership.role,
        ) { onSave(role) }
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .physicalPress(enabled = !state.saving, onClick = onDeactivate),
            contentAlignment = Alignment.Center,
        ) { Text("Desactivar acceso", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = colors.coralText) }
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
