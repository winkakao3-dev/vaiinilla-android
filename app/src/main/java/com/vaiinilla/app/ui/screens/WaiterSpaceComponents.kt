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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.core.venue.LocalVenueContext
import com.vaiinilla.app.domain.model.AccountSplit
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.domain.model.MoneyInput
import com.vaiinilla.app.domain.model.Reservation
import com.vaiinilla.app.domain.model.ReservationSlots
import com.vaiinilla.app.domain.model.TipChoice
import com.vaiinilla.app.domain.model.TipPercents
import com.vaiinilla.app.domain.model.VenueContext
import com.vaiinilla.app.domain.model.amountFor
import com.vaiinilla.app.domain.model.asTipWire
import com.vaiinilla.app.domain.repository.AbonoMode
import com.vaiinilla.app.domain.repository.AccountCollection
import com.vaiinilla.app.domain.repository.AccountOrder
import com.vaiinilla.app.domain.repository.AccountPaymentMethod
import com.vaiinilla.app.domain.repository.SpaceAvailability
import com.vaiinilla.app.domain.repository.SpaceAvailabilityState
import com.vaiinilla.app.domain.repository.SpaceRent
import com.vaiinilla.app.domain.repository.SpaceSessionDetail
import com.vaiinilla.app.domain.repository.TableSpace
import com.vaiinilla.app.domain.time.BusinessTime
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.RollingText
import com.vaiinilla.app.ui.components.TurnProgressBar
import com.vaiinilla.app.ui.components.animateSelectionColor
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.components.clockText
import com.vaiinilla.app.ui.components.millisUntil
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.turnFraction
import com.vaiinilla.app.ui.operational.RentalDraft
import com.vaiinilla.app.ui.screens.admin.AdminPrimaryButton
import com.vaiinilla.app.ui.screens.admin.AdminSegmented
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Duraciones que el mesero puede elegir al abrir el turno de una cancha. */
private val TurnDurations = listOf(60, 90, 120)

/** Las de `reservas/disponibilidad` mientras no llegan: el backend solo acepta estas. */
internal val DefaultRentalDurations = listOf(90, 180, 270, 360)

/** Cuánto se alarga un turno con cada toque. */
private val ExtensionSteps = listOf(30, 60)

/** Billetes con los que suele pagar la gente: de ahí salen los atajos de "efectivo recibido". */
private val BillDenominations = listOf(20, 50, 100, 200, 500, 1_000)

