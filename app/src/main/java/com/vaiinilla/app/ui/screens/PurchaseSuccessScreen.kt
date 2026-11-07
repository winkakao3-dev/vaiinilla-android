package com.vaiinilla.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.core.venue.LocalVenueContext
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.order.FactTone
import com.vaiinilla.app.ui.order.JourneyVenue
import com.vaiinilla.app.ui.order.PurchaseCelebrationKind
import com.vaiinilla.app.ui.order.celebrationMessage
import com.vaiinilla.app.ui.order.journeyFacts
import com.vaiinilla.app.ui.order.orderJourney
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

/** La compra ya quedó: qué se pagó, qué falta y a dónde sigues (Doop 15). Se queda hasta que elijas. */
@Composable
fun PurchaseSuccessScreen(
    order: OrderDetail,
    kind: PurchaseCelebrationKind,
    onFollowOrder: () -> Unit,
    onViewOrders: () -> Unit,
    venue: JourneyVenue = JourneyVenue(),
) {
    val colors = LocalVaiinillaColors.current
    val reduceMotion = reducedMotion()
    val haptics = rememberVaiinillaHaptics()
    val entry = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(order.summary.id, kind) {
        haptics.success()
        if (!reduceMotion) entry.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 320f))
    }
    val paid = kind == PurchaseCelebrationKind.PAYMENT_CONFIRMED
    val venueContext = LocalVenueContext.current
    val facts = remember(order) { journeyFacts(order, orderJourney(order, venue)) }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.paper)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(124.dp)
                    .graphicsLayer {
                        alpha = entry.value.coerceIn(0f, 1f)
                        scaleX = entry.value
                        scaleY = entry.value
                    }.background(colors.accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = if (paid) "Compra confirmada" else "Pedido recibido",
                tint = colors.accentInk,
                modifier = Modifier.size(58.dp),
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(
            listOfNotNull("Pedido #${order.summary.folio}", venue.name).joinToString(" · ").uppercase(),
            color = colors.muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.2.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (paid) "¡Compra\nconfirmada!" else "¡Pedido\nrecibido!",
            color = colors.ink,
            fontSize = 36.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1.4).sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = celebrationMessage(venueContext, order, kind),
            color = colors.ink2,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (facts.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(colors.paper2)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                facts.forEachIndexed { index, fact ->
                    if (index > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            fact.label,
                            color = colors.ink2,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            fact.value,
                            color =
                                when (fact.tone) {
                                    FactTone.GOOD -> colors.accent
                                    FactTone.DUE -> colors.yolk
                                    FactTone.PLAIN -> colors.ink
                                },
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.End,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            // Una dirección larga ocupaba todo el renglón y la etiqueta quedaba de una letra por
                            // renglón («Te lo llevan a», S01): el valor comparte el ancho y se corta en dos líneas.
                            modifier = Modifier.weight(1.6f, fill = false),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        SuccessButton("Seguir pedido", background = colors.ink, content = colors.onInk, onClick = onFollowOrder)
        Spacer(Modifier.height(10.dp))
        SuccessButton(
            "Ver mis pedidos",
            background = Color.Transparent,
            content = colors.ink,
            border = colors.ink.copy(alpha = 0.12f),
            onClick = onViewOrders,
        )
    }
}

@Composable
private fun SuccessButton(
    text: String,
    background: Color,
    content: Color,
    border: Color = Color.Transparent,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .physicalPress(onClick = onClick)
                .clip(shape)
                .background(background)
                .border(1.5.dp, border, shape)
                .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = content, fontSize = 16.sp, fontWeight = FontWeight.Black)
    }
}
