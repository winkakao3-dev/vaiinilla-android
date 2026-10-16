package com.vaiinilla.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.screens.OperationalColors

/** Hoja de la administración: título, cerrar y el contenido con scroll. No se cierra mientras guarda. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdminSheet(
    title: String,
    colors: OperationalColors,
    busy: Boolean,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        sheetState = sheetState,
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 36.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier =
                        Modifier
                            .physicalPress(enabled = !busy, scale = PhysicalPressScale.Small, onClick = onDismiss)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colors.cardBackground)
                            .border(1.dp, colors.cardBorder, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Cerrar",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            content()
        }
    }
}

/** Tarjeta con borde de las pestañas de administración. */
@Composable
internal fun AdminCard(
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.cardBackground)
                .border(1.dp, colors.cardBorder, shape)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
internal fun AdminSectionTitle(
    text: String,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
) {
    Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary, modifier = modifier)
}

/** Una fila "etiqueta ······ valor" de las listas del resumen y del detalle. */
@Composable
internal fun AdminValueRow(
    label: String,
    value: String,
    colors: OperationalColors,
    detail: String? = null,
    strong: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                fontSize = 14.sp,
                fontWeight = if (strong) FontWeight.Bold else FontWeight.SemiBold,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (detail != null) Text(detail, fontSize = 12.sp, color = colors.textSecondary)
        }
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
    }
}

/** Texto de error con reintento, para cuando una pestaña no pudo cargar. */
@Composable
internal fun AdminLoadError(
    message: String,
    colors: OperationalColors,
    onRetry: () -> Unit,
) {
    AdminCard(colors = colors) {
        Text(message, fontSize = 14.sp, color = colors.textPrimary)
        Text(
            "Reintentar",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            modifier =
                Modifier
                    .physicalPress(
                        scale = PhysicalPressScale.Small,
                        onClick = onRetry,
                    ).padding(vertical = 4.dp),
        )
    }
}