private val StateCoral = Color(0xFFE5645A)
private val StateAmber = Color(0xFFE9A23B)

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
            // Una cuenta sin turno no tiene reloj: se dice desde cuándo está abierta.
            val since = endsAtClock(availability.startedAt)?.let { "Desde las $it" }
            if (remaining != null) {
                "${remainingText(remaining)} restantes$pending"
            } else {
                "${since ?: "Cuenta abierta"}$pending"
            }
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
                .atZone(BusinessTime.zone)
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
    // Solo el punto lleva color: la píldora es neutra para no competir con Cobrar.
    val (container, content) =
        when (state) {
            SpaceAvailabilityState.LIBRE -> colors.pillBackground to colors.textMuted
            SpaceAvailabilityState.OCUPADA -> colors.pillBackground to colors.highlightBorder
            SpaceAvailabilityState.EN_GRACIA -> colors.pillBackground to StateAmber
            SpaceAvailabilityState.POR_COBRAR -> colors.pillBackground to StateCoral
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
            Text(label, color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
    tick: State<Int>,
    acting: Boolean,
    readOnly: Boolean,
    colors: OperationalColors,
    onOpenTurn: (Int?) -> Unit,
    onExtend: (Int) -> Unit,
    onRelease: () -> Unit,
    onDismissCollection: () -> Unit,
    /** Cancha con precio: se renta y se cobra en lugar de abrir o alargar el turno a mano. */
    rentable: Boolean = false,
    /** `players`: en un boliche, cuántos juegan (null sin dato); `name`: a nombre de quién (#27). */
    onRent: (minutes: Int, start: Instant?, players: Int?, name: String?) -> Unit = { _, _, _, _ -> },
    /** U04: lo elegido para rentar este espacio, guardado en el ViewModel; null sin elegir. */
    rentalDraft: RentalDraft? = null,
    /** null: el picker guarda lo suyo y se pierde al cerrar (solo pruebas y capturas). */
    onRentalDraftChange: ((minutes: Int?, players: Int, name: String) -> Unit)? = null,
    /** "Cobrar cuenta": la hoja que contiene la sección cambia al cobro. */
    onStartCollect: () -> Unit = {},
    /** Precio por hora del tablero: el detalle de la sesión no lo trae. */
    pricePerHour: String? = null,
    /** Hay pedidos listos por entregar: entregar es lo primero y cobrar queda en segundo plano. */
    hasReadyOrders: Boolean = false,
    /** Pedidos del espacio que aún no se entregan. */
    ordersInCourse: Int = 0,
    /** Duraciones que acepta el backend para rentar; con otras responde 400. */
    rentalDurations: List<Int> = DefaultRentalDurations,
    rentalDurationsFailed: Boolean = false,
    onRetryDurations: (() -> Unit)? = null,
    /** R25: cuántos caben en la pista, para el selector de jugadores. */
    rentalMaxPlayers: Int = ReservationSlots.MAX_PLAYERS,
    /** Administración ve la cuenta pero no la cobra: el backend le niega /sesion/cobros y /abonos. */
    canCharge: Boolean = true,
    /** La hoja ya lista los pedidos de la cuenta arriba: aquí solo van el total y las acciones. */
    showOrders: Boolean = true,
    /** Tomar un pedido para el espacio, junto a "Cobrar cuenta". */
    onTakeOrder: (() -> Unit)? = null,
    /** Lo que dice el tablero del espacio: trae la siguiente reserva (el detalle de la sesión no). */
    nextReservation: SpaceAvailability? = null,
) {
    val venue = LocalVenueContext.current
    // Solo el botón que se tocó muestra su progreso; los demás quedan quietos.
    var busyKey by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(acting) { if (!acting) busyKey = null }
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
    // Lo que de verdad falta: lo pendiente menos lo ya abonado, como lo calcula el backend (`restante`).
    val remaining = account?.remaining?.moneyOrZero() ?: pending
    val paidIn = account?.paidIn?.moneyOrZero() ?: BigDecimal.ZERO

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
            tick.value
            val remaining = millisUntil(clockTarget) ?: 0L
            val inGrace = availability.state == SpaceAvailabilityState.EN_GRACIA
            Column(modifier = Modifier.padding(top = 14.dp)) {
                if (isCourt) {
                    CourtTurnHero(remaining = remaining, inGrace = inGrace, endsAt = endsAtClock(availability.endsAt))
                } else {
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
                }
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

        // U05: la renta ya pagada va aparte (es su propio pedido) y no se suma a lo que falta cobrar.
        val rent = detail.rent
        if (rent != null) {
            Text(
                rentPaidLine(venue, rent),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        // #13: quién y cuántos juegan, de la renta en curso o, sin turno, de la siguiente reserva.
        val who =
            if (rent != null) {
                rentWhoLine(rent.players, rent.customerName)
            } else if (session == null) {
                nextReservation
                    ?.takeIf { it.nextReservationStart != null }
                    ?.let { rentWhoLine(it.nextReservationPlayers, it.nextReservationCustomer) }
                    ?.let { "Siguiente reserva: $it" }
            } else {
                null
            }
        if (who != null) {
            Text(
                who,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = if (rent != null) 2.dp else 6.dp),
            )
        }

        if (session == null) {
            if (isCourt && rentable) {
                RentalDurationPicker(
                    title = "Rentar ahora",
                    verb = "Rentar",
                    pricePerHour = pricePerHour ?: availability.pricePerHour,
                    durations = rentalDurations,
                    durationsFailed = rentalDurationsFailed,
                    onRetryDurations = onRetryDurations,
                    enabled = enabled,
                    colors = colors,
                    modifier = Modifier.padding(top = 14.dp),
                    draft = rentalDraft,
                    onDraftChange = onRentalDraftChange,
                    askName = true,
                    maxPlayers = rentalMaxPlayers,
                    onConfirm = { minutes, players, name -> onRent(minutes, null, players, name) },
                )
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
            if (showOrders) {
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
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = if (showOrders) 14.dp else 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        if (paidIn.signum() > 0 || !hasPending) "Falta cobrar" else "Cuenta",
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (paidIn.signum() > 0) {
                        Text(
                            "Abonado ${moneyLabel(Money.format(paidIn))}",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    // El total solo aporta si ya se cobró o abonó una parte; si no, repite lo que falta.
                    if (account.total.moneyOrZero().compareTo(remaining) != 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Total",
                                color = colors.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                moneyLabel(account.total),
                                color = colors.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                RollingText(
                    text = moneyLabel(Money.format(remaining)),
                    style = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
                    color = if (hasPending) colors.textPrimary else colors.textSecondary,
                )
            }
            if (hasPending && !canCharge) {
                Text(
                    "La cuenta se cobra desde Mesero o Caja.",
                    fontSize = 12.5.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            val showCollect = hasPending && canCharge
            if (onTakeOrder != null || showCollect) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (onTakeOrder != null) {
                        ActionPill(
                            label = "Tomar pedido",
                            enabled = enabled,
                            loading = false,
                            colors = colors,
                            primary = !showCollect && !hasReadyOrders,
                            outlined = showCollect,
                            modifier = Modifier.weight(1f),
                            onClick = onTakeOrder,
                        )
                    }
                    if (showCollect) {
                        ActionPill(
                            label = "Cobrar cuenta",
                            enabled = enabled,
                            loading = false,
                            colors = colors,
                            // Un solo lima por hoja: el lima es para entregar; cobrar va en crema (Doop W02).
                            inverted = true,
                            modifier = Modifier.weight(1f),
                        ) { onStartCollect() }
                    }
                }
            }
            if (showCollect && ordersInCourse > 0) {
                // Cobrar antes de entregar se vale, pero que no pase sin darse cuenta.
                Text(
                    if (ordersInCourse == 1) {
                        "Ojo: 1 pedido de esta cuenta todavía no se entrega."
                    } else {
                        "Ojo: $ordersInCourse pedidos de esta cuenta todavía no se entregan."
                    },
                    color = if (colors.isDark) StateAmber else Color(0xFF9A5B00),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                )
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
                // Renovar = rentar el siguiente horario pegado al turno y cobrarlo. Si el turno ya terminó
                // (cancha libre con la sesión aún abierta), se renta desde ahora: pegarlo al fin daba
                // RESERVATION_IN_PAST (D07).
                val turnEnd = session.endsAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
                val renewFrom = turnEnd?.takeIf { it.isAfter(Instant.now()) }
                RentalDurationPicker(
                    title = if (renewFrom != null) "Alargar turno" else "Rentar ahora",
                    verb = if (renewFrom != null) "Alargar" else "Rentar",
                    pricePerHour = pricePerHour ?: availability.pricePerHour,
                    durations = rentalDurations,
                    durationsFailed = rentalDurationsFailed,
                    onRetryDurations = onRetryDurations,
                    enabled = enabled,
                    colors = colors,
                    renewal = renewFrom != null,
                    primary = !hasPending,
                    draft = rentalDraft,
                    onDraftChange = onRentalDraftChange,
                    askName = true,
                    maxPlayers = rentalMaxPlayers,
                    // #27: alargar el turno conserva los jugadores y el nombre de la renta en curso.
                    onConfirm = { minutes, players, name ->
                        onRent(minutes, renewFrom, players ?: detail.rent?.players, name ?: detail.rent?.customerName)
                    },
                )
            } else if (isCourt || session.endsAt != null) {
                CollectLabel(if (session.endsAt == null) "Iniciar turno" else "Alargar turno", colors)
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
            // Liberar aparece hasta que la cuenta está cobrada; antes, la hoja ya dice por qué no.
            AnimatedVisibility(
                visible = !hasPending,
                enter =
                    fadeIn(spring(stiffness = 300f)) + expandVertically(spring(stiffness = Spring.StiffnessMediumLow)),
                exit = fadeOut(spring(stiffness = 500f)) + shrinkVertically(spring(stiffness = Spring.StiffnessMedium)),
            ) {
                // Cobrada y sin nada por entregar, lo que sigue es liberarla para el siguiente cliente: si no,
                // sus pedidos se suman a la misma cuenta.
                val readyToRelease = ordersInCourse == 0 && account?.orders?.isNotEmpty() == true
                Column(
                    modifier =
                        Modifier.padding(top = if (rentable || isCourt || session.endsAt != null) 14.dp else 0.dp),
                ) {
                    if (readyToRelease) {
                        Text(
                            "Todo cobrado y entregado. Libérala para que el siguiente cliente empiece una cuenta nueva.",
                            color = colors.textSecondary,
                            fontSize = 12.5.sp,
                            modifier = Modifier.padding(bottom = 10.dp),
                        )
                    }
                    // La pregunta se queda hasta que confirme o cancele; si se iba sola, parecía que no pasaba nada.
                    var confirmingRelease by rememberSaveable { mutableStateOf(false) }
                    if (ordersInCourse > 0) {
                        Text(
                            "Hay pedidos sin entregar: no se puede liberar todavía.",
                            color = colors.textSecondary,
                            fontSize = 12.5.sp,
                            modifier = Modifier.padding(bottom = 10.dp),
                        )
                    }
                    ActionPill(
                        label = if (confirmingRelease) "¿Seguro? Se cierra la cuenta" else "Liberar espacio",
                        enabled = enabled && ordersInCourse == 0,
                        loading = acting && busyKey == "release",
                        colors = colors,
                        primary = readyToRelease || confirmingRelease,
                    ) {
                        if (confirmingRelease) {
                            confirmingRelease = false
                            busyKey = "release"
                            onRelease()
                        } else {
                            confirmingRelease = true
                        }
                    }
                    if (confirmingRelease) {
                        // #19: antes de liberar se dice qué pasa con lo pagado; nadie lo tenía claro.
                        val accountPaid = account?.paid?.moneyOrZero() ?: BigDecimal.ZERO
                        val rentPaid = detail.rent?.paid?.moneyOrZero() ?: BigDecimal.ZERO
                        Text(
                            releaseOutcomeText(
                                somethingPaid = accountPaid.signum() > 0 || rentPaid.signum() > 0,
                                timeLeft = (millisUntil(session.endsAt) ?: 0L) > 0L,
                            ),
                            color = colors.textSecondary,
                            fontSize = 12.5.sp,
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        )
                        TextButton(
                            onClick = { confirmingRelease = false },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        ) {
                            Text("Cancelar", color = colors.textSecondary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Lo que pasa con lo pagado al liberar (#19): lo cobrado se queda en la caja del turno (el backend solo cierra la
 * sesión, no devuelve nada), el tiempo que quedaba de una renta se pierde y el siguiente cliente empieza cuenta nueva.
 */
internal fun releaseOutcomeText(
    somethingPaid: Boolean,
    timeLeft: Boolean,
): String =
    buildString {
        if (somethingPaid) {
            append("Lo que ya se cobró se queda en la caja del turno: liberar no lo devuelve.")
        } else {
            append("No hay nada cobrado en esta cuenta.")
        }
        if (timeLeft) append(" El tiempo que le quedaba se pierde.")
        append(" La cuenta se cierra y el siguiente cliente empieza una nueva.")
    }

@Composable
private fun SectionCard(
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Sin caja: la hoja ya es la superficie; los bloques se separan con aire y una línea.
    val reduced = reducedMotion()
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = colors.cardBorder, thickness = 1.dp)
        Column(
            modifier =
                Modifier
                    .padding(top = 14.dp)
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
            Text(moneyLabel(order.total), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.textPrimary)
            Text(
                if (order.pending) "sin cobrar" else "cobrado",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 2.dp),
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
                append("Total ${moneyLabel(collection.total)}")
                if (collection.method == AccountPaymentMethod.TERMINAL) {
                    append(" · Cobrado con la terminal")
                } else {
                    append(" · Recibido ${moneyLabel(collection.received)}")
                }
                if (!collection.settled) append(" · Quedan ${moneyLabel(collection.remaining)} por cobrar")
            },
            fontSize = 12.5.sp,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = 10.dp),
        )
        // #19: sin cambio que dar, «Entrega de cambio $0» solo confundía.
        if (collection.method == AccountPaymentMethod.CASH && collection.change.moneyOrZero().signum() > 0) {
            Text(
                "Entrega de cambio",
                fontSize = 12.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 10.dp),
            )
            RollingText(
                text = moneyLabel(shownChange),
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
    /** Un segundo renglón, como el precio de una duración. */
    detail: String? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(if (detail != null) 18.dp else 24.dp)
    // Lo elegido va en tinta: el lima queda para la acción principal de la hoja.
    val container = animateSelectionColor(if (selected) colors.textPrimary else colors.cardInner, "chip-container")
    val ink = animateSelectionColor(if (selected) colors.background else colors.textPrimary, "chip-ink")
    val detailInk = animateSelectionColor(if (selected) colors.background else colors.textSecondary, "chip-detail")
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.45f,
        animationSpec = if (reducedMotion()) snap() else spring(stiffness = Spring.StiffnessMedium),
        label = "chip-alpha",
    )
    Box(
        modifier =
            modifier
                .semantics { if (selected) this.selected = true }
                .physicalPress(enabled = enabled, onClick = onClick)
                .graphicsLayer { this.alpha = alpha }
                .height(if (detail != null) 58.dp else 48.dp)
                .clip(shape)
                .background(container)
                .border(1.dp, if (selected) colors.textPrimary else colors.cardBorder, shape),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Con 4 duraciones «+1 h 30» no cabía y se cortaba en «+1 h»: la letra se achica antes (D07).
            Text(
                label,
                fontWeight = FontWeight.Bold,
                color = ink,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 14.sp, stepSize = 0.5.sp),
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            if (detail != null) {
                Text(
                    detail,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = detailInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
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
    /** En tinta en vez de lima: la acción final de una hoja que ya usa el lima para otra cosa. */
    inverted: Boolean = false,
    /** Solo el contorno: la opción secundaria junto a la principal. */
    outlined: Boolean = false,
    shape: Shape = CircleShape,
    height: Dp = 50.dp,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    onClick: () -> Unit,
) {
    val active = enabled && !loading
    val container =
        animateSelectionColor(
            when {
                outlined -> Color.Transparent
                loading && inverted -> colors.textPrimary
                loading -> colors.accentLime
                !enabled -> colors.pillBackground
                inverted -> colors.textPrimary
                primary -> colors.accentLime
                else -> colors.cardInner
            },
            "action-container",
        )
    val ink =
        animateSelectionColor(
            when {
                outlined && enabled -> colors.textPrimary
                outlined -> colors.textSecondary
                loading && inverted -> colors.background
                loading -> colors.accentInk
                !enabled -> colors.textSecondary
                inverted -> colors.background
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
                .height(height)
                .clip(shape)
                .background(container)
                .then(if (outlined) Modifier.border(1.5.dp, colors.cardBorder, shape) else Modifier)
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
                Text(label, fontWeight = fontWeight, fontSize = fontSize, color = ink, maxLines = 1)
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
        tip: String?,
    ) -> Unit,
    title: String? = null,
    /** La renta de mostrador solo se cobra en efectivo; la cuenta abierta también con la terminal. */
    allowTerminal: Boolean = true,
    /** La propina va en el cobro de la cuenta; la renta de mostrador no la lleva. */
    allowTip: Boolean = true,
    /** Lo que falta (pendiente menos abonado); null sin abonos. */
    remaining: String? = null,
    /** Lo ya abonado sin liquidar. */
    paidIn: String = "0.00",
    /** Abonar por monto o partes iguales (docs/dividir-cuenta.md); null lo oculta. */
    onAbono: AbonoHandler? = null,
    /** El modo con que abre la hoja (capturas y pruebas). */
    initialSplit: SplitMode = SplitMode.ALL,
    /** Quien abre la hoja ya dice qué se cobra: sin el monto grande de arriba. */
    showsHeader: Boolean = true,
    /** Personas que faltan tras un abono en partes; null empieza en 2. */
    initialPeople: Int? = null,
    /** Controles propios de quien abre la hoja (la renta: duración y jugadores), bajo el monto. */
    headerExtra: (@Composable ColumnScope.() -> Unit)? = null,
    /**
     * R34: la selección de pedidos dura lo que dura esta cuenta (la sesión), no la lista que trae cada sondeo.
     * Un pedido que llega después entra sin marcar y uno que ya no está se quita.
     */
    selectionKey: Any? = Unit,
    /** R16: hay algo sin mandar (jugadores de la renta): el botón se apaga y dice esto. */
    blockedReason: String? = null,
) {
    val payableIds = payable.map { it.id }.toSet()
    var chosen by rememberSaveable(selectionKey, stateSaver = StringSetSaver) { mutableStateOf(payableIds) }
    val selected = chosen intersect payableIds
    val paidInAmount = paidIn.moneyOrZero()
    val remainingAmount = remaining?.moneyOrZero() ?: total.moneyOrZero()
    val splitLocked = paidInAmount.signum() > 0
    var split by rememberSaveable(initialSplit) {
        mutableStateOf(
            if (splitLocked &&
                initialSplit in SplitMode.WHOLE_ORDERS
            ) {
                SplitMode.AMOUNT
            } else {
                initialSplit
            },
        )
    }
    if (splitLocked && split in SplitMode.WHOLE_ORDERS) split = SplitMode.AMOUNT
    var abonoText by rememberSaveable { mutableStateOf("") }
    var people by rememberSaveable(initialPeople) { mutableIntStateOf(initialPeople ?: 2) }
    val byOrders = onAbono == null || split in SplitMode.WHOLE_ORDERS
    val splitting = (onAbono == null || split == SplitMode.ORDERS) && payable.size >= 2
    val allSelected = selected.size == payable.size
    val totalAmount =
        when {
            !byOrders && split == SplitMode.PARTS -> AccountSplit.partAmount(remainingAmount, people)
            !byOrders ->
                MoneyInput.parse(abonoText)?.setScale(2, java.math.RoundingMode.DOWN)
                    ?: BigDecimal.ZERO
            payable.isEmpty() -> total.moneyOrZero()
            // R13: el servidor espera lo que falta de la cuenta, no la suma de los totales brutos.
            allSelected -> (remaining ?: total).moneyOrZero()
            else ->
                payable.filter { it.id in selected }.fold(BigDecimal.ZERO) { sum, order ->
                    sum + order.due.moneyOrZero()
                }
        }
    val abonoValid = byOrders || AccountSplit.isValidAmount(totalAmount, remainingAmount)
    val totalLabel = Money.format(totalAmount)
    var tipChoice by rememberSaveable(stateSaver = TipChoiceSaver) { mutableStateOf<TipChoice>(TipChoice.None) }
    val tipAmount = if (allowTip) tipChoice.amountFor(totalAmount) else BigDecimal.ZERO
    // Lo que paga la mesa: la cuenta más la propina. El total esperado sigue siendo la cuenta.
    val chargeAmount = totalAmount + tipAmount
    val chargeLabel = Money.format(chargeAmount)
    // Lo que el cajero escribió; sin tocar nada el efectivo sigue al total (cobro justo).
    var typed by rememberSaveable { mutableStateOf<String?>(null) }
    var method by rememberSaveable { mutableStateOf(AccountPaymentMethod.CASH) }
    var tipOpen by rememberSaveable { mutableStateOf(false) }
    var otherCash by rememberSaveable { mutableStateOf(false) }
    val withTerminal = allowTerminal && method == AccountPaymentMethod.TERMINAL
    val text = typed ?: chargeLabel
    val received = MoneyInput.parse(text)
    val difference = received?.let { it - chargeAmount }
    val canConfirm =
        abonoValid && totalAmount.signum() > 0 && (withTerminal || (difference != null && difference.signum() >= 0))
    val shortfall = !withTerminal && difference != null && difference.signum() < 0
    val quickAmounts = remember(chargeLabel) { quickCashAmounts(chargeAmount) }
    val changeInk = animateSelectionColor(if (shortfall) StateCoral else colors.textPrimary, "change-ink")

    // Con letra grande o dividiendo la cuenta el contenido no cabe: lo de arriba se desplaza y el botón
    // de cobrar se queda fijo abajo, siempre a la mano.
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (showsHeader) {
                Column(modifier = Modifier.arrive(0)) {
                    Text(
                        title ?: when {
                            !byOrders -> "Abonar a la cuenta"
                            splitting && !allSelected -> "Cobrar parte de la cuenta"
                            else -> "Cobrar cuenta"
                        },
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    RollingText(
                        text = moneyLabel(totalLabel),
                        style = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.6).sp),
                        color = colors.textPrimary,
                    )
                }
            }
            if (headerExtra != null) {
                Column(modifier = Modifier.arrive(1), content = headerExtra)
            }
            if (onAbono != null) {
                if (paidInAmount.signum() > 0) {
                    Text(
                        "Abonado ${moneyLabel(paidInAmount)} · Falta ${moneyLabel(remainingAmount)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        modifier = Modifier.arrive(1),
                    )
                }
                Column(modifier = Modifier.arrive(1), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CollectLabel("¿Qué se cobra ahora?", colors)
                    AdminSegmented(
                        options = SplitMode.entries.map { it to it.label },
                        selected = split,
                        colors = colors,
                        enabled = !confirming,
                        onSelect = { mode ->
                            if (!(splitLocked && mode in SplitMode.WHOLE_ORDERS)) {
                                split = mode
                                if (mode == SplitMode.ALL) chosen = payableIds
                                typed = null
                            }
                        },
                    )
                    if (splitLocked) {
                        Text(
                            "Hay abonos sin liquidar: termina la división por montos.",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                        )
                    }
                    if (split == SplitMode.AMOUNT) {
                        OperationalField(
                            value = abonoText,
                            onValueChange = { raw ->
                                abonoText = MoneyInput.sanitize(raw)
                                typed = null
                            },
                            label = "Monto de este abono",
                            colors = colors,
                            supporting = "Falta ${moneyLabel(Money.format(remainingAmount))}",
                            isError = abonoText.isNotBlank() && !abonoValid,
                            enabled = !confirming,
                            keyboardType = KeyboardType.Decimal,
                        )
                    }
                    if (split == SplitMode.PARTS) {
                        Text(
                            "¿Entre cuántos? Paga $${Money.format(
                                AccountSplit.partAmount(remainingAmount, people),
                            )} cada uno; " +
                                "el último paga lo que quede.",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            PartsChoices.forEach { n ->
                                ChipButton(
                                    label = if (n == 1) "Resto" else "$n",
                                    enabled = !confirming,
                                    colors = colors,
                                    modifier = Modifier.weight(1f),
                                    selected = people == n,
                                ) {
                                    people = n
                                    typed = null
                                }
                            }
                        }
                    }
                }
            }
            if (splitting) {
                PayerShortcuts(
                    orders = payable,
                    enabled = !confirming,
                    colors = colors,
                    onPick = { ids ->
                        chosen = ids
                        typed = null
                    },
                )
                SplitOrdersPicker(
                    orders = payable,
                    selected = selected,
                    enabled = !confirming,
                    colors = colors,
                    onToggle = { id ->
                        chosen = if (id in selected) selected - id else selected + id
                        typed = null
                    },
                )
                if (!allSelected && selected.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            partialPayer(payable, selected)?.let { "Parte de $it" } ?: "Esta parte",
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                        )
                        RollingText(
                            text = moneyLabel(totalLabel),
                            style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Black),
                            color = colors.textPrimary,
                        )
                    }
                }
            }
            if (allowTerminal) {
                Column(modifier = Modifier.arrive(1), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CollectLabel("¿Cómo paga?", colors)
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
                    "Cobra $$chargeLabel en tu terminal de tarjeta y confirma aquí. No entra al efectivo de la caja.",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.arrive(2),
                )
            } else {
                Column(modifier = Modifier.arrive(1), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        ChipButton(
                            label = "Exacto",
                            enabled = !confirming,
                            colors = colors,
                            modifier = Modifier.weight(1f),
                            selected = !otherCash && received != null && received.compareTo(chargeAmount) == 0,
                        ) {
                            typed = null
                            otherCash = false
                        }
                        quickAmounts.take(2).forEach { amount ->
                            ChipButton(
                                label = moneyLabel(amount.setScale(0)),
                                enabled = !confirming,
                                colors = colors,
                                modifier = Modifier.weight(1f),
                                selected = !otherCash && received != null && received.compareTo(amount) == 0,
                            ) {
                                typed = Money.format(amount)
                                otherCash = false
                            }
                        }
                        ChipButton(
                            label = "Otro",
                            enabled = !confirming,
                            colors = colors,
                            modifier = Modifier.weight(1f),
                            selected = otherCash,
                        ) { otherCash = true }
                    }
                    if (otherCash) {
                        OperationalField(
                            value = text,
                            onValueChange = { typed = MoneyInput.sanitize(it) },
                            label = "Otro monto",
                            colors = colors,
                            enabled = !confirming,
                            keyboardType = KeyboardType.Decimal,
                        )
                    }
                }
                val change = (difference ?: BigDecimal.ZERO).abs()
                Row(
                    modifier =
                        Modifier
                            .arrive(2)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                animateSelectionColor(
                                    if (shortfall) StateCoral.copy(alpha = 0.18f) else colors.accentLime,
                                    "change-background",
                                ),
                            ).padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (shortfall) "Falta" else "Su cambio",
                        fontSize = 14.sp,
                        color = if (shortfall) changeInk else colors.accentInk,
                        fontWeight = FontWeight.Bold,
                    )
                    RollingText(
                        text = moneyLabel(Money.format(change)),
                        style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
                        color = if (shortfall) changeInk else colors.accentInk,
                    )
                }
            }
            if (allowTip) {
                Column(modifier = Modifier.arrive(2), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .physicalPress(scale = PhysicalPressScale.Small) { tipOpen = !tipOpen }
                                .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Propina",
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            (if (tipAmount.signum() > 0) moneyLabel(Money.format(tipAmount)) else "Sin propina") + " ›",
                            fontSize = 13.sp,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    if (tipOpen) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            ChipButton(
                                label = "Sin",
                                enabled = !confirming,
                                colors = colors,
                                modifier = Modifier.weight(1f),
                                selected = tipChoice == TipChoice.None,
                            ) {
                                tipChoice = TipChoice.None
                                typed = null
                            }
                            TipPercents.forEach { percent ->
                                ChipButton(
                                    label = "$percent%",
                                    enabled = !confirming,
                                    colors = colors,
                                    modifier = Modifier.weight(1f),
                                    selected = tipChoice == TipChoice.Percent(percent),
                                ) {
                                    tipChoice = TipChoice.Percent(percent)
                                    typed = null
                                }
                            }
                            ChipButton(
                                label = "Otro",
                                enabled = !confirming,
                                colors = colors,
                                modifier = Modifier.weight(1f),
                                selected = tipChoice is TipChoice.Custom,
                            ) {
                                tipChoice = TipChoice.Custom((tipChoice as? TipChoice.Custom)?.amount.orEmpty())
                                typed = null
                            }
                        }
                        val custom = tipChoice as? TipChoice.Custom
                        if (custom != null) {
                            OperationalField(
                                value = custom.amount,
                                onValueChange = { raw ->
                                    tipChoice = TipChoice.Custom(MoneyInput.sanitize(raw))
                                    typed = null
                                },
                                label = "Monto de la propina",
                                colors = colors,
                                enabled = !confirming,
                                keyboardType = KeyboardType.Decimal,
                            )
                        }
                    }
                }
            }
        }
        Box(modifier = Modifier.padding(horizontal = 22.dp).arrive(3)) {
            AdminPrimaryButton(
                title =
                    when {
                        blockedReason != null -> blockedReason
                        !byOrders && withTerminal -> "Abonado en la terminal $$chargeLabel"
                        !byOrders -> "Abonar $$chargeLabel"
                        withTerminal -> "Cobrado en la terminal $$chargeLabel"
                        else ->
                            "Cobrar $$chargeLabel" +
                                (pendingAfterPartial(payable, selected).takeIf { byOrders }?.let { " · $it" } ?: "")
                    },
                colors = colors,
                busy = confirming,
                enabled = canConfirm && blockedReason == null,
            ) {
                val orderIds = if (allSelected) null else selected.toList()
                val tip = tipAmount.asTipWire()
                if (!byOrders && onAbono != null) {
                    val mode = if (split == SplitMode.PARTS) AbonoMode.PARTS else AbonoMode.AMOUNT
                    onAbono(
                        if (withTerminal) AccountPaymentMethod.TERMINAL else AccountPaymentMethod.CASH,
                        mode,
                        totalLabel.takeIf { mode == AbonoMode.AMOUNT },
                        people.takeIf { mode == AbonoMode.PARTS },
                        received?.let { Money.format(it) }.takeIf { !withTerminal },
                        Money.format(remainingAmount),
                        tip,
                    )
                } else if (withTerminal) {
                    onConfirm(AccountPaymentMethod.TERMINAL, null, totalLabel, orderIds, tip)
                } else {
                    received?.let { onConfirm(AccountPaymentMethod.CASH, Money.format(it), totalLabel, orderIds, tip) }
                }
            }
        }
    }
}

/** Un abono por monto o partes iguales, como lo manda la hoja de cobro. */
internal typealias AbonoHandler = (
    method: AccountPaymentMethod,
    mode: AbonoMode,
    amount: String?,
    parts: Int?,
    received: String?,
    expectedRemaining: String,
    tip: String?,
) -> Unit

/** Cómo se divide la cuenta en la hoja de cobro. */
internal enum class SplitMode(
    val label: String,
) {
    ALL("Todo"),
    ORDERS("Por pedidos"),
    AMOUNT("Por monto"),
    PARTS("Partes"),
    ;

    companion object {
        val WHOLE_ORDERS = setOf(ALL, ORDERS)
    }
}

/** Entre cuántos se divide; "Resto" (1) paga todo lo que queda. */
private val PartsChoices = listOf(2, 3, 4, 5, 1)

/** "Parte de Ana": marca lo que Ana pidió y lo que dijo que paga, sin lo que otra persona dijo pagarle (I07). */
@Composable
private fun PayerShortcuts(
    orders: List<AccountOrder>,
    enabled: Boolean,
    colors: OperationalColors,
    onPick: (Set<String>) -> Unit,
) {
    val payers = orders.mapNotNull { it.partOf }.distinct()
    if (payers.isEmpty()) return
    Row(
        modifier = Modifier.fillMaxWidth().arrive(1),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        payers.forEach { payer ->
            ChipButton(
                label = "Parte de $payer",
                enabled = enabled,
                colors = colors,
                modifier = Modifier.weight(1f),
            ) { onPick(orders.filter { it.partOf == payer }.map { it.id }.toSet()) }
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
    Column(modifier = Modifier.arrive(1), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(colors.cardBackground)
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(22.dp)),
        ) {
            orders.forEachIndexed { index, order ->
                val checked = order.id in selected
                val ink = animateSelectionColor(if (checked) colors.textPrimary else colors.textMuted, "split-ink")
                if (index >
                    0
                ) {
                    HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(horizontal = 16.dp))
                }
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = enabled) { onToggle(order.id) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = null,
                        enabled = enabled,
                        colors =
                            CheckboxDefaults.colors(
                                checkedColor = colors.accentLime,
                                checkmarkColor = colors.accentInk,
                                uncheckedColor = colors.textMuted,
                            ),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "#${order.folio} · ${order.owner ?: order.clientName ?: "Cliente"}" +
                                (order.payer?.let { " · Paga $it" } ?: ""),
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = ink,
                        )
                    }
                    Text(
                        moneyLabel(order.due),
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = ink,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
            }
        }
    }
}

/** Las etiquetas de la hoja de cobro, en versalitas como en el resto del personal. */
@Composable
private fun CollectLabel(
    text: String,
    colors: OperationalColors,
) {
    Text(
        text.uppercase(),
        fontSize = 11.5.sp,
        color = colors.textSecondary,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.4.sp,
    )
}

/** De quién es lo que se cobra ahora: "Ana", o null si son varios o nadie dijo su nombre. */
internal fun partialPayer(
    payable: List<AccountOrder>,
    selected: Set<String>,
): String? =
    payable
        .filter { it.id in selected }
        .map { it.partOf ?: it.clientName }
        .distinct()
        .singleOrNull()

/** "faltan $158 de Luis": lo que queda sin cobrar cuando se cobra solo una parte por pedidos. */
internal fun pendingAfterPartial(
    payable: List<AccountOrder>,
    selected: Set<String>,
): String? {
    val rest = payable.filter { it.id !in selected }
    if (rest.isEmpty() || rest.size == payable.size) return null
    val amount = rest.fold(BigDecimal.ZERO) { sum, order -> sum + order.due.moneyOrZero() }
    val names = rest.mapNotNull { it.partOf ?: it.clientName }.distinct()
    val who = if (names.isEmpty()) "" else " de ${names.joinToString(" y ")}"
    return "faltan ${moneyLabel(Money.format(amount))}$who"
}

private fun rentalLabel(minutes: Int): String =
    when {
        minutes % 60 == 0 -> "${minutes / 60} h"
        minutes > 60 -> "${minutes / 60} h ${minutes % 60}"
        else -> "$minutes min"
    }

/** «Renta de pista · pagada $300 (2 h)»: lo ya pagado del turno, que no se vuelve a cobrar. */
internal fun rentPaidLine(
    venue: VenueContext,
    rent: SpaceRent,
): String =
    buildString {
        append("Renta de ${venue.vocabulary.court} · pagada ${moneyLabel(rent.paid)}")
        if (rent.minutes > 0) append(" (${rentalLabel(rent.minutes)})")
    }

/** «4 jugadores · a nombre de Ana»; null si no hay ninguno de los dos datos. */
internal fun rentWhoLine(
    players: Int?,
    customerName: String?,
): String? =
    listOfNotNull(
        players?.takeIf { it > 0 }?.let { "$it ${if (it == 1) "jugador" else "jugadores"}" },
        customerName?.takeIf { it.isNotBlank() }?.let { "a nombre de ${it.trim()}" },
    ).joinToString(" · ").ifEmpty { null }

/** Lo que cuesta rentar esos minutos, con la misma regla que el servidor; null sin precio. */
internal fun rentalPrice(
    pricePerHour: String?,
    minutes: Int,
): String? = pricePerHour?.let { runCatching { moneyLabel(ReservationSlots.amountFor(it, minutes)) }.getOrNull() }

/** "Rentar 1 h · $150": el botón repite lo elegido y su precio antes de cobrar. */
internal fun rentalActionLabel(
    verb: String,
    minutes: Int,
    pricePerHour: String?,
): String = listOfNotNull("$verb ${rentalLabel(minutes)}", rentalPrice(pricePerHour, minutes)).joinToString(" · ")

/**
 * Duraciones con su precio para rentar o renovar una cancha. Tocar una duración solo la marca;
 * el cobro empieza con el botón de abajo, que dice qué se renta y cuánto cuesta.
 */
@Composable
internal fun RentalDurationPicker(
    title: String,
    verb: String,
    pricePerHour: String?,
    durations: List<Int>,
    enabled: Boolean,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    renewal: Boolean = false,
    /** Con una cuenta por cobrar, Cobrar es la acción principal y esta queda como secundaria. */
    primary: Boolean = true,
    durationsFailed: Boolean = false,
    onRetryDurations: (() -> Unit)? = null,
    /**
     * U04: lo elegido vive en el ViewModel para que cerrar el cobro (o salir de la hoja) no lo borre.
     * Con [onDraftChange] null el picker guarda lo suyo (pruebas y capturas).
     */
    draft: RentalDraft? = null,
    onDraftChange: ((minutes: Int?, players: Int, name: String) -> Unit)? = null,
    /** #27: Mesero y Caja anotan a nombre de quién va la renta (opcional). */
    askName: Boolean = false,
    /** R25: cuántos caben en la pista (`capacidad` de la disponibilidad). */
    maxPlayers: Int = ReservationSlots.MAX_PLAYERS,
    onConfirm: (minutes: Int, players: Int?, name: String?) -> Unit,
) {
    val venue = LocalVenueContext.current
    var localMinutes by remember(durations) { mutableStateOf<Int?>(null) }
    var localPlayers by remember { mutableIntStateOf(0) }
    var localName by remember { mutableStateOf("") }
    val hoisted = onDraftChange != null
    val chosen = if (hoisted) draft?.minutes?.takeIf { it in durations } else localMinutes
    val players = if (hoisted) draft?.players ?: 0 else localPlayers
    val name = if (hoisted) draft?.customerName.orEmpty() else localName
    val update: (Int?, Int, String) -> Unit = { m, p, n ->
        if (onDraftChange != null) {
            onDraftChange(m, p, n)
        } else {
            localMinutes = m
            localPlayers = p
            localName = n
        }
    }
    if (durations.isEmpty()) {
        // Sin las duraciones del servidor no se ofrece ninguna: una inventada termina en 400.
        Column(modifier = modifier.fillMaxWidth()) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = colors.textSecondary)
            Text(
                if (durationsFailed) "No pudimos leer los tiempos de renta." else "Cargando tiempos de renta…",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (durationsFailed && onRetryDurations != null) {
                ChipButton(
                    label = "Reintentar",
                    enabled = enabled,
                    colors = colors,
                    modifier = Modifier.padding(top = 8.dp),
                    onClick = onRetryDurations,
                )
            }
        }
        return
    }
    // En un boliche se anota cuántos juegan, para preparar la pista y los zapatos. R11: es obligatorio (el
    // servidor responde PLAYERS_REQUIRED sin ellos); la renovación conserva los del turno.
    val askPlayers = venue.capabilities.asksPlayerCount && !renewal
    val playersMissing = askPlayers && players == 0
    val showName = askName && !renewal
    Column(modifier = modifier.fillMaxWidth()) {
        if (askPlayers) {
            PlayersStepper(players = players, enabled = enabled, colors = colors, max = maxPlayers) {
                update(chosen, it, name)
            }
        }
        if (showName) {
            OperationalField(
                value = name,
                onValueChange = { update(chosen, players, it.take(RENTAL_NAME_MAX)) },
                label = "Nombre (opcional)",
                colors = colors,
                placeholder = "A nombre de quién",
                enabled = enabled,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = colors.textSecondary)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            durations.forEach { minutes ->
                ChipButton(
                    label = (if (renewal) "+" else "") + rentalLabel(minutes),
                    enabled = enabled,
                    colors = colors,
                    modifier = Modifier.weight(1f),
                    selected = chosen == minutes,
                    detail = rentalPrice(pricePerHour, minutes),
                ) { update(minutes, players, name) }
            }
        }
        AnimatedVisibility(
            visible = chosen != null,
            enter = fadeIn(spring(stiffness = 300f)) + expandVertically(spring(stiffness = Spring.StiffnessMediumLow)),
            exit = fadeOut(spring(stiffness = 500f)) + shrinkVertically(spring(stiffness = Spring.StiffnessMedium)),
        ) {
            // El último valor elegido se queda mientras el botón se va, para que no parpadee.
            var shown by remember { mutableIntStateOf(durations.firstOrNull() ?: 0) }
            chosen?.let { shown = it }
            ActionPill(
                label = if (playersMissing) "Anota cuántos juegan" else rentalActionLabel(verb, shown, pricePerHour),
                enabled = enabled && !playersMissing,
                loading = false,
                colors = colors,
                primary = primary,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                chosen?.let {
                    onConfirm(
                        it,
                        players.takeIf { askPlayers && it > 0 },
                        name.trim().takeIf { showName && it.isNotEmpty() },
                    )
                }
            }
        }
    }
}

