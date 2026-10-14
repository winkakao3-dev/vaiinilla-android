package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode

/**
 * El avatar del personal abre un menú corto: el tema, cambiar de modo y, en Caja con la caja abierta,
 * cerrarla. Así el encabezado queda en el lugar a la izquierda, Vaini (la ayuda) y el avatar.
 */
@Composable
internal fun StaffAvatarMenu(
    initials: String,
    colors: OperationalColors,
    themeMode: VaiinillaThemeMode,
    onThemeChange: ((VaiinillaThemeMode) -> Unit)?,
    onChangeMode: () -> Unit,
    onCloseCash: (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier =
                Modifier
                    .physicalPress(scale = PhysicalPressScale.Small) { expanded = true }
                    .semantics { contentDescription = "Menú de la cuenta" }
                    .size(width = 48.dp, height = 40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.textPrimary),
            contentAlignment = Alignment.Center,
        ) {
            Text(initials, color = colors.background, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(16.dp),
        ) {
            if (onThemeChange != null) {
                Text(
                    "Tema",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
                VaiinillaThemeMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(themeModeName(mode), color = colors.textPrimary) },
                        trailingIcon = {
                            if (mode == themeMode) {
                                Icon(Icons.Outlined.Check, contentDescription = "Elegido", tint = colors.textPrimary)
                            }
                        },
                        onClick = { onThemeChange(mode) },
                    )
                }
                HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(vertical = 4.dp))
            }
            DropdownMenuItem(
                text = { Text("Cambiar de modo", color = colors.textPrimary, fontWeight = FontWeight.SemiBold) },
                onClick = {
                    expanded = false
                    onChangeMode()
                },
            )
            if (onCloseCash != null) {
                DropdownMenuItem(
                    text = { Text("Cerrar caja", color = colors.textPrimary, fontWeight = FontWeight.SemiBold) },
                    onClick = {
                        expanded = false
                        onCloseCash()
                    },
                )
            }
        }
    }
}
