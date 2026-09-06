package com.vaiinilla.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.mode.RestrictedMode
import com.vaiinilla.app.domain.model.CatalogProductDraft
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderItem
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.VaiinillaAssistantOrb
import com.vaiinilla.app.ui.components.VaiinillaMark
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.mode.UnifiedTestModeManager
import com.vaiinilla.app.ui.operational.OperationalUiState

// Shared Color Palette for 1:1 Parity
private val PaletteCream = Color(0xFFF7F3E7)
private val PaletteCream2 = Color(0xFFEFEBDD)
private val PaletteCream3 = Color(0xFFE6E1D3)
private val PaletteInk = Color(0xFF171816)
private val PaletteInk2 = Color(0xFF30332E)
private val PaletteMuted = Color(0xFF73766D)
private val PaletteLime = Color(0xFFB7DE63)
private val PaletteLime2 = Color(0xFFD7F49A)
private val PaletteLime3 = Color(0xFF96C83F)
private val PaletteForest = Color(0xFF304427)
private val PaletteWhite = Color(0xFFFFFEF9)
private val PaletteDanger = Color(0xFFE45244)
private val PaletteAmber = Color(0xFFFFCB55)
private val PaletteCardBg = Color(0xC7FFFEF9)
private val PaletteCardBorder = Color(0x12171816)