/** El backend admite `nombre_cliente` de hasta 120 caracteres. */
private const val RENTAL_NAME_MAX = 120

/** Jugadores de una pista de boliche: de 1 a la capacidad de la pista (30 sin capacidad), o sin anotar. */
@Composable
internal fun PlayersStepper(
    players: Int,
    enabled: Boolean,
    colors: OperationalColors,
    /** R25: la capacidad de la pista. */
    max: Int = ReservationSlots.MAX_PLAYERS,
    /** R11: con la renta ya apartada no se regresa a «Sin anotar». */
    min: Int = 0,
    onChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text("Jugadores", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = colors.textSecondary)
            Text(
                if (players == 0) "Sin anotar" else "$players ${if (players == 1) "jugador" else "jugadores"}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = colors.textPrimary,
            )
        }
        ChipButton(label = "−", enabled = enabled && players > min, colors = colors) { onChange(players - 1) }
        ChipButton(label = "+", enabled = enabled && players < max, colors = colors) { onChange(players + 1) }
    }
}

/**
 * Cobro en efectivo de una renta de mostrador: el total es lo que paga el cliente (el precio de la
 * app con efectivo), con los mismos atajos de billete y el cambio. Cerrar sin cobrar libera el horario
 * y lo elegido (duración y jugadores) regresa a la hoja de la pista.
 *
 * U04: aquí mismo se cambia la duración y, en un boliche, los jugadores: el servidor no edita un
 * apartado, así que [onChange] lo suelta y vuelve a apartar (como el cliente en su hoja de pago).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RentalCollectSheet(
    rental: Reservation,
    confirming: Boolean,
    colors: OperationalColors,
    /** R16: lleva los jugadores tocados que aún no se mandaban (null si no hay) para que no se pierdan. */
    onDismiss: (pendingPlayers: Int?) -> Unit,
    onConfirm: (received: String) -> Unit,
    /** Duraciones que acepta el servidor; vacía oculta el cambio de duración. */
    durations: List<Int> = emptyList(),
    /** Vuelve a apartar con otra duración o jugadores; null no deja cambiarlos. */
    onChange: ((minutes: Int, players: Int?) -> Unit)? = null,
    /** R25: cuántos caben en la pista. */
    maxPlayers: Int = ReservationSlots.MAX_PLAYERS,
    /** R16: los jugadores tocados que aún no se mandan; los guarda el ViewModel (null = los de la reserva). */
    playersDraft: Int? = null,
    onPlayersDraft: (Int) -> Unit = {},
) {
    val venue = LocalVenueContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // R16: el borrador de jugadores lo guarda el ViewModel, para que «Cobrar», cerrar y girar lo vean.
    val players = playersDraft ?: (rental.players ?: 0)
    val askPlayers = onChange != null && venue.capabilities.asksPlayerCount
    val playersDirty = askPlayers && players != (rental.players ?: 0)
    // R11: una renovación (empieza al terminar el turno) hereda los jugadores; la renta de ahora los pide.
    val renewal = remember(rental.id) { rental.start.isAfter(Instant.now()) }
    val playersMissing = askPlayers && !renewal && players == 0
    ModalBottomSheet(
        onDismissRequest = { onDismiss(players.takeIf { playersDirty }) },
        sheetState = sheetState,
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        CollectAccountContent(
            total = rental.customerPrice?.cashOrBalance ?: rental.amount,
            payable = emptyList(),
            confirming = confirming,
            colors = colors,
            title = "Rentar ${rental.courtName ?: venue.vocabulary.court} · ${rentalLabel(rental.durationMinutes)}",
            // #16: el servidor solo cobra la renta de mostrador en efectivo; la hoja lo dice en lugar de esconderlo.
            allowTerminal = false,
            allowTip = false,
            onConfirm = { _, received, _, _, _ -> received?.let(onConfirm) },
            // R16: con jugadores sin mandar se cobraría la renta vieja: «Cobrar» espera a que se vuelva a apartar.
            blockedReason =
                when {
                    playersDirty -> "Apartando…"
                    playersMissing -> "Anota cuántos juegan"
                    else -> null
                },
            headerExtra = {
                RentalCollectControls(
                    rental = rental,
                    durations = durations,
                    enabled = !confirming,
                    colors = colors,
                    onChange = onChange,
                    players = players,
                    onPlayers = onPlayersDraft,
                    maxPlayers = maxPlayers,
                    minPlayers = if (renewal) 0 else 1,
                )
            },
        )
    }
}

