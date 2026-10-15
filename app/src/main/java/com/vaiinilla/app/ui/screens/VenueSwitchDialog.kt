package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.vaiinilla.app.ui.components.EditorialAccentButton
import com.vaiinilla.app.ui.components.EditorialSecondaryButton
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

/**
 * Va encima de cualquier pantalla y no solo de Descubrir: el QR de otra cafetería también llega con el menú
 * o el carrito abiertos, y ahí la pregunta no se veía y el QR no hacía nada.
 */
@Composable
fun VenueSwitchDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = colors.paper, shape = RoundedCornerShape(28.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("¿Cambiar de cafetería?", color = colors.ink, fontWeight = FontWeight.Black, fontSize = 19.sp)
                Text(
                    "Tu carrito pertenece a otra cafetería. Si continúas, no se mezclará con este pedido — " +
                        "quedará guardado por separado en la cafetería anterior.",
                    color = colors.muted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Spacer(Modifier.height(18.dp))
                EditorialAccentButton(text = "Cambiar", onClick = onConfirm)
                Spacer(Modifier.height(8.dp))
                EditorialSecondaryButton(text = "Cancelar", onClick = onDismiss)
            }
        }
    }
}
