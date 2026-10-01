package com.vaiinilla.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.domain.model.Reservation
import com.vaiinilla.app.domain.repository.AccountCollection
import com.vaiinilla.app.domain.repository.AccountOrder
import com.vaiinilla.app.domain.repository.AccountPaymentMethod
import com.vaiinilla.app.domain.repository.SpaceAvailability
import com.vaiinilla.app.domain.repository.SpaceAvailabilityState
import com.vaiinilla.app.domain.repository.SpaceSessionDetail
import com.vaiinilla.app.domain.repository.TableSpace
import com.vaiinilla.app.ui.components.RollingText
import com.vaiinilla.app.ui.components.TurnProgressBar
import com.vaiinilla.app.ui.components.animateSelectionColor
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.components.clockText
import com.vaiinilla.app.ui.components.millisUntil
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.turnFraction
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Duraciones que el mesero puede elegir al abrir el turno de una cancha. */
private val TurnDurations = listOf(60, 90, 120)

/** Cuánto se alarga un turno con cada toque. */
private val ExtensionSteps = listOf(30, 60)

/** Billetes con los que suele pagar la gente: de ahí salen los atajos de "efectivo recibido". */
private val BillDenominations = listOf(20, 50, 100, 200, 500, 1_000)

private val StateCoral = Color(0xFFE5645A)
private val StateAmber = Color(0xFFE9A23B)
private val StateAmberInk = Color(0xFF3B2A05)

/** "42 min", "1 h 05 min" o "0:45": lo que queda de un turno, dicho corto. */
internal fun remainingText(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1_000
    return when {
        totalSeconds < 60 -> "0:${totalSeconds.toString().padStart(2, '0')}"
        totalSeconds < 3_600 -> "${(totalSeconds + 59) / 60} min"
        else -> {
            val minutes = (totalSeconds % 3_600) / 60
            "${totalSeconds / 3_600} h ${minutes.toString().padStart(2, '0')} min"
        }
    }
}

/**
 * Lo que el mosaico dice de un espacio ocupado (turno restante, gracia, por cobrar) o null si
 * está libre. `tick` entra para que el reloj avance cada segundo.
 */
internal fun spaceAvailabilityCaption(
    availability: SpaceAvailability?,
    tick: Int,
): String? {
    tick.hashCode()
    availability ?: return null
    val pending = if (availability.settled) "" else "\nPor cobrar"
    return when (availability.state) {
        SpaceAvailabilityState.LIBRE -> null
        SpaceAvailabilityState.OCUPADA -> {
            val remaining = millisUntil(availability.endsAt)
            if (remaining != null) "${remainingText(remaining)} restantes$pending" else "Cuenta abierta$pending"
        }
        SpaceAvailabilityState.EN_GRACIA -> "Turno terminado$pending"
        SpaceAvailabilityState.POR_COBRAR -> "Por cobrar"
    }
}

/** Avance del turno para la barra del mosaico; null si el espacio no tiene un turno en curso. */
internal fun spaceTileProgress(
    availability: SpaceAvailability?,
    tick: Int,
): Float? {
    tick.hashCode()
    availability ?: return null
    return when (availability.state) {
        SpaceAvailabilityState.OCUPADA -> turnFraction(availability.startedAt, availability.endsAt)
        SpaceAvailabilityState.EN_GRACIA, SpaceAvailabilityState.POR_COBRAR ->
            availability.endsAt?.let { 1f }
        SpaceAvailabilityState.LIBRE -> null
    }
}

private fun stateLabel(state: SpaceAvailabilityState): String =
    when (state) {
        SpaceAvailabilityState.LIBRE -> "Libre"
        SpaceAvailabilityState.OCUPADA -> "Ocupada"
        SpaceAvailabilityState.EN_GRACIA -> "En gracia"
        SpaceAvailabilityState.POR_COBRAR -> "Por cobrar"
    }

