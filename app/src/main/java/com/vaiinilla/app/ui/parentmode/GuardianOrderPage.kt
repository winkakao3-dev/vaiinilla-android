package com.vaiinilla.app.ui.parentmode

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.repository.OrderRepository
import com.vaiinilla.app.ui.components.GlassCircleButton
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.minTouchTarget
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.order.JourneyVenue
import com.vaiinilla.app.ui.screens.OrderJourneyPage
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Cada cuánto se vuelve a leer el pedido mientras el tutor lo mira. */
private const val GUARDIAN_ORDER_POLL_MS = 20_000L

/** La nota de abajo: el tutor solo mira; cancelar o pagar lo hace el hijo o lo apartado de su ficha. */
internal fun guardianOrderNote(childName: String): String =
    "Lo pidió $childName. Aquí solo lo ves: no se puede cancelar ni pagar desde tu cuenta."

/**
 * El pedido de un hijo, abierto desde su ficha o desde el aviso del pedido (P08). Se lee con la sesión del tutor
 * (GET /pedidos/:id) y se dibuja con el mismo seguimiento que ve el hijo, pero sin acciones.
 */
@Composable
internal fun GuardianOrderPage(
    orderId: String,
    childName: String,
    orders: OrderRepository?,
    onBack: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    var order by remember(orderId) { mutableStateOf<OrderDetail?>(null) }
    var error by remember(orderId) { mutableStateOf<String?>(null) }
    var attempt by remember(orderId) { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(orderId, attempt) {
        val repository = orders
        if (repository == null) {
            error = "No pudimos abrir el pedido."
            return@LaunchedEffect
        }
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                withContext(Dispatchers.IO) { repository.getOrder(orderId) }
                    .onSuccess {
                        order = it
                        error = null
                    }.onFailure { if (order == null) error = it.toUserFacingMessage() }
                delay(GUARDIAN_ORDER_POLL_MS)
            }
        }
    }
    val shown = order
    if (shown != null) {
        OrderJourneyPage(
            order = shown,
            qrPayload = null,
            onBack = onBack,
            modifier = Modifier.testTag("guardian-order"),
            venue = JourneyVenue(school = true),
            guestNotice = guardianOrderNote(childName),
            readOnly = true,
        )
        return
    }
    Box(Modifier.fillMaxSize().background(colors.paper).testTag("guardian-order")) {
        val message = error
        if (message == null) {
            CircularProgressIndicator(
                color = colors.ink,
                strokeWidth = 2.dp,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 72.dp)) {
                Text("No pudimos abrir el pedido", color = colors.ink, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(message, color = colors.muted, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                Text(
                    "Reintentar",
                    color = colors.ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    modifier =
                        Modifier
                            .padding(top = 10.dp)
                            .physicalPress(scale = PhysicalPressScale.Small) {
                                error = null
                                attempt += 1
                            }.minTouchTarget(),
                )
            }
        }
        GlassCircleButton(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Regresar",
            onClick = onBack,
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(start = 16.dp, top = 6.dp),
        )
    }
}