@OptIn(ExperimentalMaterial3Api::class)
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
    onSwitchToRole: ((OperationalRole) -> Unit)? = null,
    restrictedMode: RestrictedMode? = null,
    onToggleProductAvailable: (productId: Int, available: Boolean) -> Unit = { _, _ -> },
    onCreateCashierProduct: (CatalogProductDraft, ByteArray?, String?, String?) -> Unit = { _, _, _, _ -> },
    onUploadCashierProductImage: (Int, ByteArray, String, String) -> Unit = { _, _, _, _ -> },
) {
    val haptics = rememberVaiinillaHaptics()
    var highlightedTarget by remember { mutableStateOf<String?>(null) }
    var addProductSheetOpen by remember { mutableStateOf(false) }
    var scanSheetOpen by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val recentOrder = state.orders.firstOrNull { it.summary.id == "order-047" } ?: state.orders.firstOrNull()
    val products = state.catalog?.products.orEmpty()
    val activeCount = products.count { it.available }
    val pausedCount = products.size - activeCount

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(PaletteCream),
    ) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Topline
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        VaiinillaMark(modifier = Modifier.size(34.dp))
                        Column {
                            Text(
                                "Vaiinilla",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = PaletteInk,
                            )
                            Text(
                                "Cuenta de caja",
                                fontSize = 12.sp,
                                color = PaletteMuted,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    // Account chip to switch to Cocina
                    Box(
                        modifier =
                            Modifier
                                .size(width = 48.dp, height = 40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(PaletteInk)
                                .clickable {
                                    haptics.impact()
                                    if (onSwitchToRole != null) {
                                        onSwitchToRole(OperationalRole.KITCHEN)
                                    } else {
                                        onChangeMode?.invoke()
                                    }
                                },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "DR",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                    }
                }
            }

            // Title & Subtitle
            item {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        "TURNO DE HOY",
                        color = PaletteMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.6.sp,
                    )
                    Text(
                        "Caja en control.",
                        color = PaletteInk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        letterSpacing = (-1.2).sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        "Entrega pedidos y mantén el menú disponible para todos.",
                        color = PaletteMuted,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            // Pedido reciente section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        "Pedido reciente",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = PaletteInk,
                    )
                    Text(
                        if (recentOrder?.summary?.state == OrderState.DELIVERED) "Completado" else "Por entregar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PaletteMuted,
                    )
                }
            }

            item {
                val isDelivered = recentOrder?.summary?.state == OrderState.DELIVERED
                val isOrderReady = recentOrder?.summary?.state == OrderState.READY
                val highlightScan = highlightedTarget == "scan_button"

                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .shadow(12.dp, RoundedCornerShape(26.dp), spotColor = Color(0x1A171816))
                            .border(
                                width = if (highlightScan) 2.5.dp else 1.dp,
                                color = if (highlightScan) PaletteLime3 else PaletteCardBorder,
                                shape = RoundedCornerShape(26.dp),
                            ),
                    shape = RoundedCornerShape(26.dp),
                    color = PaletteCardBg,
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column {
                                Text(
                                    "Pedido más reciente",
                                    color = PaletteMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "#${recentOrder?.summary?.folio ?: "047"}",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PaletteInk,
                                )
                            }
                            // Status Pill
                            Row(
                                modifier =
                                    Modifier
                                        .clip(CircleShape)
                                        .background(PaletteCream2)
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (isDelivered) PaletteLime3 else PaletteAmber),
                                )
                                Text(
                                    if (isDelivered) {
                                        "Entregado"
                                    } else if (isOrderReady) {
                                        "Listo"
                                    } else {
                                        "En proceso"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PaletteInk,
                                )
                            }
                        }

                        // Meta details
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Text(
                                "${recentOrder?.items?.sumOf { it.quantity } ?: 2} productos",
                                color = PaletteInk2,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "$${recentOrder?.summary?.total ?: "140"}",
                                color = PaletteInk2,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "Para llevar",
                                color = PaletteInk2,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        // Action scan button
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(58.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isDelivered) PaletteForest else PaletteInk)
                                    .clickable(enabled = !isDelivered) {
                                        haptics.impact()
                                        scanSheetOpen = true
                                    }.padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isDelivered) Color(0x33FFFFFF) else Color(0xFF2A2C29)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            if (isDelivered) Icons.Outlined.Check else Icons.Outlined.QrCodeScanner,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                    Column {
                                        Text(
                                            if (isDelivered) "Pedido entregado" else "Escanear QR",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            if (isDelivered) {
                                                "QR #${recentOrder?.summary?.folio ?: "047"} confirmado"
                                            } else {
                                                "Confirma que el pedido fue entregado"
                                            },
                                            color = Color(0xFFC9CCC2),
                                            fontSize = 11.sp,
                                        )
                                    }
                                }
                                Icon(
                                    Icons.AutoMirrored.Outlined.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Productos Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            "Productos",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = PaletteInk,
                        )
                        Text(
                            "$activeCount activos · $pausedCount pausados",
                            fontSize = 12.sp,
                            color = PaletteMuted,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    // Glass FAB for Add Product
                    val highlightAdd = highlightedTarget == "add_product"
                    Box(
                        modifier =
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0x99FFFEF9))
                                .border(
                                    width = if (highlightAdd) 2.dp else 1.dp,
                                    color = if (highlightAdd) PaletteLime3 else Color(0x33FFFFFF),
                                    shape = CircleShape,
                                ).shadow(8.dp, CircleShape, spotColor = Color(0x1F171816))
                                .clickable {
                                    haptics.impact()
                                    addProductSheetOpen = true
                                },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = "Añadir producto",
                            tint = PaletteInk,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }

            // Products Inventory List
            items(products, key = { it.id }) { product ->
                val highlightSwitch = highlightedTarget == "product_switch"
                ProductRowCard(
                    product = product,
                    highlightSwitch = highlightSwitch,
                    onToggle = { active ->
                        haptics.impact()
                        onToggleProductAvailable(product.id, active)
                        toastMessage = "${product.name} ${if (active) "activo" else "pausado"}"
                    },
                )
            }
        }

        // Assistant Floating Orb
        VaiinillaAssistantOrb(
            role = OperationalRole.CASHIER,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = 24.dp),
            onHighlightTarget = { highlightedTarget = it },
        )

        // Toast feedback
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 16.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = PaletteInk,
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier.size(20.dp).clip(CircleShape).background(PaletteLime3),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("✓", color = PaletteInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        toastMessage.orEmpty(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // Sheet: Add Product
        if (addProductSheetOpen) {
            AddProductSheet(
                onDismiss = { addProductSheetOpen = false },
                onAdd = { name, price ->
                    UnifiedTestModeManager.addProduct(name, price)
                    addProductSheetOpen = false
                    toastMessage = "$name añadido al menú"
                },
            )
        }

        // Sheet: Scanner QR
        if (scanSheetOpen) {
            QrSimulatedScannerSheet(
                orderFolio = "${recentOrder?.summary?.folio ?: "047"}",
                onDismiss = { scanSheetOpen = false },
                onDetect = {
                    val targetId = recentOrder?.summary?.id ?: "order-047"
                    UnifiedTestModeManager.updateOrderState(targetId, OrderState.DELIVERED)
                    if (recentOrder != null) {
                        onDeliver(recentOrder.summary.id, recentOrder.summary.version)
                    }
                    scanSheetOpen = false
                    toastMessage = "Pedido #${recentOrder?.summary?.folio ?: "047"} entregado"
                },
            )
        }
    }
}

