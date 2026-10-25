package com.vaiinilla.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.repository.SpaceAvailability
import com.vaiinilla.app.domain.repository.SpaceAvailabilityState
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import kotlinx.coroutines.delay

/**
 * Las canchas dentro del menú: un encabezado con cuántas están libres y una fila de tarjetas que miden
 * una columna de la cuadrícula de comida, con su mismo hueco y esquinas; se desliza de orilla a orilla. Las ocupadas cuentan en vivo lo que les
 * falta y llevan una barra con el avance del turno. Tocar cualquier parte abre la renta.
 */
@Composable
fun CourtsCarousel(
    courts: List<SpaceAvailability>,
    modifier: Modifier = Modifier,
    /** Margen lateral de la pantalla: la fila lo cruza para llegar a la orilla. */
    bleed: Dp = 0.dp,
    /** Hueco entre columnas de la cuadrícula de abajo: cada tarjeta mide una columna, como las de comida. */
    columnGap: Dp = 14.dp,
    /** Abre la renta de canchas; sin él, las tarjetas solo informan. */
    onOpen: (() -> Unit)? = null,
) {
    if (courts.isEmpty()) return
    val colors = LocalVaiinillaColors.current
    val free = courts.count { it.isFreeNow }
    val anyRunning = courts.any { it.hasRunningTurn }
    // El reloj solo corre mientras alguna cancha tiene turno; si todas están libres no hay nada que contar.
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(anyRunning) {
        while (anyRunning) {
            delay(1_000)
            tick += 1
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                Text("Canchas", color = colors.ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Row(modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)) {
                    NumberTicker(value = free, label = "courts-free") { count ->
                        Text("$count", color = colors.muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        (if (free == 1) " libre" else " libres") + " de ${courts.size}",
                        color = colors.muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (onOpen != null) {
                Row(
                    modifier =
                        Modifier
                            .heightIn(min = 40.dp)
                            .clip(RoundedCornerShape(50))
                            .physicalPress(scale = PhysicalPressScale.Small, onClick = onOpen)
                            .padding(start = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Rentar", color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colors.ink,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val cardWidth = (maxWidth - columnGap) / 2
            // Una fila normal y no perezosa: son pocas canchas y así todas toman la altura de la más alta
            // aunque un nombre ocupe dos líneas.
            Row(
                modifier =
                    Modifier
                        .bleedHorizontally(bleed)
                        .height(IntrinsicSize.Min)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = bleed),
                horizontalArrangement = Arrangement.spacedBy(columnGap),
            ) {
                courts.forEachIndexed { index, court ->
                    key(court.space.id) {
                        CourtCard(
                            court = court,
                            tick = tick,
                            reserveBar = anyRunning,
                            modifier =
                                Modifier
                                    .width(cardWidth)
                                    .fillMaxHeight()
                                    .arrive(index + 1, key = court.space.id)
                                    .physicalPress(enabled = onOpen != null) { onOpen?.invoke() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CourtCard(
    court: SpaceAvailability,
    tick: Int,
    /** Deja el espacio de la barra aunque esta no tenga turno, para que todas midan lo mismo. */
    reserveBar: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    // Apartada, cuenta regresiva y avance dependen de la hora, no de un dato nuevo: se recalculan con el tick.
    tick.hashCode()
    val held = court.isHeldAt()
    val remaining = if (court.hasRunningTurn) millisUntil(court.releasesAt ?: court.endsAt) else null
    val progress =
        if (court.state == SpaceAvailabilityState.OCUPADA) turnFraction(court.startedAt, court.endsAt) else null
    val dot =
        animateSelectionColor(
            when {
                court.isFreeNow -> colors.accent
                else -> colors.coral
            },
            "court-card-dot",
        )

    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(24.dp))
                .background(colors.paper2)
                .padding(18.dp),
    ) {
        Text(
            court.space.name,
            color = colors.ink,
            fontSize = 17.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        court.pricePerHour?.let {
            Text(
                "${moneyLabel(it)} / hora",
                color = colors.ink.copy(alpha = 0.72f),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
            if (remaining != null && remaining > 0) {
                Text(
                    "Se libera en ",
                    color = colors.ink.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 6.dp),
                )
                RollingText(
                    text = clockText(remaining),
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    color = colors.ink,
                )
            } else {
                Text(
                    when {
                        court.isFreeNow -> "Libre ahora"
                        held && court.isWaitingPayment -> "Apartándose"
                        held -> "Apartada"
                        court.state == SpaceAvailabilityState.POR_COBRAR -> "Cerrando turno"
                        else -> "Por liberarse"
                    },
                    color = colors.ink.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
        if (progress != null) {
            TurnProgressBar(
                fraction = progress,
                track = colors.ink.copy(alpha = 0.1f),
                fill = colors.ink.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 10.dp),
                height = 4.dp,
            )
        } else if (reserveBar) {
            Spacer(Modifier.height(14.dp))
        }
    }
}

/** Hay un turno corriendo (o en sus minutos de gracia): la tarjeta cuenta lo que falta. */
private val SpaceAvailability.hasRunningTurn: Boolean
    get() = state == SpaceAvailabilityState.OCUPADA || state == SpaceAvailabilityState.EN_GRACIA

/** Ensancha el contenido `amount` a cada lado para que cruce el margen de la pantalla. */
private fun Modifier.bleedHorizontally(amount: Dp): Modifier =
    layout { measurable, constraints ->
        val extra = (amount * 2).roundToPx()
        val width = constraints.maxWidth + extra
        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
        layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
    }
