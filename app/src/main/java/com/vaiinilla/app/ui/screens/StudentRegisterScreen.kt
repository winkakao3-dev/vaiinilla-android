package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.auth.student.StudentAuthUiState
import com.vaiinilla.app.ui.components.AuthAccessField
import com.vaiinilla.app.ui.components.AuthAccessFieldKind
import com.vaiinilla.app.ui.components.AuthAccessScaffold
import com.vaiinilla.app.ui.components.AuthInkSubmitButton
import com.vaiinilla.app.ui.components.AuthLegalCheckRow
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode

@Composable
fun StudentRegisterScreen(
    state: StudentAuthUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordConfirmChange: (String) -> Unit = {},
    onContextualIdChange: (String) -> Unit,
    onTermsChange: (Boolean) -> Unit,
    onPrivacyChange: (Boolean) -> Unit = {},
    onRegister: () -> Unit,
    onLogin: () -> Unit,
    onForgotPassword: () -> Unit,
) {
    AuthAccessScaffold(
        kicker = "Nueva cuenta",
        title = "Crea tu cuenta.",
        intro = "Así puedes pedir y seguir tu comida en Vaiinilla.",
        loading = state.loading,
        hintPrefix = "¿Ya tienes cuenta?",
        hintAction = "Inicia sesión",
        onHintAction = onLogin,
        privacyUrl = state.privacyUrl,
        termsUrl = state.termsUrl,
        showBack = true,
        onBack = onBack,
    ) {
        AuthAccessField(
            value = state.name,
            onValueChange = onNameChange,
            label = "Nombre",
            placeholder = "Tu nombre",
            kind = AuthAccessFieldKind.Person,
            imeAction = ImeAction.Next,
        )
        Spacer(Modifier.height(10.dp))
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
            placeholder = "Mínimo 6 caracteres",
            kind = AuthAccessFieldKind.Password,
            imeAction = ImeAction.Next,
        )
        Spacer(Modifier.height(10.dp))
        AuthAccessField(
            value = state.passwordConfirm,
            onValueChange = onPasswordConfirmChange,
            label = "Confirmar contraseña",
            placeholder = "Repite tu contraseña",
            kind = AuthAccessFieldKind.Password,
            imeAction = if (state.clientIdRequired) ImeAction.Next else ImeAction.Done,
            onImeAction = { if (!state.clientIdRequired) onRegister() },
        )
        if (state.clientIdRequired) {
            Spacer(Modifier.height(10.dp))
            AuthAccessField(
                value = state.contextualId,
                onValueChange = onContextualIdChange,
                label = state.clientIdLabel,
                placeholder = state.clientIdLabel,
                kind = AuthAccessFieldKind.Id,
                imeAction = ImeAction.Done,
                onImeAction = onRegister,
            )
        }
        Spacer(Modifier.height(16.dp))
        AuthLegalCheckRow(
            checked = state.termsAccepted,
            onCheckedChange = onTermsChange,
            label = "Acepto los términos y condiciones",
            linkLabel = "Leer términos",
            url = state.termsUrl,
        )
        AuthLegalCheckRow(
            checked = state.privacyAccepted,
            onCheckedChange = onPrivacyChange,
            label = "Acepto el aviso de privacidad",
            linkLabel = "Leer privacidad",
            url = state.privacyUrl,
        )
        state.errorMessage?.let { error ->
            Spacer(Modifier.height(16.dp))
            AuthErrorBanner(error)
        }
        if (state.emailExistsSuggestion) {
            Spacer(Modifier.height(16.dp))
            val colors = LocalVaiinillaColors.current
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colors.yolk.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Este correo ya está registrado.", color = colors.ink, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Iniciar sesión",
                        color = colors.ink,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable(onClick = onLogin),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Recuperar acceso",
                        color = colors.ink,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable(onClick = onForgotPassword),
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        AuthInkSubmitButton(
            text = "Crear cuenta",
            onClick = onRegister,
            enabled = !state.loading,
        )
    }
}

@Preview(name = "Auth · registro", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun StudentRegisterScreenPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        StudentRegisterScreen(
            state = StudentAuthUiState(name = "Dani", email = "dani@correo.com"),
            onBack = {},
            onNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onPasswordConfirmChange = {},
            onContextualIdChange = {},
            onTermsChange = {},
            onPrivacyChange = {},
            onRegister = {},
            onLogin = {},
            onForgotPassword = {},
        )
    }
}

@Composable
internal fun AuthErrorBanner(message: String) {
    val colors = LocalVaiinillaColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.coral.copy(alpha = 0.22f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(message, color = colors.ink, modifier = Modifier.padding(14.dp), fontSize = 14.sp, lineHeight = 20.sp)
    }
}
