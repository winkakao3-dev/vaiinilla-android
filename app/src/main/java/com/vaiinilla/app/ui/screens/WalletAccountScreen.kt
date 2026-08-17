package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vaiinilla.app.ui.account.AccountDeletionStatus
import com.vaiinilla.app.ui.account.AccountDeletionUiState
import com.vaiinilla.app.ui.components.AuthAccessField
import com.vaiinilla.app.ui.components.AuthAccessFieldKind
import com.vaiinilla.app.ui.components.EditorialAccentButton
import com.vaiinilla.app.ui.components.EditorialPrimaryButton
import com.vaiinilla.app.ui.components.VaiinillaQrCode
import com.vaiinilla.app.ui.components.WalletScreenShell
import com.vaiinilla.app.ui.components.WalletSubflowTopBar
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.discovery.QrPayloadParser
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.LocalVaiinillaThemeMode
import com.vaiinilla.app.ui.theme.LocalVaiinillaThemeModeChanger
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode

private val ThemeControlWidth = 132.dp

@Composable
fun WalletAccountScreen(
    onBack: () -> Unit,
    displayName: String = "",
    email: String = "",
    userId: String? = null,
    signedIn: Boolean = false,
    hasStaffModes: Boolean = false,
    onOpenStaffModes: () -> Unit = {},
    onChangeVenue: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onSignIn: () -> Unit = {},
    accountDeletionState: AccountDeletionUiState = AccountDeletionUiState(),
    onRequestAccountDeletion: () -> Unit = {},
    onConfirmAccountDeletion: () -> Unit = {},
    onCancelAccountDeletion: () -> Unit = {},
    onSubmitAccountDeletionPassword: (String) -> Unit = {},
    onRetryAccountDeletion: () -> Unit = {},
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val currentMode = LocalVaiinillaThemeMode.current
    val onThemeModeChange = LocalVaiinillaThemeModeChanger.current
    var themeMenuExpanded by remember { mutableStateOf(false) }

    val qrValue = userId?.trim()?.takeIf { it.isNotEmpty() }?.let(QrPayloadParser::encodeUser)
    WalletScreenShell(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp),
        ) {
            WalletSubflowTopBar(
                title = "Configuración",
                onBack = {
                    haptics.click()
                    onBack()
                },
                trailing = {
                    Box {
                        Row(
                            modifier =
                                Modifier
                                    .width(ThemeControlWidth)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.paper2)
                                    .border(1.dp, colors.line, RoundedCornerShape(12.dp))
                                    .clickable {
                                        haptics.selection()
                                        themeMenuExpanded = true
                                    }.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "Tema: ${currentMode.label}",
                                color = colors.ink,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Icon(
                                Icons.Outlined.KeyboardArrowDown,
                                contentDescription = "Menú de tema",
                                tint = colors.muted,
                                modifier = Modifier.size(16.dp),
                            )
                        }

                        DropdownMenu(
                            expanded = themeMenuExpanded,
                            onDismissRequest = { themeMenuExpanded = false },
                            modifier =
                                Modifier
                                    .width(ThemeControlWidth)
                                    .background(colors.paper2)
                                    .border(1.dp, colors.line, RoundedCornerShape(12.dp)),
                        ) {
                            VaiinillaThemeMode.entries.forEach { mode ->
                                val isSelected = mode == currentMode
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = mode.label,
                                            color = if (isSelected) colors.accent else colors.ink,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp,
                                        )
                                    },
                                    onClick = {
                                        haptics.selection()
                                        onThemeModeChange?.invoke(mode)
                                        themeMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                },
            )
            Column(modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 28.dp)) {
                Text(
                    "Código personal",
                    color = colors.muted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
                Text(
                    displayName.ifBlank { "Tu cuenta" },
                    color = colors.ink,
                    fontSize = 36.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1.6).sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (email.isNotBlank()) {
                    Text(
                        email,
                        color = colors.muted,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(32.dp))
                            .background(colors.paper2)
                            .border(1.dp, colors.line, RoundedCornerShape(32.dp))
                            .padding(24.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        Text(
                            "Para recargar saldo",
                            color = colors.ink,
                            fontSize = 24.sp,
                            lineHeight = 32.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.7).sp,
                        )
                        Text(
                            "Muestra este código en Caja para encontrar tu cuenta.",
                            color = colors.muted,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
                        )
                        Surface(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = 16.dp,
                                        shape = RoundedCornerShape(24.dp),
                                        ambientColor = Color(0x17171717),
                                        spotColor = Color(0x17171717),
                                    ),
                            color = Color.White,
                            shape = RoundedCornerShape(24.dp),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                if (qrValue != null) {
                                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                        VaiinillaQrCode(
                                            value = qrValue,
                                            qrSize = maxWidth,
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.padding(top = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Box(
                                            modifier =
                                                Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(colors.accent.copy(alpha = 0.22f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Box(
                                                modifier =
                                                    Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(colors.accent),
                                            )
                                        }
                                        Text(
                                            "Listo para escanear",
                                            color = colors.muted,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.4.sp,
                                        )
                                    }
                                } else {
                                    Text(
                                        "Inicia sesión para ver tu código.",
                                        color = colors.muted,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (hasStaffModes) {
                    EditorialAccentButton(text = "Cambiar a modo personal", onClick = onOpenStaffModes)
                    EditorialPrimaryButton(
                        text = "Cambiar de tienda",
                        onClick = onChangeVenue,
                        background = colors.paper2,
                        contentColor = colors.ink,
                    )
                } else {
                    EditorialAccentButton(text = "Cambiar de tienda", onClick = onChangeVenue)
                }
                if (signedIn) {
                    EditorialPrimaryButton(
                        text = "Cerrar sesión",
                        onClick = onSignOut,
                        background = colors.paper2,
                        contentColor = colors.ink,
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        color = colors.coral.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(22.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("Eliminar cuenta", color = colors.ink, fontSize = 19.sp, fontWeight = FontWeight.Black)
                            Text(
                                "Esta acción eliminará tu acceso y tus datos personales de Vaiinilla. " +
                                    "Algunos registros de pedidos, pagos y aceptación de términos se conservarán " +
                                    "de forma anónima por motivos legales y contables. Esta acción no se puede deshacer.",
                                color = colors.muted,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                            )
                            EditorialPrimaryButton(
                                text = "Eliminar cuenta",
                                onClick = onRequestAccountDeletion,
                                background = colors.coral,
                                contentColor = colors.paper,
                            )
                        }
                    }
                } else {
                    EditorialPrimaryButton(text = "Iniciar sesión", onClick = onSignIn)
                }
            }
            Text(
                "También pueden buscarte por nombre en Caja.",
                color = colors.muted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
            )
        }

        when (val status = accountDeletionState.status) {
            AccountDeletionStatus.Idle,
            AccountDeletionStatus.Success,
            -> Unit
            AccountDeletionStatus.Confirmation ->
                AccountDeletionConfirmationDialog(
                    onConfirm = onConfirmAccountDeletion,
                    onDismiss = onCancelAccountDeletion,
                )
            is AccountDeletionStatus.Reauthentication ->
                AccountDeletionReauthenticationDialog(
                    state = accountDeletionState,
                    busy = status.busy,
                    onSubmit = onSubmitAccountDeletionPassword,
                    onDismiss = onCancelAccountDeletion,
                )
            AccountDeletionStatus.Deleting -> AccountDeletionProgressDialog()
            is AccountDeletionStatus.RecoverableError ->
                AccountDeletionErrorDialog(
                    message = accountDeletionState.errorMessage.orEmpty(),
                    retryable = status.retryable,
                    onRetry = onRetryAccountDeletion,
                    onDismiss = onCancelAccountDeletion,
                )
        }
    }
}

@Composable
private fun AccountDeletionConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = colors.paper, shape = RoundedCornerShape(28.dp)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "¿Eliminar definitivamente tu cuenta?",
                    color = colors.ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    "Perderás el acceso a todos tus establecimientos, pedidos, saldo y funciones de Vaiinilla. Esta acción es permanente.",
                    color = colors.muted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                EditorialPrimaryButton(
                    text = "Eliminar mi cuenta",
                    onClick = onConfirm,
                    background = colors.coral,
                    contentColor = colors.paper,
                )
                EditorialPrimaryButton(
                    text = "Cancelar",
                    onClick = onDismiss,
                    background = colors.paper2,
                    contentColor = colors.ink,
                )
            }
        }
    }
}

@Composable
private fun AccountDeletionReauthenticationDialog(
    state: AccountDeletionUiState,
    busy: Boolean,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    var password by remember { mutableStateOf("") }
    androidx.compose.runtime.LaunchedEffect(state.status is AccountDeletionStatus.Reauthentication) {
        if (state.status !is AccountDeletionStatus.Reauthentication) password = ""
    }
    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !busy, dismissOnClickOutside = !busy),
    ) {
        Surface(color = colors.paper, shape = RoundedCornerShape(28.dp)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Confirma tu identidad", color = colors.ink, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(
                    "Por seguridad, vuelve a ingresar tu contraseña antes de eliminar la cuenta.",
                    color = colors.muted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                AuthAccessField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Contraseña",
                    placeholder = "Tu contraseña",
                    kind = AuthAccessFieldKind.Password,
                    error = state.errorMessage,
                    onImeAction = { if (!busy) onSubmit(password) },
                )
                EditorialPrimaryButton(
                    text = "Continuar",
                    onClick = { onSubmit(password) },
                    enabled = !busy,
                    background = colors.coral,
                    contentColor = colors.paper,
                )
                EditorialPrimaryButton(
                    text = "Cancelar",
                    onClick = onDismiss,
                    enabled = !busy,
                    background = colors.paper2,
                    contentColor = colors.ink,
                )
            }
        }
    }
}