private fun stateSubtitle(
    availability: SpaceAvailability,
    rentable: Boolean = false,
): String =
    when (availability.state) {
        SpaceAvailabilityState.LIBRE ->
            when {
                availability.isHeldAt() && availability.isWaitingPayment ->
                    "Apartada: alguien la rentó y está pagando. No la ocupes."
                availability.isHeldAt() -> "Reservada ahora: el turno se abre solo."
                rentable -> "Se renta y se paga antes de jugar."
                else -> "Abre el turno cuando lleguen."
            }
        SpaceAvailabilityState.OCUPADA ->
            if (availability.endsAt != null) "Turno en curso." else "Cuenta abierta: se libera a mano."
        SpaceAvailabilityState.EN_GRACIA ->
            if (rentable) {
                "Terminó el turno. Tienen ${availability.graceMinutes} min para renovar (se cobra) o liberar."
            } else {
                "Terminó el turno. Tienen ${availability.graceMinutes} min para decidir si siguen."
            }
        SpaceAvailabilityState.POR_COBRAR -> "No se libera hasta que se cobre la cuenta."
    }

private fun endsAtClock(iso: String?): String? =
    iso?.let {
        runCatching {
            Instant
                .parse(it)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("h:mm a", Locale("es", "MX")))
        }.getOrNull()
    }

private fun String.moneyOrZero(): BigDecimal = runCatching { Money.parse(this) }.getOrDefault(BigDecimal.ZERO)

private data class StatePalette(
    val container: Color,
    val content: Color,
)

/** El color de cada estado sigue al estado con un resorte, igual que los mosaicos del tablero. */
@Composable
private fun statePalette(
    state: SpaceAvailabilityState,
    colors: OperationalColors,
): StatePalette {
    val (container, content) =
        when (state) {
            SpaceAvailabilityState.LIBRE -> colors.pillBackground to colors.textSecondary
            SpaceAvailabilityState.OCUPADA -> colors.accentLime to colors.accentInk
            SpaceAvailabilityState.EN_GRACIA -> StateAmber to StateAmberInk
            SpaceAvailabilityState.POR_COBRAR -> StateCoral to Color.White
        }
    return StatePalette(
        container = animateSelectionColor(container, "space-pill-container"),
        content = animateSelectionColor(content, "space-pill-content"),
    )
}

