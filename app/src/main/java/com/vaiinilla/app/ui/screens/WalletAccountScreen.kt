package com.vaiinilla.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vaiinilla.app.domain.guardianship.PersonNames
import com.vaiinilla.app.ui.account.AccountDeletionStatus
import com.vaiinilla.app.ui.account.AccountDeletionUiState
import com.vaiinilla.app.ui.components.AuthAccessField
import com.vaiinilla.app.ui.components.AuthAccessFieldKind
import com.vaiinilla.app.ui.components.EditorialPrimaryButton
import com.vaiinilla.app.ui.components.EditorialSecondaryButton
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.TotpMfaChallengeForm
import com.vaiinilla.app.ui.components.WalletScreenShell
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
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
    venueName: String? = null,
    hasStaffModes: Boolean = false,
    onOpenStaffModes: () -> Unit = {},
    onChangeVenue: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onOpenNotices: (() -> Unit)? = null,
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
    deletionWarnings: AccountDeletionWarnings = AccountDeletionWarnings(),
    termsUrl: String = "",
    privacyUrl: String = "",
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val currentMode = LocalVaiinillaThemeMode.current
    val onThemeModeChange = LocalVaiinillaThemeModeChanger.current
    var qrOpen by remember { mutableStateOf(false) }
    var confirmingSignOut by remember { mutableStateOf(false) }
    if (confirmingSignOut) {
        SignOutConfirmDialog(onConfirm = onSignOut, onDismiss = { confirmingSignOut = false })
    }
    val uriHandler = LocalUriHandler.current

    val qrValue = userId?.trim()?.takeIf { it.isNotEmpty() }?.let(QrPayloadParser::encodeUser)
    if (qrOpen) WalletQrDialog(qrValue = qrValue, onDismiss = { qrOpen = false })
    WalletScreenShell(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp, bottom = 28.dp),
        ) {
            AccountRoundButton(
                contentDescription = "Regresar",
                onClick = {
                    haptics.click()
                    onBack()
                },
            )
            AccountHeader(
                displayName = displayName,
                email = email,
                modifier = Modifier.padding(start = 4.dp, top = 10.dp).arrive(index = 0),
            )
            AccountCard(modifier = Modifier.padding(top = 20.dp).arrive(index = 1)) {
                if (signedIn) {
                    AccountRow(
                        title = "Mi código",
                        subtitle = if (qrValue != null) "Para recargar o cobrar en Caja" else "Sale cuando cargue tu saldo",
                        icon = Icons.Outlined.QrCode2,
                        enabled = qrValue != null,
                        onClick = {
                            haptics.click()
                            qrOpen = true
                        },
                    )
                    AccountDivider()
                }
                AccountRow(
                    title = venueName?.takeIf { it.isNotBlank() } ?: "Elige tu cafetería",
                    subtitle = if (venueName.isNullOrBlank()) "Aún no eliges una" else "Tu cafetería · cambiar",
                    icon = Icons.Outlined.Storefront,
                    onClick = {
                        haptics.click()
                        onChangeVenue()
                    },
                )
                if (signedIn && hasStaffModes) {
                    AccountDivider()
                    AccountRow(
                        title = "Modos de personal",
                        subtitle = staffModesLine(deletionWarnings.staffRoles, venueName),
                        icon = Icons.Outlined.Badge,
                        onClick = onOpenStaffModes,
                    )
                }
            }
            AccountCap("Apariencia", modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 10.dp).arrive(index = 2))
            ThemeTiles(
                current = currentMode,
                onSelect = { mode ->
                    haptics.selection()
                    onThemeModeChange?.invoke(mode)
                },
                modifier = Modifier.arrive(index = 2),
            )
            AccountCard(modifier = Modifier.padding(top = 20.dp).arrive(index = 3)) {
                if (signedIn && onOpenNotices != null) {
                    AccountRow(
                        title = "Avisos recibidos",
                        icon = Icons.Outlined.NotificationsNone,
                        onClick = {
                            haptics.click()
                            onOpenNotices()
                        },
                    )
                    AccountDivider()
                }
                AccountRow(
                    title = "Ayuda y soporte",
                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                    onClick = onOpenSupport,
                )
                LegalRows(termsUrl = termsUrl, privacyUrl = privacyUrl)
            }
            if (signedIn) {
                AccountPillButton(
                    text = "Cerrar sesión",
                    onClick = { confirmingSignOut = true },
                    modifier = Modifier.padding(top = 20.dp).arrive(index = 4),
                )
                Text(
                    "Eliminar mi cuenta",
                    color = AccountDangerText,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier =
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 6.dp)
                            .clip(CircleShape)
                            .physicalPress(scale = PhysicalPressScale.Small, onClick = onRequestAccountDeletion)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .arrive(index = 5),
                )
                TextButton(
                    onClick = { uriHandler.openUri("https://app.vaiinilla.app/eliminar-cuenta") },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(
                        text = "O pide que la borren desde la web",
                        color = colors.muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            } else {
                EditorialPrimaryButton(
                    text = "Iniciar sesión",
                    onClick = onSignIn,
                    modifier = Modifier.padding(top = 20.dp),
                )
            }
        }

        AccountDeletionSheet(
            visible = accountDeletionState.status == AccountDeletionStatus.Confirmation,
            warnings = deletionWarnings,
            onConfirm = onConfirmAccountDeletion,
            onDismiss = onCancelAccountDeletion,
        )
        when (val status = accountDeletionState.status) {
            AccountDeletionStatus.Idle,
            AccountDeletionStatus.Success,
            AccountDeletionStatus.Confirmation,
            -> Unit
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

private val AccountAvatarFill = Color(0xFFC9A8FF)
private val AccountAvatarInk = Color(0xFF2A1748)
private val AccountDangerText = Color(0xFFFF8B86)

/** «Caja y Cocina en Pastelería Vaini»; sin modos conocidos, lo que puede ser. */
internal fun staffModesLine(
    roles: List<String>,
    venueName: String?,
): String {
    if (roles.isEmpty()) return "Caja, cocina o mesero"
    val place = venueName?.takeIf { it.isNotBlank() }?.let { " en $it" }.orEmpty()
    return joinSpanish(roles) + place
}

/** Sin nombre propio la cuenta trae su correo como nombre: entonces se dice «Tu cuenta» y no el correo dos veces. */
internal fun accountTitle(
    displayName: String,
    email: String,
): String = displayName.trim().takeIf { it.isNotEmpty() && it != email.trim() && '@' !in it } ?: "Tu cuenta"

@Composable
private fun AccountHeader(
    displayName: String,
    email: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val title = accountTitle(displayName, email)
    val hasName = title != "Tu cuenta"
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(60.dp).clip(CircleShape).background(AccountAvatarFill),
            contentAlignment = Alignment.Center,
        ) {
            if (hasName) {
                Text(PersonNames.initials(title), color = AccountAvatarInk, fontSize = 22.sp, fontWeight = FontWeight.Black)
            } else {
                Icon(Icons.Rounded.Person, contentDescription = null, tint = AccountAvatarInk, modifier = Modifier.size(28.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = colors.ink,
                fontSize = 24.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.9).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (email.isNotBlank()) {
                Text(
                    email,
                    color = colors.muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AccountRoundButton(
    contentDescription: String,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Box(
        modifier =
            Modifier
                .size(40.dp)
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
                .clip(CircleShape)
                .background(colors.paper2)
                .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = colors.ink, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun AccountCap(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text.uppercase(),
        color = LocalVaiinillaColors.current.muted,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 2.2.sp,
        modifier = modifier,
    )
}

@Composable
private fun AccountCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(LocalVaiinillaColors.current.paper2),
        content = content,
    )
}

@Composable
private fun AccountDivider() = HorizontalDivider(color = LocalVaiinillaColors.current.line, thickness = 1.dp)

/** Una fila de Cuenta (Doop E12): ícono en su cuadro, título, la línea de abajo y la flecha. */
@Composable
private fun AccountRow(
    title: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    subtitle: String? = null,
    enabled: Boolean = true,
    trailingRotation: Float = 0f,
    indent: Boolean = false,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .physicalPress(enabled = enabled, scale = PhysicalPressScale.Small, onClick = onClick)
                .alpha(if (enabled) 1f else 0.5f)
                .padding(start = if (indent) 62.dp else 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(walletTile(colors)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = colors.ink2, modifier = Modifier.size(18.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            subtitle?.let {
                Text(it, color = colors.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.muted,
            modifier = Modifier.size(18.dp).rotate(trailingRotation),
        )
    }
}

@Composable
private fun AccountPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = LocalVaiinillaColors.current.paper2,
    contentColor: Color = LocalVaiinillaColors.current.ink,
    height: Int = 52,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height.dp)
                .physicalPress(onClick = onClick)
                .clip(CircleShape)
                .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = contentColor, fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold)
    }
}

private data class ThemeTileLook(
    val fill: Color,
    val label: Color,
    val restBorder: Color,
)

/** Apariencia en cuatro muestras (Doop E12): la elegida lleva el borde lima. */
@Composable
private fun ThemeTiles(
    current: VaiinillaThemeMode,
    onSelect: (VaiinillaThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val reduced = reducedMotion()
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        VaiinillaThemeMode.entries.forEach { mode ->
            val look =
                when (mode) {
                    VaiinillaThemeMode.System -> ThemeTileLook(ThemeTileDark, ThemeTileDark, colors.line)
                    VaiinillaThemeMode.Light -> ThemeTileLook(ThemeTileCream, ThemeTileDark, colors.line)
                    VaiinillaThemeMode.Dark -> ThemeTileLook(Color(0xFF2A2B27), Color(0xFFDEDBD0), colors.line)
                    VaiinillaThemeMode.Amoled -> ThemeTileLook(Color.Black, Color(0xFFDEDBD0), Color(0xFF33342F))
                }
            val selected = mode == current
            val border by animateColorAsState(
                targetValue = if (selected) colors.accent else look.restBorder,
                animationSpec = if (reduced) snap() else spring(stiffness = 500f),
                label = "theme-tile-border",
            )
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(64.dp)
                        .physicalPress(scale = PhysicalPressScale.Small, onClick = { onSelect(mode) })
                        .clip(RoundedCornerShape(14.dp))
                        .drawBehind {
                            drawRect(look.fill)
                            if (mode == VaiinillaThemeMode.System) {
                                val half =
                                    Path().apply {
                                        moveTo(0f, 0f)
                                        lineTo(size.width, 0f)
                                        lineTo(0f, size.height)
                                        close()
                                    }
                                drawPath(half, ThemeTileCream)
                            }
                        }.border(2.dp, border, RoundedCornerShape(14.dp))
                        .semantics {
                            this.selected = selected
                            role = Role.RadioButton
                        }.padding(8.dp),
                contentAlignment = Alignment.BottomStart,
            ) {
                val name = themeTileName(mode)
                if (mode == VaiinillaThemeMode.System) {
                    Text(
                        name,
                        color = look.label,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ThemeTileCream.copy(alpha = 0.9f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                } else {
                    Text(
                        name,
                        color = look.label,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 11.5.sp, stepSize = 0.5.sp),
                    )
                }
            }
        }
    }
}

private val ThemeTileCream = Color(0xFFF5F1E5)
private val ThemeTileDark = Color(0xFF1D1E1C)

/** El nombre corto de la muestra: «Automático» no cabe en una de cuatro. */
internal fun themeTileName(mode: VaiinillaThemeMode): String =
    if (mode == VaiinillaThemeMode.System) "Auto" else themeModeName(mode)
/** Lo que se pierde al borrar la cuenta, para decirlo antes y no después (Doop E13). */
data class AccountDeletionWarnings(
    val balance: String? = null,
    val venueName: String? = null,
    val principalChildren: List<String> = emptyList(),
    val staffRoles: List<String> = emptyList(),
)

internal fun accountDeletionPoints(warnings: AccountDeletionWarnings): List<String> =
    buildList {
        if (warnings.principalChildren.isNotEmpty()) {
            val names = joinSpanish(warnings.principalChildren)
            val verb = if (warnings.principalChildren.size == 1) "te tiene" else "te tienen"
            add("$names $verb como tutor principal. Pasa a otro tutor como principal o devuelve su saldo primero.")
        }
        add("Pierdes tus pedidos, tus avisos y tus tarjetas guardadas.")
        if (warnings.staffRoles.isNotEmpty()) add("Pierdes tus modos de ${joinSpanish(warnings.staffRoles)}.")
    }

/** El saldo solo se avisa si queda algo: con $0 no hay nada que perder. */
internal fun accountDeletionBalance(visibleBalance: String?): String? =
    visibleBalance?.trim()?.toBigDecimalOrNull()?.takeIf { it.signum() > 0 }?.let { moneyLabel(it.toPlainString()) }

private fun joinSpanish(items: List<String>): String =
    when (items.size) {
        0 -> ""
        1 -> items.single()
        else -> items.dropLast(1).joinToString(", ") + " y " + items.last()
    }

/**
 * «Términos y privacidad» en una fila (Doop E12): abre los dos documentos vigentes debajo. Con uno solo, lo abre
 * directo; sin dirección no se muestra.
 */
@Composable
private fun LegalRows(
    termsUrl: String,
    privacyUrl: String,
) {
    val uriHandler = LocalUriHandler.current
    val reduced = reducedMotion()
    val rows = listOf("Términos de uso" to termsUrl, "Aviso de privacidad" to privacyUrl).filter { it.second.isNotBlank() }
    if (rows.isEmpty()) return
    var open by remember { mutableStateOf(false) }
    val turn by animateFloatAsState(
        targetValue = if (open) 90f else 0f,
        animationSpec = if (reduced) snap() else spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "legal-chevron",
    )
    AccountDivider()
    AccountRow(
        title = "Términos y privacidad",
        icon = Icons.Outlined.Description,
        trailingRotation = if (rows.size > 1) turn else 0f,
        onClick = {
            if (rows.size == 1) runCatching { uriHandler.openUri(rows.single().second) } else open = !open
        },
    )
    AnimatedVisibility(
        visible = open && rows.size > 1,
        enter = if (reduced) fadeIn(snap()) else expandVertically(spring(dampingRatio = 0.85f, stiffness = 420f)) + fadeIn(),
        exit = if (reduced) fadeOut(snap()) else shrinkVertically(spring(stiffness = 500f)) + fadeOut(),
    ) {
        Column {
            rows.forEach { (title, url) ->
                AccountDivider()
                AccountRow(title = title, indent = true, onClick = { runCatching { uriHandler.openUri(url) } })
            }
        }
    }
}

/** Eliminar tu cuenta (Doop E13): lo que se pierde, antes de pedir la contraseña. */
@Composable
private fun AccountDeletionSheet(
    visible: Boolean,
    warnings: AccountDeletionWarnings,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    WalletBottomSheet(visible = visible, onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 24.dp),
        ) {
            Text(
                "Eliminar tu cuenta",
                color = colors.ink,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.1).sp,
            )
            Text(
                "No se puede deshacer.",
                color = colors.muted,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (warnings.balance != null) {
                Column(
                    modifier =
                        Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.yolk.copy(alpha = 0.14f))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        "Aún tienes ${warnings.balance} de saldo",
                        color = if (colors.isDark) colors.yolk else Color(0xFF7A5A00),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                    )
                    val caja = warnings.venueName?.let { "la Caja de $it" } ?: "Caja"
                    Text(
                        "Gástalo o pide en $caja que te lo regresen antes. Al eliminar la cuenta se pierde.",
                        color = colors.ink2,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Column(modifier = Modifier.padding(top = 18.dp, start = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                accountDeletionPoints(warnings).forEach { point ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier.padding(top = 8.dp).size(6.dp).clip(CircleShape).background(colors.muted),
                        )
                        Text(point, color = colors.ink2, fontSize = 14.5.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            AccountPillButton(
                text = "Eliminar para siempre",
                onClick = onConfirm,
                background = colors.coral,
                contentColor = Color.White,
                height = 54,
                modifier = Modifier.padding(top = 24.dp),
            )
            AccountPillButton(
                text = "Mejor no",
                onClick = onDismiss,
                modifier = Modifier.padding(top = 10.dp),
            )
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

/** Salir pide confirmación, como en iOS (W04; QAIOS-17): un toque de más no debe cerrar la sesión. */
@Composable
internal fun SignOutConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) = com.vaiinilla.app.ui.parentmode.ParentConfirmDialog(
    title = "¿Cerrar sesión?",
    message = "Vas a salir de tu cuenta en este teléfono. Para volver tendrás que entrar de nuevo.",
    confirmLabel = "Cerrar sesión",
    destructive = true,
    onConfirm = onConfirm,
    onDismiss = onDismiss,
)
