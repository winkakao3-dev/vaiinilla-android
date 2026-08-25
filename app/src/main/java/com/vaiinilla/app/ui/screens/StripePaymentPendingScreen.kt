package com.vaiinilla.app.ui.screens

import androidx.compose.animation.Crossfade
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.vaiinilla.app.domain.model.StripePaymentStatus
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.order.StripePaymentPhase

private val StripeBg = Color(0xFF0D0D0D)
private val StripeInk = Color(0xFFF3EFE4)
private val StripeMuted = Color(0xFFB8B3AA)
private val StripePanel = Color(0xFF1A1B19)
private val StripeLine = Color(0x42F3EFE4)
private val StripeLime = Color(0xFFB7D464)
private val StripeLimeInk = Color(0xFF172008)
private val StripeCoral = Color(0xFFFF8F77)

@Composable
fun StripePaymentPendingScreen(
    order: OrderDetail,
    phase: StripePaymentPhase,
    message: String?,
    retrying: Boolean,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onViewOrders: () -> Unit,
) {
    val reduceMotion = reducedMotion()
    val waiting = phase == StripePaymentPhase.PENDING || phase == StripePaymentPhase.PROCESSING_CONFIRMATION
    val timedOut = phase == StripePaymentPhase.TIMED_OUT
    val failed = phase == StripePaymentPhase.FAILED
    val canceled = phase == StripePaymentPhase.CANCELED
    val confirmed = phase == StripePaymentPhase.CONFIRMED
    val status = order.payment?.status
    val title =
        when {
            confirmed -> "Pago confirmado"
            failed -> "Pago no completado"
            canceled -> "Pago cancelado"
            timedOut -> "Seguimos confirmando tu pago"
            status == StripePaymentStatus.PROCESSING -> "Pago en proceso"
            status == StripePaymentStatus.REQUIRES_ACTION -> "Necesitamos completar una acción"
            else -> "Confirmando tu pago"
        }
    val body =
        message
            ?: when {
                waiting -> "Estamos verificando el pago con Vaiinilla."
                timedOut -> "El pago todavía no tiene una confirmación final."
                failed -> "El backend confirmó que el pago no se completó."
                canceled -> "El backend confirmó que el pago fue cancelado."
                confirmed -> "Tu pedido ya puede continuar."
                else -> "Estamos verificando el estado del pago."
            }
    val transition = rememberInfiniteTransition(label = "stripe-payment-pulse")
    val pulse by
        if (reduceMotion || !waiting) {
            remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
        } else {
            transition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                label = "stripe-payment-pulse-scale",
            )
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(StripeBg)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .border(1.dp, StripeLine, CircleShape)
                    .physicalPress(onClick = onViewOrders),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Ver mis pedidos",
                tint = StripeInk,
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Crossfade(
                targetState = phase,
                animationSpec = tween(durationMillis = if (reduceMotion) 0 else 180),
                label = "stripe-payment-status-transition",
            ) {
                val displayedWaiting =
                    it == StripePaymentPhase.PENDING || it == StripePaymentPhase.PROCESSING_CONFIRMATION
                val displayedIcon =
                    when (it) {
                        StripePaymentPhase.CONFIRMED -> Icons.Outlined.CheckCircle
                        StripePaymentPhase.FAILED -> Icons.Outlined.ErrorOutline
                        StripePaymentPhase.CANCELED -> Icons.Outlined.Cancel
                        StripePaymentPhase.TIMED_OUT -> Icons.Outlined.Refresh
                        else -> Icons.Outlined.Sync
                    }
                val displayedTint =
                    when (it) {
                        StripePaymentPhase.FAILED,
                        StripePaymentPhase.CANCELED,
                        -> StripeCoral
                        else -> StripeLime
                    }
                Box(
                    modifier =
                        Modifier
                            .size(112.dp)
                            .graphicsLayer {
                                val displayedPulse = if (displayedWaiting) pulse else 1f
                                scaleX = displayedPulse
                                scaleY = displayedPulse
                            }.background(StripePanel, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = displayedIcon,
                        contentDescription =
                            when (it) {
                                StripePaymentPhase.PENDING,
                                StripePaymentPhase.PROCESSING_CONFIRMATION,
                                -> "Pago en verificación"
                                StripePaymentPhase.CONFIRMED -> "Pago confirmado"
                                StripePaymentPhase.FAILED -> "Pago no completado"
                                StripePaymentPhase.CANCELED -> "Pago cancelado"
                                else -> "Estado del pago"
                            },
                        tint = displayedTint,
                        modifier = Modifier.size(62.dp),
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            Text(
                text = title,
                color = StripeInk,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = body,
                color = StripeMuted,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center,
            )
            if (waiting) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "No cierres la app mientras confirmamos.",
                    color = StripeMuted.copy(alpha = 0.78f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(24.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = StripePanel,
                shape = RoundedCornerShape(20.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            "Total a pagar",
                            color = StripeMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Pedido #${order.summary.folio}",
                            color = StripeInk,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    Text(
                        moneyLabel(order.summary.total),
                        color = StripeInk,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            when {
                timedOut -> {
                    StripeActionButton(
                        label = "Actualizar estado",
                        onClick = onRefresh,
                        enabled = true,
                    )
                }
                failed || canceled -> {
                    StripeActionButton(
                        label = if (retrying) "Preparando pago…" else "Reintentar pago",
                        onClick = onRetry,
                        enabled = !retrying,
                    )
                }
                else -> {
                    StripeActionButton(
                        label = "Ver mis pedidos",
                        onClick = onViewOrders,
                        enabled = true,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            if (timedOut || failed || canceled) {
                StripeSecondaryAction(label = "Ver mis pedidos", onClick = onViewOrders)
            }
        }
    }
}

@Composable
private fun StripeActionButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .physicalPress(enabled = enabled, onClick = onClick),
        color = if (enabled) StripeLime else StripePanel,
        shape = RoundedCornerShape(18.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (enabled) StripeLimeInk else StripeMuted,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun StripeSecondaryAction(
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(1.dp, StripeLine, RoundedCornerShape(16.dp))
                .physicalPress(onClick = onClick),
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                color = StripeInk,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}
