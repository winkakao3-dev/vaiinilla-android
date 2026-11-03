package com.vaiinilla.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.auth.student.StudentAuthSession
import com.vaiinilla.app.domain.mode.AuthorizedInvitation
import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.time.BusinessTime
import com.vaiinilla.app.ui.components.EditorialAccentButton
import com.vaiinilla.app.ui.components.EditorialSecondaryButton
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
                InvitationHeader(invitation = state.invitation)
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
                    InvitationDetails(invitation = invitation, session = state.session, expiresLabel = expiresLabel)
                }
            }
            item {
                if (state.session == null) {
                    Text("Necesitas una cuenta verificada para continuar.", color = colors.muted)
                    EditorialAccentButton(text = "Iniciar sesión o registrarme", onClick = onAuthenticate)
                } else {
                    Text(
                        "Al aceptar, apruebas los términos para personal y el aviso de privacidad vigentes.",
                        color = colors.muted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
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
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    EditorialAccentButton(
                        text = if (state.loading && state.invitation != null) "Validando…" else "Aceptar invitación",
                        onClick = onAccept,
                        enabled = !state.loading && state.session != null && state.invitation != null,
                    )
                    EditorialSecondaryButton(text = "Ahora no", onClick = onBack)
                }
            }
        }
    }
}

/** El título del L14: quién invita y a qué rol, como «Trabaja en Caja». */
internal fun invitationHeadline(invitation: AuthorizedInvitation?): Pair<String, String> {
    val business = invitation?.establishmentName?.trim().orEmpty()
    val inviter = if (business.isEmpty()) "Te invitan a Vaiinilla" else "$business te invita"
    val role = invitation?.role
    val title =
        when {
            invitation == null -> "Acepta la invitación para entrar a un modo autorizado."
            role == OperationalRole.ADMIN -> "Administra el negocio"
            role != null && role != OperationalRole.CLIENT -> "Trabaja en ${role.label}"
            else -> "Únete al equipo"
        }
    return inviter to title
}

/** La invitación es para un correo: si la sesión es de otro, el servidor la va a rechazar. */
internal fun invitationMatchesSession(
    invitation: AuthorizedInvitation,
    sessionEmail: String?,
): Boolean? {
    val invited = invitation.invitedEmail.trim()
    val current = sessionEmail?.trim().orEmpty()
    if (invited.isEmpty() || current.isEmpty()) return null
    return invited.equals(current, ignoreCase = true)
}

@Composable
private fun InvitationHeader(invitation: AuthorizedInvitation?) {
    val colors = LocalVaiinillaColors.current
    val (inviter, title) = invitationHeadline(invitation)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (invitation != null) {
            val initial = invitation.establishmentName.trim().firstOrNull()?.uppercaseChar() ?: 'V'
            Box(
                modifier =
                    Modifier
                        .size(72.dp)
                        .background(colors.paper2, RoundedCornerShape(20.dp))
                        .border(1.dp, colors.line, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(initial.toString(), color = colors.ink, fontSize = 30.sp, fontWeight = FontWeight.Black)
            }
            Text(
                inviter,
                color = colors.muted,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 12.dp),
            )
        } else {
            Text("Acceso invitado", color = colors.accent, fontWeight = FontWeight.Black, letterSpacing = 1.3.sp)
        }
        Text(title, color = colors.ink, fontSize = 32.sp, lineHeight = 35.sp, fontWeight = FontWeight.Black)
        if (invitation != null) {
            Text(
                "Entras con las mismas pantallas de ese rol y lo que hagas queda a tu nombre.",
                color = colors.ink,
                fontSize = 15.sp,
                lineHeight = 21.sp,
            )
        }
    }
}

@Composable
private fun InvitationDetails(
    invitation: AuthorizedInvitation,
    session: StudentAuthSession?,
    expiresLabel: String?,
) {
    val colors = LocalVaiinillaColors.current
    val matches = invitationMatchesSession(invitation, session?.email)
    Surface(
        color = colors.paper2,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, colors.line),
    ) {
        Column {
            var first = true
            if (invitation.invitedEmail.isNotBlank()) {
                InvitationDetailRow(label = "Invitada para", value = invitation.invitedEmail, ok = matches, divider = false)
                first = false
            }
            if (session != null) {
                val verified = if (session.emailVerified) "correo verificado" else "correo sin verificar"
                InvitationDetailRow(
                    label = "Tu sesión",
                    value = "${session.email} · $verified",
                    ok = session.emailVerified && matches != false,
                    divider = !first,
                )
                first = false
            }
            if (expiresLabel != null) {
                InvitationDetailRow(label = "Válida hasta", value = expiresLabel, ok = null, divider = !first)
                first = false
            }
            if (invitation.requiresRemoteAcceptance) {
                InvitationDetailRow(
                    label = "Al aceptar",
                    value = "Vaiinilla revisa tu correo y la vigencia",
                    ok = null,
                    divider = !first,
                )
            }
        }
    }
    if (matches == false) {
        Text(
            "Entraste con otro correo. Sal y entra con ${invitation.invitedEmail} para aceptarla.",
            color = colors.coral,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun InvitationDetailRow(
    label: String,
    value: String,
    ok: Boolean?,
    divider: Boolean,
) {
    val colors = LocalVaiinillaColors.current
    Column {
        if (divider) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, color = colors.muted, fontSize = 13.sp)
                Text(value, color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            if (ok != null) {
                Box(
                    modifier =
                        Modifier
                            .size(22.dp)
                            .background(
                                (if (ok) colors.accent else colors.coral).copy(alpha = 0.22f),
                                RoundedCornerShape(11.dp),
                            ).semantics { contentDescription = if (ok) "Coincide" else "No coincide" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (ok) Icons.Outlined.Check else Icons.Outlined.Close,
                        contentDescription = null,
                        tint = if (ok) colors.accent else colors.coral,
                        modifier = Modifier.size(14.dp),
                    )
                }
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
