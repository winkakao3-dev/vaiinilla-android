package com.vaiinilla.app.ui.hotel.guest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vaiinilla.app.domain.hotel.ChargeCategory
import com.vaiinilla.app.domain.hotel.Stay
import com.vaiinilla.app.domain.hotel.StayStatus
import com.vaiinilla.app.domain.model.Venue
import com.vaiinilla.app.ui.components.cremeMoment
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import java.math.BigDecimal

/** H06: la cuenta de la estancia. La app solo la muestra: el pago se hace en recepción al salir. */
@Composable
fun HotelAccountScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HotelStayViewModel = viewModel(),
) {
    LaunchedEffect(Unit) { viewModel.load() }
    val state = viewModel.state.value
    HotelAccountContent(stay = state.stay, error = state.error, modifier = modifier)
}

/** Una línea de la cuenta, ya lista para mostrar. */
internal data class AccountLine(
    val title: String,
    val detail: String?,
    val amount: BigDecimal,
)

/**
 * El servidor lista todos los pedidos de la estancia, pero su cuenta solo suma los que van a la habitación y siguen
 * sin cobrar (`por_cobrar`): uno pagado aparte o ya cobrado por la mesa saldría como línea sin entrar en el total.
 */
private fun billedOrders(stay: Stay) =
    if (stay.account != null &&
        stay.status != StayStatus.CHECKED_OUT
    ) {
        stay.orders.filter { it.pendingPayment }
    } else {
        stay.orders
    }

internal fun isClosed(stay: Stay): Boolean = stay.status == StayStatus.CHECKED_OUT

/**
 * El encabezado de la cuenta: en una estancia abierta, lo cargado a la habitación; en una ya cerrada, lo que se cobró
 * en el check-out (`total_cobrado`), porque la cuenta calculada deja de contar lo que ya se pagó.
 */
internal fun accountHeadline(stay: Stay): Pair<String, BigDecimal> {
    val charged = stay.totalCharged?.toBigDecimalOrNull()
    return if (isClosed(stay) &&
        charged != null
    ) {
        "TOTAL COBRADO" to charged
    } else {
        "CARGOS A LA HABITACIÓN" to roomCharges(stay)
    }
}

internal fun accountLines(stay: Stay): List<AccountLine> {
    val orders =
        billedOrders(stay).map {
            val title =
                it.itemsSummary?.takeIf { items -> items.isNotBlank() }
                    ?: it.folio?.let { folio -> "Room service · pedido $folio" }
                    ?: "Room service"
            AccountLine(
                title,
                listOfNotNull(it.createdAt?.let(::cremeMoment), "room service").joinToString(" · "),
                it.total.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            )
        }
    val charges =
        stay.charges.map {
            val kind = it.category.takeIf { category -> category != ChargeCategory.OTHER }?.label
            AccountLine(
                it.concept,
                listOfNotNull(it.createdAt?.let(::cremeMoment), kind).joinToString(" · ").ifEmpty { null },
                it.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            )
        }
    return orders + charges
}

/**
 * Lo que lleva cargado a la habitación: room service y cargos de recepción. La estancia se muestra aparte. Si el
 * servidor mandó la cuenta calculada se usa esa; si no, la suma de las líneas.
 */
internal fun roomCharges(stay: Stay): BigDecimal {
    val orders = stay.account?.ordersTotal?.toBigDecimalOrNull()
    val charges = stay.account?.chargesTotal?.toBigDecimalOrNull()
    if (orders != null && charges != null) return orders + charges
    return accountLines(stay).fold(BigDecimal.ZERO) { sum, line -> sum + line.amount }
}

internal fun stayCost(stay: Stay): BigDecimal? =
    stay.account?.nightsTotal?.toBigDecimalOrNull()
        ?: stay.nightlyRate?.toBigDecimalOrNull()?.multiply(BigDecimal(stay.nights))

@Composable
fun HotelAccountContent(
    stay: Stay?,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    val colors = LocalVaiinillaColors.current
    Box(modifier = modifier.fillMaxSize().background(colors.paper)) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(
                        rememberScrollState(),
                    ).padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val eyebrow =
                listOfNotNull(stay?.roomName ?: Venue.roomTitle, shortDate(stay?.checkOut)?.let { "salida el $it" })
                    .joinToString(" · ")
            Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(eyebrow, color = colors.muted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    stay?.let { accountHeadline(it).first } ?: "CARGOS A LA HABITACIÓN",
                    color = colors.muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    if (stay == null) "—" else moneyLabel(accountHeadline(stay).second.toPlainString()),
                    color = colors.ink,
                    fontSize = 52.sp,
                    lineHeight = 54.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.5).sp,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                )
            }
            val lines = stay?.let(::accountLines).orEmpty()
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HotelPanel {
                    if (lines.isEmpty()) {
                        Text(
                            if (stay ==
                                null
                            ) {
                                "Cargando tu cuenta…"
                            } else {
                                "Todavía no tienes cargos. Lo que pidas al cuarto aparece aquí."
                            },
                            color = colors.muted,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    lines.forEachIndexed { index, line ->
                        if (index > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                        AccountRow(line)
                    }
                }
                stay?.let { StayCostPanel(it) }
            }
            Text(
                error ?: "Los cargos de comida y servicios se suman a tu cuenta final.",
                color = if (error != null) colors.coral else colors.muted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Box(Modifier.height(96.dp))
        }
        PayAtFrontDeskNote(closed = stay?.let(::isClosed) == true, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun AccountRow(line: AccountLine) {
    val colors = LocalVaiinillaColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(line.title, color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Black)
            line.detail?.let { Text(it, color = colors.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        }
        Text(
            moneyLabel(line.amount.toPlainString()),
            color = colors.ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            style = TextStyle(fontFeatureSettings = "tnum"),
        )
    }
}

@Composable
private fun StayCostPanel(stay: Stay) {
    val colors = LocalVaiinillaColors.current
    val cost = stayCost(stay) ?: return
    HotelPanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val nights = if (stay.nights == 1) "1 noche" else "${stay.nights} noches"
                Text("Estancia ($nights)", color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(
                    if (isClosed(stay)) "Pagada en recepción" else "Se paga en recepción",
                    color = colors.muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                moneyLabel(cost.toPlainString()),
                color = colors.muted,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                style = TextStyle(fontFeatureSettings = "tnum"),
            )
        }
    }
}

/** Donde el dibujo tenía «Pagar ahora»: la app no cobra, y lo dice. */
@Composable
private fun PayAtFrontDeskNote(
    closed: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val shape = RoundedCornerShape(29.dp)
    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp).navigationBarsPadding()) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(
                        58.dp,
                    ).border(1.dp, colors.line, shape)
                    .background(colors.paper, shape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (closed) "Cuenta cerrada · gracias por tu estancia" else "Pagas en recepción al salir",
                color = colors.ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}
