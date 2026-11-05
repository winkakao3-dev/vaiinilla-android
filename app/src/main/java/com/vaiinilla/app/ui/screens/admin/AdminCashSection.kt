package com.vaiinilla.app.ui.screens.admin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.MoneyInput
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.repository.CashLastClose
import com.vaiinilla.app.ui.admin.AdminUiState
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.receiptLocalTime
import com.vaiinilla.app.ui.theme.Yolk
import java.math.BigDecimal

/** Lo que la sección de Caja necesita del resto del modo. */
class AdminCashActions(
    val onOpenCash: (String, () -> Unit) -> Unit,
    val onCloseCash: (String, () -> Unit) -> Unit,
    val onRefresh: () -> Unit = {},
)

/** Caja, como los marcos 39, 40 y 41 del lienzo. Cobrar y entregar quedan para una cuenta de Caja. */
@Composable
internal fun AdminCashSection(
    state: AdminUiState,
    colors: OperationalColors,
    readOnly: Boolean,
    actions: AdminCashActions,
    onBack: () -> Unit,
) {
    var sheet by remember { mutableStateOf<CashSheet?>(null) }
    val open = state.cashSessionId != null
    val enabled = !readOnly && !state.saving
    LaunchedEffect(Unit) { actions.onRefresh() }

    AdminSectionPage(
        title = "Caja",
        colors = colors,
        notice = state.notice.takeIf { sheet == null },
        noticeIsError = state.notice != null && state.noticeIsError,
        onBack = onBack,
        subtitle = "El efectivo del día. Abre la caja para cobrar en mostrador.".takeIf { !open && state.cashLoaded },
        trailing = {
            if (state.cashLoaded && !state.cashCheckFailed) {
                AdminBadge(
                    if (open) "Abierta" else "Cerrada",
                    colors,
                    tone = if (open) AdminBadgeTone.OK else AdminBadgeTone.NEUTRAL,
                    dot = open,
                )
            }
        },
    ) {
        item(key = "caja") {
            AnimatedContent(
                targetState =
                    when {
                        state.cashCheckFailed -> CashView.FAILED
                        !state.cashLoaded -> CashView.LOADING
                        open -> CashView.OPEN
                        else -> CashView.CLOSED
                    },
                transitionSpec = {
                    (
                        fadeIn(
                            spring(stiffness = 400f),
                        ) + scaleIn(spring(dampingRatio = 0.82f, stiffness = 400f), 0.97f)
                    ) togetherWith
                        fadeOut(spring(stiffness = 700f))
                },
                label = "admin-caja",
            ) { view ->
                when (view) {
                    CashView.FAILED ->
                        AdminPanel(colors) {
                            Text(
                                "No se pudo consultar la caja",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = colors.textPrimary,
                            )
                            AdminHint("Revisa tu conexión e inténtalo de nuevo.", colors)
                            Spacer(Modifier.height(12.dp))
                            AdminSecondaryButton("Reintentar", colors, outlined = true, onClick = actions.onRefresh)
                        }
                    CashView.LOADING ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(colors.cardBackground),
                        )
                    CashView.CLOSED ->
                        Column {
                            ClosedCard(readOnly, enabled, colors) { sheet = CashSheet.OPEN }
                            LastCloseCard(state.cashLastClose, colors)
                        }
                    CashView.OPEN -> OpenSession(state, readOnly, enabled, colors) { sheet = CashSheet.CLOSE }
                }
            }
        }
    }

    val error = state.notice.takeIf { state.noticeIsError }
    when (sheet) {
        CashSheet.OPEN ->
            AmountSheet(
                title = "Abrir caja",
                label = "Fondo inicial",
                hint = "Cuenta el efectivo con el que empiezas y escríbelo. Es el fondo contra el que se cuadra el cierre.",
                colors = colors,
                saving = state.saving,
                error = error,
                initial = state.suggestedCashFloat,
                onDismiss = { sheet = null },
            ) { amount -> actions.onOpenCash(amount) { sheet = null } }
        CashSheet.CLOSE ->
            AmountSheet(
                title = "Cerrar caja",
                label = "Efectivo contado",
                hint = "Cuenta el efectivo que hay en la caja y escríbelo.",
                colors = colors,
                saving = state.saving,
                error = error,
                expected = state.cashExpected,
                initial = state.cashExpected,
                checksExpected = true,
                onDismiss = { sheet = null },
            ) { amount -> actions.onCloseCash(amount) { sheet = null } }
        null -> Unit
    }
}

