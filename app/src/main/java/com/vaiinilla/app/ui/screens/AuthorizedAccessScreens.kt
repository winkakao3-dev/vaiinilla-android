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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.time.BusinessTime
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

/**
 * Quien administra un negocio también tiene Caja, Mesero y Cocina ahí, para cubrir a quien falte. Se explica
 * debajo de la lista para que no parezca que alguien más le dio esos accesos.
 */
internal fun adminCoverNote(staffModes: List<AuthorizedMode>): String? {
    val covered =
        staffModes
            .filter { it.role == OperationalRole.ADMIN }
            .filter { admin ->
                staffModes.any { it.establishmentId == admin.establishmentId && it.role != OperationalRole.ADMIN }
            }.map { it.establishmentName }
            .distinct()
    if (covered.isEmpty()) return null
    val where = if (covered.size == 1) " en ${covered.single()}" else ""
    return "Como eres administración$where, también tienes Caja, Mesero y Cocina para cubrir a quien falte. " +
        "Entras con las mismas pantallas de ese rol y lo que hagas queda a tu nombre."
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

private fun formatInvitationExpiry(instant: Instant): String {
    val formatter =
        DateTimeFormatter
            .ofPattern("d MMM yyyy · HH:mm", Locale.forLanguageTag("es-MX"))
            .withZone(BusinessTime.zone)
    return formatter.format(instant)
}
