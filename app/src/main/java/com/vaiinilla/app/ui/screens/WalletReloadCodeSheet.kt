package com.vaiinilla.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.VaiinillaQrCode
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

private val ReloadQrCream = Color(0xFFF5F1E5)
private val ReloadQrInk = Color(0xFF1D1E1C)

/**
 * Recargar saldo (Doop E03): la hoja con el código para Caja y los tres pasos. «Con tarjeta» se ve como lo que viene
 * (la recarga con tarjeta en la cartera propia espera al siguiente ciclo, decisión del 25 oct).
 */
@Composable
internal fun WalletReloadCodeSheet(
    visible: Boolean,
    qrValue: String?,
    holderName: String?,
    venueName: String?,
    onClose: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    ScreenBrightnessBoost(active = visible)
    WalletBottomSheet(visible = visible, onDismiss = onClose) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Recargar saldo",
                color = colors.ink,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.1).sp,
            )
            Box(
                modifier =
                    Modifier
                        .minimumInteractiveComponentSize()
                        .size(36.dp)
                        .physicalPress(scale = PhysicalPressScale.Small, onClick = onClose)
                        .clip(CircleShape)
                        .background(colors.paper2)
                        .semantics { contentDescription = "Cerrar" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Close, contentDescription = null, tint = colors.ink2, modifier = Modifier.size(16.dp))
            }
        }
        ReloadMethodSwitch(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp))
        ReloadCode(
            qrValue = qrValue,
            holderName = holderName,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 20.dp),
        )
        Column(
            modifier =
                Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 22.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(colors.paper2)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ReloadStep(number = "1", text = "Dile a la cajera cuánto quieres recargar y paga en efectivo.")
            ReloadStep(number = "2", text = "Ella escanea este código.")
            ReloadStep(number = null, text = "El saldo te aparece aquí al momento, con su aviso.")
        }
        Text(
            listOfNotNull(
                "Sube el brillo si Caja no lo lee.",
                venueName?.takeIf { it.isNotBlank() }?.let { "Vale solo en $it." },
            ).joinToString(" "),
            color = colors.muted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 28.dp),
        )
    }
}

@Composable
private fun ReloadMethodSwitch(modifier: Modifier = Modifier) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier = modifier.fillMaxWidth().clip(CircleShape).background(colors.paper2).padding(4.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(CircleShape)
                    .background(colors.ink)
                    .semantics { selected = true },
            contentAlignment = Alignment.Center,
        ) {
            Text("En Caja", color = colors.onInk, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        }
        Row(
            modifier = Modifier.weight(1f).height(38.dp).semantics { disabled() },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Con tarjeta", color = colors.muted, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "PRONTO",
                color = colors.ink2,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.6.sp,
                modifier =
                    Modifier
                        .padding(start = 6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(walletTile(colors))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun ReloadCode(
    qrValue: String?,
    holderName: String?,
    modifier: Modifier = Modifier,
) {
    val reduced = reducedMotion()
    val pop = remember { Animatable(if (reduced) 1f else 0.88f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 380f)) }
    Column(
        modifier =
            modifier
                .graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                }.clip(RoundedCornerShape(26.dp))
                .background(ReloadQrCream)
                .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (qrValue != null) {
            VaiinillaQrCode(
                value = qrValue,
                qrSize = 250.dp,
                background = ReloadQrCream,
                foreground = ReloadQrInk,
                inset = 0.dp,
            )
        } else {
            Box(modifier = Modifier.size(250.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Tu código todavía no está disponible.",
                    color = ReloadQrInk,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
        // Sin nombre, la cuenta trae su correo como nombre: no se le enseña a Caja.
        holderName?.takeIf { it.isNotBlank() && '@' !in it }?.let {
            Text(
                it,
                color = Color(0xFF55564F),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

/** Un paso de la recarga; sin número, la palomita del final. */
@Composable
private fun ReloadStep(
    number: String?,
    text: String,
) {
    val colors = LocalVaiinillaColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier =
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (number == null) colors.accent else walletTile(colors)),
            contentAlignment = Alignment.Center,
        ) {
            if (number == null) {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.accentInk, modifier = Modifier.size(14.dp))
            } else {
                Text(number, color = colors.ink, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
        }
        Text(text, color = colors.ink, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
    }
}