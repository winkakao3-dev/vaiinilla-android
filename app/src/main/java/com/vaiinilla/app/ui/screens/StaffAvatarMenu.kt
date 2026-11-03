package com.vaiinilla.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.notices.LocalOpenReceivedNotices
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode

/**
 * El avatar del personal abre la tarjeta de la cuenta del lienzo de Doop (S01): de quién es el turno, los avisos,
 * cambiar de modo, el tema y, en Caja con la caja abierta, cerrarla, en coral y al final para no tocarla sin querer.
 */
@Composable
internal fun StaffAvatarMenu(
    initials: String,
    colors: OperationalColors,
    themeMode: VaiinillaThemeMode,
    onThemeChange: ((VaiinillaThemeMode) -> Unit)?,
    onChangeMode: () -> Unit,
    onCloseCash: (() -> Unit)? = null,
    modeLabel: String = "",
    placeName: String = "",
    /** N11: Caja con la caja abierta ve los movimientos del cajón. */
    onOpenCashDrawer: (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    var shiftOpen by remember { mutableStateOf(false) }
    StaffAvatarBadge(
        initials = initials,
        modeLabel = modeLabel,
        modifier =
            Modifier
                .physicalPress(scale = PhysicalPressScale.Small) { expanded = true }
                .semantics { contentDescription = "Menú de la cuenta" },
    )
    if (expanded) {
        StaffAccountCard(
            initials = initials,
            colors = colors,
            themeMode = themeMode,
            onThemeChange = onThemeChange,
            modeLabel = modeLabel,
            placeName = placeName,
            onDismiss = { expanded = false },
            onChangeMode = {
                expanded = false
                onChangeMode()
            },
            onCloseCash =
                onCloseCash?.let { close ->
                    {
                        expanded = false
                        close()
                    }
                },
            onOpenShift = {
                expanded = false
                shiftOpen = true
            },
            onOpenCashDrawer =
                onOpenCashDrawer?.let { open ->
                    {
                        expanded = false
                        open()
                    }
                },
        )
    }
    if (shiftOpen) {
        StaffShiftSheet(modeLabel = modeLabel, placeName = placeName, colors = colors, onDismiss = { shiftOpen = false })
    }
}

/** Cada modo tiene su color de avatar, como en Doop: Caja rosa, Cocina naranja, Mesero azul. */
private fun avatarColors(modeLabel: String): Pair<Color, Color> =
    when (modeLabel) {
        "Caja" -> Color(0xFFE9B8C8) to Color(0xFF3A1622)
        "Cocina" -> Color(0xFFF4C98A) to Color(0xFF3A2410)
        "Mesero" -> Color(0xFFB8D4E9) to Color(0xFF10283A)
        else -> Color(0xFFD5E8A6) to Color(0xFF182008)
    }

@Composable
private fun StaffAvatarBadge(
    initials: String,
    modeLabel: String,
    modifier: Modifier = Modifier,
    size: Int = 44,
) {
    val (fill, ink) = avatarColors(modeLabel)
    Box(
        modifier = modifier.size(size.dp).clip(CircleShape).background(fill),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, color = ink, fontWeight = FontWeight.Black, fontSize = (size * 0.34f).sp)
    }
}

@Composable
private fun StaffAccountCard(
    initials: String,
    colors: OperationalColors,
    themeMode: VaiinillaThemeMode,
    onThemeChange: ((VaiinillaThemeMode) -> Unit)?,
    modeLabel: String,
    placeName: String,
    onDismiss: () -> Unit,
    onChangeMode: () -> Unit,
    onCloseCash: (() -> Unit)?,
    onOpenShift: () -> Unit,
    onOpenCashDrawer: (() -> Unit)?,
) {
    val reduceMotion = rememberReducedMotion()
    val appear = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow))
    }
    var themeOpen by remember { mutableStateOf(false) }
    val openNotices = LocalOpenReceivedNotices.current
    val coral = LocalVaiinillaColors.current.coral
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        onDismiss()
                    },
        ) {
            Column(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 64.dp, end = 16.dp)
                        .width(290.dp)
                        .graphicsLayer {
                            val p = appear.value
                            alpha = p.coerceIn(0f, 1f)
                            scaleX = 0.85f + 0.15f * p
                            scaleY = 0.85f + 0.15f * p
                            transformOrigin = TransformOrigin(1f, 0f)
                        }.shadow(24.dp, RoundedCornerShape(26.dp))
                        .clip(RoundedCornerShape(26.dp))
                        .background(colors.cardBackground)
                        // Toques dentro de la tarjeta no la cierran.
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                        .padding(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StaffAvatarBadge(initials, modeLabel)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            placeName.ifBlank { "Vaiinilla" },
                            color = colors.textPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (modeLabel.isNotBlank()) {
                            Text(modeLabel, color = colors.textSecondary, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                        }
                    }
                }
                HorizontalDivider(color = colors.cardBorder)
                Spacer(Modifier.height(6.dp))
                if (openNotices != null) {
                    AccountRow(Icons.Outlined.Notifications, "Avisos recibidos", colors = colors) {
                        onDismiss()
                        openNotices()
                    }
                }
                AccountRow(Icons.Outlined.Savings, "Mi turno y mis propinas", colors = colors, onClick = onOpenShift)
                AccountRow(Icons.Outlined.SwapHoriz, "Cambiar de modo", value = modeLabel, colors = colors, onClick = onChangeMode)
                if (onThemeChange != null) {
                    AccountRow(
                        Icons.Outlined.DarkMode,
                        "Tema",
                        value = themeModeName(themeMode),
                        colors = colors,
                        onClick = { themeOpen = !themeOpen },
                    )
                    AnimatedVisibility(
                        visible = themeOpen,
                        enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                        exit = shrinkVertically(spring(stiffness = Spring.StiffnessMedium)) + fadeOut(),
                    ) {
                        Column(modifier = Modifier.padding(start = 44.dp, bottom = 4.dp)) {
                            VaiinillaThemeMode.entries.forEach { mode ->
                                ThemeOption(mode, selected = mode == themeMode, colors = colors) { onThemeChange(mode) }
                            }
                        }
                    }
                }
                if (onOpenCashDrawer != null) {
                    AccountRow(Icons.Outlined.PointOfSale, "Movimientos del cajón", colors = colors, onClick = onOpenCashDrawer)
                }
                if (onCloseCash != null) {
                    AccountRow(Icons.Outlined.Lock, "Cerrar caja", tint = coral, colors = colors, onClick = onCloseCash)
                }
            }
        }
    }
}

@Composable
private fun AccountRow(
    icon: ImageVector,
    label: String,
    colors: OperationalColors,
    value: String = "",
    tint: Color = colors.textPrimary,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp)
                .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(label, color = tint, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (value.isNotBlank()) {
            Text(value, color = colors.textSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ThemeOption(
    mode: VaiinillaThemeMode,
    selected: Boolean,
    colors: OperationalColors,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
                .height(44.dp)
                .padding(horizontal = 12.dp)
                // TalkBack dice "seleccionado" en el tema actual, igual que en Cuenta.
                .semantics(mergeDescendants = true) {
                    this.selected = selected
                    this.role = Role.RadioButton
                },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            themeModeName(mode),
            color = if (selected) colors.textPrimary else colors.textSecondary,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
        )
        if (selected) Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
    }
}