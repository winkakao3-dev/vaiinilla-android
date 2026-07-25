package com.vaiinilla.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.ui.components.paymentMethodLabel
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.theme.Lime
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import kotlinx.coroutines.delay
import kotlin.math.sin

private val PrinterInk = Color(0xFF111210)
private val PrinterMuted = Color(0xFF9A9C96)
private val PaperInk = Color(0xFF171817)
private val PaperText = Color(0xFFF4F1E7)
private val PaperMuted = Color(0xFFA9AAA4)
private val Rule = Color(0xFFE7E4DA)

@Composable
fun OrderConfirmationScreen(
    order: OrderDetail?,
    onReturnToMenu: () -> Unit,
    onViewTracking: () -> Unit = {},
    onViewSticker: () -> Unit = {},
    screenshotPrinted: Boolean = false,
) {
    if (order == null) {
        val colors = LocalVaiinillaColors.current
        Box(
            modifier = Modifier.fillMaxSize().background(colors.paper),
            contentAlignment = Alignment.Center,
        ) {
            Button(
                onClick = onReturnToMenu,
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.accentInk),
            ) {
                Text("Volver al menú", fontWeight = FontWeight.Black)
            }
        }
        return
    }

    val printProgress = remember(order.summary.id) { Animatable(if (screenshotPrinted) 1f else 0f) }
    var printed by remember(order.summary.id) { mutableStateOf(screenshotPrinted) }

    LaunchedEffect(order.summary.id, screenshotPrinted) {
        if (screenshotPrinted) {
            printed = true
            printProgress.snapTo(1f)
            return@LaunchedEffect
        }
        printed = false
        printProgress.snapTo(0f)
        delay(180)
        printProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2_650, easing = LinearEasing),
        )
        printed = true
    }

    val colors = LocalVaiinillaColors.current
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
            .statusBarsPadding(),
    ) {
        val compact = maxWidth < 360.dp
        val horizontalPadding = if (compact) 12.dp else 18.dp
        val paperPadding = if (compact) 15.dp else 22.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ReceiptConfirmHeader(
                paymentMethod = order.summary.paymentMethod,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding, vertical = 12.dp),
            )

            ReceiptPrinterMachine(
                folio = order.summary.folio,
                printed = printed,
                paymentMethod = order.summary.paymentMethod,
                modifier = Modifier.fillMaxWidth(),
            )

            ReceiptPaperOutput(
                order = order,
                contentPadding = paperPadding,
                progress = { printProgress.value },
                modifier = Modifier
                    .padding(horizontal = horizontalPadding)
                    .fillMaxWidth(),
            )

            AnimatedVisibility(
                visible = printed,
                enter = fadeIn(tween(300)) + slideInVertically(
                    animationSpec = tween(360),
                    initialOffsetY = { it / 3 },
                ),
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = horizontalPadding, vertical = 18.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ReceiptCollectionUnlockStrip(
                        paymentMethod = order.summary.paymentMethod,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = onViewTracking,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.accentInk,
                        ),
                    ) {
                        Text(
                            "Seguir pedido",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    Button(
                        onClick = onViewSticker,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.paper2,
                            contentColor = colors.ink,
                        ),
                    ) {
                        Text(
                            "Ver sticker completo",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    Button(
                        onClick = onReturnToMenu,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrinterInk,
                            contentColor = colors.paper,
                        ),
                    ) {
                        Text(
                            "Volver al menú",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptConfirmHeader(
    paymentMethod: PaymentMethod,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val copy = confirmationCopy(paymentMethod)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                copy.eyebrow,
                color = colors.muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp,
            )
            Text(
                copy.title,
                color = colors.ink,
                fontSize = 26.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                copy.subtitle,
                color = colors.muted,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Box(
            modifier = Modifier
                .padding(start = 12.dp)
                .size(52.dp)
                .background(colors.accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("✓", color = colors.accentInk, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
    }
}

private data class ConfirmationCopy(
    val eyebrow: String,
    val title: String,
    val subtitle: String,
)

private fun confirmationCopy(paymentMethod: PaymentMethod): ConfirmationCopy = when (paymentMethod) {
    PaymentMethod.CASH -> ConfirmationCopy(
        eyebrow = "PEDIDO CREADO",
        title = "Tu pase de Caja acaba de salir.",
        subtitle = "Págalo en efectivo y usa este receipt sticker para identificar la orden.",
    )
    PaymentMethod.BALANCE -> ConfirmationCopy(
        eyebrow = "PAGO CONFIRMADO",
        title = "Tu compra se volvió un sticker.",
        subtitle = "El saldo fue descontado y Cocina ya recibió la comanda.",
    )
    PaymentMethod.CARD -> ConfirmationCopy(
        eyebrow = "TARJETA AUTORIZADA",
        title = "Tu comprobante digital está saliendo.",
        subtitle = "La compra fue autorizada y el pedido ya llegó a Cocina.",
    )
}

@Composable
private fun ReceiptCollectionUnlockStrip(
    paymentMethod: PaymentMethod,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val unlockedLabel = when (paymentMethod) {
        PaymentMethod.CASH -> "Receipt editorial desbloqueado"
        PaymentMethod.BALANCE -> "Vaiinilla Core añadido a tu colección"
        PaymentMethod.CARD -> "Live Receipt añadido a tu colección"
    }
    Surface(
        modifier = modifier,
        color = colors.paper2,
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text("✦", color = colors.accent, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Column {
                Text(
                    unlockedLabel,
                    color = colors.ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    "Se guardó automáticamente en tu colección.",
                    color = colors.muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ReceiptPrinterMachine(
    folio: Int,
    printed: Boolean,
    paymentMethod: PaymentMethod,
    modifier: Modifier = Modifier,
) {
    val statusLabel = if (printed) "STICKER LISTO" else "IMPRIMIENDO STICKER…"
    val ledTransition = rememberInfiniteTransition(label = "receipt-printer-led")
    val ledAlpha by ledTransition.animateFloat(
        initialValue = 1f,
        targetValue = .32f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 420),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "receipt-printer-led-alpha",
    )

    Box(
        modifier = modifier
            .height(184.dp)
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF1B1C1A), Color(0xFF080908), Color(0xFF141513)),
                ),
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 24.dp, end = 24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "VAIINILLA / RECEIPT LAB",
                    modifier = Modifier.weight(1f),
                    color = PrinterMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.6.sp,
                    maxLines = 1,
                )
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(8.dp)
                        .background(Color(0xFF30312E), CircleShape),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (paymentMethod.isInstantDemoPayment) "COMANDA ENVIADA" else "PASE DE CAJA",
                    color = PaperText,
                    fontSize = 23.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.4).sp,
                )
                Text(
                    "ORDER #$folio",
                    color = Lime,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .graphicsLayer { alpha = if (printed) 1f else ledAlpha }
                        .background(Color(0xFFFFD15B), CircleShape),
                )
                Text(
                    statusLabel,
                    modifier = Modifier.padding(start = 9.dp),
                    color = Color(0xFFB7B8B2),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.3.sp,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "VNL–10",
                    color = Color(0xFF6D6F69),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 30.dp, vertical = 15.dp)
                .fillMaxWidth()
                .height(27.dp)
                .background(Color(0xFF030403), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(5.dp)
                    .background(Color(0xFF30312E), CircleShape),
            )
        }
    }
}

@Composable
private fun ReceiptPaperOutput(
    order: OrderDetail,
    contentPadding: Dp,
    progress: () -> Float,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .graphicsLayer {
                val current = progress().coerceIn(0f, 1f)
                translationY = if (current < 1f) sin(current * 110f) * 1.4f else 0f
                alpha = .55f + (.45f * current)
                shadowElevation = if (current >= .995f) 18.dp.toPx() else 0f
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                clip = false
            }
            .drawWithContent {
                val current = progress().coerceIn(0f, 1f)
                val contentDrawScope = this
                clipRect(
                    left = 0f,
                    top = 0f,
                    right = size.width,
                    bottom = size.height * current,
                ) {
                    contentDrawScope.drawContent()
                }
            },
        color = PaperInk,
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
        shadowElevation = 18.dp,
    ) {
        Column(
            modifier = Modifier.padding(
                start = contentPadding,
                top = 18.dp,
                end = contentPadding,
                bottom = 24.dp,
            ),
        ) {
            Text(
                "ANTOJO",
                color = PaperText,
                fontSize = 48.sp,
                lineHeight = 46.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-2).sp,
            )

            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                listOf("XS", "S", "M", "XL", "XXL").forEach { size ->
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .border(1.dp, PaperText, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(size, color = PaperText, fontSize = 9.sp)
                    }
                }
            }

            Spacer(Modifier.height(84.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(modifier = Modifier.weight(1.25f)) {
                    TicketBarcode(
                        seed = order.summary.folio,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp),
                    )
                    Text(
                        order.summary.id.takeLast(14).uppercase(),
                        color = PaperText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                        letterSpacing = 1.2.sp,
                    )
                }
                Column(
                    modifier = Modifier.weight(.85f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(76.dp)
                            .border(2.dp, PaperText, RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("VNNL", color = PaperText, fontSize = 24.sp)
                    }
                    Text(
                        "Pase de Caja · pago en efectivo.",
                        modifier = Modifier.padding(top = 7.dp),
                        color = PaperMuted,
                        fontSize = 8.sp,
                        lineHeight = 10.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Text(
                "PEDIDO #${order.summary.folio}",
                modifier = Modifier.padding(top = 22.dp),
                color = PaperText,
                fontSize = 28.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                "PASE DE CAJA",
                color = PaperText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                listOf("✦", "☼", "◉", "⌁").forEach { glyph ->
                    Box(
                        modifier = Modifier
                            .size(45.dp)
                            .border(1.dp, PaperText, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(glyph, color = PaperText, fontSize = 16.sp)
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 1.dp),
                thickness = 1.dp,
                color = Rule,
            )
            ReceiptMetadataGrid(order)

            Text(
                "DETALLE DEL PEDIDO",
                modifier = Modifier.padding(top = 20.dp),
                color = PaperMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
            )

            order.items.forEachIndexed { index, item ->
                Column(modifier = Modifier.padding(top = 13.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            "${item.quantity} × ${item.productName}",
                            modifier = Modifier.weight(1f),
                            color = PaperText,
                            fontSize = 15.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            moneyLabel(item.subtotal),
                            modifier = Modifier.padding(start = 12.dp),
                            color = PaperText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    if (item.options.isNotEmpty()) {
                        Text(
                            item.options.joinToString(" · ") { it.name },
                            modifier = Modifier.padding(top = 4.dp),
                            color = PaperMuted,
                            fontSize = 10.sp,
                            lineHeight = 13.sp,
                        )
                    }
                    if (index != order.items.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 12.dp),
                            color = Color.White.copy(alpha = .12f),
                        )
                    }
                }
            }

            order.kitchenNotes.takeIf { it.isNotBlank() }?.let { notes ->
                Text(
                    "INSTRUCCIONES PARA COCINA",
                    modifier = Modifier.padding(top = 22.dp),
                    color = PaperMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .border(1.dp, PaperText.copy(alpha = .7f), RoundedCornerShape(2.dp))
                        .padding(12.dp),
                ) {
                    Text(
                        notes,
                        color = PaperText,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 24.dp),
                thickness = 1.dp,
                color = Rule,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        "TOTAL",
                        color = PaperMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                    )
                    Text(
                        "Confirmado por el pedido",
                        color = PaperMuted,
                        fontSize = 9.sp,
                    )
                }
                Text(
                    moneyLabel(order.summary.total),
                    color = PaperText,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Black,
                )
            }

            Text(
                "PRESENTA ESTE PASE EN CAJA PARA PAGAR. EL PEDIDO PERMANECE POR COBRAR HASTA QUE CAJA CONFIRME EL PAGO.",
                modifier = Modifier.padding(top = 24.dp),
                color = PaperMuted,
                fontSize = 8.sp,
                lineHeight = 11.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ReceiptMetadataGrid(order: OrderDetail) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            ReceiptMetadataCell(
                value = order.summary.operationalDate,
                label = "FECHA",
                modifier = Modifier.weight(1f),
            )
            ReceiptMetadataCell(
                value = "#${order.summary.folio}",
                label = "PEDIDO",
                modifier = Modifier.weight(1f),
            )
            ReceiptMetadataCell(
                value = moneyLabel(order.summary.total),
                label = "TOTAL",
                modifier = Modifier.weight(1f),
            )
        }
        HorizontalDivider(thickness = 1.dp, color = Rule)
        Row(modifier = Modifier.fillMaxWidth()) {
            ReceiptMetadataCell("EFECTIVO", "PAGO", Modifier.weight(1f))
            ReceiptMetadataCell("PARA LLEVAR", "DESTINO", Modifier.weight(1f))
            ReceiptMetadataCell("POR COBRAR", "ESTADO", Modifier.weight(1f))
        }
        HorizontalDivider(thickness = 1.dp, color = Rule)
    }
}

@Composable
private fun ReceiptMetadataCell(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(58.dp)
            .border(width = .5.dp, color = Rule.copy(alpha = .65f))
            .padding(horizontal = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            value,
            color = PaperText,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        Text(
            label,
            modifier = Modifier.padding(top = 3.dp),
            color = PaperMuted,
            fontSize = 7.sp,
            letterSpacing = .5.sp,
        )
    }
}

@Composable
private fun TicketBarcode(
    seed: Int,
    modifier: Modifier = Modifier,
) {
    val pattern = rememberBarcodePattern(seed)
    Canvas(modifier = modifier) {
        val unit = size.width / pattern.sum()
        var x = 0f
        pattern.forEachIndexed { index, widthUnits ->
            val width = unit * widthUnits
            if (index % 2 == 0) {
                drawLine(
                    color = PaperText,
                    start = Offset(x + width / 2f, 0f),
                    end = Offset(x + width / 2f, size.height),
                    strokeWidth = width.coerceAtLeast(1f),
                )
            }
            x += width
        }
    }
}

private fun rememberBarcodePattern(seed: Int): List<Float> {
    val normalized = seed.toString().ifBlank { "10" }
    val widths = mutableListOf<Float>()
    repeat(5) { cycle ->
        normalized.forEachIndexed { index, char ->
            val digit = char.digitToIntOrNull() ?: 1
            widths += 1f + ((digit + index + cycle) % 3)
            widths += 1f + ((digit + cycle) % 2)
        }
    }
    return widths
}