@Composable
private fun ProductRowCard(
    product: Product,
    highlightSwitch: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, PaletteCardBorder, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xB8FFFEF9),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                // Product Thumbnail
                Box(
                    modifier =
                        Modifier
                            .size(62.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(PaletteCream2),
                    contentAlignment = Alignment.Center,
                ) {
                    ProductImage(
                        imageUrl = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = PaletteInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "$${product.counterPrice} · ${if (product.available) "Disponible" else "Pausado"}",
                        fontSize = 13.sp,
                        color = PaletteMuted,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            // Styled Switch
            val switchBorderColor = if (highlightSwitch) PaletteLime3 else Color.Transparent
            Box(
                modifier =
                    Modifier
                        .border(2.dp, switchBorderColor, CircleShape)
                        .padding(2.dp),
            ) {
                Switch(
                    checked = product.available,
                    onCheckedChange = onToggle,
                    colors =
                        SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PaletteLime3,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFD5D3CB),
                            uncheckedBorderColor = Color.Transparent,
                            checkedBorderColor = Color.Transparent,
                        ),
                )
            }
        }
    }
}

@Composable
fun KitchenOperationalScreen(
    state: OperationalUiState,
    onBack: () -> Unit,
    onStart: (orderId: String, version: Int) -> Unit,
    onReady: (orderId: String, version: Int) -> Unit,
    onChangeMode: (() -> Unit)? = null,
    onSwitchToRole: ((OperationalRole) -> Unit)? = null,
    restrictedMode: RestrictedMode? = null,
) {
    val haptics = rememberVaiinillaHaptics()
    var highlightedTarget by remember { mutableStateOf<String?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val activeOrder = state.orders.firstOrNull { it.summary.id == "order-047" } ?: state.orders.firstOrNull()
    val upcomingOrders = state.orders.filterNot { it.summary.id == activeOrder?.summary?.id }
    val orderState = activeOrder?.summary?.state ?: OrderState.PAID
    val isPreparing = orderState == OrderState.PREPARING
    val isReady = orderState == OrderState.READY || orderState == OrderState.DELIVERED

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(PaletteCream),
    ) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Topline
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        VaiinillaMark(modifier = Modifier.size(34.dp))
                        Column {
                            Text(
                                "Vaiinilla",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = PaletteInk,
                            )
                            Text(
                                "Cuenta de cocina",
                                fontSize = 12.sp,
                                color = PaletteMuted,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    // Account chip to switch to Caja
                    Box(
                        modifier =
                            Modifier
                                .size(width = 48.dp, height = 40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(PaletteInk)
                                .clickable {
                                    haptics.impact()
                                    if (onSwitchToRole != null) {
                                        onSwitchToRole(OperationalRole.CASHIER)
                                    } else {
                                        onChangeMode?.invoke()
                                    }
                                },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "CK",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                    }
                }
            }

            // Title & Subtitle
            item {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        "COMANDAS EN VIVO",
                        color = PaletteMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.6.sp,
                    )
                    Text(
                        "Una comanda a la vez.",
                        color = PaletteInk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        letterSpacing = (-1.2).sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        "Cambia el estado cuando el trabajo real cambie. El equipo verá la actualización.",
                        color = PaletteMuted,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            // En preparación Section Label
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        "En preparación",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = PaletteInk,
                    )
                    Text(
                        if (isReady) {
                            "Lista para recoger"
                        } else if (isPreparing) {
                            "Preparación iniciada"
                        } else {
                            "Pedido #${activeOrder?.summary?.folio ?: "047"}"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PaletteMuted,
                    )
                }
            }

            // Kitchen Hero Ticket Card
            item {
                val cardBg = if (isReady) Color(0xFFF2F8E7) else PaletteCardBg
                val cardBorder = if (isReady) Color(0x4796C83F) else PaletteCardBorder

                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .shadow(12.dp, RoundedCornerShape(26.dp), spotColor = Color(0x1A171816))
                            .border(1.dp, cardBorder, RoundedCornerShape(26.dp)),
                    shape = RoundedCornerShape(26.dp),
                    color = cardBg,
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column {
                                Text(
                                    "#${activeOrder?.summary?.folio ?: "047"}",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-1.dp).value.sp,
                                    color = PaletteInk,
                                )
                                Text(
                                    "Recibida hace 02:18",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PaletteMuted,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }

                            // Badge
                            val badgeBg =
                                if (isReady) {
                                    PaletteLime3
                                } else if (isPreparing) {
                                    Color(0xFF334429)
                                } else {
                                    PaletteInk
                                }
                            val badgeColor = if (isReady) PaletteInk else Color.White
                            Box(
                                modifier =
                                    Modifier
                                        .clip(CircleShape)
                                        .background(badgeBg)
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    if (isReady) {
                                        "Lista"
                                    } else if (isPreparing) {
                                        "Preparando"
                                    } else {
                                        "Nueva"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor,
                                )
                            }
                        }

                        // Items list
                        Column(
                            modifier = Modifier.padding(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            val items =
                                activeOrder?.items?.takeIf { it.isNotEmpty() }
                                    ?: listOf(
                                        OrderItem(
                                            id = 1,
                                            productId = 1,
                                            productName = "Hot dog",
                                            preparationStation = PreparationStation.KITCHEN,
                                            quantity = 1,
                                            unitDigitalPrice = "75.00",
                                            subtotal = "75.00",
                                            options = emptyList(),
                                        ),
                                        OrderItem(
                                            id = 2,
                                            productId = 2,
                                            productName = "Vaiinilla Latte",
                                            preparationStation = PreparationStation.CASHIER,
                                            quantity = 1,
                                            unitDigitalPrice = "65.00",
                                            subtotal = "65.00",
                                            options = emptyList(),
                                        ),
                                    )

                            items.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(PaletteCream2),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            "${item.quantity}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            color = PaletteInk,
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            item.productName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = PaletteInk,
                                        )
                                        Text(
                                            if (item.productName.contains("Latte", ignoreCase = true)) {
                                                "Caliente · leche entera"
                                            } else {
                                                "Sin cebolla · extra mostaza"
                                            },
                                            color = PaletteMuted,
                                            fontSize = 12.sp,
                                        )
                                    }
                                    Text(
                                        "$${item.unitDigitalPrice}",
                                        color = PaletteInk2,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }

                        // Notes Box
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFEEE8D8))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Text(
                                "Nota: Entregar todo junto.",
                                fontSize = 12.sp,
                                color = Color(0xFF5E5B51),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        // Dual Action Buttons
                        val highlightPrep = highlightedTarget == "prep_button"
                        val highlightReady = highlightedTarget == "ready_button"

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // Button: Preparando
                            Box(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(PaletteCream2)
                                        .border(
                                            width = if (highlightPrep || isPreparing) 2.dp else 0.dp,
                                            color =
                                                if (isPreparing) {
                                                    PaletteInk
                                                } else if (highlightPrep) {
                                                    PaletteLime3
                                                } else {
                                                    Color.Transparent
                                                },
                                            shape = RoundedCornerShape(16.dp),
                                        ).clickable(enabled = !isPreparing && !isReady) {
                                            haptics.impact()
                                            val targetId = activeOrder?.summary?.id ?: "order-047"
                                            UnifiedTestModeManager.updateOrderState(targetId, OrderState.PREPARING)
                                            if (activeOrder != null) {
                                                onStart(activeOrder.summary.id, activeOrder.summary.version)
                                            }
                                            toastMessage = "Comanda en preparación"
                                        },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "Preparando",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = PaletteInk,
                                )
                            }

                            // Button: Ya se preparó
                            Box(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isReady) {
                                                PaletteLime3
                                            } else if (isPreparing) {
                                                PaletteLime
                                            } else {
                                                Color(0x66B7DE63)
                                            },
                                        ).border(
                                            width = if (highlightReady || isReady) 2.dp else 0.dp,
                                            color =
                                                if (isReady) {
                                                    PaletteInk
                                                } else if (highlightReady) {
                                                    PaletteLime3
                                                } else {
                                                    Color.Transparent
                                                },
                                            shape = RoundedCornerShape(16.dp),
                                        ).clickable(enabled = isPreparing && !isReady) {
                                            haptics.success()
                                            val targetId = activeOrder?.summary?.id ?: "order-047"
                                            UnifiedTestModeManager.updateOrderState(targetId, OrderState.READY)
                                            if (activeOrder != null) {
                                                onReady(activeOrder.summary.id, activeOrder.summary.version)
                                            }
                                            toastMessage = "Pedido #047 listo"
                                        },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "Ya se preparó",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isPreparing || isReady) PaletteInk else PaletteMuted,
                                )
                            }
                        }
                    }
                }
            }

            // Siguientes Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        "Siguientes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = PaletteInk,
                    )
                    Text(
                        "2 comandas",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PaletteMuted,
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Queue Row 48
                    QueueTicketRow(
                        folio = "48",
                        title = "1 producto · Barra",
                        subtitle = "Sándwich de pavo",
                        time = "00:42",
                    )
                    // Queue Row 49
                    QueueTicketRow(
                        folio = "49",
                        title = "3 productos · Barra",
                        subtitle = "2 bebidas · 1 comida",
                        time = "00:18",
                    )
                }
            }
        }

        // Assistant Floating Orb
        VaiinillaAssistantOrb(
            role = OperationalRole.KITCHEN,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = 24.dp),
            onHighlightTarget = { highlightedTarget = it },
        )

        // Toast feedback
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 16.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = PaletteInk,
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier.size(20.dp).clip(CircleShape).background(PaletteLime3),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("✓", color = PaletteInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        toastMessage.orEmpty(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueTicketRow(
    folio: String,
    title: String,
    subtitle: String,
    time: String,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, PaletteCardBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xB0FFFEF9),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PaletteInk),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    folio,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = PaletteInk,
                )
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    color = PaletteMuted,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }

            Text(
                time,
                color = PaletteMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// Modal Sheet: Add Product
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddProductSheet(
    onDismiss: () -> Unit,
    onAdd: (name: String, price: Int) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFFBF8EF),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Nuevo producto",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = PaletteInk,
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Cerrar", tint = PaletteInk)
                }
            }

            // Image placeholder
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFF1EDE1))
                        .border(1.dp, Color(0x33171816), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = PaletteMuted)
                    Text(
                        "Foto de producto (JPG/PNG)",
                        fontSize = 12.sp,
                        color = PaletteMuted,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // Name
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Nombre", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PaletteMuted)
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    textStyle = TextStyle(color = PaletteInk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                    cursorBrush = SolidColor(PaletteInk),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0x1F171816), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (name.isEmpty()) Text("Ej. Matcha frío", color = PaletteMuted, fontSize = 15.sp)
                            inner()
                        }
                    },
                )
            }

            // Price
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Precio ($)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PaletteMuted)
                BasicTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it.filter { char -> char.isDigit() } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(color = PaletteInk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                    cursorBrush = SolidColor(PaletteInk),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0x1F171816), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (priceStr.isEmpty()) Text("75", color = PaletteMuted, fontSize = 15.sp)
                            inner()
                        }
                    },
                )
            }

            // Submit Button
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(PaletteLime)
                        .clickable(enabled = name.isNotBlank() && priceStr.isNotBlank()) {
                            val price = priceStr.toIntOrNull() ?: 0
                            onAdd(name.trim(), price)
                        },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Añadir al menú",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = PaletteInk,
                )
            }
        }
    }
}

