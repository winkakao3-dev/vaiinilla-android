package com.vaiinilla.app.ui.screens

import android.net.Uri
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.vaiinilla.app.domain.mode.RestrictedMode
import com.vaiinilla.app.domain.model.CatalogProductDraft
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.ui.components.ASSISTANT_ANCHOR_ADD_PRODUCT
import com.vaiinilla.app.ui.components.ASSISTANT_ANCHOR_CASHIER_ORDER_CARD
import com.vaiinilla.app.ui.components.ASSISTANT_ANCHOR_KITCHEN_CARD
import com.vaiinilla.app.ui.components.ASSISTANT_ANCHOR_OPEN_CASH
import com.vaiinilla.app.ui.components.ASSISTANT_ANCHOR_QR_SCAN
import com.vaiinilla.app.ui.components.OperationalAssistantHost
import com.vaiinilla.app.ui.components.OperationalAssistantPalette
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.VaiinillaAssistantButton
import com.vaiinilla.app.ui.components.VaiinillaMark
import com.vaiinilla.app.ui.components.assistantAnchor
import com.vaiinilla.app.ui.components.assistantKitchenReadyAnchor
import com.vaiinilla.app.ui.components.assistantKitchenStartAnchor
import com.vaiinilla.app.ui.components.assistantProductSwitchAnchor
import com.vaiinilla.app.ui.components.assistantQueueOrderAnchor
import com.vaiinilla.app.ui.components.cashierAssistantGuides
import com.vaiinilla.app.ui.components.kitchenAssistantGuides
import com.vaiinilla.app.ui.components.rememberAssistantAnchorRegistry
import com.vaiinilla.app.ui.components.rememberOperationalAssistantController
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.operational.OperationalUiState
import com.vaiinilla.app.ui.theme.AccentInk
import com.vaiinilla.app.ui.theme.Cream
import com.vaiinilla.app.ui.theme.CreamDeep
import com.vaiinilla.app.ui.theme.Ink
import com.vaiinilla.app.ui.theme.Lime
import com.vaiinilla.app.ui.theme.Line
import com.vaiinilla.app.ui.theme.LocalVaiinillaThemeMode
import com.vaiinilla.app.ui.theme.LocalVaiinillaThemeModeChanger
import com.vaiinilla.app.ui.theme.MutedInk
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import kotlinx.coroutines.delay

/**
 * Adaptive operational color tokens supporting Light, Dark, and pure-pitch AMOLED.
 */
data class OperationalColors(
    val background: Color,
    val surfacePaper: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val cardInner: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentLime: Color,
    val accentInk: Color,
    val pillBackground: Color,
    val pillBorder: Color,
    val buttonSecondary: Color,
    val buttonSecondaryInk: Color,
    val highlightBorder: Color,
    val isDark: Boolean,
)

