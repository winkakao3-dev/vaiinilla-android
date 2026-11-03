package com.vaiinilla.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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