// Modal Sheet: QR Scanner Simulation
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QrSimulatedScannerSheet(
    orderFolio: String,
    onDismiss: () -> Unit,
    onDetect: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF111410),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Escanear pedido",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Cerrar", tint = Color.White)
                }
            }

            // Camera frame simulation
            Box(
                modifier =
                    Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color(0xFF20261D))
                        .border(2.dp, PaletteLime3, RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        Icons.Outlined.QrCodeScanner,
                        contentDescription = null,
                        tint = PaletteLime,
                        modifier = Modifier.size(64.dp),
                    )
                    Text(
                        "Coloca el QR dentro del marco",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Text(
                "Detectando pedido #$orderFolio...",
                color = Color(0xFFB9BEB4),
                fontSize = 13.sp,
            )

            // Simulate Detection Button
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(PaletteLime)
                        .clickable(onClick = onDetect),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Simular detección (#$orderFolio)",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = PaletteInk,
                )
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
    onSwitchToRole: (OperationalRole) -> Unit = {},
    restrictedMode: com.vaiinilla.app.domain.mode.RestrictedMode? = null,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(PaletteCream),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Modo Mesero", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PaletteInk)
            Text("Entregas de pedidos en espacio", fontSize = 14.sp, color = PaletteMuted)
        }
    }
}