private enum class CashView { FAILED, LOADING, CLOSED, OPEN }

private enum class CashSheet { OPEN, CLOSE }

@Composable
private fun ClosedCard(
    readOnly: Boolean,
    enabled: Boolean,
    colors: OperationalColors,
    onOpen: () -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        Modifier
            .adminRise(0)
            .fillMaxWidth()
            .clip(shape)
            .background(colors.cardBackground)
            .then(if (colors.isDark) Modifier else Modifier.border(1.dp, colors.cardBorder, shape))
            .padding(horizontal = 18.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(22.dp)).background(colors.textPrimary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.PointOfSale,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(26.dp),
            )
        }
        Text(
            "Caja cerrada",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = colors.textPrimary,
            modifier = Modifier.padding(top = 14.dp),
        )
        Text(
            "Al abrirla cuentas el fondo con el que empiezas.",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (!readOnly) {
            Spacer(Modifier.height(18.dp))
            AdminPrimaryButton("Abrir caja", colors, enabled = enabled, onClick = onOpen)
        }
    }
}

/** Cómo quedó el último cierre (marco 39). El servidor solo lo manda con la caja abierta, por eso puede faltar. */
@Composable
private fun LastCloseCard(
    last: CashLastClose?,
    colors: OperationalColors,
) {
    val date = last?.closedAt?.let { at -> listOfNotNull(adminShortDate(at), receiptLocalTime(at)).joinToString(", ") }
    AdminCapsLabel(
        date?.let { "Último cierre · $it" } ?: "Último cierre",
        colors,
        Modifier.padding(top = 12.dp, bottom = 8.dp).adminRise(1),
    )
    if (last == null) {
        Box(Modifier.padding(horizontal = 4.dp).adminRise(2)) {
            AdminHint("Cuando cierres la caja, aquí ves cómo quedó.", colors)
        }
        return
    }
    val opening = last.opening?.toBigDecimalOrNull()
    val expected = last.expected?.toBigDecimalOrNull()
    val sales =
        last.cashSales ?: if (opening != null && expected != null) (expected - opening).toPlainString() else null
    val rows =
        listOfNotNull(
            last.opening?.let { "Fondo inicial" to moneyLabelCents(it) },
            sales?.let { "Ventas en efectivo" to moneyLabelCents(it) },
            last.expected?.let { "Efectivo esperado" to moneyLabelCents(it) },
            "Efectivo contado" to moneyLabelCents(last.counted),
            ("Cerró" to "Tú").takeIf { last.closedByMe },
        )
    AdminPanel(colors, Modifier.adminRise(2), padding = PaddingValues(horizontal = 16.dp, vertical = 2.dp)) {
        rows.forEachIndexed { index, (title, value) ->
            if (index > 0) AdminRowDivider(colors)
            AdminRow(title = title, colors = colors) { AdminRowValue(value, colors) }
        }
    }
}