/** Cuánto se espera tras el último toque de jugadores antes de volver a apartar. */
private const val PLAYERS_SETTLE_MS = 700L

/** Duración, jugadores y «Solo efectivo» de la hoja de cobro de la renta. */
@Composable
internal fun RentalCollectControls(
    rental: Reservation,
    durations: List<Int>,
    enabled: Boolean,
    colors: OperationalColors,
    onChange: ((minutes: Int, players: Int?) -> Unit)?,
    /** R16: el borrador de jugadores lo guarda la hoja de cobro. */
    players: Int,
    onPlayers: (Int) -> Unit,
    maxPlayers: Int = ReservationSlots.MAX_PLAYERS,
    minPlayers: Int = 0,
) {
    val venue = LocalVenueContext.current
    // Cada cambio vuelve a apartar: los toques del stepper se juntan y se manda solo el último.
    if (onChange != null && venue.capabilities.asksPlayerCount) {
        LaunchedEffect(rental.id, players) {
            if (players == (rental.players ?: 0)) return@LaunchedEffect
            delay(PLAYERS_SETTLE_MS)
            onChange(rental.durationMinutes, players.takeIf { it > 0 })
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (onChange != null && durations.isNotEmpty()) {
            // La apartada siempre sale, aunque el dueño haya quitado esa duración mientras tanto.
            val options = (durations + rental.durationMinutes).distinct().sorted()
            CollectLabel("Duración", colors)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                options.forEach { minutes ->
                    ChipButton(
                        label = rentalLabel(minutes),
                        // R11: sin jugadores, volver a apartar con otro tiempo lo rechazaría el servidor.
                        enabled = enabled && !(venue.capabilities.asksPlayerCount && minPlayers > 0 && players == 0),
                        colors = colors,
                        modifier = Modifier.weight(1f),
                        selected = minutes == rental.durationMinutes,
                    ) {
                        if (minutes != rental.durationMinutes) onChange(minutes, players.takeIf { it > 0 })
                    }
                }
            }
        }
        if (onChange != null && venue.capabilities.asksPlayerCount) {
            PlayersStepper(
                players = players,
                enabled = enabled,
                colors = colors,
                max = maxPlayers,
                min = minPlayers,
                onChange = onPlayers,
            )
        }
        Text(
            "Solo efectivo: la renta no se cobra con terminal ni saldo.",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary,
        )
    }
}

