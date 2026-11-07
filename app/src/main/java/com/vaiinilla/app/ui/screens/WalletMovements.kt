package com.vaiinilla.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.WalletMovement
import com.vaiinilla.app.domain.time.BusinessTime
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.cremeMoment
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaColors
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth

/** Los filtros de Movimientos (Doop E04). */
internal enum class MovementFilter(
    val label: String,
) {
    ALL("Todo"),
    PURCHASES("Compras"),
    RELOADS("Recargas"),
    REFUNDS("Devoluciones"),
    CASHBACK("Cashback"),
    ;

    fun matches(type: String): Boolean =
        when (this) {
            ALL -> true
            PURCHASES -> type == "compra"
            RELOADS -> type.startsWith("recarga")
            REFUNDS -> type == "cancelacion" || type == "devolucion_articulo"
            CASHBACK -> type == "cashback"
        }
}

private const val RECENT_MOVEMENTS = 3

/** «MOVIMIENTOS · Ver todos» y los últimos tres en una tarjeta (Doop E02). */
@Composable
internal fun WalletRecentMovements(
    movements: List<WalletMovement>,
    onSeeAll: () -> Unit,
    onMenu: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 28.dp)) {
        WalletSectionHeader(
            title = "Movimientos",
            action = if (movements.isEmpty()) null else "Ver todos",
            onAction = onSeeAll,
        )
        if (movements.isEmpty()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(colors.paper2)
                        .padding(16.dp),
            ) {
                Text(
                    "Aquí verás tus recargas y lo que pagues con saldo.",
                    color = colors.muted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Ver el menú",
                    color = colors.ink,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 10.dp).physicalPress(scale = PhysicalPressScale.Small, onClick = onMenu),
                )
            }
        } else {
            WalletMovementsCard(movements.take(RECENT_MOVEMENTS)) { movement ->
                WalletMovementRow(
                    movement = movement,
                    subtitle =
                        listOfNotNull(
                            cremeMoment(movement.createdAt),
                            movementDetail(movement.place, movement.productsSummary),
                        ).joinToString(" · ").ifBlank { null },
                    showBalanceAfter = false,
                )
            }
        }
    }
}

/** «MIS HIJOS · + Agregar», «MOVIMIENTOS · Ver todos»: la etiqueta de una sección y su acción a la derecha. */
@Composable
internal fun WalletSectionHeader(
    title: String,
    action: String? = null,
    onAction: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title.uppercase(),
            color = colors.muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.2.sp,
        )
        action?.let {
            Text(
                it,
                color = positiveInk(colors),
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.physicalPress(scale = PhysicalPressScale.Small, onClick = onAction),
            )
        }
    }
}

@Composable
internal fun WalletMovementsCard(
    movements: List<WalletMovement>,
    row: @Composable (WalletMovement) -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(colors.paper2),
    ) {
        movements.forEachIndexed { index, movement ->
            if (index > 0) HorizontalDivider(color = colors.line, thickness = 1.dp)
            row(movement)
        }
    }
}

