package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import java.math.BigDecimal

/** Los billetes que se cuentan en el cajón, del más grande al más chico, con el color de cada uno. */
internal val CashBills: List<Pair<Int, Color>> =
    listOf(
        1000 to Color(0xFFD7C4A8),
        500 to Color(0xFFA9CCEB),
        200 to Color(0xFFC9B8EE),
        100 to Color(0xFFEDB8C9),
        50 to Color(0xFFB8E8C8),
        20 to Color(0xFFEFD3B0),
    )

/** El total del cajón: billetes por su cantidad más las monedas; null si las monedas no son un monto válido. */
internal fun billCountTotal(
    counts: Map<Int, Int>,
    coins: String,
): String? {
    val coinsValue = if (coins.isBlank()) "0.00" else normalizeCountedCash(coins) ?: return null
    val bills = counts.entries.sumOf { (bill, count) -> bill.toLong() * count.coerceAtLeast(0) }
    return Money.format(BigDecimal(bills) + BigDecimal(coinsValue))
}

/** N04: toca cuántos billetes hay de cada uno y la app suma; el total llena el efectivo contado. */
@Composable
internal fun CashBillCounter(
    counts: Map<Int, Int>,
    coins: String,
    colors: OperationalColors,
    enabled: Boolean,
    onCountChange: (bill: Int, count: Int) -> Unit,
    onCoinsChange: (String) -> Unit,
) {
    val haptics = rememberVaiinillaHaptics()
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(colors.cardBackground)
                .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        CashBills.forEachIndexed { index, (bill, tint) ->
            if (index > 0) HorizontalDivider(color = colors.cardBorder)
            val count = counts[bill] ?: 0
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .width(64.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(tint)
                            .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$$bill", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color(0xFF1B1B18))
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Row(
                        modifier = Modifier.clip(CircleShape).background(colors.background.copy(alpha = 0.6f)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StepButton("−", "Un billete de $$bill menos", enabled && count > 0, colors) {
                            haptics.selection()
                            onCountChange(bill, count - 1)
                        }
                        Text(
                            "$count",
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(32.dp),
                        )
                        StepButton("+", "Un billete de $$bill más", enabled, colors) {
                            haptics.selection()
                            onCountChange(bill, count + 1)
                        }
                    }
                }
                Text(
                    moneyLabel(Money.format(BigDecimal(bill.toLong() * count))),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (count > 0) colors.textPrimary else colors.textMuted,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(76.dp),
                )
            }
        }
        HorizontalDivider(color = colors.cardBorder)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Monedas",
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            BasicTextField(
                value = coins,
                onValueChange = onCoinsChange,
                singleLine = true,
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle =
                    TextStyle(
                        color = colors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                    ),
                cursorBrush = SolidColor(colors.accentLime),
                modifier =
                    Modifier
                        .width(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.background.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .semantics { contentDescription = "Monedas" },
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterEnd) {
                        if (coins.isEmpty()) Text("$0.00", color = colors.textMuted, fontSize = 17.sp)
                        inner()
                    }
                },
            )
        }
    }
}

@Composable
private fun StepButton(
    symbol: String,
    description: String,
    enabled: Boolean,
    colors: OperationalColors,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .physicalPress(enabled = enabled, scale = PhysicalPressScale.Small, onClick = onClick)
                .size(44.dp)
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            symbol,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            color = if (enabled) colors.textPrimary else colors.textMuted,
        )
    }
}