/** El turno de una cancha sobre la cancha misma: cuánto queda en grande y a qué hora termina. */
@Composable
private fun CourtTurnHero(
    remaining: Long,
    inGrace: Boolean,
    endsAt: String?,
    modifier: Modifier = Modifier,
) {
    val venue = LocalVenueContext.current
    val bowling = venue.capabilities.drawsLanes
    val field =
        animateSelectionColor(
            when {
                inGrace -> Color(0xFF5A4A1E)
                bowling -> Color(0xFF6B4A2B)
                else -> Color(0xFF2F5A2C)
            },
            "court-field",
        )
    val line = Color.White.copy(alpha = 0.35f)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(field)
                .drawBehind {
                    if (bowling) {
                        drawBowlingLane(line)
                        return@drawBehind
                    }
                    val inset = 14.dp.toPx()
                    val stroke = 1.5.dp.toPx()
                    drawRect(
                        line,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - 2 * inset, size.height - 2 * inset),
                        style = Stroke(stroke),
                    )
                    drawLine(line, Offset(size.width / 2, inset), Offset(size.width / 2, size.height - inset), stroke)
                    drawCircle(line, radius = size.height * 0.2f, center = center, style = Stroke(stroke))
                }.padding(horizontal = 22.dp, vertical = 20.dp),
    ) {
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(
                if (inGrace) "De gracia para decidir" else "Quedan",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            RollingText(
                text = remainingText(remaining),
                style = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.2).sp),
                color = Color.White,
            )
        }
        endsAt?.let {
            Text(
                "Termina $it",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 6.dp),
            )
        }
    }
}

