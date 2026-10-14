package com.vaiinilla.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    /** Acción secundaria (p. ej. limpiar una búsqueda): texto en tinta, sin el lima de la acción principal. */
    quietAction: Boolean = false,
) {
    val colors = LocalVaiinillaColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.paper2,
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Ícono neutro: un estado vacío no es una acción, el color se queda para el botón.
            Surface(color = colors.paper, shape = RoundedCornerShape(25.dp)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.muted,
                    modifier = Modifier.padding(18.dp).size(36.dp),
                )
            }
            Text(
                text = title,
                color = colors.ink,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = message,
                color = colors.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (actionLabel != null && onAction != null && quietAction) {
                TextButton(onClick = onAction, modifier = Modifier.padding(top = 10.dp)) {
                    Text(actionLabel, color = colors.ink, fontWeight = FontWeight.Black)
                }
            } else if (actionLabel != null && onAction != null) {
                EditorialAccentButton(
                    text = actionLabel,
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                )
            }
        }
    }
}
