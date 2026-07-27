package com.vaiinilla.app.ui.components.sticker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.components.moneyLabel

enum class StickerStyle(
    val label: String,
) {
    Editorial("Editorial"),
    Core("Core"),
    Limited("Limited"),
    Breakfast("Breakfast"),
    QrLive("QR Live"),
    Thermal("Térmico"),
}

enum class StickerSize(
    val label: String,
) {
    XS("XS"),
    S("S"),
    M("M"),
    XL("XL"),
    XXL("XXL"),
}

data class StickerOrderData(
    val folio: Int,
    val total: String,
    val productName: String,
    val paymentLabel: String,
    val destinationLabel: String,
    val date: String,
    val time: String = "11:42",
)

fun demoStickerOrderData() =
    StickerOrderData(
        folio = 3472,
        total = "101",
        productName = "Burrito norteño",
        paymentLabel = "Efectivo",
        destinationLabel = "Para llevar",
        date = "23/07/26",
    )

@Composable
fun StickerStyleContent(
    style: StickerStyle,
    order: StickerOrderData,
    size: StickerSize,
    modifier: Modifier = Modifier,
) {
    when (style) {
        StickerStyle.Editorial -> EditorialSticker(order, size, modifier)
        StickerStyle.Core -> CoreDropSticker(order, modifier)
        StickerStyle.Limited -> LimitedSticker(order, modifier)
        StickerStyle.Breakfast -> BreakfastClubSticker(order, modifier)
        StickerStyle.QrLive -> QrLiveSticker(order, modifier)
        StickerStyle.Thermal -> ThermalSticker(order, modifier)
    }
}

