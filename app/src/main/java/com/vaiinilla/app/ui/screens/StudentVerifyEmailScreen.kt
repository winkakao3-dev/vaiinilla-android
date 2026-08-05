package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MarkEmailRead
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
import com.vaiinilla.app.ui.components.EditorialPrimaryButton
import com.vaiinilla.app.ui.components.EditorialSectionHead
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode

@Composable
fun StudentVerifyEmailScreen(
    state: StudentAuthUiState,
    onBack: () -> Unit,
    onResend: () -> Unit,
    onCheckVerified: () -> Unit,
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
                EditorialSectionHead(title = "Verifica tu correo")
            }
            item {
                DemoEmptyState(
                    icon = Icons.Outlined.MarkEmailRead,
                    title = "Revisa tu bandeja",
                    message =
                        buildString {
                            append("Enviamos un enlace a ")
                            append(state.session?.email ?: state.email)
                            append(". Confírmalo para continuar con tu pedido.")
                            if (state.verificationSent) {
                                append(" (reenviado)")
                            }
                        },
                    actionLabel = "Ya verifiqué",
                    onAction = onCheckVerified,
                )
            }
            state.errorMessage?.let { error ->
                item { AuthErrorBanner(error) }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    EditorialAccentButton(
                        text = "Ya verifiqué mi correo",
                        onClick = onCheckVerified,
                        enabled = !state.loading,
                    )
                    EditorialPrimaryButton(
                        text = "Reenviar correo",
                        onClick = onResend,
                        enabled = !state.loading,
                        background = colors.paper2,
                        contentColor = colors.ink,
                    )
                }
            }
            item {
                Text(
                    "En modo demo local, «Ya verifiqué» simula la confirmación.",
                    color = colors.muted,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 8.dp),
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

@Preview(name = "Auth · verificar correo", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun StudentVerifyEmailScreenPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        StudentVerifyEmailScreen(
            state = StudentAuthUiState(email = "dani@utch.mx", verificationSent = true),
            onBack = {},
            onResend = {},
            onCheckVerified = {},
        )
    }
}