@Composable
fun rememberOperationalColors(): OperationalColors {
    val themeMode = LocalVaiinillaThemeMode.current
    val isSystemDark = isSystemInDarkTheme()
    val effectiveMode = themeMode.resolveEffectiveMode(isSystemDark)

    return when (effectiveMode) {
        VaiinillaThemeMode.Amoled ->
            OperationalColors(
                background = Color(0xFF000000),
                surfacePaper = Color(0xFF070806),
                cardBackground = Color(0xFF0D0E0C),
                cardBorder = Color(0xFF242620),
                cardInner = Color(0xFF141512),
                textPrimary = Color(0xFFFAF7F0),
                textSecondary = Color(0xFFA5A79E),
                textMuted = Color(0xFF75786E),
                accentLime = Color(0xFFB7DE63),
                accentInk = Color(0xFF000000),
                pillBackground = Color(0xFF181A16),
                pillBorder = Color(0xFF2C2F27),
                buttonSecondary = Color(0xFF1E201B),
                buttonSecondaryInk = Color(0xFFFAF7F0),
                highlightBorder = Color(0xFFB7DE63),
                isDark = true,
            )
        VaiinillaThemeMode.Dark ->
            OperationalColors(
                background = Color(0xFF1D1E1C),
                surfacePaper = Color(0xFF242622),
                cardBackground = Color(0xFF282A25),
                cardBorder = Color(0xFF3C3E37),
                cardInner = Color(0xFF32342E),
                textPrimary = Color(0xFFF7F3E7),
                textSecondary = Color(0xFFB2B4AA),
                textMuted = Color(0xFF86887E),
                accentLime = Color(0xFFB7DE63),
                accentInk = Color(0xFF171816),
                pillBackground = Color(0xFF2E312A),
                pillBorder = Color(0xFF45483E),
                buttonSecondary = Color(0xFF363931),
                buttonSecondaryInk = Color(0xFFF7F3E7),
                highlightBorder = Color(0xFFB7DE63),
                isDark = true,
            )
        else ->
            OperationalColors(
                background = Cream,
                surfacePaper = Cream,
                cardBackground = CreamDeep,
                cardBorder = Line,
                cardInner = Color(0xFFFDFBF4),
                textPrimary = Ink,
                textSecondary = MutedInk,
                textMuted = Color(0xFF73756C),
                accentLime = Lime,
                accentInk = AccentInk,
                pillBackground = Color(0xFFE8E3D2),
                pillBorder = Line,
                buttonSecondary = Color(0xFFE4DFCE),
                buttonSecondaryInk = Ink,
                highlightBorder = Color(0xFF96C83F),
                isDark = false,
            )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierOperationalScreen(
    state: OperationalUiState,
    onBack: () -> Unit,
    onOpenCashSession: () -> Unit,
    onCollect: (orderId: String, amount: String, version: Int) -> Unit,
    onScanDeliver: (orderId: String, version: Int) -> Unit = { _, _ -> },
    onSearchWalletClients: (String) -> Unit = {},
    onOpenWalletUserQr: () -> Unit = {},
    onReloadWallet: (userId: String, amount: String) -> Unit = { _, _ -> },
    onChangeMode: (() -> Unit)? = null,
    restrictedMode: RestrictedMode? = null,
    onToggleProductAvailable: (productId: Int, available: Boolean) -> Unit = { _, _ -> },
    onCreateCashierProduct: (CatalogProductDraft, ByteArray?, String?, String?, (String?) -> Unit) -> Unit =
        { _, _, _, _, _ -> },
    onUploadCashierProductImage: (Int, ByteArray, String, String) -> Unit = { _, _, _, _ -> },
    assistantUserKey: String = "cashier",
) {
    val colors = rememberOperationalColors()
    val themeMode = LocalVaiinillaThemeMode.current
    val themeChanger = LocalVaiinillaThemeModeChanger.current
    val haptics = rememberVaiinillaHaptics()
    var addProductSheetOpen by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            tick += 1
        }
    }
    val assistantRegistry = rememberAssistantAnchorRegistry()
    val assistantController = rememberOperationalAssistantController(assistantUserKey, OperationalRole.CASHIER)
    val assistantFocusRequester = remember { FocusRequester() }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2_200)
            toastMessage = null
        }
    }
    var assistantPulse by remember { mutableIntStateOf(0) }

    // Caja solo actúa sobre pedidos por cobrar o listos para entregar; el resto es de cocina.
    var selectedCashierOrderId by remember { mutableStateOf<String?>(null) }
    val actionableCashierOrders =
        state.orders.filter {
            it.summary.state == OrderState.PENDING_PAYMENT || it.summary.state == OrderState.READY
        }
    val recentOrder =
        actionableCashierOrders.firstOrNull { it.summary.id == selectedCashierOrderId }
            ?: actionableCashierOrders.firstOrNull()
            ?: state.orders.firstOrNull()
    val queuedCashierOrders =
        actionableCashierOrders.filterNot { it.summary.id == recentOrder?.summary?.id }
    val products = state.catalog?.products.orEmpty()
    val activeCount = products.count { it.available }
    val pausedCount = products.size - activeCount
    val canCreateProduct =
        state.catalog?.categories?.isNotEmpty() == true &&
            restrictedMode != RestrictedMode.READ_ONLY &&
            !state.acting
    val firstGuidableProductId = products.firstOrNull { it.available }?.id ?: products.firstOrNull()?.id
    val cashierGuides =
        cashierAssistantGuides(
            readyOrderId = recentOrder?.takeIf { it.summary.state == OrderState.READY }?.summary?.id,
            readyOrderFolio = recentOrder?.takeIf { it.summary.state == OrderState.READY }?.summary?.folio,
            firstProductId = firstGuidableProductId,
            cashSessionOpen = state.cashSessionOpen,
            canCreateProduct = canCreateProduct,
        )
    val assistantPalette =
        OperationalAssistantPalette(
            background = colors.background,
            surface = colors.cardBackground,
            surface2 = colors.cardInner,
            ink = colors.textPrimary,
            muted = colors.textSecondary,
            line = colors.cardBorder,
            lime = colors.accentLime,
            limeInk = colors.accentInk,
            isDark = colors.isDark,
        )
    LaunchedEffect(state.cashSessionOpen) {
        if (state.cashSessionOpen == true) {
            assistantController.onStateChanged("caja-abierta")
        }
    }
    LaunchedEffect(addProductSheetOpen) {
        if (addProductSheetOpen) assistantController.onStateChanged("alta-producto-abierta")
    }
    LaunchedEffect(state.orders) {
        state.orders
            .filter { it.summary.state == OrderState.DELIVERED }
            .forEach { order -> assistantController.onStateChanged("pedido-entregado:${order.summary.id}") }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            if (event.changes.any { it.pressed && !it.previousPressed }) {
                                assistantPulse += 1
                            }
                        }
                    }
                },
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
                                color = colors.textPrimary,
                            )
                            Text(
                                "Cuenta de caja",
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (assistantController.activeGuide == null) {
                            VaiinillaAssistantButton(
                                onClick = {
                                    haptics.selection()
                                    assistantController.open()
                                },
                                reactionSignal = assistantPulse,
                                isDarkTheme = colors.isDark,
                                hasPendingFirstSteps = false,
                                modifier = Modifier.focusRequester(assistantFocusRequester),
                                touchSize = 44.dp,
                            )
                        }

                        // Quick Theme Switcher Button
                        IconButton(
                            onClick = {
                                haptics.selection()
                                val next =
                                    when (themeMode) {
                                        VaiinillaThemeMode.Light -> VaiinillaThemeMode.Dark
                                        VaiinillaThemeMode.Dark -> VaiinillaThemeMode.Amoled
                                        VaiinillaThemeMode.Amoled -> VaiinillaThemeMode.Light
                                        VaiinillaThemeMode.System -> VaiinillaThemeMode.Dark
                                    }
                                themeChanger?.invoke(next)
                            },
                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.cardBackground)
                                    .border(1.dp, colors.cardBorder, RoundedCornerShape(14.dp)),
                        ) {
                            Icon(
                                imageVector =
                                    when (themeMode) {
                                        VaiinillaThemeMode.Light -> Icons.Outlined.LightMode
                                        VaiinillaThemeMode.Dark -> Icons.Outlined.DarkMode
                                        VaiinillaThemeMode.Amoled -> Icons.Outlined.Diamond
                                        VaiinillaThemeMode.System -> Icons.Outlined.AutoMode
                                    },
                                contentDescription = "Cambiar tema",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        // Account / Role chip
                        Box(
                            modifier =
                                Modifier
                                    .size(width = 48.dp, height = 40.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.textPrimary)
                                    .clickable {
                                        haptics.impact()
                                        onChangeMode?.invoke() ?: onBack()
                                    },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "DR",
                                color = colors.background,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
            }

            // Title & Subtitle
            item {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        "TURNO DE HOY",
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.6.sp,
                    )
                    Text(
                        "Caja en control.",
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        letterSpacing = (-1.2).sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        "Entrega pedidos y mantén el menú disponible para todos.",
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            // Session Alert if Closed
            if (state.cashSessionOpen == false) {
                item {
                    Surface(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFE45244), RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0x1AE45244),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Sesión de caja cerrada",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = colors.textPrimary,
                                )
                                Text(
                                    "Ábrela para cobrar y recibir nuevos pedidos.",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary,
                                )
                            }
                            Button(
                                onClick = {
                                    haptics.impact()
                                    onOpenCashSession()
                                },
                                enabled = !state.acting && restrictedMode != RestrictedMode.READ_ONLY,
                                modifier = Modifier.assistantAnchor(assistantRegistry, ASSISTANT_ANCHOR_OPEN_CASH),
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = colors.accentLime,
                                        contentColor = colors.accentInk,
                                    ),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("Abrir caja", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Pedido reciente section header
            if (recentOrder == null) {
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
                            color = colors.textPrimary,
                        )
                        val recentStatus = "Al día"
                        Text(
                            text = recentStatus,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary,
                        )
                    }
                }
            }

            // Hero Recent Order Card or Empty State
            item {
                if (recentOrder != null) {
                    val isDelivered = recentOrder.summary.state == OrderState.DELIVERED
                    val isOrderReady = recentOrder.summary.state == OrderState.READY
                    val isPendingPayment = recentOrder.summary.state == OrderState.PENDING_PAYMENT
                    var cashReceivedInput by
                        remember(recentOrder.summary.id) {
                            mutableStateOf(recentOrder.summary.total)
                        }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .assistantAnchor(assistantRegistry, ASSISTANT_ANCHOR_CASHIER_ORDER_CARD),
                            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                            color = TicketPaper,
                            shadowElevation = 10.dp,
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Column {
                                        Text(
                                            "PEDIDO · ${elapsedShort(recentOrder.summary.createdAt, tick).uppercase()}",
                                            color = TicketMuted,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 1.6.sp,
                                        )
                                        Text(
                                            "#${recentOrder.summary.folio}",
                                            fontSize = 46.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = (-1.5).sp,
                                            color = TicketInk,
                                        )
                                    }
                                    StampLabel(
                                        text =
                                            if (isDelivered) {
                                                "ENTREGADO"
                                            } else if (isPendingPayment) {
                                                "POR COBRAR"
                                            } else if (isOrderReady) {
                                                "LISTO"
                                            } else {
                                                "EN ESPERA"
                                            },
                                        live = !isDelivered,
                                    )
                                }

                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    recentOrder.items.forEach { item ->
                                        ReceiptLine(
                                            quantity = item.quantity,
                                            name = item.productName,
                                            detail =
                                                item.options
                                                    .joinToString(" · ") { it.name }
                                                    .ifEmpty { null },
                                            price = "$${item.unitDigitalPrice}",
                                        )
                                    }
                                }

                                TicketPerf()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom,
                                ) {
                                    Text(
                                        "TOTAL",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.4.sp,
                                        color = TicketMuted,
                                    )
                                    Text(
                                        "$${recentOrder.summary.total}",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-1).sp,
                                        color = TicketInk,
                                    )
                                }

                                if (isPendingPayment) {
                                    // Pedido pagado en efectivo: hay que cobrarlo antes de que exista
                                    // cualquier posibilidad de avanzarlo a cocina/entrega.
                                    BasicTextField(
                                        value = cashReceivedInput,
                                        onValueChange = { raw ->
                                            cashReceivedInput =
                                                raw.filter { it.isDigit() || it == '.' }
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        enabled = !state.acting && restrictedMode != RestrictedMode.READ_ONLY,
                                        textStyle =
                                            TextStyle(
                                                color = TicketInk,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            ),
                                        cursorBrush = SolidColor(TicketInk),
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(top = 12.dp)
                                                .height(46.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(TicketInk.copy(alpha = 0.05f))
                                                .border(1.dp, TicketLine, RoundedCornerShape(12.dp)),
                                        decorationBox = { inner ->
                                            Box(
                                                modifier =
                                                    Modifier
                                                        .fillMaxSize()
                                                        .padding(horizontal = 14.dp),
                                                contentAlignment = Alignment.CenterStart,
                                            ) {
                                                if (cashReceivedInput.isEmpty()) {
                                                    Text(
                                                        "Efectivo recibido · $${recentOrder.summary.total}",
                                                        color = TicketMuted,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                    )
                                                }
                                                inner()
                                            }
                                        },
                                    )
                                    Button(
                                        onClick = {
                                            haptics.impact()
                                            onCollect(
                                                recentOrder.summary.id,
                                                cashReceivedInput,
                                                recentOrder.summary.version,
                                            )
                                        },
                                        enabled =
                                            cashReceivedInput.isNotBlank() &&
                                                !state.acting &&
                                                restrictedMode != RestrictedMode.READ_ONLY,
                                        colors =
                                            ButtonDefaults.buttonColors(
                                                containerColor = colors.accentLime,
                                                contentColor = colors.accentInk,
                                                disabledContainerColor = TicketLine,
                                                disabledContentColor = TicketMuted,
                                            ),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(top = 10.dp)
                                                .height(50.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Payments,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Cobrar $${recentOrder.summary.total}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                        )
                                    }
                                } else {
                                    // La entrega para llevar exige el QR del alumno. No existe transición manual sin token.
                                    Button(
                                        onClick = {
                                            haptics.impact()
                                            onScanDeliver(recentOrder.summary.id, recentOrder.summary.version)
                                        },
                                        enabled =
                                            isOrderReady &&
                                                restrictedMode != RestrictedMode.READ_ONLY,
                                        colors =
                                            ButtonDefaults.buttonColors(
                                                containerColor = TicketInk,
                                                contentColor = TicketPaper,
                                                disabledContainerColor = TicketLine,
                                                disabledContentColor = TicketMuted,
                                            ),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(top = 12.dp)
                                                .height(50.dp)
                                                .assistantAnchor(assistantRegistry, ASSISTANT_ANCHOR_QR_SCAN),
                                        contentPadding = PaddingValues(horizontal = 14.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.QrCodeScanner,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            if (isOrderReady) {
                                                "Escanear QR para entregar"
                                            } else {
                                                "Disponible cuando esté LISTO"
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                        )
                                    }
                                }
                            }
                        }
                        TicketZigzag()
                    }
                } else {
                    Surface(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .border(1.dp, colors.cardBorder, RoundedCornerShape(26.dp)),
                        shape = RoundedCornerShape(26.dp),
                        color = colors.cardBackground,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                "No hay pedidos pendientes",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = colors.textPrimary,
                            )
                            Text(
                                text = "Los pedidos que entren por ventanilla o app aparecerán aquí.",
                                fontSize = 13.sp,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            // Pending-in-caja queue: every other order awaiting payment or pickup.
            if (queuedCashierOrders.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            "Pendientes en caja",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = colors.textPrimary,
                        )
                        Text(
                            "${queuedCashierOrders.size} pedidos",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary,
                        )
                    }
                }
                items(queuedCashierOrders, key = { it.summary.id }) { order ->
                    val queueTitle =
                        order.items
                            .joinToString(" · ") { "${it.quantity}x ${it.productName}" }
                            .ifEmpty { "Productos del menú" }
                    QueueTicketRow(
                        folio = "#${order.summary.folio}",
                        title = queueTitle,
                        subtitle =
                            if (order.summary.destination.name == "TAKE_AWAY") {
                                "Para llevar"
                            } else {
                                "Comer aquí"
                            },
                        time =
                            if (order.summary.state == OrderState.PENDING_PAYMENT) {
                                "Por cobrar"
                            } else {
                                "Por entregar"
                            },
                        colors = colors,
                        onClick = {
                            haptics.selection()
                            selectedCashierOrderId = order.summary.id
                            toastMessage = "Pedido #${order.summary.folio} seleccionado"
                        },
                    )
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
                            color = colors.textPrimary,
                        )
                        Text(
                            "$activeCount activos · $pausedCount pausados",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    // Add product
                    Box(
                        modifier =
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(colors.cardBackground)
                                .border(1.dp, colors.cardBorder, CircleShape)
                                .assistantAnchor(assistantRegistry, ASSISTANT_ANCHOR_ADD_PRODUCT)
                                .shadow(8.dp, CircleShape, spotColor = Color(0x1F171816))
                                .clickable(enabled = canCreateProduct) {
                                    haptics.impact()
                                    addProductSheetOpen = true
                                },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = "Agregar producto",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }

            // Products List
            if (products.isNotEmpty()) {
                items(products.chunked(2), key = { it.first().id }) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        pair.forEach { product ->
                            ProductGridCard(
                                product = product,
                                colors = colors,
                                assistantRegistry = assistantRegistry,
                                modifier = Modifier.weight(1f),
                                onToggle = { isAvailable ->
                                    haptics.selection()
                                    onToggleProductAvailable(product.id, isAvailable)
                                    assistantController.onAnchorTapped(assistantProductSwitchAnchor(product.id))
                                },
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            } else {
                item {
                    Surface(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .border(1.dp, colors.cardBorder, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        color = colors.cardBackground,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                "Catálogo vacío",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = colors.textPrimary,
                            )
                            Text(
                                text = "Usa el botón '+' para registrar el primer producto de la cafetería.",
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }

        // Floating Toast Notification
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 30.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.textPrimary,
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier.size(20.dp).clip(CircleShape).background(colors.accentLime),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = colors.accentInk,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                    Text(
                        toastMessage.orEmpty(),
                        color = colors.background,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // Sheet: Add Product
        if (addProductSheetOpen) {
            val categoryId =
                state.catalog
                    ?.categories
                    ?.firstOrNull()
                    ?.id
            if (categoryId != null) {
                AddProductSheet(
                    saving = state.acting,
                    errorMessage = state.errorMessage,
                    onDismiss = {
                        if (!state.acting) addProductSheetOpen = false
                    },
                    onAdd = { name, price, imageBytes, imageFilename, imageMime, station ->
                        val draft =
                            CatalogProductDraft(
                                categoryId = categoryId,
                                preparationStation = station,
                                name = name,
                                description = "",
                                ingredients = "",
                                allergens = "",
                                estimatedTimeMinutes = 5,
                                counterPrice = "$price.00",
                                available = true,
                            )
                        onCreateCashierProduct(
                            draft,
                            imageBytes,
                            imageFilename,
                            imageMime,
                        ) { warning ->
                            addProductSheetOpen = false
                            toastMessage = warning ?: "$name registrado para el menú"
                        }
                    },
                )
            }
        }
        OperationalAssistantHost(
            controller = assistantController,
            registry = assistantRegistry,
            guides = cashierGuides,
            manualTitle = "Manual de Caja",
            palette = assistantPalette,
            buttonFocusRequester = assistantFocusRequester,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private val TicketPaper = Color(0xFFF5ECDA)
private val TicketInk = Color(0xFF1D1C18)
private val TicketMuted = Color(0xFF6B6656)
private val TicketLine = Color(0xFFD8CDB4)
private val StampGreen = Color(0xFF5A7A1E)

private fun elapsedSinceMs(iso: String): Long? =
    runCatching {
        java.time.Duration
            .between(java.time.Instant.parse(iso), java.time.Instant.now())
            .toMillis()
    }.getOrNull()

private fun elapsedShort(
    iso: String,
    tick: Int,
): String {
    tick.hashCode()
    val ms = elapsedSinceMs(iso) ?: return "recién"
    return when {
        ms < 60_000 -> "ahora"
        ms < 3_600_000 -> "hace ${ms / 60_000} min"
        ms < 86_400_000 -> "hace ${ms / 3_600_000} h"
        else -> "hace ${ms / 86_400_000} d"
    }
}

private fun formatTimer(ms: Long?): String = ms?.let { "%d:%02d".format(it / 60_000, (it % 60_000) / 1_000) } ?: "--:--"

@Composable
private fun TicketZigzag() {
    Canvas(modifier = Modifier.fillMaxWidth().height(11.dp)) {
        val tooth = 14.dp.toPx()
        val path = Path()
        path.moveTo(0f, 0f)
        path.lineTo(size.width, 0f)
        var x = size.width
        while (x >= tooth) {
            path.lineTo(x - tooth / 2f, size.height)
            path.lineTo(x - tooth, 0f)
            x -= tooth
        }
        path.lineTo(0f, 0f)
        path.close()
        withTransform({ translate(0f, 3.5f) }) {
            drawPath(path, Color(0x33000000))
        }
        drawPath(path, TicketPaper)
    }
}

@Composable
private fun TicketPerf() {
    Canvas(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).height(2.dp)) {
        drawLine(
            color = TicketLine,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 8f)),
        )
    }
}

@Composable
private fun StampLabel(
    text: String,
    live: Boolean,
) {
    Box(
        modifier =
            Modifier
                .rotate(4f)
                .clip(RoundedCornerShape(8.dp))
                .border(2.dp, if (live) StampGreen else TicketMuted, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.3.sp,
            color = if (live) StampGreen else TicketMuted,
        )
    }
}

@Composable
private fun ReceiptLine(
    quantity: Int,
    name: String,
    detail: String?,
    price: String,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Text("$quantity", fontWeight = FontWeight.Black, fontSize = 13.5.sp, color = TicketInk)
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = TicketInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Canvas(
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp).height(2.dp),
                ) {
                    drawLine(
                        color = TicketLine,
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = 1.6.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.5f, 7f)),
                    )
                }
                Text(price, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TicketInk)
            }
            if (detail != null) {
                Text(detail, color = TicketMuted, fontSize = 11.5.sp, modifier = Modifier.padding(top = 1.dp))
            }
        }
    }
}

