package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vaiinilla.app.ui.auth.student.StudentAuthUiState
import com.vaiinilla.app.ui.components.DemoEmptyState
import com.vaiinilla.app.ui.components.EditorialAccentButton
import com.vaiinilla.app.ui.components.EditorialSectionHead
import com.vaiinilla.app.ui.components.EditorialTextField
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode

@Composable
fun StudentForgotPasswordScreen(
    state: StudentAuthUiState,
    onBack: () -> Unit,
    onEmailChange: (String) -> Unit,
    onSendReset: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.paper),
    ) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                }
                EditorialSectionHead(title = "Recuperar acceso")
            }
            if (state.passwordResetSent) {
                item {
                    DemoEmptyState(
                        icon = Icons.Outlined.LockReset,
                        title = "Correo enviado",
                        message =
                            "Si existe una cuenta con ese correo, recibirás instrucciones " +
                                "para restablecer tu contraseña.",
                        actionLabel = "Volver",
                        onAction = onBack,
                    )
                }
            } else {
                item {
                    EditorialTextField(
                        value = state.email,
                        onValueChange = onEmailChange,
                        label = "Correo",
                        placeholder = "tu@correo.com",
                    )
                }
                state.errorMessage?.let { error ->
                    item { AuthErrorBanner(error) }
                }
                item {
                    EditorialAccentButton(
                        text = "Enviar enlace",
                        onClick = onSendReset,
                        enabled = !state.loading,
                    )
                }
                item {
                    Text(
                        "Te enviaremos un enlace para crear una nueva contraseña.",
                        color = colors.muted,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.accent)
            }
        }
    }
}

@Preview(name = "Auth · recuperar contraseña", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun StudentForgotPasswordScreenPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        StudentForgotPasswordScreen(
            state = StudentAuthUiState(email = "dani@utch.mx"),
            onBack = {},
            onEmailChange = {},
            onSendReset = {},
        )
    }
}
