package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.data.outbox.PreferencesStuckOrderSettings
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.offline.OfflineStaffOrder
import com.vaiinilla.app.domain.outbox.OutboxDelayRules
import com.vaiinilla.app.domain.outbox.StuckOrderRules
import kotlinx.coroutines.delay
import java.time.Instant

private val AlertRed = Color(0xFFFFD9D2)
private val AlertInk = Color(0xFF4A0E04)
private const val TICK_MS = 30_000L

/** Hora que se refresca sola cada 30 s para que «lleva 3 min» no se quede congelado. */
@Composable
private fun rememberTickingNow(): Instant {
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(TICK_MS)
            now = Instant.now()
        }
    }
    return now
}

/**
 * Una comanda guardada sin internet que lleva rato sin llegar: Cocina todavía no la tiene. Es el aviso que no se
 * puede pasar por alto, aparte de la franja amarilla de «sin internet».
 */
@Composable
internal fun UndeliveredOrdersAlert(
    pending: List<OfflineStaffOrder>,
    modifier: Modifier = Modifier,
) {
    val now = rememberTickingNow()
    val waiting = pending.filter { OutboxDelayRules.isUrgent(it.capturedAt, now) }
    if (waiting.isEmpty()) return
    val oldest = waiting.maxOf { OutboxDelayRules.minutesWaiting(it.capturedAt, now) }
    AlertBox(modifier) {
        Text(
            if (waiting.size ==
                1
            ) {
                "Una comanda no ha llegado a Cocina"
            } else {
                "${waiting.size} comandas no han llegado a Cocina"
            },
            color = AlertInk,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
        )
        Text(
            "${waiting.joinToString(" · ") { it.localFolio }} lleva $oldest min guardada en este teléfono. " +
                "Llévala a Cocina a mano; se manda sola en cuanto haya red.",
            color = AlertInk,
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
            lineHeight = 17.sp,
        )
    }
}

/** Comandas que llevan más del tiempo del negocio sin cambiar de estado; avisa a Caja. */
@Composable
internal fun StuckOrdersAlert(
    orders: List<OrderSummary>,
    establishmentId: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val policy =
        remember(establishmentId) {
            establishmentId?.let { runCatching { PreferencesStuckOrderSettings(context).policyFor(it) }.getOrNull() }
        }
    val now = rememberTickingNow()
    val stuck = policy?.let { StuckOrderRules.find(orders, it, now) }.orEmpty()
    if (stuck.isEmpty()) return
    AlertBox(modifier) {
        Text(
            if (stuck.size ==
                1
            ) {
                "Una comanda lleva rato sin avanzar"
            } else {
                "${stuck.size} comandas llevan rato sin avanzar"
            },
            color = AlertInk,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
        )
        Text(
            stuck
                .take(
                    MAX_LISTED,
                ).joinToString(" · ") { "#${it.folio} ${it.state.label.lowercase()} ${it.minutes} min" },
            color = AlertInk,
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
            lineHeight = 17.sp,
        )
    }
}

/** Lo que Caja ve arriba: comandas que no llegaron a Cocina y comandas paradas. */
@Composable
internal fun CashierOutboxAlerts(
    pending: List<OfflineStaffOrder>,
    orders: List<OrderSummary>,
    establishmentId: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        UndeliveredOrdersAlert(pending = pending)
        StuckOrdersAlert(orders = orders, establishmentId = establishmentId)
    }
}

private const val MAX_LISTED = 4

@Composable
private fun AlertBox(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(AlertRed)
                .semantics { liveRegion = LiveRegionMode.Assertive }
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        content()
    }
}