@Composable
private fun OpenSession(
    state: AdminUiState,
    readOnly: Boolean,
    enabled: Boolean,
    colors: OperationalColors,
    onClose: () -> Unit,
) {
    val opening = state.cashOpeningAmount?.toBigDecimalOrNull()
    val expected = state.cashExpected?.toBigDecimalOrNull()
    val cashIn = if (opening != null && expected != null) (expected - opening).toPlainString() else null
    val card =
        state.summary
            ?.paymentMethods
            ?.firstOrNull { it.label == PaymentMethod.STRIPE.label }
            ?.amount
    Column {
        AdminCapsLabel(
            state.cashOpenedAt?.let { "Sesión de caja · desde ${receiptLocalTime(it)}" } ?: "Sesión de caja",
            colors,
            Modifier.padding(bottom = 8.dp).adminRise(0),
        )
        Column(
            Modifier
                .adminRise(1)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(colors.accentLime)
                .padding(14.dp),
        ) {
            Text(
                "Efectivo esperado en caja",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.accentInk.copy(alpha = 0.75f),
            )
            Text(
                (state.cashExpected ?: state.cashOpeningAmount)?.let(::moneyLabelCents) ?: "—",
                fontSize = 40.sp,
                lineHeight = 46.sp,
                letterSpacing = (-1.4).sp,
                fontWeight = FontWeight.Black,
                color = colors.accentInk,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 24.sp, maxFontSize = 40.sp),
            )
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CashTile("Fondo", state.cashOpeningAmount?.let(::moneyLabel) ?: "—", colors, Modifier.weight(1f))
                CashTile("Efectivo", cashIn?.let { "+${moneyLabel(it)}" } ?: "—", colors, Modifier.weight(1f))
                CashTile("Tarjeta", card?.let(::moneyLabel) ?: "—", colors, Modifier.weight(1f))
            }
        }
        AdminCapsLabel(
            "Cobros en efectivo",
            colors,
            Modifier.padding(top = 16.dp, bottom = 8.dp).adminRise(2),
        )
        if (state.cashOrders.isEmpty()) {
            Box(Modifier.padding(horizontal = 4.dp).adminRise(2)) {
                AdminHint("Aún no hay cobros en efectivo en esta sesión.", colors)
            }
        } else {
            AdminPanel(colors, Modifier.adminRise(2), padding = PaddingValues(horizontal = 16.dp, vertical = 2.dp)) {
                state.cashOrders.forEachIndexed { index, order ->
                    if (index > 0) AdminRowDivider(colors)
                    CashChargeRow(order, colors)
                }
            }
        }
        Box(Modifier.padding(top = 10.dp, start = 4.dp, end = 4.dp).adminRise(3)) {
            AdminHint("Los pagos con tarjeta no entran al efectivo de la caja.", colors)
        }
        if (!readOnly) {
            Spacer(Modifier.height(14.dp))
            Box(Modifier.adminRise(3)) {
                AdminSecondaryButton(
                    "Cerrar caja",
                    colors,
                    icon = Icons.Outlined.Lock,
                    outlined = true,
                    enabled = enabled,
                    onClick = onClose,
                )
            }
        }
    }
}

@Composable
private fun CashTile(
    label: String,
    value: String,
    colors: OperationalColors,
    modifier: Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.accentInk.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = colors.accentInk.copy(alpha = 0.7f),
            maxLines = 1,
        )
        Text(
            value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            color = colors.accentInk,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 15.sp),
        )
    }
}

@Composable
private fun CashChargeRow(
    order: OrderDetail,
    colors: OperationalColors,
) {
    val summary = order.summary
    val place = summary.space?.name ?: order.customerName
    val what = order.items.firstOrNull()?.productName
    val time = receiptLocalTime(summary.createdAt)
    AdminRow(
        title = listOfNotNull("#${summary.folio}", place).joinToString(" · "),
        colors = colors,
        detail = listOfNotNull(what, time).joinToString(" · "),
    ) {
        Text(
            "+${moneyLabelCents(summary.total)}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = if (colors.isDark) colors.accentLime else colors.accentInk,
        )
    }
}

/**
 * Pide un monto con el teclado del lienzo; se cierra solo si el servidor lo aceptó. Al cerrar caja muestra lo esperado
 * y dice si lo contado cuadra, sobra o falta.
 */
@Composable
private fun AmountSheet(
    title: String,
    label: String,
    hint: String,
    colors: OperationalColors,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    initial: String? = null,
    expected: String? = null,
    checksExpected: Boolean = false,
    onConfirm: (String) -> Unit,
) {
    var amount by remember { mutableStateOf(initial.orEmpty()) }
    AdminSheet(title = title, colors = colors, busy = saving, onDismiss = onDismiss) {
        AdminHint(hint, colors)
        if (checksExpected) {
            Row(
                Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.cardBackground)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Efectivo esperado",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    expected?.let(::moneyLabelCents) ?: "—",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (expected == null) colors.textMuted else colors.textPrimary,
                )
            }
        }
        AmountDisplay(label, amount, colors)
        if (checksExpected) CountVerdict(expected, amount, colors)
        Keypad(enabled = !saving, colors = colors) { key -> amount = pressKey(amount, key) }
        AdminSheetError(error)
        val money = amount.toBigDecimalOrNull()
        AdminPrimaryButton(
            title =
                when {
                    saving -> "Guardando…"
                    money != null -> "$title con ${moneyLabelCents(money.toPlainString())}"
                    else -> title
                },
            colors = colors,
            busy = saving,
            enabled = amount.isNotEmpty(),
        ) { onConfirm(amount) }
    }
}

