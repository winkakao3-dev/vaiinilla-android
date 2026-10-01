package com.vaiinilla.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.repository.SpaceAvailability
import com.vaiinilla.app.domain.repository.SpaceAvailabilityState
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import kotlinx.coroutines.delay

private val CourtAmber = Color(0xFFE9A23B)

/**
 * Mapa de canchas: cuáles están libres y cuánto le falta a las ocupadas. Cada cancha es un mosaico
 * cuyo color sigue al estado con un resorte, con una barra que avanza con el turno; las cifras
 * ruedan al cambiar y los mosaicos llegan escalonados.
 */
@Composable
fun CourtsMapCard(
    courts: List<SpaceAvailability>,
    modifier: Modifier = Modifier,
    /** Abre la renta de canchas; sin él, el mapa solo informa. */
    onOpen: (() -> Unit)? = null,
) {
    if (courts.isEmpty()) return
    val colors = LocalVaiinillaColors.current
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            tick += 1
        }
    }
    val free = courts.count { it.isFreeNow }

    Column(modifier = modifier.fillMaxWidth().arrive(0), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column {
                Text(
                    "CANCHAS",
                    color = colors.muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.4.sp,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    NumberTicker(value = free, label = "courts-free") { count ->
                        Text(
                            "$count",
                            color = colors.ink,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp,
                        )
                    }
                    Text(
                        if (free == 1) " libre ahora" else " libres ahora",
                        color = colors.ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }
            }
            if (onOpen != null) {
                Text(
                    "Rentar",
                    color = colors.accentInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier =
                        Modifier
                            .padding(bottom = 4.dp)
                            .physicalPress(onClick = onOpen)
                            .clip(RoundedCornerShape(50))
                            .background(colors.accent)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            } else {
                Text(
                    "de ${courts.size}",
                    color = colors.muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            courts.forEachIndexed { index, court ->
                CourtTile(
                    court = court,
                    tick = tick,
                    modifier =
                        Modifier
                            .arrive(index + 1, key = court.space.id)
                            .physicalPress(enabled = onOpen != null) { onOpen?.invoke() },
                )
            }
        }
    }
}

@Composable
private fun CourtTile(
    court: SpaceAvailability,
    tick: Int,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val shape = RoundedCornerShape(22.dp)
    val state = court.state
    // El apartado empieza y termina con la hora, sin nuevo dato del backend: se recalcula con el tick.
    tick.hashCode()
    val held = court.isHeldAt()
    val container =
        animateSelectionColor(
            when {
                held -> colors.coral.copy(alpha = 0.16f)
                state == SpaceAvailabilityState.LIBRE -> colors.accent.copy(alpha = 0.32f)
                state == SpaceAvailabilityState.OCUPADA -> colors.paper2
                state == SpaceAvailabilityState.EN_GRACIA -> CourtAmber.copy(alpha = 0.22f)
                else -> colors.coral.copy(alpha = 0.16f)
            },
            "court-container",
        )
    val border =
        animateSelectionColor(
            when {
                held -> colors.coral
                state == SpaceAvailabilityState.LIBRE -> colors.accent
                state == SpaceAvailabilityState.OCUPADA -> colors.line
                state == SpaceAvailabilityState.EN_GRACIA -> CourtAmber
                else -> colors.coral
            },
            "court-border",
        )
    val ink = animateSelectionColor(if (court.isFreeNow) colors.ink else colors.ink2, "court-ink")

    // El reloj corre solo mientras hay un turno; el resto del tiempo no hay nada que contar.
    tick.hashCode()
    val untilFree = millisUntil(court.releasesAt)
    val clock =
        if (state == SpaceAvailabilityState.OCUPADA ||
            state == SpaceAvailabilityState.EN_GRACIA
        ) {
            untilFree
        } else {
            null
        }
    val progress =
        when (state) {
            SpaceAvailabilityState.OCUPADA -> turnFraction(court.startedAt, court.endsAt)
            SpaceAvailabilityState.EN_GRACIA, SpaceAvailabilityState.POR_COBRAR -> court.endsAt?.let { 1f }
            SpaceAvailabilityState.LIBRE -> null
        }

    Column(
        modifier =
            modifier
                .width(132.dp)
                .height(124.dp)
                .clip(shape)
                .background(container)
                .border(1.dp, border, shape)
                .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(border))
            Text(
                if (held) "Apartada" else courtStateLabel(state),
                color = ink,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Column {
            Text(
                courtShortName(court.space.name),
                color = ink,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.2).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (clock != null) {
                Row(verticalAlignment = Alignment.Bottom) {
                    RollingText(
                        text = clockText(clock),
                        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                        color = colors.muted,
                    )
                    Text(
                        " para liberarse",
                        color = colors.muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            } else {
                Text(
                    when {
                        held && court.isWaitingPayment -> "Esperando pago"
                        held -> "Reservada ahora"
                        else -> courtHint(state)
                    },
                    color = colors.muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (progress != null) {
                TurnProgressBar(
                    fraction = progress,
                    track = colors.ink.copy(alpha = 0.1f),
                    fill = if (state == SpaceAvailabilityState.OCUPADA) colors.accent2 else border,
                    modifier = Modifier.padding(top = 8.dp),
                    height = 4.dp,
                )
            }
        }
    }
}

private fun courtStateLabel(state: SpaceAvailabilityState): String =
    when (state) {
        SpaceAvailabilityState.LIBRE -> "Libre"
        SpaceAvailabilityState.OCUPADA -> "Ocupada"
        SpaceAvailabilityState.EN_GRACIA -> "Por liberarse"
        SpaceAvailabilityState.POR_COBRAR -> "Ocupada"
    }

private fun courtHint(state: SpaceAvailabilityState): String =
    when (state) {
        SpaceAvailabilityState.LIBRE -> "Disponible"
        SpaceAvailabilityState.POR_COBRAR -> "Terminando"
        else -> ""
    }

/** "Cancha 3" → "3": el número es lo que se lee de lejos. */
private fun courtShortName(name: String): String = Regex("(\\d+)\\s*$").find(name)?.groupValues?.getOrNull(1) ?: name
