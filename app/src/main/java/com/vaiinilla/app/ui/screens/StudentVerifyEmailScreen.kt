package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.auth.student.StudentAuthUiState
import com.vaiinilla.app.ui.components.AuthBanner
import com.vaiinilla.app.ui.components.AuthBannerTone
import com.vaiinilla.app.ui.components.AuthChip
import com.vaiinilla.app.ui.components.AuthGhostButton
import com.vaiinilla.app.ui.components.AuthHeading
import com.vaiinilla.app.ui.components.AuthIconTile
import com.vaiinilla.app.ui.components.AuthLimeButton
import com.vaiinilla.app.ui.components.AuthNumberedSteps
import com.vaiinilla.app.ui.components.AuthStage
import com.vaiinilla.app.ui.components.AuthStageColors
import com.vaiinilla.app.ui.components.authArrive
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode

@Composable
fun StudentVerifyEmailScreen(
    state: StudentAuthUiState,
    onBack: () -> Unit,
    onResend: () -> Unit,
    onCheckVerified: () -> Unit,
) {
    val email = state.session?.email ?: state.email
    val secondsLeft = rememberSecondsUntil(state.resendLockedUntilMs)
    AuthStage(
        loading = state.loading,
        onBack = onBack,
        contentTopPadding = 20.dp,
        actions = {
            AuthLimeButton(
                text = if (state.loading) "Revisando…" else "Ya lo verifiqué",
                onClick = onCheckVerified,
                enabled = !state.loading,
            )
            Spacer(Modifier.height(10.dp))
            AuthGhostButton(
                text = if (secondsLeft > 0) "Mandar otra vez en ${formatCountdown(secondsLeft)}" else "Mandar el correo otra vez",
                onClick = onResend,
                enabled = !state.loading && secondsLeft == 0L,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "El enlace abre app.vaiinilla.app para verificar.\nSi no lo ves, busca en spam o promociones.",
                color = AuthStageColors.Faint,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            AuthIconTile(icon = Icons.Outlined.Email, size = 150.dp, iconSize = 72.dp, badge = "1")
            Spacer(Modifier.height(28.dp))
            AuthHeading(
                cap = "Un paso más",
                title = if (state.verificationSent) "Revisa tu bandeja." else "Falta mandarte\nel correo.",
                subtitle =
                    if (state.verificationSent) {
                        "Te mandamos un enlace para confirmar que el correo es tuyo."
                    } else {
                        "Tu cuenta ya existe, pero no pudimos mandar el correo. Toca «Mandar el correo otra vez»."
                    },
                centered = true,
                modifier = Modifier.authArrive(1),
            )
            if (email.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                AuthChip(icon = Icons.Outlined.Email, text = email, modifier = Modifier.authArrive(2))
            }
            state.errorMessage?.let { error ->
                Spacer(Modifier.height(14.dp))
                AuthBanner(error, AuthBannerTone.Error)
            }
            Spacer(Modifier.height(22.dp))
            AuthNumberedSteps(
                steps =
                    listOf(
                        "Abre el correo de Vaiinilla",
                        "Toca el enlace para verificar",
                        "Vuelve aquí y toca «Ya lo verifiqué»",
                    ),
                modifier = Modifier.authArrive(3),
            )
        }
    }
}

@Preview(name = "Auth · verificar correo", showBackground = true, widthDp = 390, heightDp = 844)
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
