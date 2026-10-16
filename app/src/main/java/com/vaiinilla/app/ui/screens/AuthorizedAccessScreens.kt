package com.vaiinilla.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.RoomService
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.ui.components.EditorialAccentButton
import com.vaiinilla.app.ui.components.EmptyState
import com.vaiinilla.app.ui.components.SettingsGroup
import com.vaiinilla.app.ui.components.SettingsRow
import com.vaiinilla.app.ui.components.SettingsTrailing
import com.vaiinilla.app.ui.components.SkeletonBlock
import com.vaiinilla.app.ui.components.WalletScreenShell
import com.vaiinilla.app.ui.components.WalletSubflowTopBar
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
                    EditorialAccentButton(text = "Iniciar sesión o registrarme", onClick = onAuthenticate)
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
    onBack: (() -> Unit)?,
    onSelectMode: (AuthorizedMode) -> Unit,
    onReturnToClient: () -> Unit,
    onSignOut: () -> Unit = {},
) {
    val colors = LocalVaiinillaColors.current
    val activeContext = state.activeContext
    val staffModes = state.modes.filter { it.role != OperationalRole.CLIENT }
    val clientMode = state.modes.firstOrNull { it.role == OperationalRole.CLIENT }
    // Una lista sencilla como la de Cuenta: sin encabezado de color ni lema, solo cómo entrar.
    WalletScreenShell(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(bottom = 28.dp),
        ) {
            WalletSubflowTopBar(title = "Modos", onBack = onBack)
            Column(
                modifier = Modifier.padding(horizontal = 20.dp).padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                if (state.loading && staffModes.isEmpty()) {
                    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(120.dp), corner = 20.dp)
                }
                if (!state.loading && staffModes.isEmpty() && state.errorMessage == null) {
                    EmptyState(
                        icon = Icons.Outlined.VpnKey,
                        title = "Sin modos de personal",
                        message = "Cuando administración te invite a Caja, Cocina o Mesero, aparecen aquí.",
                    )
                }
                if (staffModes.isNotEmpty()) {
                    SettingsGroup(title = "Personal") {
                        staffModes.forEachIndexed { index, mode ->
                            val isActive =
                                activeContext?.role == mode.role &&
                                    activeContext.establishmentId == mode.establishmentId &&
                                    activeContext.membershipId == mode.membershipId
                            SettingsRow(
                                title =
                                    if (isActive) {
                                        "Continuar como ${mode.role.label}"
                                    } else {
                                        "Entrar como ${mode.role.label}"
                                    },
                                subtitle = mode.establishmentName,
                                icon = staffModeIcon(mode.role),
                                trailing = SettingsTrailing.Check,
                                checked = isActive,
                                enabled = !state.loading,
                                onClick = { onSelectMode(mode) },
                                showDivider = index != staffModes.lastIndex,
                            )
                        }
                    }
                }
                state.errorMessage?.let { error ->
                    Surface(color = colors.coral.copy(alpha = 0.16f), shape = RoundedCornerShape(16.dp)) {
                        Text(
                            error,
                            color = colors.ink,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }
                state.message?.let { message ->
                    Text(message, color = colors.muted, fontWeight = FontWeight.SemiBold)
                }
                SettingsGroup(title = "Cliente") {
                    SettingsRow(
                        title = "Pedir como cliente",
                        icon = Icons.Outlined.Person,
                        onClick = { if (clientMode != null) onSelectMode(clientMode) else onReturnToClient() },
                        showDivider = false,
                    )
                }
                SettingsGroup(title = null) {
                    SettingsRow(
                        title = "Cerrar sesión",
                        trailing = SettingsTrailing.None,
                        onClick = onSignOut,
                        showDivider = false,
                    )
                }
            }
        }
    }
}

private fun staffModeIcon(role: OperationalRole) =
    when (role) {
        OperationalRole.CASHIER -> Icons.Outlined.PointOfSale
        OperationalRole.KITCHEN -> Icons.Outlined.Restaurant
        OperationalRole.WAITER -> Icons.Outlined.RoomService
        OperationalRole.ADMIN -> Icons.Outlined.Storefront
        OperationalRole.CLIENT -> Icons.Outlined.Person
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
