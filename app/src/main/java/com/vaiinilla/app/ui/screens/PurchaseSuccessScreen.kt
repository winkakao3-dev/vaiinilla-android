package com.vaiinilla.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.order.PurchaseCelebrationKind

private val SuccessBg = Color(0xFF0D0D0D)
private val SuccessInk = Color(0xFFF3EFE4)
private val SuccessMuted = Color(0xFFB8B3AA)
private val SuccessPanel = Color(0xFF1A1B19)
private val SuccessLime = Color(0xFFB7D464)
private val SuccessLimeInk = Color(0xFF172008)
private val SuccessLine = Color(0x42F3EFE4)

private const val SUCCESS_ENTRY_MS = 450
private const val SUCCESS_HOLD_MS = 1100L
private const val SUCCESS_EXIT_MS = 250

private val SuccessEase = CubicBezierEasing(0.22f, 0.8f, 0.25f, 1f)

@Composable
fun PurchaseSuccessScreen(
    order: OrderDetail,
    kind: PurchaseCelebrationKind,
    onFinished: () -> Unit,
    durationMillis: Long = SUCCESS_ENTRY_MS + SUCCESS_HOLD_MS + SUCCESS_EXIT_MS,
) {
    val reduceMotion = reducedMotion()
    val haptics = rememberVaiinillaHaptics()
    val latestOnFinished by rememberUpdatedState(onFinished)
    val entry = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val exit = remember { Animatable(0f) }
    val pulseTransition = rememberInfiniteTransition(label = "purchase-success-pulse")
    val pulse by
        if (reduceMotion) {
            remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
        } else {
            pulseTransition.animateFloat(
                initialValue = 0.96f,
                targetValue = 1.04f,
                animationSpec = infiniteRepeatable(tween(950), RepeatMode.Reverse),
                label = "purchase-success-pulse-scale",
            )
        }

    LaunchedEffect(order.summary.id, kind) {
        haptics.success()
        if (durationMillis <= 0L) {
            entry.snapTo(1f)
            latestOnFinished()
            return@LaunchedEffect
        }
        if (reduceMotion) {
            entry.snapTo(1f)
            delayForSuccess(durationMillis)
            latestOnFinished()
            return@LaunchedEffect
        }

        entry.animateTo(1f, tween(SUCCESS_ENTRY_MS, easing = SuccessEase))
        delayForSuccess(SUCCESS_HOLD_MS)
        exit.animateTo(1f, tween(SUCCESS_EXIT_MS, easing = SuccessEase))
        latestOnFinished()
    }

    val paid = kind == PurchaseCelebrationKind.PAYMENT_CONFIRMED
    val title = if (paid) "¡Compra confirmada!" else "¡Pedido recibido!"
    val message =
        if (paid) {
            "Tu pago fue autorizado y tu pedido ya está en marcha."
        } else {
            "Paga en Caja para continuar con tu pedido."
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(SuccessBg)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .graphicsLayer {
                    alpha = 1f - exit.value
                    translationY = -24.dp.toPx() * exit.value
                }.semantics {
                    heading()
                },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(164.dp)
                    .graphicsLayer {
                        alpha = entry.value
                        scaleX = entry.value * pulse
                        scaleY = entry.value * pulse
                    }.background(SuccessPanel, CircleShape)
                    .border(1.dp, SuccessLine, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(112.dp)
                        .background(SuccessLime, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = if (paid) "Compra confirmada" else "Pedido recibido",
                    tint = SuccessLimeInk,
                    modifier = Modifier.size(72.dp),
                )
            }
        }
        Spacer(Modifier.height(36.dp))
        Text(
            text = title,
            color = SuccessInk,
            fontSize = 32.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = message,
            color = SuccessMuted,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(30.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SuccessPanel,
            shape = RoundedCornerShape(22.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Pedido #${order.summary.folio}",
                        color = SuccessInk,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "Mostrando tu comprobante…",
                        color = SuccessMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    moneyLabel(order.summary.total),
                    color = SuccessLime,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

private suspend fun delayForSuccess(durationMillis: Long) {
    if (durationMillis <= 0L) return
    kotlinx.coroutines.delay(durationMillis)
}
