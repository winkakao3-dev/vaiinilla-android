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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaiinilla.app.ui.auth.student.StudentAuthUiState
import com.vaiinilla.app.ui.components.EditorialAccentButton
import com.vaiinilla.app.ui.components.EditorialSectionHead
import com.vaiinilla.app.ui.components.EditorialTextField
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

@Composable
fun StudentLoginScreen(
    state: StudentAuthUiState,
    onBack: () -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onForgotPassword: () -> Unit,
    onRegister: () -> Unit,
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
                EditorialSectionHead(title = "Iniciar sesión")
            }
            item {
                EditorialTextField(
                    value = state.email,
                    onValueChange = onEmailChange,
                    label = "Correo",
                    placeholder = "tu@correo.com",
                )
            }
            item {
                EditorialTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    label = "Contraseña",
                    placeholder = "Tu contraseña",
                )
            }
            item {
                TextButton(onClick = onForgotPassword) {
                    Text("¿Olvidaste tu contraseña?")
                }
            }
            state.errorMessage?.let { error ->
                item { AuthErrorBanner(error) }
            }
            item {
                EditorialAccentButton(
                    text = "Entrar",
                    onClick = onLogin,
                    enabled = !state.loading,
                )
            }
            item {
                TextButton(onClick = onRegister) {
                    Text("Crear cuenta nueva")
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
