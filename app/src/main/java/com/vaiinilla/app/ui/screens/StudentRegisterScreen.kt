package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaiinilla.app.ui.auth.student.StudentAuthUiState
import com.vaiinilla.app.ui.components.EditorialAccentButton
import com.vaiinilla.app.ui.components.EditorialSectionHead
import com.vaiinilla.app.ui.components.EditorialTextField
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

@Composable
fun StudentRegisterScreen(
    state: StudentAuthUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onContextualIdChange: (String) -> Unit,
    onTermsChange: (Boolean) -> Unit,
    onRegister: () -> Unit,
    onLogin: () -> Unit,
    onForgotPassword: () -> Unit,
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
                EditorialSectionHead(title = "Registro")
            }
            item {
                EditorialTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = "Nombre",
                    placeholder = "Tu nombre",
                )
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
                    placeholder = "Mínimo 6 caracteres",
                )
            }
            if (state.clientIdRequired) {
                item {
                    EditorialTextField(
                        value = state.contextualId,
                        onValueChange = onContextualIdChange,
                        label = state.clientIdLabel,
                        placeholder = state.clientIdLabel,
                    )
                }
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        checked = state.termsAccepted,
                        onCheckedChange = onTermsChange,
                    )
                    Text(
                        "Acepto los términos y condiciones",
                        color = colors.ink,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
            state.errorMessage?.let { error ->
                item { AuthErrorBanner(error) }
            }
            if (state.emailExistsSuggestion) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.yolk.copy(alpha = 0.35f),
                        shape =
                            androidx.compose.foundation.shape
                                .RoundedCornerShape(16.dp),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Este correo ya está registrado.", color = colors.ink, fontWeight = FontWeight.Black)
                            TextButton(onClick = onLogin) {
                                Text("Iniciar sesión", fontWeight = FontWeight.ExtraBold)
                            }
                            TextButton(onClick = onForgotPassword) {
                                Text("Recuperar acceso", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
            item {
                EditorialAccentButton(
                    text = "Crear cuenta",
                    onClick = onRegister,
                    enabled = !state.loading,
                )
            }
        }
        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.accent)
            }
        }
    }
}

@Composable
internal fun AuthErrorBanner(message: String) {
    val colors = LocalVaiinillaColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.coral.copy(alpha = 0.22f),
        shape =
            androidx.compose.foundation.shape
                .RoundedCornerShape(16.dp),
    ) {
        Text(message, color = colors.ink, modifier = Modifier.padding(14.dp))
    }
}