@Composable
private fun SpaceStatePill(
    state: SpaceAvailabilityState,
    colors: OperationalColors,
) {
    val palette = statePalette(state, colors)
    val reduced = reducedMotion()
    Row(
        modifier =
            Modifier
                .clip(CircleShape)
                .background(palette.container)
                .padding(horizontal = 13.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(palette.content))
        AnimatedContent(
            targetState = stateLabel(state),
            transitionSpec = {
                if (reduced) {
                    fadeIn(snap()) togetherWith fadeOut(snap())
                } else {
                    (
                        fadeIn(
                            spring(stiffness = 300f),
                        ) + scaleIn(spring(dampingRatio = 0.8f, stiffness = 400f), 0.92f)
                    ) togetherWith
                        fadeOut(spring(stiffness = 500f))
                }
            },
            label = "space-pill-label",
        ) { label ->
            Text(label, color = palette.content, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Turno y cuenta de un espacio dentro de la hoja del mesero: abrir el turno, alargarlo, cobrar la
 * cuenta y liberar. El servidor es la autoridad: aquí solo se deshabilita lo que sabemos que no procede.
 * Cada bloque llega con un resorte escalonado y los números ruedan cuando cambian.
 */
private val TICKET_TIME_FORMAT: java.time.format.DateTimeFormatter =
    java.time.format.DateTimeFormatter
        .ofPattern("dd/MM/yyyy HH:mm")

@Composable
internal fun WaiterSpaceSection(
    space: TableSpace,
    detail: SpaceSessionDetail?,
    loading: Boolean,
    lastCollection: AccountCollection?,
    tick: Int,
    acting: Boolean,
    readOnly: Boolean,
    colors: OperationalColors,
    onOpenTurn: (Int?) -> Unit,
    onExtend: (Int) -> Unit,
    onCollect: (
        method: AccountPaymentMethod,
        received: String?,
        expectedTotal: String,
        orderIds: List<String>?,
    ) -> Unit,
    onRelease: () -> Unit,
    onDismissCollection: () -> Unit,
    /** Cancha con precio: se renta y se cobra en lugar de abrir o alargar el turno a mano. */
    rentable: Boolean = false,
    onRent: (minutes: Int, start: Instant?) -> Unit = { _, _ -> },
) {
    var collecting by remember { mutableStateOf(false) }
    // Solo el botón que se tocó muestra su progreso; los demás quedan quietos.
    var busyKey by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(acting) { if (!acting) busyKey = null }
    LaunchedEffect(lastCollection) { if (lastCollection != null) collecting = false }
    val isCourt = space.type == "cancha"
    val enabled = !acting && !readOnly

    if (lastCollection != null) {
        CollectionResultCard(lastCollection, colors, onDismissCollection)
    }

    if (detail == null) {
        if (loading) {
            Text(
                "Cargando la cuenta…",
                fontSize = 13.sp,
                color = colors.textSecondary,
                modifier = Modifier.arrive(0),
            )
        }
        return
    }

    val availability = detail.availability
    val account = detail.account
    val session = detail.session
    val pending = account?.pending?.moneyOrZero() ?: BigDecimal.ZERO
    val hasPending = pending.signum() > 0

    SectionCard(colors, Modifier.arrive(0)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SpaceStatePill(availability.state, colors)
            endsAtClock(availability.endsAt)?.let { end ->
                // Tras terminar el turno, decir "hasta las" sería mentira: ya pasó.
                val ended = (millisUntil(availability.endsAt) ?: 0L) < 0L
                Text(
                    if (ended) "terminó a las $end" else "hasta las $end",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        val clockTarget =
            when (availability.state) {
                SpaceAvailabilityState.OCUPADA -> availability.endsAt
                SpaceAvailabilityState.EN_GRACIA -> availability.releasesAt
                else -> null
            }
        AnimatedVisibility(
            visible = clockTarget != null,
            enter = fadeIn(spring(stiffness = 300f)) + expandVertically(spring(stiffness = Spring.StiffnessMediumLow)),
            exit = fadeOut(spring(stiffness = 500f)) + shrinkVertically(spring(stiffness = Spring.StiffnessMedium)),
        ) {
            tick.hashCode()
            val remaining = millisUntil(clockTarget) ?: 0L
            val inGrace = availability.state == SpaceAvailabilityState.EN_GRACIA
            Column(modifier = Modifier.padding(top = 14.dp)) {
                RollingText(
                    text = clockText(remaining),
                    style = TextStyle(fontSize = 46.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.5).sp),
                    color = colors.textPrimary,
                )
                Text(
                    if (inGrace) "de gracia para decidir" else "restantes",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.SemiBold,
                )
                turnFraction(availability.startedAt, availability.endsAt)?.let { played ->
                    TurnProgressBar(
                        fraction = if (inGrace) 1f else played,
                        track = colors.pillBackground,
                        fill = if (inGrace) StateAmber else colors.accentLime,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }

        AnimatedContent(
            targetState = stateSubtitle(availability, rentable),
            transitionSpec = { fadeIn(spring(stiffness = 300f)) togetherWith fadeOut(spring(stiffness = 500f)) },
            label = "space-subtitle",
        ) { subtitle ->
            Text(subtitle, fontSize = 12.5.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 10.dp))
        }

        if (session == null) {
            if (isCourt && rentable) {
                Text(
                    "Rentar ahora · se cobra al rentar",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 14.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TurnDurations.forEach { minutes ->
                        ChipButton(rentalLabel(minutes), enabled, colors, Modifier.weight(1f)) {
                            onRent(minutes, null)
                        }
                    }
                }
            } else if (isCourt) {
                Text(
                    "Abrir turno",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 14.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TurnDurations.forEach { minutes ->
                        ChipButton("$minutes min", enabled, colors, Modifier.weight(1f)) {
                            busyKey = "open"
                            onOpenTurn(minutes)
                        }
                    }
                }
            } else {
                ActionPill(
                    label = "Abrir cuenta",
                    enabled = enabled,
                    loading = acting && busyKey == "open",
                    colors = colors,
                    modifier = Modifier.padding(top = 14.dp),
                ) {
                    busyKey = "open"
                    onOpenTurn(null)
                }
            }
        }
    }

    if (account != null && account.orders.isNotEmpty()) {
        SectionCard(colors, Modifier.arrive(1)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Cuenta", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                Text(
                    "${account.orders.size} ${if (account.orders.size == 1) "pedido" else "pedidos"}",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            account.orders.forEachIndexed { index, order ->
                AccountOrderRow(order, colors, Modifier.arrive(index + 2, key = order.id))
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        "Falta cobrar",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    RollingText(
                        text = "$${account.pending}",
                        style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.8).sp),
                        color = if (hasPending) StateCoral else colors.textSecondary,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total", color = colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    RollingText(
                        text = "$${account.total}",
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                    )
                }
            }
            AnimatedVisibility(
                visible = hasPending,
                enter =
                    fadeIn(spring(stiffness = 300f)) + expandVertically(spring(stiffness = Spring.StiffnessMediumLow)),
                exit = fadeOut(spring(stiffness = 500f)) + shrinkVertically(spring(stiffness = Spring.StiffnessMedium)),
            ) {
                ActionPill(
                    label = "Cobrar cuenta",
                    enabled = enabled,
                    loading = false,
                    colors = colors,
                    modifier = Modifier.padding(top = 14.dp),
                ) { collecting = true }
            }
            val context = LocalContext.current
            ActionPill(
                label = "Imprimir cuenta",
                enabled = account.orders.isNotEmpty(),
                loading = false,
                colors = colors,
                primary = false,
                modifier = Modifier.padding(top = 10.dp),
            ) {
                val printedAt =
                    java.time.LocalDateTime
                        .now()
                        .format(TICKET_TIME_FORMAT)
                printAccountTicket(context, space.name, buildAccountTicketHtml(space.name, account, printedAt))
            }
        }
    }

    if (session != null) {
        SectionCard(colors, Modifier.arrive(2)) {
            if (rentable) {
                // Renovar = rentar el siguiente horario pegado al turno y cobrarlo.
                Text(
                    "Renovar · se cobra al renovar",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    color = colors.textSecondary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TurnDurations.forEach { minutes ->
                        ChipButton("+${rentalLabel(minutes)}", enabled, colors, Modifier.weight(1f)) {
                            onRent(minutes, session.endsAt?.let { runCatching { Instant.parse(it) }.getOrNull() })
                        }
                    }
                }
            } else if (isCourt || session.endsAt != null) {
                Text(
                    if (session.endsAt == null) "Iniciar turno" else "Alargar turno",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    color = colors.textSecondary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ExtensionSteps.forEach { minutes ->
                        ChipButton("+$minutes min", enabled, colors, Modifier.weight(1f)) {
                            busyKey = "extend"
                            onExtend(minutes)
                        }
                    }
                }
            }
            ActionPill(
                label = "Liberar espacio",
                enabled = enabled && !hasPending,
                loading = acting && busyKey == "release",
                colors = colors,
                primary = !hasPending,
                modifier = Modifier.padding(top = if (rentable || isCourt || session.endsAt != null) 14.dp else 0.dp),
            ) {
                busyKey = "release"
                onRelease()
            }
            AnimatedVisibility(
                visible = hasPending,
                enter =
                    fadeIn(spring(stiffness = 300f)) + expandVertically(spring(stiffness = Spring.StiffnessMediumLow)),
                exit = fadeOut(spring(stiffness = 500f)) + shrinkVertically(spring(stiffness = Spring.StiffnessMedium)),
            ) {
                Text(
                    "Cobra la cuenta para poder liberar el espacio.",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }

    if (collecting && account != null) {
        CollectAccountSheet(
            total = account.pending,
            payable = account.orders.filter { it.pending && it.payAtEnd },
            confirming = acting,
            colors = colors,
            onDismiss = { if (!acting) collecting = false },
            onConfirm = { method, received, expected, ids -> onCollect(method, received, expected, ids) },
        )
    }
}

@Composable
private fun SectionCard(
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val reduced = reducedMotion()
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.cardBackground,
        modifier = modifier.fillMaxWidth().border(1.dp, colors.cardBorder, RoundedCornerShape(18.dp)),
    ) {
        Column(
            modifier =
                Modifier
                    .padding(16.dp)
                    .animateContentSize(
                        if (reduced) snap() else spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow),
                    ),
            content = content,
        )
    }
}

@Composable
private fun AccountOrderRow(
    order: AccountOrder,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
) {
    val pillBackground =
        animateSelectionColor(
            if (order.pending) StateCoral.copy(alpha = 0.16f) else colors.pillBackground,
            "order-pill-bg",
        )
    val pillInk = animateSelectionColor(if (order.pending) StateCoral else colors.textSecondary, "order-pill-ink")
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "#${order.folio} · ${order.clientName ?: "Cliente"}",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = colors.textPrimary,
            )
            if (order.itemsSummary.isNotBlank()) {
                Text(order.itemsSummary, fontSize = 12.sp, color = colors.textSecondary)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("$${order.total}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.textPrimary)
            Text(
                if (order.pending) "sin cobrar" else "cobrado",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = pillInk,
                modifier =
                    Modifier
                        .padding(top = 3.dp)
                        .clip(CircleShape)
                        .background(pillBackground)
                        .padding(horizontal = 9.dp, vertical = 3.dp),
            )
        }
    }
}

/** La cuenta cobrada se celebra un instante: una palomita que rebota y el cambio que rueda hasta su valor. */
@Composable
internal fun CollectionResultCard(
    collection: AccountCollection,
    colors: OperationalColors,
    onDismiss: () -> Unit,
) {
    val reduced = reducedMotion()
    var shownChange by remember(collection) { mutableStateOf("0.00") }
    LaunchedEffect(collection) {
        if (!reduced) delay(220)
        shownChange = collection.change
    }
    val checkScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = if (reduced) snap() else spring(dampingRatio = 0.45f, stiffness = 380f),
        label = "collection-check",
    )
    SectionCard(colors, Modifier.arrive(0, key = collection)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier =
                    Modifier
                        .size(30.dp)
                        .graphicsLayer {
                            scaleX = checkScale
                            scaleY = checkScale
                        }.clip(CircleShape)
                        .background(colors.accentLime),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = colors.accentInk,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                if (collection.settled) "Cuenta cobrada" else "Cobro registrado",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = colors.textPrimary,
            )
        }
        Text(
            buildString {
                append("Total $${collection.total}")
                if (collection.method == AccountPaymentMethod.TERMINAL) {
                    append(" · Cobrado con la terminal")
                } else {
                    append(" · Recibido $${collection.received}")
                }
                if (!collection.settled) append(" · Quedan $${collection.remaining} por cobrar")
            },
            fontSize = 12.5.sp,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = 10.dp),
        )
        if (collection.method == AccountPaymentMethod.CASH) {
            Text(
                "Entrega de cambio",
                fontSize = 12.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 10.dp),
            )
            RollingText(
                text = "$$shownChange",
                style = TextStyle(fontSize = 38.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.2).sp),
                color = colors.textPrimary,
            )
        }
        ActionPill(
            "Listo",
            enabled = true,
            loading = false,
            colors = colors,
            modifier = Modifier.padding(top = 14.dp),
            onClick = onDismiss,
        )
    }
}

