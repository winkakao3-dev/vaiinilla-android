package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vaiinilla.app.ui.account.AccountDeletionStatus
import com.vaiinilla.app.ui.account.AccountDeletionUiState
import com.vaiinilla.app.ui.components.AuthAccessField
import com.vaiinilla.app.ui.components.AuthAccessFieldKind
import com.vaiinilla.app.ui.components.EditorialPrimaryButton
import com.vaiinilla.app.ui.components.EditorialSecondaryButton
import com.vaiinilla.app.ui.components.SettingsGroup
import com.vaiinilla.app.ui.components.SettingsRow
import com.vaiinilla.app.ui.components.SettingsTrailing
import com.vaiinilla.app.ui.components.TotpMfaChallengeForm
import com.vaiinilla.app.ui.components.WalletScreenShell
import com.vaiinilla.app.ui.components.WalletSubflowTopBar
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.discovery.QrPayloadParser
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.LocalVaiinillaThemeMode
import com.vaiinilla.app.ui.theme.LocalVaiinillaThemeModeChanger
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode

/** Nombres de tema en palabras de todos los días; "Amoled" es jerga de pantallas. */
internal fun themeModeName(mode: VaiinillaThemeMode): String =
    when (mode) {
        VaiinillaThemeMode.System -> "Automático"
        VaiinillaThemeMode.Light -> "Día"
        VaiinillaThemeMode.Dark -> "Noche"
        VaiinillaThemeMode.Amoled -> "Medianoche"
    }

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
    onOpenSupport: () -> Unit = {},
    accountDeletionState: AccountDeletionUiState = AccountDeletionUiState(),
    onRequestAccountDeletion: () -> Unit = {},
    onConfirmAccountDeletion: () -> Unit = {},
    onCancelAccountDeletion: () -> Unit = {},
    onSubmitAccountDeletionPassword: (String) -> Unit = {},
    onAccountDeletionMfaCodeChange: (String) -> Unit = {},
    onAccountDeletionMfaFactorSelected: (String) -> Unit = {},
    onSubmitAccountDeletionMfa: () -> Unit = {},
    onRetryAccountDeletion: () -> Unit = {},
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val currentMode = LocalVaiinillaThemeMode.current
    val onThemeModeChange = LocalVaiinillaThemeModeChanger.current
    var qrOpen by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    val qrValue = userId?.trim()?.takeIf { it.isNotEmpty() }?.let(QrPayloadParser::encodeUser)
    if (qrOpen) WalletQrDialog(qrValue = qrValue, onDismiss = { qrOpen = false })
    WalletScreenShell(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(bottom = 28.dp),
        ) {
            WalletSubflowTopBar(
                title = "Cuenta",
                onBack = {
                    haptics.click()
                    onBack()
                },
            )
            Column(
                modifier = Modifier.padding(horizontal = 20.dp).padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 4.dp).arrive(index = 0)) {
                    Text(
                        displayName.ifBlank { "Tu cuenta" },
                        color = colors.ink,
                        fontSize = 28.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.Black,
                    )
                    if (email.isNotBlank()) {
                        Text(email, color = colors.muted, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                if (signedIn) {
                    SettingsGroup(title = "Cuenta", modifier = Modifier.arrive(index = 1)) {
                        SettingsRow(
                            title = "Mi código",
                            subtitle = "Muéstralo en caja para recargar saldo",
                            icon = Icons.Outlined.QrCode2,
                            onClick = {
                                haptics.click()
                                qrOpen = true
                            },
                            showDivider = hasStaffModes,
                        )
                        if (hasStaffModes) {
                            SettingsRow(
                                title = "Modos de personal",
                                subtitle = "Caja, cocina o mesero",
                                icon = Icons.Outlined.Badge,
                                onClick = onOpenStaffModes,
                                showDivider = false,
                            )
                        }
                    }
                }
                SettingsGroup(title = "Cafetería", modifier = Modifier.arrive(index = 2)) {
                    SettingsRow(
                        title = "Cambiar de cafetería",
                        icon = Icons.Outlined.Storefront,
                        onClick = onChangeVenue,
                        showDivider = false,
                    )
                }
                SettingsGroup(title = "Apariencia", modifier = Modifier.arrive(index = 3)) {
                    val modes = VaiinillaThemeMode.entries
                    modes.forEachIndexed { index, mode ->
                        SettingsRow(
                            title = themeModeName(mode),
                            trailing = SettingsTrailing.Check,
                            checked = mode == currentMode,
                            onClick = {
                                haptics.selection()
                                onThemeModeChange?.invoke(mode)
                            },
                            showDivider = index != modes.lastIndex,
                        )
                    }
                }
                SettingsGroup(title = "Ayuda", modifier = Modifier.arrive(index = 4)) {
                    SettingsRow(
                        title = "Ayuda y soporte",
                        icon = Icons.AutoMirrored.Outlined.HelpOutline,
                        onClick = onOpenSupport,
                        showDivider = false,
                    )
                }
                if (signedIn) {
                    // Separado de lo demás: salir no es un ajuste más.
                    SettingsGroup(title = null, modifier = Modifier.arrive(index = 5)) {
                        SettingsRow(
                            title = "Cerrar sesión",
                            trailing = SettingsTrailing.None,
                            onClick = onSignOut,
                            showDivider = false,
                        )
                    }
                    Column(modifier = Modifier.arrive(index = 6)) {
                        SettingsGroup(title = null) {
                            SettingsRow(
                                title = "Eliminar cuenta",
                                titleColor = colors.coral,
                                trailing = SettingsTrailing.None,
                                onClick = onRequestAccountDeletion,
                                showDivider = false,
                            )
                        }
                        TextButton(
                            onClick = { uriHandler.openUri("https://app.vaiinilla.app/eliminar-cuenta") },
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            Text(
                                text = "O solicita la eliminación desde la web",
                                color = colors.muted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                } else {
                    EditorialPrimaryButton(text = "Iniciar sesión", onClick = onSignIn)
                }
            }
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
            is AccountDeletionStatus.MfaChallenge ->
                AccountDeletionMfaDialog(
                    status = status,
                    errorMessage = accountDeletionState.errorMessage,
                    onCodeChange = onAccountDeletionMfaCodeChange,
                    onFactorSelected = onAccountDeletionMfaFactorSelected,
                    onSubmit = onSubmitAccountDeletionMfa,
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
                    contentColor = colors.onInk,
                )
                EditorialSecondaryButton(
                    text = "Cancelar",
                    onClick = onDismiss,
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
                    contentColor = colors.onInk,
                )
                EditorialSecondaryButton(
                    text = "Cancelar",
                    onClick = onDismiss,
                    enabled = !busy,
                )
            }
        }
    }
}

@Composable
private fun AccountDeletionMfaDialog(
    status: AccountDeletionStatus.MfaChallenge,
    errorMessage: String?,
    onCodeChange: (String) -> Unit,
    onFactorSelected: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Dialog(
        onDismissRequest = { if (!status.busy) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !status.busy, dismissOnClickOutside = !status.busy),
    ) {
        Surface(color = colors.paper, shape = RoundedCornerShape(28.dp)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Confirma tu identidad", color = colors.ink, fontSize = 20.sp, fontWeight = FontWeight.Black)
                TotpMfaChallengeForm(
                    factors = status.challenge.factors,
                    selectedFactorUid = status.factorUid,
                    code = status.code,
                    loading = status.busy,
                    errorMessage = errorMessage,
                    onFactorSelected = onFactorSelected,
                    onCodeChange = onCodeChange,
                    onSubmit = onSubmit,
                    onCancel = onDismiss,
                    submitBackground = colors.coral,
                    submitContentColor = colors.paper,
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
                        contentColor = colors.onInk,
                    )
                }
                EditorialSecondaryButton(
                    text = "Cancelar",
                    onClick = onDismiss,
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
