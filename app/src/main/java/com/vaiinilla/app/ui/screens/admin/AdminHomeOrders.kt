package com.vaiinilla.app.ui.screens.admin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.DeliveryAddress
import com.vaiinilla.app.domain.model.HomeCheckoutRules
import com.vaiinilla.app.domain.model.HomeOrderAction
import com.vaiinilla.app.domain.model.HomeOrderBoard
import com.vaiinilla.app.domain.model.HomeOrderStep
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.time.BusinessTime
import com.vaiinilla.app.ui.admin.AdminUiState
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.screens.ActionPill
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.OperationalField
import java.net.URLEncoder
import java.time.LocalDate

/**
 * La agenda de un negocio de repostería (CASA-09): «Me deben» arriba con lo entregado que falta cobrar y luego cada
 * día con sus entregas. Tocar un pedido abre su hoja con las acciones.
 */
internal fun LazyListScope.agendaItems(
    state: AdminUiState,
    colors: OperationalColors,
    onRetry: () -> Unit,
    onOpen: (OrderDetail) -> Unit,
) {
    val error = state.agendaError
    if (error != null && state.agenda.isEmpty() && state.owed.isEmpty()) {
        item(
            key = "agenda-error",
        ) { Box(Modifier.padding(horizontal = 16.dp)) { AdminLoadError(error, colors, onRetry) } }
        return
    }
    if (state.owed.isNotEmpty()) {
        item(key = "me-deben") {
            Box(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                AdminCard(colors) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AdminSectionTitle("Me deben", colors)
                        Box(Modifier.weight(1f))
                        Text(
                            moneyLabel(HomeOrderBoard.total(state.owed)),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = colors.textPrimary,
                        )
                    }
                    AdminHint("Entregados que falta cobrar. Toca uno para anotar cómo te pagó.", colors)
                    state.owed.forEach { order ->
                        AgendaRow(order, colors, due = true) { onOpen(order) }
                    }
                }
            }
        }
    }
    item(key = "agenda-head") {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text("Lo que entregas cada día", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        }
    }
    val days = state.agenda.filter { it.orders.isNotEmpty() }
    if (days.isEmpty() && !state.agendaLoading) {
        item(key = "agenda-vacia") {
            Box(Modifier.padding(horizontal = 20.dp)) {
                AdminCard(colors) {
                    Text(
                        "Sin entregas pendientes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = colors.textPrimary,
                    )
                    AdminHint("Cuando alguien te pida para un día, sale aquí en ese día.", colors)
                }
            }
        }
    }
    items(days, key = { "dia-${it.date}" }) { day ->
        val today = LocalDate.now(BusinessTime.zone)
        Box(Modifier.animateItem().padding(horizontal = 20.dp, vertical = 6.dp)) {
            AdminCard(colors) {
                AdminSectionTitle(
                    "${HomeCheckoutRules.dayLabel(day.date, today)} · " +
                        if (day.orders.size == 1) "1 pedido" else "${day.orders.size} pedidos",
                    colors,
                )
                day.orders.forEach { order -> AgendaRow(order, colors, due = false) { onOpen(order) } }
            }
        }
    }
    if (state.agendaLoading) {
        item(key = "agenda-cargando") {
            Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color = colors.textPrimary,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun AgendaRow(
    order: OrderDetail,
    colors: OperationalColors,
    due: Boolean,
    onClick: () -> Unit,
) {
    val who = order.user?.name ?: order.customerName ?: order.homeDelivery?.address?.address ?: ""
    Row(
        modifier = Modifier.fillMaxWidth().physicalPress(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "#${order.summary.folio} · $who",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 1,
            )
            Text(
                listOfNotNull(order.summary.state.label, order.summary.deliverySlot).joinToString(" · "),
                fontSize = 12.sp,
                color = colors.textSecondary,
            )
        }
        Text(
            moneyLabel(if (due) order.homeDelivery?.dueOnArrival ?: order.summary.total else order.summary.total),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
        )
    }
}

/** A dónde va el pedido, con Mapas y Llamar, para quien sale a entregarlo. */
@Composable
internal fun HomeDeliveryAdminCard(
    order: OrderDetail,
    colors: OperationalColors,
) {
    val home = order.homeDelivery ?: return
    val uri = LocalUriHandler.current
    val haptics = rememberVaiinillaHaptics()
    AdminCard(colors) {
        AdminSectionTitle("Entrega", colors)
        Text(home.address.address, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        if (home.address.reference.isNotBlank()) AdminHint(home.address.reference, colors)
        order.summary.deliveryDate?.let { date ->
            AdminValueRow(
                "Día",
                HomeCheckoutRules.dayLabel(date, LocalDate.now(BusinessTime.zone)) +
                    (order.summary.deliverySlot?.let { " · $it" } ?: ""),
                colors,
            )
        }
        home.fee?.let { AdminValueRow("Envío", moneyLabel(it), colors) }
        home.deposit?.takeIf { (it.toBigDecimalOrNull()?.signum() ?: 0) > 0 }?.let {
            AdminValueRow("Anticipo pagado", moneyLabel(it), colors)
        }
        home.dueOnArrival
            ?.takeIf {
                home.hasAmountDue
            }?.let { AdminValueRow("Por cobrar al entregar", moneyLabel(it), colors, strong = true) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionPill(
                label = "Abrir en Mapas",
                enabled = true,
                loading = false,
                colors = colors,
                primary = false,
                modifier = Modifier.weight(1f),
            ) {
                haptics.click()
                runCatching { uri.openUri(mapsUrl(home.address)) }
            }
            if (home.address.phone.isNotBlank()) {
                ActionPill(
                    label = "Llamar",
                    enabled = true,
                    loading = false,
                    colors = colors,
                    primary = false,
                    modifier = Modifier.weight(1f),
                ) {
                    haptics.click()
                    runCatching { uri.openUri("tel:${home.address.phone}") }
                }
            }
        }
    }
}

/** El siguiente paso de un pedido a domicilio o encargo, según su estado (como en iOS). */
@Composable
internal fun HomeOrderActionsCard(
    order: OrderDetail,
    busy: Boolean,
    colors: OperationalColors,
    onAction: (HomeOrderAction) -> Unit,
) {
    val step = HomeOrderBoard.step(order) ?: return
    val haptics = rememberVaiinillaHaptics()
    var rejecting by remember(order.summary.id) { mutableStateOf(false) }
    var reason by remember(order.summary.id) { mutableStateOf("") }
    var code by remember(order.summary.id) { mutableStateOf("") }

    fun run(action: HomeOrderAction) {
        haptics.impact()
        onAction(action)
    }
    AdminCard(colors) {
        AnimatedContent(
            targetState = step to rejecting,
            transitionSpec = { fadeIn(spring(stiffness = 400f)) togetherWith fadeOut(spring(stiffness = 400f)) },
            label = "paso-domicilio",
        ) { (current, isRejecting) ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (current) {
                    HomeOrderStep.DECIDE ->
                        if (isRejecting) {
                            AdminSectionTitle("¿Rechazar el encargo?", colors)
                            AdminHint("Se le devuelve lo que pagó y le llega el motivo.", colors)
                            OperationalField(
                                value = reason,
                                onValueChange = { reason = it.take(200) },
                                label = "Motivo",
                                colors = colors,
                                enabled = !busy,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ActionPill(
                                    "Cancelar",
                                    !busy,
                                    false,
                                    colors,
                                    primary = false,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    rejecting = false
                                }
                                ActionPill(
                                    "Rechazar",
                                    !busy && reason.isNotBlank(),
                                    busy,
                                    colors,
                                    modifier = Modifier.weight(1f),
                                ) { run(HomeOrderAction.Reject(reason)) }
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ActionPill(
                                    "Rechazar",
                                    !busy,
                                    false,
                                    colors,
                                    primary = false,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    rejecting = true
                                }
                                ActionPill("Aceptar encargo", !busy, busy, colors, modifier = Modifier.weight(1.4f)) {
                                    run(HomeOrderAction.Accept)
                                }
                            }
                        }
                    HomeOrderStep.SEND_OUT -> {
                        ActionPill("Salió a entregar", !busy, busy, colors) { run(HomeOrderAction.SendOut) }
                        AdminHint("Al cliente le llega su código para recibirlo.", colors)
                    }
                    HomeOrderStep.DELIVER -> {
                        OperationalField(
                            value = code,
                            onValueChange = { code = it.filter(Char::isDigit).take(4) },
                            label = "Código del cliente",
                            colors = colors,
                            placeholder = "0000",
                            supporting = "Pídele al cliente los 4 dígitos que le llegaron.",
                            enabled = !busy,
                            keyboardType = KeyboardType.NumberPassword,
                        )
                        ActionPill("Entregado", !busy && code.length == 4, busy, colors) {
                            run(HomeOrderAction.Deliver(code))
                        }
                    }
                    HomeOrderStep.COLLECT -> {
                        AdminHint("Anota cómo te pagó al recibir.", colors)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ActionPill("Cobré en efectivo", !busy, busy, colors, modifier = Modifier.weight(1f)) {
                                run(HomeOrderAction.Collect("efectivo"))
                            }
                            ActionPill(
                                "Me pagó por transferencia",
                                !busy,
                                false,
                                colors,
                                primary = false,
                                modifier = Modifier.weight(1f),
                            ) { run(HomeOrderAction.Collect("transferencia")) }
                        }
                    }
                }
            }
        }
    }
}

private fun mapsUrl(address: DeliveryAddress): String {
    val destination =
        if (address.latitude != null && address.longitude != null) {
            "${address.latitude},${address.longitude}"
        } else {
            URLEncoder.encode(address.address, "UTF-8")
        }
    return "https://www.google.com/maps/dir/?api=1&destination=$destination"
}
