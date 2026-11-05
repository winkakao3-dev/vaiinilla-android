package com.vaiinilla.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.auth.student.StudentAuthSession
import com.vaiinilla.app.domain.mode.AuthorizedInvitation
import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.time.BusinessTime
import com.vaiinilla.app.ui.components.ClientPillButton
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
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
                .statusBarsPadding()
                .navigationBarsPadding(),
    ) {
        InvitationCloseButton(onClick = onBack, modifier = Modifier.padding(start = 12.dp, top = 4.dp))
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            InvitationHeader(invitation = state.invitation)
            if (state.loading && state.invitation == null && state.errorMessage == null) {
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
            state.invitation?.let { invitation ->
                InvitationDetails(
                    invitation = invitation,
                    session = state.session,
                    expiresLabel = expiresLabel,
                    modifier = Modifier.padding(top = 18.dp).arrive(2),
                )
            }
            if (state.session == null) {
                Text("Necesitas una cuenta verificada para continuar.", color = colors.muted, fontSize = 14.sp)
            } else {
                Text(
                    buildAnnotatedString {
                        append("Al aceptar, apruebas los ")
                        withStyle(SpanStyle(color = colors.ink2, fontWeight = FontWeight.Bold)) {
                            append("términos para personal")
                        }
                        append(" y el ")
                        withStyle(SpanStyle(color = colors.ink2, fontWeight = FontWeight.Bold)) {
                            append("aviso de privacidad")
                        }
                        append(" vigentes.")
                    },
                    color = colors.muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 4.dp).arrive(3),
                )
            }
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
            AnimatedVisibility(
                visible = state.message != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                state.message?.let { message ->
                    Text(
                        message,
                        color = if (colors.isDark) colors.accent else colors.accentInk,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.session == null) {
                ClientPillButton(text = "Iniciar sesión o registrarme", onClick = onAuthenticate)
            } else {
                ClientPillButton(
                    text = if (state.loading && state.invitation != null) "Validando…" else "Aceptar invitación",
                    onClick = onAccept,
                    enabled = !state.loading && state.invitation != null,
                )
            }
            ClientPillButton(text = "Ahora no", onClick = onBack, primary = false, height = 52.dp)
        }
    }
}

@Composable
private fun InvitationCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    Box(
        modifier =
            modifier
                .size(48.dp)
                .physicalPress(scale = PhysicalPressScale.Small) {
                    haptics.click()
                    onClick()
                }.semantics {
                    role = Role.Button
                    contentDescription = "Cerrar"
                },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(colors.paper2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Close, contentDescription = null, tint = colors.ink2, modifier = Modifier.size(16.dp))
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
    Column {
        if (invitation != null) {
            val initial = invitation.establishmentName.trim().firstOrNull()?.uppercaseChar() ?: 'V'
            Box(
                modifier =
                    Modifier
                        .padding(top = 4.dp)
                        .arrive(0)
                        .size(84.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(colors.paper2),
                contentAlignment = Alignment.Center,
            ) {
                Text(initial.toString(), color = colors.ink, fontSize = 34.sp, fontWeight = FontWeight.Black)
            }
            Text(
                inviter,
                color = colors.muted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 20.dp).arrive(1),
            )
        } else {
            Text(
                "Acceso invitado",
                color = if (colors.isDark) colors.accent else colors.accentInk,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.3.sp,
            )
        }
        Text(
            title,
            color = colors.ink,
            fontSize = 34.sp,
            lineHeight = 36.sp,
            letterSpacing = (-1.4).sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = 4.dp).arrive(1),
        )
        if (invitation != null) {
            Text(
                "Entras con las mismas pantallas de ese rol y lo que hagas queda a tu nombre.",
                color = colors.ink2,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 10.dp).arrive(1),
            )
        }
    }
}

@Composable
private fun InvitationDetails(
    invitation: AuthorizedInvitation,
    session: StudentAuthSession?,
    expiresLabel: String?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val matches = invitationMatchesSession(invitation, session?.email)
    Column(modifier = modifier) {
        Surface(
            color = colors.paper2,
            shape = RoundedCornerShape(22.dp),
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
                color = colors.coralText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, color = colors.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(value, color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            }
            if (ok != null) {
                Box(
                    modifier =
                        Modifier
                            .size(22.dp)
                            .background((if (ok) colors.accent else colors.coral).copy(alpha = 0.16f), CircleShape)
                            .semantics { contentDescription = if (ok) "Coincide" else "No coincide" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (ok) Icons.Outlined.Check else Icons.Outlined.Close,
                        contentDescription = null,
                        tint = if (ok) colors.accent else colors.coral,
                        modifier = Modifier.size(13.dp),
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

/** Como en el dibujo: «viernes 6 de nov, 18:00», a la hora del negocio. */
internal fun formatInvitationExpiry(instant: Instant): String {
    val formatter =
        DateTimeFormatter
            .ofPattern("EEEE d 'de' MMM, HH:mm", Locale.forLanguageTag("es-MX"))
            .withZone(BusinessTime.zone)
    return formatter.format(instant).replace(".", "")
}
