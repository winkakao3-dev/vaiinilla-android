package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.RoomService
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.mode.RestrictedMode
import com.vaiinilla.app.domain.model.CatalogProductDraft
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.ui.components.AuthHeroSheetScaffold
import com.vaiinilla.app.ui.components.AuthInkSubmitButton
import com.vaiinilla.app.ui.components.AuthSheetHeader
import com.vaiinilla.app.ui.components.OperationalEmptyState
import com.vaiinilla.app.ui.components.OrderSummaryCard
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.operational.OperationalUiState
import com.vaiinilla.app.ui.theme.Coral
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import java.math.BigDecimal

@Composable
fun CashierOperationalScreen(
    state: OperationalUiState,
    onBack: () -> Unit,
    onOpenCashSession: () -> Unit,
    onCollect: (orderId: String, amount: String, version: Int) -> Unit,
    onDeliver: (orderId: String, version: Int) -> Unit,
    onScanDeliver: (orderId: String, version: Int) -> Unit = { _, _ -> },
    onSearchWalletClients: (String) -> Unit = {},
    onOpenWalletUserQr: () -> Unit = {},
    onReloadWallet: (userId: String, amount: String) -> Unit = { _, _ -> },
    onChangeMode: (() -> Unit)? = null,
    restrictedMode: RestrictedMode? = null,
    onToggleProductAvailable: (productId: Int, available: Boolean) -> Unit = { _, _ -> },
    onCreateCashierProduct: (CatalogProductDraft, ByteArray?, String?, String?) -> Unit = { _, _, _, _ -> },
    onUploadCashierProductImage: (Int, ByteArray, String, String) -> Unit = { _, _, _, _ -> },
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val pending = state.orders.filter { it.summary.state == OrderState.PENDING_PAYMENT }
    val ready = state.orders.filter { it.summary.state == OrderState.READY }
    var walletSearch by remember { mutableStateOf("") }
    var walletAmount by remember { mutableStateOf("100.00") }
    val canReloadWallet =
        state.cashSessionOpen == true &&
            !state.acting &&
            restrictedMode != RestrictedMode.READ_ONLY

    AuthHeroSheetScaffold(
        kicker = "Caja",
        title = "Ventanilla de pagos.",
        intro = "Cobra en efectivo y entrega en barra.",
        loading = false,
        showBack = true,
        onBack = onBack,
        kickerIcon = Icons.Outlined.PointOfSale,
        scrollSheet = false,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                AuthSheetHeader(
                    kicker = "Caja",
                    title = "Ventanilla de pagos.",
                    intro = "Cobra en efectivo y entrega en barra.",
                    kickerIcon = Icons.Outlined.PointOfSale,
                )
            }
            if (onChangeMode != null && !state.acting) {
                item {
                    WorkerModeLink(onChangeMode)
                }
            }
            restrictedMode?.let { mode -> item { RestrictedModeNotice(mode) } }
            state.errorMessage?.let { message -> item { OperationalError(message) } }
            item {
                CashierCatalogPanel(
                    catalog = state.catalog,
                    acting = state.acting,
                    enabled = restrictedMode != RestrictedMode.READ_ONLY,
                    onToggleAvailable = onToggleProductAvailable,
                    onCreateProduct = onCreateCashierProduct,
                    onUploadImage = onUploadCashierProductImage,
                )
            }
            item {
                val open = state.cashSessionOpen
                Text(
                    text =
                        when (open) {
                            true -> "Sesión de caja abierta"
                            false -> "Sesión de caja cerrada — ábrela para recibir pedidos"
                            null -> "Consultando sesión de caja…"
                        },
                    color = colors.muted,
                )
                if (open == false) {
                    AuthInkSubmitButton(
                        text = "Abrir caja (500.00)",
                        onClick = onOpenCashSession,
                        enabled = !state.acting && restrictedMode != RestrictedMode.READ_ONLY,
                    )
                }
            }
            item {
                Text("Recargas de saldo", color = colors.ink, fontWeight = FontWeight.Black)
                Text(
                    "Busca por nombre o escanea el QR de cuenta del alumno. Caja tiene que estar abierta.",
                    color = colors.muted,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = walletSearch,
                        onValueChange = { walletSearch = it },
                        modifier = Modifier.weight(1f),
                        enabled = !state.walletSearchLoading,
                        label = { Text("Nombre o identificador contextual", color = colors.muted) },
                        singleLine = true,
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colors.ink,
                                unfocusedTextColor = colors.ink,
                                focusedBorderColor = colors.accent,
                                unfocusedBorderColor = colors.line,
                            ),
                    )
                    IconButton(
                        onClick = onOpenWalletUserQr,
                        enabled = !state.walletSearchLoading && restrictedMode != RestrictedMode.READ_ONLY,
                    ) {
                        Icon(
                            Icons.Outlined.QrCodeScanner,
                            contentDescription = "Escanear QR del alumno",
                            tint = colors.ink,
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    OutlinedTextField(
                        value = walletAmount,
                        onValueChange = { walletAmount = it },
                        modifier = Modifier.weight(1f),
                        enabled = canReloadWallet,
                        label = { Text("Monto", color = colors.muted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colors.ink,
                                unfocusedTextColor = colors.ink,
                                focusedBorderColor = colors.accent,
                                unfocusedBorderColor = colors.line,
                            ),
                    )
                    Button(
                        onClick = { onSearchWalletClients(walletSearch) },
                        enabled = !state.walletSearchLoading && walletSearch.trim().length >= 2,
                        modifier = Modifier.defaultMinSize(minWidth = 112.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = colors.accent,
                                contentColor = colors.accentInk,
                            ),
                    ) {
                        Text(if (state.walletSearchLoading) "Buscando…" else "Buscar", fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (state.walletClients.isEmpty() && walletSearch.trim().length >= 2 && !state.walletSearchLoading) {
                item {
                    Text("No hay clientes coincidentes en este establecimiento.", color = colors.muted)
                }
            } else {
                items(state.walletClients, key = { it.userId }) { client ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(client.name, color = colors.ink, fontWeight = FontWeight.Black)
                        client.contextualId?.let { identifier ->
                            Text(identifier, color = colors.muted)
                        }
                        Button(
                            onClick = { onReloadWallet(client.userId, walletAmount) },
                            enabled = canReloadWallet,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = colors.accent,
                                    contentColor = colors.accentInk,
                                ),
                        ) {
                            Text("Registrar recarga", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            state.walletReloadReceipt?.let { receipt ->
                item {
                    Text("Recarga registrada", color = colors.accent, fontWeight = FontWeight.Black)
                    Text(
                        "Saldo: $${receipt.previousBalance} + $${receipt.amount} = $${receipt.newBalance}",
                        color = colors.ink,
                    )
                }
            }
            item { SectionLabel("Por cobrar") }
            if (pending.isEmpty()) {
                item {
                    OperationalEmptyState(
                        title = "Sin pedidos por cobrar",
                        message = "Cuando un alumno confirme en efectivo, aparecerá aquí.",
                    )
                }
            } else {
                items(pending, key = { it.summary.id }) { order ->
                    CashCollectionCard(
                        order = order,
                        acting = state.acting,
                        onCollect = onCollect,
                        restrictedMode = restrictedMode,
                    )
                }
            }
            item { SectionLabel("Entregas en barra") }
            if (ready.isEmpty()) {
                item {
                    OperationalEmptyState(
                        title = "Sin entregas listas",
                        message = "Los pedidos para llevar listos se entregan desde aquí.",
                    )
                }
            } else {
                items(ready, key = { it.summary.id }) { order ->
                    val hasLocalPickupToken = !order.pickupToken.isNullOrBlank()
                    OrderSummaryCard(
                        order = order,
                        actionLabel = if (hasLocalPickupToken) "Confirmar entrega" else "Escanear QR y entregar",
                        enabled = !state.acting && restrictedMode != RestrictedMode.READ_ONLY,
                        onAction = {
                            if (hasLocalPickupToken) {
                                onDeliver(order.summary.id, order.summary.version)
                            } else {
                                onScanDeliver(order.summary.id, order.summary.version)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CashCollectionCard(
    order: OrderDetail,
    acting: Boolean,
    onCollect: (orderId: String, amount: String, version: Int) -> Unit,
    restrictedMode: RestrictedMode?,
) {
    val colors = LocalVaiinillaColors.current
    var received by remember(order.summary.id) { mutableStateOf("") }
    var confirmOpen by remember(order.summary.id) { mutableStateOf(false) }
    val normalizedReceived = received.trim().replace(',', '.')
    val receivedAmount = runCatching { BigDecimal(normalizedReceived) }.getOrNull()
    val totalAmount = runCatching { BigDecimal(order.summary.total) }.getOrNull()
    val change =
        if (receivedAmount != null && totalAmount != null) {
            receivedAmount.subtract(totalAmount)
        } else {
            null
        }
    val canReview =
        !acting &&
            restrictedMode != RestrictedMode.READ_ONLY &&
            receivedAmount != null &&
            receivedAmount > BigDecimal.ZERO &&
            change != null &&
            change >= BigDecimal.ZERO

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = received,
            onValueChange = { value ->
                received = value.filter { it.isDigit() || it == '.' || it == ',' }.take(12)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = restrictedMode != RestrictedMode.READ_ONLY && !acting,
            label = { Text("Efectivo recibido", color = colors.muted) },
            placeholder = { Text("Escribe el monto entregado por el alumno") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.ink,
                    unfocusedTextColor = colors.ink,
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.line,
                ),
        )
        Text(
            text =
                when {
                    received.isBlank() -> "Total a cobrar: ${moneyLabel(order.summary.total)}"
                    change != null && change >= BigDecimal.ZERO -> "Cambio: ${moneyLabel(change.toPlainString())}"
                    else -> "Monto insuficiente"
                },
            color = if (received.isNotBlank() && (change == null || change < BigDecimal.ZERO)) Coral else colors.ink,
            fontWeight = FontWeight.Bold,
        )
        OrderSummaryCard(
            order = order,
            actionLabel = "Revisar cobro",
            enabled = canReview,
            onAction = { confirmOpen = true },
        )
    }

    if (confirmOpen) {
        AlertDialog(
            onDismissRequest = { if (!acting) confirmOpen = false },
            title = { Text("Verifica el cobro") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pedido #${order.summary.folio}")
                    Text("Total: ${moneyLabel(order.summary.total)}", fontWeight = FontWeight.Bold)
                    Text("Recibido: ${moneyLabel(receivedAmount?.toPlainString() ?: normalizedReceived)}")
                    Text("Cambio: ${moneyLabel(change?.toPlainString() ?: "0.00")}")
                    Text(
                        "Confirma solo después de contar el efectivo. Esta acción registra el pago y no debe repetirse.",
                        color = colors.muted,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmOpen = false
                        onCollect(order.summary.id, normalizedReceived, order.summary.version)
                    },
                    enabled = canReview,
                ) {
                    Text("Confirmar efectivo")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmOpen = false },
                    enabled = !acting,
                ) {
                    Text("Volver")
                }
            },
        )
    }
}

@Composable
fun KitchenOperationalScreen(
    state: OperationalUiState,
    onBack: () -> Unit,
    onStart: (orderId: String, version: Int) -> Unit,
    onReady: (orderId: String, version: Int) -> Unit,
    onChangeMode: (() -> Unit)? = null,
    restrictedMode: RestrictedMode? = null,
) {
    val haptics = rememberVaiinillaHaptics()
    val active =
        state.orders.filter {
            it.summary.state == OrderState.PAID || it.summary.state == OrderState.PREPARING
        }
    AuthHeroSheetScaffold(
        kicker = "Cocina",
        title = "Comandas en fuego.",
        intro = "Empieza preparación y marca cuando esté listo.",
        loading = false,
        showBack = true,
        onBack = onBack,
        kickerIcon = Icons.Outlined.Restaurant,
        scrollSheet = false,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                AuthSheetHeader(
                    kicker = "Cocina",
                    title = "Comandas en fuego.",
                    intro = "Empieza preparación y marca cuando esté listo.",
                    kickerIcon = Icons.Outlined.Restaurant,
                )
            }
            if (onChangeMode != null && !state.acting) {
                item {
                    WorkerModeLink(onChangeMode)
                }
            }
            restrictedMode?.let { mode -> item { RestrictedModeNotice(mode) } }
            state.errorMessage?.let { message -> item { OperationalError(message) } }
            if (active.isEmpty()) {
                item {
                    OperationalEmptyState(
                        title = "Sin comandas",
                        message = "Cuando Caja confirme un pago con items de cocina, aparecerán aquí.",
                    )
                }
            } else {
                items(active, key = { it.summary.id }) { order ->
                    val action =
                        if (order.summary.state == OrderState.PAID) {
                            "Empezar preparación" to {
                                haptics.impact()
                                onStart(order.summary.id, order.summary.version)
                            }
                        } else {
                            "Marcar como listo" to {
                                haptics.success()
                                onReady(order.summary.id, order.summary.version)
                            }
                        }
                    OrderSummaryCard(
                        order = order,
                        actionLabel = action.first,
                        enabled = !state.acting && restrictedMode != RestrictedMode.READ_ONLY,
                        onAction = action.second,
                    )
                }
            }
        }
    }
}

@Composable
fun WaiterOperationalScreen(
    state: OperationalUiState,
    onBack: () -> Unit,
    onDeliver: (orderId: String, version: Int) -> Unit,
    onScanDeliver: (orderId: String, version: Int) -> Unit = { _, _ -> },
    onChangeMode: (() -> Unit)? = null,
    restrictedMode: RestrictedMode? = null,
) {
    val haptics = rememberVaiinillaHaptics()
    val ready = state.orders.filter { it.summary.state == OrderState.READY }
    AuthHeroSheetScaffold(
        kicker = "Mesero",
        title = "Entrega en el espacio.",
        intro = "Los pedidos listos para mesa aparecen aquí.",
        loading = false,
        showBack = true,
        onBack = onBack,
        kickerIcon = Icons.Outlined.RoomService,
        scrollSheet = false,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                AuthSheetHeader(
                    kicker = "Mesero",
                    title = "Entrega en el espacio.",
                    intro = "Los pedidos listos para mesa aparecen aquí.",
                    kickerIcon = Icons.Outlined.RoomService,
                )
            }
            if (onChangeMode != null && !state.acting) {
                item {
                    WorkerModeLink(onChangeMode)
                }
            }
            restrictedMode?.let { mode -> item { RestrictedModeNotice(mode) } }
            state.errorMessage?.let { message -> item { OperationalError(message) } }
            if (ready.isEmpty()) {
                item {
                    OperationalEmptyState(
                        title = "Sin mesas esperando",
                        message = "Los pedidos en espacio listos para entregar aparecerán aquí.",
                    )
                }
            } else {
                items(ready, key = { it.summary.id }) { order ->
                    val hasLocalPickupToken = !order.pickupToken.isNullOrBlank()
                    OrderSummaryCard(
                        order = order,
                        actionLabel =
                            if (hasLocalPickupToken) {
                                "Confirmar entrega en espacio"
                            } else {
                                "Escanear QR y entregar en espacio"
                            },
                        enabled = !state.acting && restrictedMode != RestrictedMode.READ_ONLY,
                        onAction = {
                            if (hasLocalPickupToken) {
                                onDeliver(order.summary.id, order.summary.version)
                            } else {
                                onScanDeliver(order.summary.id, order.summary.version)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Preview(name = "Caja", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun CashierOperationalScreenPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        CashierOperationalScreen(
            state = OperationalUiState(cashSessionOpen = false),
            onBack = {},
            onOpenCashSession = {},
            onCollect = { _, _, _ -> },
            onDeliver = { _, _ -> },
        )
    }
}

@Preview(name = "Cocina", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun KitchenOperationalScreenPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        KitchenOperationalScreen(
            state = OperationalUiState(),
            onBack = {},
            onStart = { _, _ -> },
            onReady = { _, _ -> },
        )
    }
}

@Preview(name = "Mesero", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun WaiterOperationalScreenPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        WaiterOperationalScreen(
            state = OperationalUiState(),
            onBack = {},
            onDeliver = { _, _ -> },
        )
    }
}

@Composable
private fun WorkerModeLink(onChangeMode: (() -> Unit)?) {
    if (onChangeMode == null) return
    val colors = LocalVaiinillaColors.current
    Text(
        "Cambiar modo",
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .clickable(role = Role.Button, onClick = onChangeMode),
        color = colors.muted,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun RestrictedModeNotice(mode: RestrictedMode) {
    val colors = LocalVaiinillaColors.current
    Text(
        text =
            when (mode) {
                RestrictedMode.READ_ONLY ->
                    "Este establecimiento está en solo lectura. Las acciones operativas están deshabilitadas."
                RestrictedMode.OPERATIONAL_CLOSE ->
                    "Este establecimiento está en cierre operativo. El servidor limita las acciones disponibles."
            },
        color = colors.muted,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun SectionLabel(text: String) {
    val colors = LocalVaiinillaColors.current
    Text(
        text,
        color = colors.ink,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Black,
    )
}

@Composable
private fun OperationalError(message: String) {
    Text(message, color = Coral, fontWeight = FontWeight.Bold)
}