@Composable
private fun EditorialSticker(
    order: StickerOrderData,
    size: StickerSize,
    modifier: Modifier = Modifier,
) {
    val bg = Color(0xFF171817)
    val text = Color(0xFFF4F1E7)
    val muted = Color(0xFFA9AAA4)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(bg, RoundedCornerShape(24.dp))
                .padding(22.dp),
    ) {
        Text("ANTOJO", color = text, fontWeight = FontWeight.Black, fontSize = 42.sp, letterSpacing = (-1.5).sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
            StickerSize.entries.forEach { chip ->
                Box(
                    modifier =
                        Modifier
                            .size(32.dp)
                            .border(
                                width = if (chip == size) 2.dp else 1.dp,
                                color = if (chip == size) text else text.copy(alpha = 0.5f),
                                shape = CircleShape,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(chip.label, color = text, fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        Spacer(Modifier.height(72.dp))
        StickerMetaGrid(order, text, muted)
        Text(
            order.productName.uppercase(),
            color = text,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            modifier = Modifier.padding(top = 18.dp),
        )
        Text("School edition 01", color = muted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
        Text(
            "Presenta este sticker en caja. Instrucciones de cocina en el reverso del pase.",
            color = muted,
            fontSize = 9.sp,
            lineHeight = 12.sp,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
        )
    }
}

@Composable
private fun CoreDropSticker(
    order: StickerOrderData,
    modifier: Modifier = Modifier,
) {
    val bg = Color(0xFF1B1C1A)
    val text = Color(0xFFF4F1E7)
    val lime = Color(0xFFB9D86D)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(bg, RoundedCornerShape(24.dp))
                .padding(22.dp),
    ) {
        Text(
            "VAIINILLA RECEIPT STICKER DROP 024 YA ES TUYO.",
            color = lime,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            letterSpacing = 0.8.sp,
        )
        Text(
            "PEDIDO PAGADO",
            color = text,
            fontWeight = FontWeight.Black,
            fontSize = 28.sp,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            order.productName,
            color = text,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text("Montado norteño · combo demo", color = text.copy(alpha = 0.65f), fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StickerField("ORDEN", "#${order.folio}")
            StickerField("DESTINO", order.destinationLabel)
            StickerField("HORA", order.time)
        }
        Text(
            "Total pagado ${order.paymentLabel} · ${moneyLabel(order.total)}",
            color = text,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            "VNL-${order.folio}-${order.total}MX",
            color = lime,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text("COMMON 01/24", color = text.copy(alpha = 0.5f), fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun LimitedSticker(
    order: StickerOrderData,
    modifier: Modifier = Modifier,
) {
    val bg = Color(0xFF2A1510)
    val text = Color(0xFFF4F1E7)
    val fire = Color(0xFFF15B55)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(bg, RoundedCornerShape(24.dp))
                .padding(22.dp),
    ) {
        Text("HOT LUNCH", color = fire, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp)
        Text("limited edition", color = text, fontWeight = FontWeight.Black, fontSize = 26.sp)
        Text(
            "Montado norteño",
            color = text,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text("serie fuego", color = text.copy(alpha = 0.7f), fontSize = 12.sp)
        Box(
            modifier =
                Modifier
                    .padding(top = 16.dp)
                    .background(fire, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text("RARE", color = text, fontWeight = FontWeight.Black, fontSize = 11.sp)
        }
        Text(
            "013/150",
            color = text,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            "Colecciona 3 stickers limited y desbloquea un combo gratis.",
            color = text.copy(alpha = 0.65f),
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun BreakfastClubSticker(
    order: StickerOrderData,
    modifier: Modifier = Modifier,
) {
    val bg = Color(0xFF171817)
    val text = Color(0xFFF4F1E7)
    val yolk = Color(0xFFFFD15B)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(bg, RoundedCornerShape(24.dp))
                .padding(22.dp),
    ) {
        Text(
            "AM BREAKFAST CLUB 2026",
            color = yolk,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
            letterSpacing = 0.6.sp,
        )
        Text(
            "Progreso 3/5",
            color = text,
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text("1 × ${order.productName}", color = text, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
        Text("Café americano", color = text.copy(alpha = 0.65f), fontSize = 12.sp)
        Text(
            "MORNING-${order.folio}-DA",
            color = yolk,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun QrLiveSticker(
    order: StickerOrderData,
    modifier: Modifier = Modifier,
) {
    val bg = Color(0xFF0A0B0A)
    val text = Color(0xFF9AE66D)
    val muted = Color(0xFF6D8F5A)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(bg, RoundedCornerShape(24.dp))
                .border(1.dp, text.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                .padding(22.dp),
    ) {
        Text(
            "VNL://ORDER LIVE RECEIPT",
            color = text,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
        )
        Text(
            "READY TO PICK",
            color = text,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            modifier = Modifier.padding(top = 10.dp),
        )
        Spacer(Modifier.height(12.dp))
        TerminalField("ORDER_ID", "#${order.folio}", text, muted)
        TerminalField("PRODUCT", order.productName, text, muted)
        TerminalField("PAYMENT", order.paymentLabel, text, muted)
        TerminalField("DESTINATION", order.destinationLabel, text, muted)
        TerminalField("TOTAL", moneyLabel(order.total), text, muted)
        Text(
            "VERIFIED HASH",
            color = muted,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 14.dp),
        )
    }
}

@Composable
private fun ThermalSticker(
    order: StickerOrderData,
    modifier: Modifier = Modifier,
) {
    val bg = Color(0xFFF4F1E7)
    val text = Color(0xFF171817)
    val muted = Color(0xFF77796F)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(bg, RoundedCornerShape(8.dp))
                .padding(18.dp),
    ) {
        Text(
            "VAIINILLA CAFETERÍA",
            color = text,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "#${order.folio}",
            color = text,
            fontWeight = FontWeight.Black,
            fontSize = 36.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = muted.copy(alpha = 0.4f))
        Text("1 × ${order.productName}", color = text, fontSize = 13.sp)
        Text("Pagado: ${order.paymentLabel}", color = muted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
        Text(
            "Gracias, Dani",
            color = text,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 12.dp),
        )
        ThermalBarcode(modifier = Modifier.fillMaxWidth().height(48.dp).padding(top = 12.dp))
    }
}

@Composable
private fun StickerMetaGrid(
    order: StickerOrderData,
    text: Color,
    muted: Color,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            StickerMetaCell(order.date, "FECHA", text, muted, Modifier.weight(1f))
            StickerMetaCell("#${order.folio}", "PEDIDO", text, muted, Modifier.weight(1f))
            StickerMetaCell(moneyLabel(order.total), "TOTAL", text, muted, Modifier.weight(1f))
        }
        HorizontalDivider(color = muted.copy(alpha = 0.35f))
        Row(modifier = Modifier.fillMaxWidth()) {
            StickerMetaCell(order.paymentLabel.uppercase(), "PAGO", text, muted, Modifier.weight(1f))
            StickerMetaCell(order.destinationLabel.uppercase(), "DESTINO", text, muted, Modifier.weight(1f))
            StickerMetaCell("ACTIVO", "ESTADO", text, muted, Modifier.weight(1f))
        }
        HorizontalDivider(color = muted.copy(alpha = 0.35f))
    }
}

@Composable
private fun StickerMetaCell(
    value: String,
    label: String,
    text: Color,
    muted: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = text, fontWeight = FontWeight.Black, fontSize = 10.sp, textAlign = TextAlign.Center)
        Text(label, color = muted, fontSize = 7.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun StickerField(
    label: String,
    value: String,
) {
    Column {
        Text(label, color = Color(0xFF77796F), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color(0xFFF4F1E7), fontWeight = FontWeight.Black, fontSize = 13.sp)
    }
}

@Composable
private fun TerminalField(
    label: String,
    value: String,
    text: Color,
    muted: Color,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            "$label:",
            color = muted,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            modifier = Modifier.weight(0.4f),
        )
        Text(value, color = text, fontFamily = FontFamily.Monospace, fontSize = 10.sp, modifier = Modifier.weight(0.6f))
    }
}

@Composable
private fun ThermalBarcode(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val barCount = 28
        val barWidth = size.width / (barCount * 2)
        var x = 0f
        repeat(barCount) { index ->
            if (index % 2 == 0) {
                drawRect(
                    color = Color(0xFF171817),
                    topLeft =
                        androidx.compose.ui.geometry
                            .Offset(x, 0f),
                    size =
                        androidx.compose.ui.geometry
                            .Size(barWidth * (1 + index % 3), size.height),
                )
            }
            x += barWidth * 2
        }
    }
}
