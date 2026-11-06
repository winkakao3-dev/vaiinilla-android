package com.vaiinilla.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

/** Volver, en el círculo de 40 dp de las páginas del cliente del lienzo de Doop. */
@Composable
internal fun ClientBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Volver",
) {
    val colors = LocalVaiinillaColors.current
    Box(
        modifier =
            modifier
                .minimumInteractiveComponentSize()
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

/** Volver, el título grande y la línea de abajo (Renta tu cancha, Avisos recibidos, Ayuda). */
@Composable
internal fun ClientPageHeader(
    title: String,
    subtitle: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Column(modifier = modifier) {
        ClientBackButton(onClick = onBack)
        Text(
            title,
            color = colors.ink,
            fontSize = 30.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1.2).sp,
            modifier = Modifier.padding(start = 4.dp, top = 10.dp),
        )
        if (!subtitle.isNullOrEmpty()) {
            Text(
                subtitle,
                color = colors.muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
            )
        }
    }
}

/** La etiqueta en mayúsculas espaciadas que abre cada grupo («PENDIENTES», «TEMAS»). */
@Composable
internal fun ClientCap(
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

/** Un grupo de filas en una tarjeta de 22 dp. */
@Composable
internal fun ClientCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(LocalVaiinillaColors.current.paper2),
        content = content,
    )
}

@Composable
internal fun ClientCardDivider() = HorizontalDivider(color = LocalVaiinillaColors.current.line, thickness = 1.dp)

/** El asa de las hojas del cliente (¿Compartes la mesa?, Llamar al mesero). */
@Composable
internal fun ClientSheetHandle() {
    Box(
        Modifier
            .padding(top = 10.dp)
            .size(width = 40.dp, height = 5.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(LocalVaiinillaColors.current.muted.copy(alpha = 0.3f)),
    )
}

/** El botón redondo de las hojas y páginas del cliente: tinta si es la acción principal, papel si no. */
@Composable
internal fun ClientPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    enabled: Boolean = true,
    height: Dp = 56.dp,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height)
                .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
                .physicalPress(enabled = enabled, scale = PhysicalPressScale.Small) {
                    haptics.click()
                    onClick()
                }.clip(RoundedCornerShape(percent = 50))
                .background(if (primary) colors.ink else colors.paper2)
                .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (primary) colors.paper else colors.ink,
            fontSize = if (primary) 16.5.sp else 15.5.sp,
            fontWeight = if (primary) FontWeight.Black else FontWeight.ExtraBold,
        )
    }
}