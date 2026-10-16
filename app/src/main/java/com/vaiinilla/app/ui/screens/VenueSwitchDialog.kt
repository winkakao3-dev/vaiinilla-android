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
    /**
     * El mismo lugar, otra mesa (o de para llevar a una mesa): cada mesa tiene su carrito, así que también se
     * pregunta, pero no es "otra cafetería". [nextSpaceName] es la mesa a la que entra; `null` es para llevar.
     */
    sameVenue: Boolean = false,
    nextSpaceName: String? = null,
) {
    val colors = LocalVaiinillaColors.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = colors.paper, shape = RoundedCornerShape(28.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                val title =
                    when {
                        !sameVenue -> "¿Cambiar de cafetería?"
                        nextSpaceName != null -> "¿Pedir en $nextSpaceName?"
                        else -> "¿Pedir para llevar?"
                    }
                val body =
                    if (sameVenue) {
                        "Cada mesa tiene su propio carrito. Lo que ya elegiste se queda guardado donde estabas y " +
                            "aquí empiezas uno nuevo."
                    } else {
                        "Tu carrito pertenece a otra cafetería. Si continúas, no se mezclará con este pedido — " +
                            "quedará guardado por separado en la cafetería anterior."
                    }
                Text(title, color = colors.ink, fontWeight = FontWeight.Black, fontSize = 19.sp)
                Text(
                    body,
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