@Composable
private fun TicketDivider(colors: OperationalColors) {
    Box(modifier = Modifier.fillMaxWidth().height(18.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxWidth().height(1.5.dp)) {
            drawLine(
                color = colors.cardBorder,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 8f)),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier =
                    Modifier
                        .offset(x = (-26).dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(colors.background),
            )
            Box(
                modifier =
                    Modifier
                        .offset(x = 26.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(colors.background),
            )
        }
    }
}

@Composable
private fun ProductGridCard(
    product: Product,
    colors: OperationalColors,
    assistantRegistry: com.vaiinilla.app.ui.components.AssistantAnchorRegistry? = null,
    modifier: Modifier = Modifier,
    onToggle: (Boolean) -> Unit,
) {
    Surface(
        modifier = modifier.border(1.dp, colors.cardBorder, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = colors.cardBackground,
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(62.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.cardInner),
                    contentAlignment = Alignment.Center,
                ) {
                    ProductImage(
                        imageUrl = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(
                                if (product.available) colors.accentLime else colors.cardBackground,
                            ).then(
                                if (assistantRegistry != null) {
                                    Modifier.assistantAnchor(
                                        assistantRegistry,
                                        assistantProductSwitchAnchor(product.id),
                                    )
                                } else {
                                    Modifier
                                },
                            ).clickable { onToggle(!product.available) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector =
                            if (product.available) Icons.Rounded.Check else Icons.Outlined.Close,
                        contentDescription =
                            if (product.available) "Disponible" else "Pausado",
                        tint = if (product.available) colors.accentInk else colors.textMuted,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Text(
                product.name,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "$${product.digitalPrice}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (colors.isDark) colors.accentLime else colors.highlightBorder,
                )
                Box(
                    modifier =
                        Modifier
                            .clip(CircleShape)
                            .background(colors.cardInner)
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                ) {
                    Text(
                        if (product.preparationStation == PreparationStation.KITCHEN) {
                            "COCINA"
                        } else {
                            "BARRA"
                        },
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp,
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitchenOperationalScreen(
    state: OperationalUiState,
    onBack: () -> Unit,
    onStart: (orderId: String, version: Int) -> Unit,
    onReady: (orderId: String, version: Int) -> Unit,
    onChangeMode: (() -> Unit)? = null,
    restrictedMode: RestrictedMode? = null,
    assistantUserKey: String = "kitchen",
) {
    val colors = rememberOperationalColors()
    val themeMode = LocalVaiinillaThemeMode.current
    val themeChanger = LocalVaiinillaThemeModeChanger.current
    val haptics = rememberVaiinillaHaptics()
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var assistantPulse by remember { mutableIntStateOf(0) }
    val assistantRegistry = rememberAssistantAnchorRegistry()
    val assistantController = rememberOperationalAssistantController(assistantUserKey, OperationalRole.KITCHEN)
    val assistantFocusRequester = remember { FocusRequester() }

    var selectedOrderId by remember { mutableStateOf<String?>(null) }
    val activeOrder =
        state.orders.firstOrNull { it.summary.id == selectedOrderId }
            ?: state.orders.firstOrNull()
    val upcomingOrders = state.orders.filterNot { it.summary.id == activeOrder?.summary?.id }
    val orderState = activeOrder?.summary?.state ?: OrderState.PAID
    val isPreparing = orderState == OrderState.PREPARING
    val isReady = orderState == OrderState.READY || orderState == OrderState.DELIVERED
    val nextGuidableOrder = upcomingOrders.firstOrNull()
    val kitchenGuides =
        kitchenAssistantGuides(
            orderId = activeOrder?.summary?.id,
            orderFolio = activeOrder?.summary?.folio,
            isPreparing = isPreparing,
            isReady = isReady,
            nextOrderId = nextGuidableOrder?.summary?.id,
            nextOrderFolio = nextGuidableOrder?.summary?.folio,
        )
    val kitchenAssistantPalette =
        OperationalAssistantPalette(
            background = colors.background,
            surface = colors.cardBackground,
            surface2 = colors.cardInner,
            ink = colors.textPrimary,
            muted = colors.textSecondary,
            line = colors.cardBorder,
            lime = colors.accentLime,
            limeInk = colors.accentInk,
            isDark = colors.isDark,
        )
    LaunchedEffect(state.orders) {
        state.orders.forEach { order ->
            when (order.summary.state) {
                OrderState.PREPARING -> {
                    assistantController.onStateChanged("comanda-preparando:${order.summary.id}")
                }
                OrderState.READY, OrderState.DELIVERED -> {
                    assistantController.onStateChanged("comanda-lista:${order.summary.id}")
                }
                else -> Unit
            }
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            if (event.changes.any { it.pressed && !it.previousPressed }) {
                                assistantPulse += 1
                            }
                        }
                    }
                },
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
                                color = colors.textPrimary,
                            )
                            Text(
                                "Cuenta de cocina",
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (assistantController.activeGuide == null) {
                            VaiinillaAssistantButton(
                                onClick = {
                                    haptics.selection()
                                    assistantController.open()
                                },
                                reactionSignal = assistantPulse,
                                isDarkTheme = colors.isDark,
                                hasPendingFirstSteps = false,
                                modifier = Modifier.focusRequester(assistantFocusRequester),
                                touchSize = 56.dp,
                            )
                        }

                        // Quick Theme Switcher Button
                        IconButton(
                            onClick = {
                                haptics.selection()
                                val next =
                                    when (themeMode) {
                                        VaiinillaThemeMode.Light -> VaiinillaThemeMode.Dark
                                        VaiinillaThemeMode.Dark -> VaiinillaThemeMode.Amoled
                                        VaiinillaThemeMode.Amoled -> VaiinillaThemeMode.Light
                                        VaiinillaThemeMode.System -> VaiinillaThemeMode.Dark
                                    }
                                themeChanger?.invoke(next)
                            },
                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.cardBackground)
                                    .border(1.dp, colors.cardBorder, RoundedCornerShape(14.dp)),
                        ) {
                            Icon(
                                imageVector =
                                    when (themeMode) {
                                        VaiinillaThemeMode.Light -> Icons.Outlined.LightMode
                                        VaiinillaThemeMode.Dark -> Icons.Outlined.DarkMode
                                        VaiinillaThemeMode.Amoled -> Icons.Outlined.Diamond
                                        VaiinillaThemeMode.System -> Icons.Outlined.AutoMode
                                    },
                                contentDescription = "Cambiar tema",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        // Account / Role chip
                        Box(
                            modifier =
                                Modifier
                                    .size(width = 48.dp, height = 40.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.textPrimary)
                                    .clickable {
                                        haptics.impact()
                                        onChangeMode?.invoke() ?: onBack()
                                    },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "CK",
                                color = colors.background,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
            }

            // Title & Subtitle
            item {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        "COMANDAS EN VIVO",
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.6.sp,
                    )
                    Text(
                        "Una comanda a la vez.",
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        letterSpacing = (-1.2).sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        "Cambia el estado cuando el trabajo real cambie. El equipo verá la actualización.",
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            // En preparación Section Label
            if (activeOrder == null) {
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
                            color = colors.textPrimary,
                        )
                        val prepStatus = "Al día"
                        Text(
                            text = prepStatus,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary,
                        )
                    }
                }
            }

            // Kitchen Hero Ticket Card or Empty State
            item {
                if (activeOrder != null) {
                    val destLabel =
                        if (activeOrder.summary.destination.name == "TAKE_AWAY") {
                            "PARA LLEVAR"
                        } else {
                            "COMER AQUÍ"
                        }
                    val stateLabel =
                        if (isReady) {
                            "LISTA"
                        } else if (isPreparing) {
                            "EN PREPARACIÓN"
                        } else {
                            "NUEVA"
                        }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .assistantAnchor(assistantRegistry, ASSISTANT_ANCHOR_KITCHEN_CARD),
                            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                            color = TicketPaper,
                            shadowElevation = 10.dp,
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Column {
                                        Text(
                                            "$stateLabel · $destLabel",
                                            color = TicketMuted,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 1.6.sp,
                                        )
                                        Text(
                                            "#${activeOrder.summary.folio}",
                                            fontSize = 48.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = (-1.5).sp,
                                            color = TicketInk,
                                        )
                                        Row(
                                            verticalAlignment = Alignment.Bottom,
                                            modifier = Modifier.padding(top = 2.dp),
                                        ) {
                                            Text(
                                                formatTimer(elapsedSinceMs(activeOrder.summary.createdAt)),
                                                fontSize = 19.sp,
                                                fontWeight = FontWeight.Black,
                                                color = TicketInk,
                                            )
                                            Text(
                                                "  en cocina",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TicketMuted,
                                            )
                                        }
                                    }
                                    StampLabel(
                                        text =
                                            if (isReady) {
                                                "Lista"
                                            } else if (isPreparing) {
                                                "Preparando"
                                            } else {
                                                "Nueva"
                                            },
                                        live = !isReady,
                                    )
                                }

                                TicketPerf()

                                Column {
                                    activeOrder.items.forEach { item ->
                                        ReceiptLine(
                                            quantity = item.quantity,
                                            name = item.productName,
                                            detail =
                                                item.options
                                                    .joinToString(" · ") { it.name }
                                                    .ifEmpty { null },
                                            price = "$${item.unitDigitalPrice}",
                                        )
                                    }
                                }

                                // Una sola acción: la que aplica según el estado real.
                                Button(
                                    onClick = {
                                        if (isPreparing) {
                                            haptics.success()
                                            onReady(activeOrder.summary.id, activeOrder.summary.version)
                                            toastMessage = "Comanda #${activeOrder.summary.folio} lista"
                                        } else {
                                            haptics.impact()
                                            onStart(activeOrder.summary.id, activeOrder.summary.version)
                                            toastMessage = "Comanda #${activeOrder.summary.folio} en preparación"
                                        }
                                    },
                                    enabled = !isReady && restrictedMode != RestrictedMode.READ_ONLY,
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = colors.accentLime,
                                            contentColor = colors.accentInk,
                                            disabledContainerColor = TicketLine,
                                            disabledContentColor = TicketMuted,
                                        ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(top = 12.dp)
                                            .height(50.dp)
                                            .assistantAnchor(
                                                assistantRegistry,
                                                if (isPreparing) {
                                                    assistantKitchenReadyAnchor(activeOrder.summary.id)
                                                } else {
                                                    assistantKitchenStartAnchor(activeOrder.summary.id)
                                                },
                                            ),
                                    contentPadding = PaddingValues(horizontal = 14.dp),
                                ) {
                                    Text(
                                        text =
                                            if (isReady) {
                                                "Lista para recoger"
                                            } else if (isPreparing) {
                                                "Marcar lista"
                                            } else {
                                                "Empezar a preparar"
                                            },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                    )
                                }

                                upcomingOrders.firstOrNull()?.let { next ->
                                    val nextDest =
                                        if (next.summary.destination.name == "TAKE_AWAY") {
                                            "PARA LLEVAR"
                                        } else {
                                            "COMER AQUÍ"
                                        }
                                    Text(
                                        "SIGUIENTE: #${next.summary.folio} · $nextDest",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        color = TicketMuted,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                                    )
                                }
                            }
                        }
                        TicketZigzag()
                    }
                } else {
                    Surface(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .border(1.dp, colors.cardBorder, RoundedCornerShape(26.dp)),
                        shape = RoundedCornerShape(26.dp),
                        color = colors.cardBackground,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                "Cocina al día",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = colors.textPrimary,
                            )
                            Text(
                                text = "No hay comandas pendientes en este momento.",
                                fontSize = 13.sp,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            // Siguientes Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        "En fila",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = colors.textPrimary,
                    )
                    Text(
                        "${upcomingOrders.size} comandas",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary,
                    )
                }
            }

            // Upcoming Orders List
            items(upcomingOrders, key = { it.summary.id }) { order ->
                val orderDest =
                    if (order.summary.destination.name == "TAKE_AWAY") {
                        "Para llevar"
                    } else {
                        "Comer aquí"
                    }
                val orderSummaryTitle =
                    order.items
                        .joinToString(" · ") { "${it.quantity}x ${it.productName}" }
                        .ifEmpty { "Productos del menú" }
                QueueTicketRow(
                    folio = "#${order.summary.folio}",
                    title = orderSummaryTitle,
                    subtitle = orderDest,
                    time = "En espera",
                    colors = colors,
                    modifier = Modifier.assistantAnchor(assistantRegistry, assistantQueueOrderAnchor(order.summary.id)),
                    onClick = {
                        haptics.selection()
                        selectedOrderId = order.summary.id
                        toastMessage = "Comanda #${order.summary.folio} seleccionada"
                        assistantController.onAnchorTapped(assistantQueueOrderAnchor(order.summary.id))
                    },
                )
            }
        }

        // Floating Toast Notification
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 30.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.textPrimary,
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier.size(20.dp).clip(CircleShape).background(colors.accentLime),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = colors.accentInk,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                    Text(
                        toastMessage.orEmpty(),
                        color = colors.background,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        OperationalAssistantHost(
            controller = assistantController,
            registry = assistantRegistry,
            guides = kitchenGuides,
            manualTitle = "Manual de Cocina",
            palette = kitchenAssistantPalette,
            buttonFocusRequester = assistantFocusRequester,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun QueueTicketRow(
    folio: String,
    title: String,
    subtitle: String,
    time: String,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .border(1.dp, colors.cardBorder, RoundedCornerShape(20.dp))
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = colors.cardBackground,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .defaultMinSize(minWidth = 56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.textPrimary)
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    folio,
                    color = colors.background,
                    fontWeight = FontWeight.Black,
                    fontSize = 19.sp,
                    letterSpacing = 0.5.sp,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }

            Box(
                modifier =
                    Modifier
                        .clip(CircleShape)
                        .background(colors.pillBackground)
                        .border(1.dp, colors.pillBorder, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    time,
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// Modal Sheet: Add Product with Image Picker + Presets
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddProductSheet(
    saving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onAdd: (
        name: String,
        price: Int,
        imageBytes: ByteArray?,
        imageFilename: String?,
        imageMime: String?,
        station: PreparationStation,
    ) -> Unit,
) {
    val colors = rememberOperationalColors()
    val haptics = rememberVaiinillaHaptics()
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<String?>(null) }
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var selectedImageFilename by remember { mutableStateOf<String?>(null) }
    var selectedImageMime by remember { mutableStateOf<String?>(null) }
    var imageError by remember { mutableStateOf<String?>(null) }
    var submitted by remember { mutableStateOf(false) }
    var selectedStation by remember { mutableStateOf(PreparationStation.CASHIER) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val photoPicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
            imageError = null
            selectedImageUri = null
            selectedImageBytes = null
            selectedImageFilename = null
            selectedImageMime = null

            val prepared = runCatching { prepareProductImage(context, uri) }.getOrNull()
            if (prepared == null) {
                imageError = "No se pudo preparar la foto. Prueba otra imagen de hasta 16 MB."
                return@rememberLauncherForActivityResult
            }

            selectedImageUri = uri.toString()
            selectedImageBytes = prepared.bytes
            selectedImageFilename = prepared.filename
            selectedImageMime = prepared.mimeType
        }

    ModalBottomSheet(
        onDismissRequest = { if (!saving) onDismiss() },
        sheetState = sheetState,
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 36.dp)
                    .verticalScroll(rememberScrollState()),
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
                    color = colors.textPrimary,
                )
                Box(
                    modifier =
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colors.cardBackground)
                            .border(1.dp, colors.cardBorder, CircleShape)
                            .clickable(enabled = !saving) { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Cerrar",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            // Photo drop-zone
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(112.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.cardBackground)
                        .clickable(enabled = !saving) { photoPicker.launch("image/*") },
                contentAlignment = Alignment.Center,
            ) {
                val previewUri = selectedImageUri
                if (previewUri != null) {
                    AndroidView(
                        factory = { imageContext ->
                            ImageView(imageContext).apply {
                                scaleType = ImageView.ScaleType.CENTER_CROP
                            }
                        },
                        update = { imageView ->
                            imageView.setImageURI(previewUri.toUri())
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color(0x99000000))
                                .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Toca para cambiar la foto", fontSize = 11.sp, color = Color.White)
                    }
                } else {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRoundRect(
                            color = colors.cardBorder,
                            style =
                                Stroke(
                                    width = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 7f)),
                                ),
                            cornerRadius = CornerRadius(20.dp.toPx()),
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.PhotoCamera,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(26.dp),
                        )
                        Text(
                            "Foto del producto",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(top = 5.dp),
                        )
                        Text(
                            "Toca para elegir de galería · opcional",
                            fontSize = 10.5.sp,
                            color = colors.textMuted,
                            modifier = Modifier.padding(top = 1.dp),
                        )
                    }
                }
            }

            if (imageError != null) {
                Text(
                    text = imageError.orEmpty(),
                    fontSize = 12.sp,
                    color = Color(0xFFD75A4A),
                )
            }

            // Station segmented control
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(colors.cardInner)
                        .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                listOf(
                    PreparationStation.CASHIER to "Barra / Bebidas",
                    PreparationStation.KITCHEN to "Cocina caliente",
                ).forEach { (station, label) ->
                    val selected = selectedStation == station
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(if (selected) colors.accentLime else Color.Transparent)
                                .clickable(enabled = !saving) {
                                    haptics.selection()
                                    selectedStation = station
                                }.padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) colors.accentInk else colors.textSecondary,
                        )
                    }
                }
            }

            // Name input
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                textStyle =
                    TextStyle(
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                cursorBrush = SolidColor(colors.textPrimary),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.cardInner)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                decorationBox = { inner ->
                    Column {
                        Text(
                            "NOMBRE",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = colors.textMuted,
                        )
                        Box {
                            if (name.isEmpty()) {
                                Text(
                                    "Ej. Matcha frío con avena",
                                    color = colors.textMuted,
                                    fontSize = 15.sp,
                                )
                            }
                            inner()
                        }
                    }
                },
            )

            // Price input
            BasicTextField(
                value = priceStr,
                onValueChange = { priceStr = it.filter { char -> char.isDigit() } },
                singleLine = true,
                textStyle =
                    TextStyle(
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                cursorBrush = SolidColor(colors.textPrimary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.cardInner)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                decorationBox = { inner ->
                    Column {
                        Text(
                            "PRECIO ($)",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = colors.textMuted,
                        )
                        Box {
                            if (priceStr.isEmpty()) {
                                Text("Ej. 65", color = colors.textMuted, fontSize = 15.sp)
                            }
                            inner()
                        }
                    }
                },
            )

            if (submitted && !errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    fontSize = 12.sp,
                    color = Color(0xFFD75A4A),
                )
            }

            Spacer(Modifier.height(4.dp))

            // Save button
            val isValid = name.isNotBlank() && priceStr.isNotBlank() && (priceStr.toIntOrNull() ?: 0) > 0
            Button(
                onClick = {
                    haptics.impact()
                    submitted = true
                    val price = priceStr.toIntOrNull() ?: return@Button
                    onAdd(
                        name.trim(),
                        price,
                        selectedImageBytes,
                        selectedImageFilename,
                        selectedImageMime,
                        selectedStation,
                    )
                },
                enabled = isValid && !saving,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colors.accentLime,
                        contentColor = colors.accentInk,
                        disabledContainerColor = colors.cardBorder,
                        disabledContentColor = colors.textMuted,
                    ),
                shape = CircleShape,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
            ) {
                Text(
                    if (saving) "Guardando…" else "Guardar producto",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 0.2.sp,
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
    restrictedMode: RestrictedMode? = null,
) {
    val colors = rememberOperationalColors()
    Box(
        modifier = Modifier.fillMaxSize().background(colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Modo Mesero", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = colors.textPrimary)
            Text("Entregas de pedidos en espacio", fontSize = 14.sp, color = colors.textSecondary)
        }
    }
}