/** Un toque que se aprieta: atajo de duración o de efectivo recibido. */
@Composable
internal fun ChipButton(
    label: String,
    enabled: Boolean,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    val container = animateSelectionColor(if (selected) colors.accentLime else colors.cardInner, "chip-container")
    val ink = animateSelectionColor(if (selected) colors.accentInk else colors.textPrimary, "chip-ink")
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.45f,
        animationSpec = if (reducedMotion()) snap() else spring(stiffness = Spring.StiffnessMedium),
        label = "chip-alpha",
    )
    Box(
        modifier =
            modifier
                .physicalPress(enabled = enabled, onClick = onClick)
                .graphicsLayer { this.alpha = alpha }
                .height(48.dp)
                .clip(shape)
                .background(container)
                .border(1.dp, if (selected) colors.accentLime else colors.cardBorder, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = ink,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 14.dp),
        )
    }
}

/**
 * Botón de acción que se aprieta. Al tocarlo se vuelve su propio progreso (no aparece un modal
 * aparte) y al terminar regresa a ser botón: un solo objeto que cambia de estado.
 */
@Composable
internal fun ActionPill(
    label: String,
    enabled: Boolean,
    loading: Boolean,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    val active = enabled && !loading
    val container =
        animateSelectionColor(
            when {
                loading -> colors.accentLime
                !enabled -> colors.pillBackground
                primary -> colors.accentLime
                else -> colors.cardInner
            },
            "action-container",
        )
    val ink =
        animateSelectionColor(
            when {
                loading -> colors.accentInk
                !enabled -> colors.textSecondary
                primary -> colors.accentInk
                else -> colors.textPrimary
            },
            "action-ink",
        )
    val reduced = reducedMotion()
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .physicalPress(enabled = active, onClick = onClick)
                .height(50.dp)
                .clip(shape)
                .background(container)
                .animateContentSize(if (reduced) snap() else spring(dampingRatio = 0.8f, stiffness = 500f)),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = loading,
            transitionSpec = {
                if (reduced) {
                    fadeIn(snap()) togetherWith fadeOut(snap())
                } else {
                    (
                        fadeIn(
                            spring(stiffness = 300f),
                        ) + scaleIn(spring(dampingRatio = 0.8f, stiffness = 400f), 0.9f)
                    ) togetherWith
                        fadeOut(spring(stiffness = 600f))
                }
            },
            label = "action-content",
        ) { isLoading ->
            if (isLoading) {
                CircularProgressIndicator(color = ink, strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))
            } else {
                Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ink)
            }
        }
    }
}

