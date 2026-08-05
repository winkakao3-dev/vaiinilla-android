package com.vaiinilla.app.ui.components.sticker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
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
    val time: String = "",
)

fun emptyStickerOrderData() =
    StickerOrderData(
        folio = 0,
        total = "0.00",
        productName = "Sin pedido seleccionado",
        paymentLabel = "—",
        destinationLabel = "—",
        date = "—",
        time = "—",
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
    val frame = Color(0xFFE8E6DA)
    val bg = Color(0xFF171817)
    val text = Color(0xFFF4F1E7)
    val muted = Color(0xFFA9AAA4)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(0.6f)
                .background(frame, RoundedCornerShape(22.dp))
                .padding(14.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(bg, RoundedCornerShape(5.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy((-1).dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                "ANTOJO".forEach { letter ->
                    Text(
                        letter.toString(),
                        color = text,
                        fontWeight = FontWeight.Black,
                        fontSize = 30.sp,
                        lineHeight = 30.sp,
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                StickerSize.entries.forEach { chip ->
                    Box(
                        modifier =
                            Modifier
                                .size(18.dp)
                                .border(
                                    width = if (chip == size) 1.5.dp else 1.dp,
                                    color = if (chip == size) text else text.copy(alpha = 0.65f),
                                    shape = CircleShape,
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(chip.label, color = text, fontSize = 6.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(0.62f)) {
                    EditorialBarcode(
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        color = text,
                    )
                    Text(
                        "1 200220 190045",
                        color = muted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 5.sp,
                        letterSpacing = 0.2.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Column(
                    modifier = Modifier.weight(0.38f).padding(start = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .border(1.dp, text.copy(alpha = 0.9f), RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("VNNL", color = text, fontSize = 14.sp, letterSpacing = 1.sp)
                    }
                    Text(
                        "Daily food and style exploration.\nSchool edition 01.",
                        color = muted,
                        fontSize = 5.sp,
                        lineHeight = 6.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
            Text(
                order.productName,
                color = text,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
            )
            Text(
                "Product Sticker",
                color = text,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
            ) {
                listOf(
                    Icons.Outlined.Add,
                    Icons.Outlined.AccessTime,
                    Icons.Outlined.LocationOn,
                    Icons.Outlined.Remove,
                ).forEach { icon ->
                    EditorialIconButton(icon = icon, color = text)
                }
            }
            StickerMetaGrid(order, text, muted)
            Text(
                "PRODUCT INSTRUCTIONS AND GUIDES",
                color = muted,
                fontWeight = FontWeight.Bold,
                fontSize = 7.sp,
                letterSpacing = 0.4.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
            )
            HorizontalDivider(color = muted.copy(alpha = 0.7f), modifier = Modifier.padding(top = 4.dp))
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .border(1.dp, muted.copy(alpha = 0.75f))
                        .padding(horizontal = 6.dp, vertical = 5.dp),
            ) {
                Text(
                    "CONSERVA ESTE TICKET PARA CONSULTAR TU PEDIDO. PRESENTA EL CÓDIGO AL RECOGER ESTE STICKER PARA IDENTIFICAR LA ORDEN.",
                    color = muted,
                    fontSize = 5.sp,
                    lineHeight = 6.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.weight(0.45f))
        }
    }
}

@Composable
private fun EditorialBarcode(
    modifier: Modifier = Modifier,
    color: Color,
) {
    Canvas(modifier = modifier) {
        val pattern =
            intArrayOf(
                2,
                1,
                1,
                2,
                1,
                3,
                2,
                1,
                1,
                1,
                3,
                2,
                1,
                1,
                2,
                1,
                3,
                1,
                1,
                2,
                2,
                1,
                1,
                3,
                2,
                1,
                1,
                2,
                1,
                3,
                1,
                1,
                2,
                1,
                2,
                1,
                3,
                2,
                1,
                1,
            )
        val unit = size.width / pattern.sum()
        var x = 0f
        pattern.forEachIndexed { index, widthUnits ->
            val barWidth = unit * widthUnits
            if (index % 2 == 0) {
                drawRect(
                    color = color,
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, size.height),
                )
            }
            x += barWidth
        }
    }
}

@Composable
private fun EditorialIconButton(
    icon: ImageVector,
    color: Color,
) {
    Box(
        modifier = Modifier.size(24.dp).border(1.dp, color.copy(alpha = 0.9f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
    }
}

@Composable
private fun CoreDropSticker(
    order: StickerOrderData,
    modifier: Modifier = Modifier,
) {
    val frame = Color(0xFFE8E6DA)
    val lime = Color(0xFFB9D86D)
    val ink = Color(0xFF1B1C1A)
    val innerShape = RoundedCornerShape(28.dp)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(0.6f)
                .background(frame, RoundedCornerShape(22.dp))
                .padding(14.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().clip(innerShape).background(lime),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                var x = -size.height
                while (x < size.width) {
                    drawLine(
                        color = ink.copy(alpha = 0.08f),
                        start = Offset(x, 0f),
                        end = Offset(x + size.height, size.height),
                        strokeWidth = 1f,
                    )
                    x += 7f
                }
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("VAIINILLA", color = ink, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "RECEIPT STICKER\nDROP 024",
                        color = ink,
                        fontWeight = FontWeight.Black,
                        fontSize = 6.sp,
                        lineHeight = 7.sp,
                        letterSpacing = 0.4.sp,
                        textAlign = TextAlign.End,
                    )
                }
                HorizontalDivider(color = ink.copy(alpha = 0.8f), modifier = Modifier.padding(top = 10.dp))
                Text(
                    "YA ES\nTUYO.",
                    color = ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 40.sp,
                    lineHeight = 36.sp,
                    letterSpacing = (-1.4).sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                )
                Box(
                    modifier =
                        Modifier
                            .padding(top = 14.dp)
                            .background(ink, RoundedCornerShape(24.dp))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Text(
                        "• PEDIDO PAGADO",
                        color = lime,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        letterSpacing = 0.4.sp,
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                            .background(Color.White.copy(alpha = 0.34f), RoundedCornerShape(24.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Column {
                        Text(
                            "TU PEDIDO",
                            color = ink.copy(alpha = 0.65f),
                            fontWeight = FontWeight.Black,
                            fontSize = 8.sp,
                            letterSpacing = 0.8.sp,
                        )
                        Text(
                            order.productName,
                            color = ink,
                            fontWeight = FontWeight.Black,
                            fontSize = 21.sp,
                            lineHeight = 23.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            "Asada · salsa verde · queso gratinado",
                            color = ink.copy(alpha = 0.72f),
                            fontSize = 9.sp,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CoreMetaCell("#${order.folio}", "ORDEN", ink, Modifier.weight(1f))
                    CoreMetaCell(
                        order.destinationLabel.uppercase().replace("PARA ", ""),
                        "DESTINO",
                        ink,
                        Modifier.weight(1f),
                    )
                    CoreMetaCell(order.time, "HORA", ink, Modifier.weight(1f))
                }
                HorizontalDivider(color = ink.copy(alpha = 0.8f), modifier = Modifier.padding(top = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("??", "◉", "⌁", "V").forEach { symbol ->
                        Box(
                            modifier = Modifier.weight(1f).height(64.dp).border(1.dp, ink, RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(symbol, color = ink, fontWeight = FontWeight.Black, fontSize = 25.sp)
                        }
                    }
                }
                HorizontalDivider(color = ink.copy(alpha = 0.8f), modifier = Modifier.padding(top = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "TOTAL PAGADO",
                            color = ink,
                            fontWeight = FontWeight.Black,
                            fontSize = 8.sp,
                            letterSpacing = 0.6.sp,
                        )
                        Text(
                            "Saldo Vaiinilla",
                            color = ink.copy(alpha = 0.72f),
                            fontSize = 8.sp,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Text(moneyLabel(order.total), color = ink, fontWeight = FontWeight.Black, fontSize = 38.sp)
                }
                EditorialBarcode(modifier = Modifier.fillMaxWidth().height(44.dp).padding(top = 6.dp), color = ink)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "VNL-${order.folio}-${order.total}MX",
                        color = ink.copy(alpha = 0.75f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 7.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier =
                            Modifier
                                .border(
                                    1.dp,
                                    ink,
                                    RoundedCornerShape(50),
                                ).padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            "COMMON 01/24",
                            color = ink,
                            fontWeight = FontWeight.Black,
                            fontSize = 7.sp,
                            letterSpacing = 0.4.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CoreMetaCell(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontWeight = FontWeight.Black, fontSize = 11.sp, textAlign = TextAlign.Center)
        Text(label, color = color.copy(alpha = 0.48f), fontSize = 7.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun LimitedSticker(
    order: StickerOrderData,
    modifier: Modifier = Modifier,
) {
    val frame = Color(0xFFE8E6DA)
    val coral = Color(0xFFF25755)
    val ink = Color(0xFF260D0C)
    val paper = Color(0xFFFFE9DD)
    val innerShape = RoundedCornerShape(15.dp)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(0.6f)
                .background(frame, RoundedCornerShape(22.dp))
                .padding(14.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize().clip(innerShape).background(coral)) {
            Canvas(Modifier.fillMaxSize()) {
                var x = -size.height
                while (x < size.width) {
                    drawLine(
                        color = ink.copy(alpha = 0.08f),
                        start = Offset(x, 0f),
                        end = Offset(x + size.height, size.height),
                        strokeWidth = 1f,
                    )
                    x += 7f
                }
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("VNL / DROP", color = ink, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "LIMITED\nEDITION",
                        color = ink,
                        fontWeight = FontWeight.Black,
                        fontSize = 6.sp,
                        lineHeight = 7.sp,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.End,
                    )
                }
                HorizontalDivider(color = ink.copy(alpha = 0.8f), modifier = Modifier.padding(top = 8.dp))
                Text(
                    "HOT\nLUNCH",
                    color = ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 41.sp,
                    lineHeight = 34.sp,
                    letterSpacing = (-1.6).sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                )
                Text(
                    "Edición especial\npor comprar el producto de la semana.",
                    color = ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    lineHeight = 9.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
                LimitedPerforation(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(8.dp),
                    ink = ink,
                    frame = frame,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "OBJETO DESBLOQUEADO",
                            color = ink.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Black,
                            fontSize = 6.sp,
                            letterSpacing = 0.7.sp,
                        )
                        Text(
                            order.productName,
                            color = ink,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(top = 5.dp),
                        )
                        Text("Serie fuego 02", color = ink, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text(
                            "Disponible a partir del 13 de julio.",
                            color = ink.copy(alpha = 0.65f),
                            fontSize = 5.sp,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Box(
                        modifier = Modifier.size(42.dp).background(ink, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("✱", color = paper, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    }
                }
                HorizontalDivider(color = ink.copy(alpha = 0.8f), modifier = Modifier.padding(top = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LimitedMetaCell("#${order.folio}", "ORDEN", ink, Modifier.weight(1f))
                    Box(Modifier.width(1.dp).height(30.dp).background(ink.copy(alpha = 0.7f)))
                    LimitedMetaCell(moneyLabel(order.total), "TOTAL", ink, Modifier.weight(1f))
                    Box(Modifier.width(1.dp).height(30.dp).background(ink.copy(alpha = 0.7f)))
                    LimitedMetaCell("RARE", "NIVEL", ink, Modifier.weight(1f))
                }
                HorizontalDivider(color = ink.copy(alpha = 0.8f), modifier = Modifier.padding(top = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("01", color = ink, fontWeight = FontWeight.Black, fontSize = 6.sp)
                    Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(order.productName, color = ink, fontWeight = FontWeight.Bold, fontSize = 7.sp)
                        Text("edición especial · objeto desbloqueado", color = ink.copy(alpha = 0.65f), fontSize = 5.sp)
                    }
                    Text(moneyLabel(order.total), color = ink, fontWeight = FontWeight.Black, fontSize = 8.sp)
                }
                EditorialBarcode(modifier = Modifier.fillMaxWidth().height(42.dp).padding(top = 7.dp), color = ink)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "SERIE 02 · 013/150",
                        color = ink.copy(alpha = 0.75f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 6.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier =
                            Modifier
                                .border(
                                    1.dp,
                                    ink,
                                    RoundedCornerShape(50),
                                ).padding(horizontal = 9.dp, vertical = 3.dp),
                    ) {
                        Text(
                            "LIMITED",
                            color = ink,
                            fontWeight = FontWeight.Black,
                            fontSize = 6.sp,
                            letterSpacing = 0.4.sp,
                        )
                    }
                }
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                top = 8.dp,
                            ).border(1.dp, ink.copy(alpha = 0.8f))
                            .padding(horizontal = 6.dp, vertical = 5.dp),
                ) {
                    Text(
                        "COLECCIONA TRES STICKERS LIMITED PARA DESBLOQUEAR UNA RECOMPENSA.",
                        color = ink.copy(alpha = 0.75f),
                        fontSize = 5.sp,
                        lineHeight = 6.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun LimitedPerforation(
    modifier: Modifier,
    ink: Color,
    frame: Color,
) {
    Canvas(modifier) {
        var x = 0f
        while (x < size.width) {
            drawLine(
                ink.copy(alpha = 0.75f),
                Offset(x, size.height / 2),
                Offset(x + 4f, size.height / 2),
                strokeWidth = 1f,
            )
            x += 7f
        }
        drawCircle(frame, radius = 6f, center = Offset(0f, size.height / 2))
        drawCircle(frame, radius = 6f, center = Offset(size.width, size.height / 2))
    }
}

@Composable
private fun LimitedMetaCell(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontWeight = FontWeight.Black, fontSize = 9.sp, textAlign = TextAlign.Center)
        Text(label, color = color.copy(alpha = 0.48f), fontSize = 6.sp, modifier = Modifier.padding(top = 1.dp))
    }
}

@Composable
private fun BreakfastClubSticker(
    order: StickerOrderData,
    modifier: Modifier = Modifier,
) {
    val frame = Color(0xFFE8E6DA)
    val paper = Color(0xFFF8F0D6)
    val ink = Color(0xFF29231A)
    val yolk = Color(0xFFFFCF55)
    val innerShape = RoundedCornerShape(6.dp)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(0.6f)
                .background(frame, RoundedCornerShape(22.dp))
                .padding(14.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize().clip(innerShape).background(paper)) {
            Canvas(Modifier.fillMaxSize()) {
                var x = -size.height
                while (x < size.width) {
                    drawLine(
                        color = ink.copy(alpha = 0.07f),
                        start = Offset(x, 0f),
                        end = Offset(x + size.height, size.height),
                        strokeWidth = 1f,
                    )
                    x += 7f
                }
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("VANILLA AM", color = ink, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "BREAKFAST\nCLUB 2026",
                        color = ink,
                        fontWeight = FontWeight.Black,
                        fontSize = 6.sp,
                        lineHeight = 7.sp,
                        letterSpacing = 0.4.sp,
                        textAlign = TextAlign.End,
                    )
                }
                HorizontalDivider(color = ink.copy(alpha = 0.75f), modifier = Modifier.padding(top = 8.dp))
                BreakfastSun(
                    modifier = Modifier.fillMaxWidth().height(104.dp).padding(top = 12.dp),
                    yolk = yolk,
                    ink = ink,
                )
                Text(
                    "DESAYUNO\nCOMPLETO.",
                    color = ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp,
                    lineHeight = 28.sp,
                    letterSpacing = (-0.8).sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                )
                HorizontalDivider(color = ink.copy(alpha = 0.75f), modifier = Modifier.padding(top = 10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BreakfastMetaCell("08:14", "HORA", ink, Modifier.weight(1f))
                    Box(Modifier.width(1.dp).height(25.dp).background(ink.copy(alpha = 0.55f)))
                    BreakfastMetaCell("#${order.folio}", "ORDEN", ink, Modifier.weight(1f))
                    Box(Modifier.width(1.dp).height(25.dp).background(ink.copy(alpha = 0.55f)))
                    BreakfastMetaCell(moneyLabel(order.total), "TOTAL", ink, Modifier.weight(1f))
                }
                HorizontalDivider(color = ink.copy(alpha = 0.75f), modifier = Modifier.padding(top = 3.dp))
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    BreakfastItemRow("01", order.productName, "queso · pico de gallo", moneyLabel(order.total), ink)
                    BreakfastItemRow("01", "Agua natural", "", "$7", ink)
                }
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(68.dp),
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawRoundRect(
                            color = ink,
                            style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 5f))),
                            cornerRadius =
                                androidx.compose.ui.geometry
                                    .CornerRadius(16.dp.toPx()),
                        )
                    }
                    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "PROGRESO DE MAÑANA",
                            color = ink.copy(alpha = 0.75f),
                            fontWeight = FontWeight.Black,
                            fontSize = 6.sp,
                            letterSpacing = 0.7.sp,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                        Text(
                            "3 / 5",
                            color = ink,
                            fontWeight = FontWeight.Black,
                            fontSize = 25.sp,
                            modifier = Modifier.padding(top = 1.dp),
                        )
                        Text(
                            "Dos desayunos más para desbloquear recetas nuevas.",
                            color = ink.copy(alpha = 0.65f),
                            fontSize = 5.sp,
                            modifier = Modifier.padding(top = 1.dp),
                        )
                    }
                }
                EditorialBarcode(modifier = Modifier.fillMaxWidth().height(47.dp).padding(top = 7.dp), color = ink)
                Text(
                    "MORNING-${order.folio}-0305",
                    color = ink.copy(alpha = 0.72f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 6.sp,
                    letterSpacing = 0.3.sp,
                    modifier = Modifier.padding(top = 3.dp),
                )
                Spacer(Modifier.weight(1f))
                BreakfastTearLine(modifier = Modifier.fillMaxWidth().height(13.dp), color = frame)
            }
        }
    }
}

@Composable
private fun BreakfastSun(
    modifier: Modifier,
    yolk: Color,
    ink: Color,
) {
    Canvas(modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = minOf(size.width, size.height) * 0.34f
        drawCircle(color = yolk, radius = radius, center = center)
        drawCircle(color = ink, radius = radius * 0.16f, center = center)
        repeat(8) { index ->
            val angle = index * (Math.PI / 4).toFloat()
            val start =
                Offset(
                    center.x + kotlin.math.cos(angle) * radius * 0.56f,
                    center.y + kotlin.math.sin(angle) * radius * 0.56f,
                )
            val end =
                Offset(
                    center.x + kotlin.math.cos(angle) * radius * 0.82f,
                    center.y + kotlin.math.sin(angle) * radius * 0.82f,
                )
            drawLine(color = ink, start = start, end = end, strokeWidth = 1.2f)
        }
    }
}

@Composable
private fun BreakfastItemRow(
    index: String,
    title: String,
    detail: String,
    price: String,
    color: Color,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(index, color = color, fontWeight = FontWeight.Black, fontSize = 6.sp)
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Text(title, color = color, fontSize = 7.sp, fontWeight = FontWeight.Bold)
            if (detail.isNotEmpty()) Text(detail, color = color.copy(alpha = 0.65f), fontSize = 5.sp)
        }
        Text(price, color = color, fontWeight = FontWeight.Black, fontSize = 7.sp)
    }
}

@Composable
private fun BreakfastMetaCell(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontWeight = FontWeight.Black, fontSize = 8.sp, textAlign = TextAlign.Center)
        Text(label, color = color.copy(alpha = 0.5f), fontSize = 5.sp, modifier = Modifier.padding(top = 1.dp))
    }
}

@Composable
private fun BreakfastTearLine(
    modifier: Modifier,
    color: Color,
) {
    Canvas(modifier) {
        val tooth = 14f
        val path =
            androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, 0f)
                var x = 0f
                while (x < size.width) {
                    lineTo(x + tooth / 2, size.height)
                    lineTo(x + tooth, 0f)
                    x += tooth
                }
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
        drawPath(path, color)
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