/**
 * Una pista de boliche vista desde la línea de tiro: canales a los lados, flechas de apunte y los
 * diez pinos en triángulo al fondo (derecha).
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBowlingLane(line: Color) {
    val gutter = size.height * 0.14f
    drawRect(Color.Black.copy(alpha = 0.25f), size = Size(size.width, gutter))
    drawRect(
        Color.Black.copy(alpha = 0.25f),
        topLeft = Offset(0f, size.height - gutter),
        size = Size(size.width, gutter),
    )
    // Tablones de la madera.
    val boards = 8
    val laneHeight = size.height - 2 * gutter
    for (i in 1 until boards) {
        val y = gutter + laneHeight * i / boards
        drawLine(Color.White.copy(alpha = 0.06f), Offset(0f, y), Offset(size.width, y), 1f)
    }
    // Línea de falta y flechas.
    val foul = size.width * 0.08f
    drawLine(line, Offset(foul, gutter), Offset(foul, size.height - gutter), 2.dp.toPx())
    val arrowX = size.width * 0.3f
    for (i in 1..4) {
        val y = gutter + laneHeight * i / 5
        drawLine(line, Offset(arrowX - 8.dp.toPx(), y - 4.dp.toPx()), Offset(arrowX, y), 1.5.dp.toPx())
        drawLine(line, Offset(arrowX - 8.dp.toPx(), y + 4.dp.toPx()), Offset(arrowX, y), 1.5.dp.toPx())
    }
    // Pinos: 1-2-3-4 hacia el fondo.
    val pin = Color.White.copy(alpha = 0.8f)
    val radius = size.height * 0.035f
    val step = radius * 3.2f
    val headX = size.width * 0.78f
    for (row in 0..3) {
        val x = headX + row * step * 0.9f
        for (k in 0..row) {
            val y = center.y + (k - row / 2f) * step
            drawCircle(pin, radius, Offset(x, y))
        }
    }
}
