package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vaiinilla.app.ui.auth.student.StudentAuthUiState
import com.vaiinilla.app.ui.components.EditorialAccentButton
import com.vaiinilla.app.ui.components.EditorialHero
import com.vaiinilla.app.ui.components.EditorialPrimaryButton
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode

@Composable
fun StudentAuthLandingScreen(
    state: StudentAuthUiState,
    onBack: () -> Unit,
    onRegister: () -> Unit,
    onLogin: () -> Unit,
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
            }
            item {
                EditorialHero(
                    eyebrow = "Tu primer pedido",
                    title = "Crea tu cuenta",
                    body =
                        "Para confirmar en efectivo necesitamos verificar tu correo " +
                            "y vincularte al comedor.",
                    watermark = "01",
                    actions = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            EditorialAccentButton(
                                text = "Registrarme",
                                onClick = onRegister,
                                enabled = !state.loading,
                            )
                            EditorialPrimaryButton(
                                text = "Ya tengo cuenta",
                                onClick = onLogin,
                                enabled = !state.loading,
                                background = colors.paper2,
                                contentColor = colors.ink,
                            )
                        }
                    },
                )
            }
            state.guestVenue?.let { venue ->
                item {
                    Text(
                        "Seguirás en ${venue.establishment.name}" +
                            venue.space?.let { " · ${it.name}" }.orEmpty(),
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

@Preview(name = "Auth · inicio", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun StudentAuthLandingScreenPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        StudentAuthLandingScreen(
            state = StudentAuthUiState(),
            onBack = {},
            onRegister = {},
            onLogin = {},
        )
    }
}