@Composable
private fun AccountDeletionProgressDialog() {
    val colors = LocalVaiinillaColors.current
    Dialog(onDismissRequest = {}) {
        Surface(color = colors.paper, shape = RoundedCornerShape(28.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CircularProgressIndicator(color = colors.coral)
                Text("Eliminando tu cuenta…", color = colors.ink, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun AccountDeletionErrorDialog(
    message: String,
    retryable: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = colors.paper, shape = RoundedCornerShape(28.dp)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "No pudimos eliminar tu cuenta",
                    color = colors.ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(message, color = colors.muted, fontSize = 14.sp, lineHeight = 20.sp)
                if (retryable) {
                    EditorialPrimaryButton(
                        text = "Reintentar",
                        onClick = onRetry,
                        background = colors.coral,
                        contentColor = colors.paper,
                    )
                }
                EditorialPrimaryButton(
                    text = "Cancelar",
                    onClick = onDismiss,
                    background = colors.paper2,
                    contentColor = colors.ink,
                )
            }
        }
    }
}

@Preview(name = "Mi cuenta · claro", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun WalletAccountScreenLightPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        WalletAccountScreen(
            onBack = {},
            displayName = "David Ramirez",
            email = "keinkao@gmail.com",
            userId = "u-preview",
            signedIn = true,
        )
    }
}

@Preview(name = "Mi cuenta · oscuro", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun WalletAccountScreenDarkPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Dark) {
        WalletAccountScreen(
            onBack = {},
            displayName = "David Ramirez",
            email = "keinkao@gmail.com",
            userId = "u-preview",
            signedIn = true,
        )
    }
}
