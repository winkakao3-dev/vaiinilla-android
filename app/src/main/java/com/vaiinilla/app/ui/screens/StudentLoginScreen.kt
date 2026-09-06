package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
