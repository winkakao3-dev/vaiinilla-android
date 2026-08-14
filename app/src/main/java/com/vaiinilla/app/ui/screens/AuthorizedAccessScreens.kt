package com.vaiinilla.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.RoomService
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.ui.components.AuthHeroSheetScaffold
import com.vaiinilla.app.ui.components.AuthInkSubmitButton
import com.vaiinilla.app.ui.components.EditorialAccentButton
import com.vaiinilla.app.ui.components.EditorialPrimaryButton
import com.vaiinilla.app.ui.components.EmptyState
import com.vaiinilla.app.ui.mode.AuthorizedAccessUiState
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun InvitationAcceptanceScreen(
    state: AuthorizedAccessUiState,
    onBack: () -> Unit,
    onAccept: () -> Unit,
    onAuthenticate: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val expiresLabel =
        remember(state.invitation?.expiresAt) {
            state.invitation?.expiresAt?.let(::formatInvitationExpiry)
        }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.paper)
                .statusBarsPadding(),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver", tint = colors.ink)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text("Acceso invitado", color = colors.accent, fontWeight = FontWeight.Black, letterSpacing = 1.3.sp)
                Text(
                    "Acepta la invitación para entrar a un modo autorizado.",
                    color = colors.ink,
                    fontSize = 28.sp,
                    lineHeight = 31.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (state.loading && state.invitation == null && state.errorMessage == null) {
                item {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                                .semantics { contentDescription = "Cargando invitación" },
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(color = colors.accent, modifier = Modifier.size(28.dp))
                    }
                }
            }
            state.invitation?.let { invitation ->
                item {
                    Surface(
                        color = colors.paper2,
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                invitation.establishmentName.ifBlank { "Invitación de Vaiinilla" },
                                color = colors.ink,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                            )
                            if (invitation.role != null) {
                                Text("Modo: ${invitation.role.label}", color = colors.ink, fontWeight = FontWeight.Bold)
                            }
                            if (invitation.invitedEmail.isNotBlank()) {
                                Text("Invitada para: ${invitation.invitedEmail}", color = colors.muted)
                            }
                            if (invitation.requiresRemoteAcceptance) {
                                Text(
                                    "El servidor validará el token, tu correo y su vigencia al aceptar.",
                                    color = colors.muted,
                                )
                            }
                            if (expiresLabel != null) {
                                Text("Válida hasta: $expiresLabel", color = colors.muted, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
            item {
                state.session?.let { session ->
                    Text("Sesión actual: ${session.email}", color = colors.muted)
                    Text(
                        if (session.emailVerified) "Correo verificado" else "Correo pendiente de verificación",
                        color = if (session.emailVerified) colors.accent else colors.coral,
                        fontWeight = FontWeight.Bold,
                    )
                } ?: run {
                    Text("Necesitas una cuenta verificada para continuar.", color = colors.muted)
                    EditorialPrimaryButton(
                        text = "Iniciar sesión o registrarme",
                        onClick = onAuthenticate,
                        background = colors.ink,
                        contentColor = colors.paper,
                    )
                }
            }
            item {
                AnimatedVisibility(
                    visible = state.errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    state.errorMessage?.let { error ->
                        Surface(
                            color = colors.coral.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text(
                                error,
                                color = colors.ink,
                                modifier = Modifier.padding(14.dp),
                            )
                        }
                    }
                }
            }
            item {
                AnimatedVisibility(
                    visible = state.message != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    state.message?.let { message ->
                        Text(message, color = colors.accent, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                EditorialAccentButton(
                    text = if (state.loading && state.invitation != null) "Validando…" else "Aceptar invitación",
                    onClick = onAccept,
                    enabled = !state.loading && state.session != null && state.invitation != null,
                )
            }
        }
    }
}

@Composable
fun AuthorizedModeScreen(
    state: AuthorizedAccessUiState,
    onBack: () -> Unit,
    onSelectMode: (AuthorizedMode) -> Unit,
    onReturnToClient: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val activeContext = state.activeContext
    val staffModes = state.modes.filter { it.role != OperationalRole.CLIENT }
    val clientMode = state.modes.firstOrNull { it.role == OperationalRole.CLIENT }
    AuthHeroSheetScaffold(
        kicker = "Personal",
        title = "Cómo vas a entrar.",
        intro = "Sólo ves los roles de esta cuenta. Caja, Cocina y Mesero son accesos aparte.",
        loading = state.loading,
        showBack = true,
        onBack = onBack,
        kickerIcon = Icons.Outlined.Badge,
    ) {
        if (!state.loading && staffModes.isEmpty() && state.errorMessage == null) {
            EmptyState(
                icon = Icons.Outlined.VpnKey,
                title = "Sin modos de personal",
                message = "Cuando administración te invite a Caja, Cocina o Mesero, aparecen aquí.",
            )
        }
        staffModes.forEach { mode ->
            val isActive =
                activeContext?.role == mode.role &&
                    activeContext.establishmentId == mode.establishmentId &&
                    activeContext.membershipId == mode.membershipId
            StaffModeCard(
                mode = mode,
                isActive = isActive,
                enabled = !state.loading,
                onSelect = { onSelectMode(mode) },
            )
            Spacer(Modifier.height(10.dp))
        }
        state.errorMessage?.let { error ->
            Spacer(Modifier.height(6.dp))
            Surface(
                color = colors.coral.copy(alpha = 0.16f),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    error,
                    color = colors.ink,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(14.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
        }
        state.message?.let { message ->
            Text(message, color = colors.accent, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "¿Entrar como alumno?",
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {
                            if (clientMode != null) onSelectMode(clientMode) else onReturnToClient()
                        },
                    ),
            color = colors.muted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun StaffModeCard(
    mode: AuthorizedMode,
    isActive: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.paper)
                .border(
                    width = if (isActive) 2.dp else 1.dp,
                    color = if (isActive) colors.accent.copy(alpha = 0.68f) else colors.line,
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                staffModeIcon(mode.role),
                contentDescription = null,
                tint = if (isActive) colors.accent else colors.muted,
                modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                Text(mode.role.label, color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(mode.establishmentName, color = colors.muted, fontSize = 13.sp, lineHeight = 18.sp)
                Text(staffModeBlurb(mode.role), color = colors.muted, fontSize = 12.sp, lineHeight = 16.sp)
                if (isActive) {
                    Text("Activo ahora", color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        AuthInkSubmitButton(
            text = if (isActive) "Continuar como ${mode.role.label}" else "Entrar como ${mode.role.label}",
            onClick = onSelect,
            enabled = enabled,
        )
    }
}

private fun staffModeIcon(role: OperationalRole) =
    when (role) {
        OperationalRole.CASHIER -> Icons.Outlined.PointOfSale
        OperationalRole.KITCHEN -> Icons.Outlined.Restaurant
        OperationalRole.WAITER -> Icons.Outlined.RoomService
        OperationalRole.CLIENT -> Icons.Outlined.Person
    }

private fun staffModeBlurb(role: OperationalRole) =
    when (role) {
        OperationalRole.CASHIER -> "Cobrar en efectivo y entregar en barra."
        OperationalRole.KITCHEN -> "Preparar comandas y marcarlas listo."
        OperationalRole.WAITER -> "Entregar pedidos en el espacio."
        OperationalRole.CLIENT -> "Pedir menú, pedidos y saldo."
    }

@Preview(name = "Invitación autorizada", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun InvitationAcceptanceScreenPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        InvitationAcceptanceScreen(
            state = AuthorizedAccessUiState(),
            onBack = {},
            onAccept = {},
            onAuthenticate = {},
        )
    }
}

@Preview(name = "Modos autorizados", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun AuthorizedModeScreenPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        AuthorizedModeScreen(
            state =
                AuthorizedAccessUiState(
                    modes =
                        listOf(
                            AuthorizedMode(
                                role = OperationalRole.CASHIER,
                                establishmentId = "e1",
                                establishmentName = "Cafetería central",
                                membershipId = "m1",
                            ),
                            AuthorizedMode(
                                role = OperationalRole.KITCHEN,
                                establishmentId = "e1",
                                establishmentName = "Cafetería central",
                                membershipId = "m2",
                            ),
                            AuthorizedMode(
                                role = OperationalRole.WAITER,
                                establishmentId = "e1",
                                establishmentName = "Cafetería central",
                                membershipId = "m3",
                            ),
                        ),
                ),
            onBack = {},
            onSelectMode = {},
            onReturnToClient = {},
        )
    }
}

private fun formatInvitationExpiry(instant: Instant): String {
    val formatter =
        DateTimeFormatter
            .ofPattern("d MMM yyyy · HH:mm", Locale.forLanguageTag("es-MX"))
            .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}
