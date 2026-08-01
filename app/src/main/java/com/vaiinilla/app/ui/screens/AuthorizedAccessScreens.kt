package com.vaiinilla.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.ui.components.DemoEmptyState
import com.vaiinilla.app.ui.components.EditorialAccentButton
import com.vaiinilla.app.ui.components.EditorialPrimaryButton
import com.vaiinilla.app.ui.mode.AuthorizedAccessUiState
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
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
                Text("Acceso invitado", color = colors.accentInk, fontWeight = FontWeight.Black, letterSpacing = 1.3.sp)
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
                        color = if (session.emailVerified) colors.accentInk else colors.coral,
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
                        Text(message, color = colors.accentInk, fontWeight = FontWeight.Bold)
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
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.paper)
                .statusBarsPadding(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver", tint = colors.ink)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Accesos autorizados", color = colors.ink, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text(
                    if (state.hasMultipleModes) "Cambiar modo" else "Selecciona cómo entrar",
                    color = colors.muted,
                    fontSize = 13.sp,
                )
            }
            if (state.loading) {
                CircularProgressIndicator(
                    color = colors.accent,
                    modifier =
                        Modifier
                            .padding(end = 12.dp)
                            .size(20.dp)
                            .semantics { contentDescription = "Actualizando modos" },
                    strokeWidth = 2.dp,
                )
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Sólo verás los accesos que pertenecen a tu cuenta.",
                    color = colors.muted,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            if (!state.loading && state.modes.isEmpty() && state.errorMessage == null) {
                item {
                    DemoEmptyState(
                        icon = Icons.Outlined.VpnKey,
                        title = "Sin modos operativos",
                        message = "Cuando aceptes una invitación vigente, aquí aparecerán tus accesos autorizados.",
                    )
                }
            }
            items(state.modes, key = { "${it.establishmentId}-${it.role.wireValue}" }) { mode ->
                val isActive =
                    activeContext?.role == mode.role &&
                        activeContext.establishmentId == mode.establishmentId
                Surface(
                    color = colors.paper2,
                    shape = RoundedCornerShape(22.dp),
                    modifier =
                        if (isActive) {
                            Modifier.border(2.dp, colors.accent, RoundedCornerShape(22.dp))
                        } else {
                            Modifier
                        },
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isActive) Icons.Outlined.CheckCircle else Icons.Outlined.Lock,
                                contentDescription = if (isActive) "Modo activo" else "Modo autorizado",
                                tint = if (isActive) colors.accent else colors.muted,
                            )
                            Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                                Text(
                                    mode.role.label,
                                    color = colors.ink,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 19.sp,
                                )
                                Text(mode.establishmentName, color = colors.muted)
                                if (isActive) {
                                    Text(
                                        "Activo ahora",
                                        color = colors.accentInk,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                    )
                                }
                            }
                        }
                        EditorialAccentButton(
                            text =
                                if (isActive) {
                                    "Continuar como ${mode.role.label}"
                                } else {
                                    "Entrar como ${mode.role.label}"
                                },
                            onClick = { onSelectMode(mode) },
                            enabled = !state.loading,
                        )
                    }
                }
            }
            item {
                EditorialPrimaryButton(
                    text = "Volver como Alumno",
                    onClick = onReturnToClient,
                    enabled = !state.loading,
                    background = colors.paper2,
                    contentColor = colors.ink,
                )
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
                                fontWeight = FontWeight.Bold,
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
                        Text(message, color = colors.accentInk, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun formatInvitationExpiry(instant: Instant): String {
    val formatter =
        DateTimeFormatter
            .ofPattern("d MMM yyyy · HH:mm", Locale.forLanguageTag("es-MX"))
            .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}