@Composable
internal fun WalletMovementRow(
    movement: WalletMovement,
    subtitle: String?,
    showBalanceAfter: Boolean,
) {
    val colors = LocalVaiinillaColors.current
    val debit = movement.amount.startsWith("-")
    val (icon, tint) = movementIcon(movement.type, debit, colors)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(walletTile(colors)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                movementTitle(movement.type, movement.description, movement.orderFolio),
                color = colors.ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.let {
                Text(
                    it,
                    color = colors.muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                if (debit) "−${moneyLabel(movement.amount.removePrefix("-"))}" else "+${moneyLabel(movement.amount)}",
                color = if (debit) colors.ink else positiveInk(colors),
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
            )
            if (showBalanceAfter) {
                Text(
                    moneyLabel(movement.balanceAfter),
                    color = colors.muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

private fun movementIcon(
    type: String,
    debit: Boolean,
    colors: VaiinillaColors,
): Pair<ImageVector, Color> =
    when {
        type == "cashback" -> Icons.Outlined.Star to colors.yolk
        MovementFilter.REFUNDS.matches(type) -> Icons.AutoMirrored.Rounded.Undo to refundInk(colors)
        type.startsWith("recarga") -> Icons.Outlined.AccountBalanceWallet to positiveInk(colors)
        type == "ajuste" -> Icons.Outlined.Tune to colors.ink2
        debit -> Icons.AutoMirrored.Outlined.ReceiptLong to colors.ink2
        else -> Icons.Outlined.AccountBalanceWallet to positiveInk(colors)
    }

/** El verde de lo que entra: el lima del dibujo de noche; de día, uno que se lea sobre el papel claro. */
internal fun positiveInk(colors: VaiinillaColors): Color = if (colors.isDark) colors.accent else Color(0xFF3D6B12)

private fun refundInk(colors: VaiinillaColors): Color = if (colors.isDark) Color(0xFF8FC7FF) else Color(0xFF1F5F99)

/** El cuadrito de los íconos y el fondo de los números de pasos (`--p3` del dibujo). */
internal fun walletTile(colors: VaiinillaColors): Color = if (colors.isDark) Color(0xFF33342F) else colors.ink.copy(alpha = 0.06f)

/**
 * Movimientos (Doop E04): lo que entró y lo que se gastó este mes, los filtros y los movimientos por día; «Ver octubre»
 * suma el mes anterior.
 */
@Composable
internal fun WalletMovementsPage(
    visible: Boolean,
    movements: List<WalletMovement>,
    onBack: () -> Unit,
    now: Instant = Instant.now(),
) {
    val reduced = reducedMotion()
    BackHandler(enabled = visible, onBack = onBack)
    AnimatedVisibility(
        visible = visible,
        enter =
            if (reduced) {
                fadeIn(snap())
            } else {
                slideInHorizontally(spring(dampingRatio = 0.9f, stiffness = 400f)) { it / 3 } + fadeIn()
            },
        exit = if (reduced) fadeOut(snap()) else slideOutHorizontally(spring(stiffness = 500f)) { it / 3 } + fadeOut(),
        modifier = Modifier.fillMaxSize(),
    ) {
        MovementsPageContent(movements = movements, onBack = onBack, now = now)
    }
}

@Composable
internal fun MovementsPageContent(
    movements: List<WalletMovement>,
    onBack: () -> Unit,
    now: Instant = Instant.now(),
) {
    val colors = LocalVaiinillaColors.current
    var filter by rememberSaveable { mutableStateOf(MovementFilter.ALL) }
    var monthsShown by rememberSaveable { mutableIntStateOf(1) }
    val today = BusinessTime.at(now, BusinessTime.zone).toLocalDate()
    val ledger = remember(movements, now) { WalletLedgerStats.from(movements, now = now) }
    val months = remember(movements) { movements.mapNotNull { it.localDate()?.let(YearMonth::from) }.distinct() }
    val shownMonths = months.take(monthsShown).toSet()
    val visibleMovements =
        movements.filter { movement ->
            val month = movement.localDate()?.let(YearMonth::from)
            filter.matches(movement.type) && (month == null || month in shownMonths)
        }
    val days = visibleMovements.groupBy { it.localDate() }
    Column(modifier = Modifier.fillMaxSize().background(colors.paper).statusBarsPadding()) {
        Box(
            modifier =
                Modifier
                    .padding(start = 16.dp, top = 4.dp)
                    .minimumInteractiveComponentSize()
                    .size(40.dp)
                    .physicalPress(scale = PhysicalPressScale.Small, onClick = onBack)
                    .clip(CircleShape)
                    .background(colors.paper2)
                    .semantics { contentDescription = "Volver" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = colors.ink, modifier = Modifier.size(18.dp))
        }
        Text(
            "Movimientos",
            color = colors.ink,
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1.5).sp,
            modifier = Modifier.padding(start = 20.dp, top = 10.dp),
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 40.dp),
        ) {
            item(key = "totals") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MonthTotal(
                        label = "Entró en ${MONTHS_SHORT[today.monthValue - 1]}",
                        value = "+${ledger.incomeThisMonth ?: "$0"}",
                        color = positiveInk(colors),
                        modifier = Modifier.weight(1f),
                    )
                    MonthTotal(
                        label = "Gastaste",
                        value = "−${ledger.spentThisMonth ?: "$0"}",
                        color = colors.ink,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item(key = "filters") {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 18.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MovementFilter.entries.forEach { option ->
                        FilterPill(option.label, selected = option == filter) {
                            filter = option
                        }
                    }
                }
            }
            if (days.isEmpty()) {
                item(key = "empty") {
                    Text(
                        if (movements.isEmpty()) "Aquí verás tus recargas y lo que pagues con saldo." else "Todavía no hay movimientos de este tipo.",
                        color = colors.muted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp, top = 20.dp),
                    )
                }
            }
            days.forEach { (day, list) ->
                item(key = "day-$day") {
                    Text(
                        movementDayLabel(day, today).uppercase(),
                        color = colors.muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.2.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 22.dp, bottom = 8.dp),
                    )
                }
                item(key = "card-$day") {
                    WalletMovementsCard(list) { movement ->
                        WalletMovementRow(
                            movement = movement,
                            subtitle =
                                listOfNotNull(
                                    movementClock(movement.createdAt),
                                    movementDetail(movement.place, movement.productsSummary),
                                ).joinToString(" · ").ifBlank { null },
                            showBalanceAfter = true,
                        )
                    }
                }
            }
            if (months.size > monthsShown) {
                item(key = "older") {
                    Box(
                        modifier =
                            Modifier
                                .padding(top = 22.dp)
                                .fillMaxWidth()
                                .height(48.dp)
                                .physicalPress { monthsShown += 1 }
                                .clip(CircleShape)
                                .border(1.dp, colors.line, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Ver ${MONTHS_LONG[months[monthsShown].monthValue - 1]}",
                            color = colors.ink2,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthTotal(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(22.dp))
                .background(colors.paper2)
                .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(label.uppercase(), color = colors.muted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.2.sp)
        Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Box(
        modifier =
            Modifier
                .height(34.dp)
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
                .semantics { this.selected = selected }
                .clip(CircleShape)
                .background(if (selected) colors.ink else colors.paper2)
                .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) colors.onInk else colors.ink2,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
        )
    }
}

private val MONTHS_SHORT = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
private val MONTHS_LONG =
    listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
private val WEEKDAYS_SHORT = listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")

internal fun WalletMovement.localDate(): LocalDate? =
    runCatching { BusinessTime.at(OffsetDateTime.parse(createdAt).toInstant()).toLocalDate() }.getOrNull()

/** «Hoy», «Ayer» o «lun 27 oct»: el encabezado de cada día en Movimientos. */
internal fun movementDayLabel(
    day: LocalDate?,
    today: LocalDate,
): String =
    when (day) {
        null -> "Sin fecha"
        today -> "Hoy"
        today.minusDays(1) -> "Ayer"
        else -> "${WEEKDAYS_SHORT[day.dayOfWeek.value - 1]} ${day.dayOfMonth} ${MONTHS_SHORT[day.monthValue - 1]}"
    }

/** «1:05 pm», en la hora del negocio; `null` si la fecha no se lee. */
internal fun movementClock(iso: String): String? {
    val local = runCatching { BusinessTime.at(OffsetDateTime.parse(iso).toInstant()) }.getOrNull() ?: return null
    val hour = local.hour % 12
    return "${if (hour == 0) 12 else hour}:${"%02d".format(local.minute)} ${if (local.hour < 12) "am" else "pm"}"
}