/** Atajos de efectivo: el siguiente billete redondo por encima del total, sin repetir. */
internal fun quickCashAmounts(total: BigDecimal): List<BigDecimal> =
    BillDenominations
        .map { bill ->
            val step = bill.toBigDecimal()
            total.divide(step, 0, RoundingMode.CEILING).multiply(step)
        }.filter { it > total }
        .distinct()
        .sorted()
        .take(3)

/**
 * Cobro de la cuenta en una hoja (como el resto de la app): el total en grande, atajos de billete,
 * el efectivo recibido y el cambio que rueda mientras se escribe. El total viaja al servidor como
 * `total_esperado`: si mientras tanto se pidió algo más, lo rechaza y no se entrega mal el cambio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectAccountSheet(
    total: String,
    payable: List<AccountOrder>,
    confirming: Boolean,
    colors: OperationalColors,
    onDismiss: () -> Unit,
    onConfirm: (
        method: AccountPaymentMethod,
        received: String?,
        expectedTotal: String,
        orderIds: List<String>?,
    ) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        CollectAccountContent(
            total = total,
            payable = payable,
            confirming = confirming,
            colors = colors,
            onConfirm = onConfirm,
        )
    }
}

/**
 * El contenido de la hoja de cobro, aparte de la hoja: así se puede probar y capturar.
 *
 * Con dos o más pedidos a la cuenta se puede dividir: cada pedido se marca o desmarca (por
 * ejemplo lo de una persona) y el total, el atajo "Justo" y el cambio siguen a la selección.
 * Con todos marcados se cobra la cuenta completa, como siempre.
 */