/** El monto grande con el borde lima, como en el lienzo; cada dígito entra desde abajo. */
@Composable
private fun AmountDisplay(
    label: String,
    amount: String,
    colors: OperationalColors,
) {
    val reduced = reducedMotion()
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .padding(top = 6.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(colors.cardBackground)
            .border(1.5.dp, colors.accentLime, shape)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics { contentDescription = "$label: ${amount.ifEmpty { "0" }} pesos" },
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textMuted)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$",
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                color = if (amount.isEmpty()) colors.textMuted else colors.textPrimary,
            )
            AnimatedContent(
                targetState = amount,
                transitionSpec = {
                    if (reduced) {
                        fadeIn(spring(stiffness = 10_000f)) togetherWith fadeOut(spring(stiffness = 10_000f))
                    } else {
                        (
                            fadeIn(spring(stiffness = 700f)) +
                                slideInVertically(spring(dampingRatio = 0.85f, stiffness = 700f)) { it / 4 }
                        ) togetherWith
                            fadeOut(spring(stiffness = 1_200f))
                    }
                },
                label = "monto",
            ) { value ->
                Text(
                    groupedAmount(value).ifEmpty { "0" },
                    fontSize = 34.sp,
                    letterSpacing = (-1).sp,
                    fontWeight = FontWeight.Black,
                    color = if (value.isEmpty()) colors.textMuted.copy(alpha = 0.6f) else colors.textPrimary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun CountVerdict(
    expected: String?,
    amount: String,
    colors: OperationalColors,
) {
    val expectedValue = expected?.toBigDecimalOrNull()
    val counted = amount.toBigDecimalOrNull()
    AnimatedVisibility(
        visible = expectedValue != null && counted != null,
        enter = fadeIn(spring(stiffness = 500f)) + slideInVertically(spring(stiffness = 500f)) { it / 3 },
        exit = fadeOut(spring(stiffness = 900f)),
    ) {
        val difference = (counted ?: BigDecimal.ZERO) - (expectedValue ?: BigDecimal.ZERO)
        val gap = moneyLabelCents(difference.abs().toPlainString())
        val (text, icon, tone) =
            when (difference.signum()) {
                0 ->
                    Triple(
                        "Cuadra con lo esperado",
                        Icons.Rounded.Verified,
                        if (colors.isDark) colors.accentLime else colors.accentInk,
                    )
                1 -> Triple("Sobran $gap", Icons.Rounded.AddCircle, if (colors.isDark) Yolk else colors.textPrimary)
                else -> Triple("Faltan $gap", Icons.Rounded.RemoveCircle, colors.coralText)
            }
        Row(
            Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, contentDescription = null, tint = tone, modifier = Modifier.size(18.dp))
            Text(text, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = tone)
        }
    }
}

private val KEYS = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "⌫")

@Composable
private fun Keypad(
    enabled: Boolean,
    colors: OperationalColors,
    onKey: (String) -> Unit,
) {
    val haptics = rememberVaiinillaHaptics()
    Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        KEYS.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    Box(
                        Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .physicalPress(scale = PhysicalPressScale.Small, enabled = enabled) {
                                haptics.click()
                                onKey(key)
                            }.clip(RoundedCornerShape(14.dp))
                            .background(colors.cardBackground)
                            .semantics {
                                contentDescription =
                                    when (key) {
                                        "⌫" -> "Borrar"
                                        "." -> "Punto"
                                        else -> key
                                    }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (key == "⌫") {
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(22.dp),
                            )
                        } else {
                            Text(key, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
                        }
                    }
                }
            }
        }
    }
}

/** "1430.00" se lee "1,430.00": lo que se teclea queda tal cual y solo se agrupan los miles. */
private fun groupedAmount(raw: String): String {
    val whole = raw.substringBefore('.')
    val grouped =
        whole
            .reversed()
            .chunked(3)
            .joinToString(",")
            .reversed()
    return if ('.' in raw) "$grouped.${raw.substringAfter('.')}" else grouped
}

/** Una tecla del teclado sobre el monto: un punto, dos decimales y el mismo tope que el campo de dinero. */
internal fun pressKey(
    amount: String,
    key: String,
): String {
    if (key == "⌫") return amount.dropLast(1)
    if (key == "." && amount.contains('.')) return amount
    val dot = amount.indexOf('.')
    if (dot >= 0 && amount.length - dot > 2) return amount
    return MoneyInput.sanitize(amount + key).take(11)
}
