package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.ui.auth.student.StudentAuthUiState
import com.vaiinilla.app.ui.components.AuthAccessField
import com.vaiinilla.app.ui.components.AuthAccessFieldKind
import com.vaiinilla.app.ui.components.AuthAccessScaffold
import com.vaiinilla.app.ui.components.AuthInkSubmitButton
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode

@Composable
fun StudentLoginScreen(
    state: StudentAuthUiState,
    onBack: () -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onContextualIdChange: (String) -> Unit = {},
    onLogin: () -> Unit,
    onForgotPassword: () -> Unit,
    onRegister: () -> Unit,
    showBack: Boolean = true,
    existingVerifiedSession: Boolean = false,
    onEnterTestMode: ((OperationalRole) -> Unit)? = null,
) {
    AuthAccessScaffold(
        kicker = if (existingVerifiedSession) "Vincular comedor" else "Acceso de estudiante",
        title = if (existingVerifiedSession) "Completa tu acceso." else "Qué bueno verte de nuevo.",
        intro =
            if (existingVerifiedSession) {
                "Tu cuenta ya está verificada. Solo falta vincularla a este comedor."
            } else {
                "Entra a tu menú, pedidos y saldo."
            },
        loading = state.loading,
        hintPrefix = if (existingVerifiedSession) null else "¿Primera vez aquí?",
        hintAction = if (existingVerifiedSession) null else "Crea tu cuenta",
        onHintAction = if (existingVerifiedSession) null else onRegister,
        privacyUrl = state.privacyUrl,
        termsUrl = state.termsUrl,
        showBack = showBack,
        onBack = onBack,
    ) {
        if (!existingVerifiedSession && onEnterTestMode != null) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFE8F5D0))
                        .border(1.dp, Color(0xFF96C83F), RoundedCornerShape(14.dp))
                        .clickable { onEnterTestMode(OperationalRole.CASHIER) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF96C83F)),
                    )
                    Text(
                        text = "Modo Test: Tienda Demo",
                        color = Color(0xFF171816),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                }
                Text(
                    text = "Probar Caja →",
                    color = Color(0xFF304427),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.height(14.dp))
        }

        if (!existingVerifiedSession) {
            AuthAccessField(
                value = state.email,
                onValueChange = onEmailChange,
                label = "Correo",
                placeholder = "tu@correo.com",
                kind = AuthAccessFieldKind.Email,
                imeAction = ImeAction.Next,
            )
            Spacer(Modifier.height(10.dp))
            AuthAccessField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = "Contraseña",
                placeholder = "Tu contraseña",
                kind = AuthAccessFieldKind.Password,
                trailingLabel = "¿La olvidaste?",
                onTrailingLabel = onForgotPassword,
                imeAction = if (state.clientIdRequired) ImeAction.Next else ImeAction.Done,
                onImeAction = { if (!state.clientIdRequired) onLogin() },
            )
        }
        state.noticeMessage?.let { message ->
            Spacer(Modifier.height(10.dp))
            AuthNoticeBanner(message)
        }
        if (state.clientIdRequired) {
            Spacer(Modifier.height(10.dp))
            AuthAccessField(
                value = state.contextualId,
                onValueChange = onContextualIdChange,
                label = state.clientIdLabel,
                placeholder = "Tu ${state.clientIdLabel.lowercase()}",
                kind = AuthAccessFieldKind.Id,
                imeAction = ImeAction.Done,
                onImeAction = onLogin,
            )
        }
        state.errorMessage?.let { error ->
            Spacer(Modifier.height(10.dp))
            AuthErrorBanner(error)
        }
        Spacer(Modifier.height(16.dp))
        AuthInkSubmitButton(
            text = if (existingVerifiedSession) "Continuar" else "Entrar a Vaiinilla",
            onClick = onLogin,
            enabled = !state.loading,
        )

        if (onEnterTestMode != null) {
            Spacer(Modifier.height(18.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFF7F3E7),
                border = BorderStroke(1.dp, Color(0xFF96C83F)),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF96C83F)),
                        )
                        Text(
                            text = "Entrar directo sin cuenta:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF171816),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF171816))
                                    .clickable { onEnterTestMode(OperationalRole.CASHIER) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Caja",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            )
                        }
                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFB7DE63))
                                    .clickable { onEnterTestMode(OperationalRole.KITCHEN) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Cocina",
                                color = Color(0xFF171816),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            )
                        }
                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFEFEBDD))
                                    .clickable { onEnterTestMode(OperationalRole.CLIENT) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Tienda",
                                color = Color(0xFF171816),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Auth · login", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun StudentLoginScreenPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        StudentLoginScreen(
            state = StudentAuthUiState(email = "dani@correo.com"),
            onBack = {},
            onEmailChange = {},
            onPasswordChange = {},
            onLogin = {},
            onForgotPassword = {},
            onRegister = {},
        )
    }
}