@Composable
internal fun CollectAccountContent(
    total: String,
    payable: List<AccountOrder>,
    confirming: Boolean,
    colors: OperationalColors,
    onConfirm: (
        method: AccountPaymentMethod,
        received: String?,
        expectedTotal: String,
        orderIds: List<String>?,
    ) -> Unit,
    title: String? = null,
    /** La renta de mostrador solo se cobra en efectivo; la cuenta abierta también con la terminal. */
    allowTerminal: Boolean = true,
) {
    var selected by remember(payable) { mutableStateOf(payable.map { it.id }.toSet()) }
    val splitting = payable.size >= 2
    val allSelected = selected.size == payable.size
    val totalAmount =
        if (payable.isEmpty()) {
            total.moneyOrZero()
        } else {
            payable.filter { it.id in selected }.fold(BigDecimal.ZERO) { sum, order -> sum + order.total.moneyOrZero() }
        }
    val totalLabel = Money.format(totalAmount)
    // Lo que el cajero escribió; sin tocar nada el efectivo sigue al total (cobro justo).
    var typed by remember { mutableStateOf<String?>(null) }
    var method by remember { mutableStateOf(AccountPaymentMethod.CASH) }
    val withTerminal = allowTerminal && method == AccountPaymentMethod.TERMINAL
    val text = typed ?: totalLabel
    val received = text.trim().toBigDecimalOrNull()?.takeIf { it.signum() >= 0 }
    val difference = received?.let { it - totalAmount }
    val canConfirm =
        totalAmount.signum() > 0 && (withTerminal || (difference != null && difference.signum() >= 0))
    val shortfall = !withTerminal && difference != null && difference.signum() < 0
    val quickAmounts = remember(totalLabel) { quickCashAmounts(totalAmount) }
    val changeInk = animateSelectionColor(if (shortfall) StateCoral else colors.textPrimary, "change-ink")

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(modifier = Modifier.arrive(0)) {
            Text(
                title ?: if (splitting && !allSelected) "Cobrar parte de la cuenta" else "Cobrar cuenta",
                fontSize = 13.sp,
                color = colors.textSecondary,
                fontWeight = FontWeight.SemiBold,
            )
            RollingText(
                text = "$$totalLabel",
                style = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.6).sp),
                color = colors.textPrimary,
            )
        }
        if (splitting) {
            SplitOrdersPicker(
                orders = payable,
                selected = selected,
                enabled = !confirming,
                colors = colors,
                onToggle = { id ->
                    selected = if (id in selected) selected - id else selected + id
                    typed = null
                },
            )
        }
        if (allowTerminal) {
            Column(modifier = Modifier.arrive(1), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "¿Cómo paga?",
                    fontSize = 12.5.sp,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ChipButton(
                        label = "Efectivo",
                        enabled = !confirming,
                        colors = colors,
                        modifier = Modifier.weight(1f),
                        selected = method == AccountPaymentMethod.CASH,
                    ) { method = AccountPaymentMethod.CASH }
                    ChipButton(
                        label = "Terminal",
                        enabled = !confirming,
                        colors = colors,
                        modifier = Modifier.weight(1f),
                        selected = method == AccountPaymentMethod.TERMINAL,
                    ) { method = AccountPaymentMethod.TERMINAL }
                }
            }
        }
        if (withTerminal) {
            Text(
                "Cobra $$totalLabel en tu terminal de tarjeta y confirma aquí. No entra al efectivo de la caja.",
                fontSize = 13.sp,
                color = colors.textSecondary,
                modifier = Modifier.arrive(2),
            )
        } else {
            Column(modifier = Modifier.arrive(1), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Efectivo recibido",
                    fontSize = 12.5.sp,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ChipButton(
                        label = "Justo",
                        enabled = !confirming,
                        colors = colors,
                        modifier = Modifier.weight(1f),
                        selected = received != null && received.compareTo(totalAmount) == 0,
                    ) { typed = null }
                    quickAmounts.forEach { amount ->
                        ChipButton(
                            label = "$${amount.setScale(0)}",
                            enabled = !confirming,
                            colors = colors,
                            modifier = Modifier.weight(1f),
                            selected = received != null && received.compareTo(amount) == 0,
                        ) { typed = Money.format(amount) }
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { typed = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Otro monto") },
                    singleLine = true,
                    enabled = !confirming,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Column(modifier = Modifier.arrive(2)) {
                Text(
                    if (shortfall) "Falta" else "Cambio",
                    fontSize = 12.5.sp,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.SemiBold,
                )
                RollingText(
                    text = "$${Money.format((difference ?: BigDecimal.ZERO).abs())}",
                    style = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
                    color = changeInk,
                )
            }
        }
        Box(modifier = Modifier.arrive(3)) {
            ActionPill(
                label = if (withTerminal) "Cobrado en la terminal $$totalLabel" else "Cobrar $$totalLabel",
                enabled = canConfirm,
                loading = confirming,
                colors = colors,
            ) {
                val orderIds = if (allSelected) null else selected.toList()
                if (withTerminal) {
                    onConfirm(AccountPaymentMethod.TERMINAL, null, totalLabel, orderIds)
                } else {
                    received?.let { onConfirm(AccountPaymentMethod.CASH, Money.format(it), totalLabel, orderIds) }
                }
            }
        }
    }
}

/** Los pedidos de la cuenta con casilla: marcar los que se cobran ahora. */
@Composable
private fun SplitOrdersPicker(
    orders: List<AccountOrder>,
    selected: Set<String>,
    enabled: Boolean,
    colors: OperationalColors,
    onToggle: (String) -> Unit,
) {
    Column(modifier = Modifier.arrive(1), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "¿Qué se cobra ahora?",
            fontSize = 12.5.sp,
            color = colors.textSecondary,
            fontWeight = FontWeight.SemiBold,
        )
        orders.forEach { order ->
            val checked = order.id in selected
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (checked) colors.cardBackground else colors.background)
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(14.dp))
                        .clickable(enabled = enabled) { onToggle(order.id) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "#${order.folio} · ${order.clientName ?: "Cliente"}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        color = colors.textPrimary,
                    )
                    Text(
                        order.itemsSummary,
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        maxLines = 1,
                    )
                }
                Text("$${order.total}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)
            }
        }
    }
}

private fun rentalLabel(minutes: Int): String =
    when {
        minutes % 60 == 0 -> "${minutes / 60} h"
        minutes > 60 -> "${minutes / 60} h ${minutes % 60}"
        else -> "$minutes min"
    }

/**
 * Cobro en efectivo de una renta de mostrador: el total es lo que paga el cliente (el precio de la
 * app con efectivo), con los mismos atajos de billete y el cambio. Cerrar sin cobrar libera el horario.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RentalCollectSheet(
    rental: Reservation,
    confirming: Boolean,
    colors: OperationalColors,
    onDismiss: () -> Unit,
    onConfirm: (received: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        CollectAccountContent(
            total = rental.customerPrice?.cashOrBalance ?: rental.amount,
            payable = emptyList(),
            confirming = confirming,
            colors = colors,
            title = "Rentar ${rental.courtName ?: "cancha"} · ${rentalLabel(rental.durationMinutes)}",
            allowTerminal = false,
            onConfirm = { _, received, _, _ -> received?.let(onConfirm) },
        )
    }
}
