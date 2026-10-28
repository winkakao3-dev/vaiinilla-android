package com.vaiinilla.app.ui.screens

import android.net.Uri
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.domain.model.MoneyInput
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderItem
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.model.SpaceCopy
import com.vaiinilla.app.domain.model.TipChoice
import com.vaiinilla.app.domain.model.TipPercents
import com.vaiinilla.app.domain.model.activeItems
import com.vaiinilla.app.domain.model.amountFor
import com.vaiinilla.app.domain.model.asTipWire
import com.vaiinilla.app.domain.model.cashAmountDue
import com.vaiinilla.app.domain.model.needsCashCollection
import com.vaiinilla.app.domain.repository.StoreImageKind
import com.vaiinilla.app.domain.repository.TableSpace
import com.vaiinilla.app.ui.components.ASSISTANT_ANCHOR_ADD_PRODUCT
import com.vaiinilla.app.ui.components.ASSISTANT_ANCHOR_CASHIER_ORDER_CARD
import com.vaiinilla.app.ui.components.ASSISTANT_ANCHOR_KITCHEN_CARD
import com.vaiinilla.app.ui.components.ASSISTANT_ANCHOR_OPEN_CASH
import com.vaiinilla.app.ui.components.ASSISTANT_ANCHOR_QR_SCAN
import com.vaiinilla.app.ui.components.EditorialAccentButton
import com.vaiinilla.app.ui.components.OperationalAssistantHost
import com.vaiinilla.app.ui.components.OperationalAssistantPalette
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.SlidingSegments
import com.vaiinilla.app.ui.components.ToastPill
import com.vaiinilla.app.ui.components.VaiinillaAssistantButton
import com.vaiinilla.app.ui.components.VaiinillaMark
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.components.assistantAnchor
import com.vaiinilla.app.ui.components.assistantKitchenReadyAnchor
import com.vaiinilla.app.ui.components.assistantKitchenStartAnchor
import com.vaiinilla.app.ui.components.assistantProductSwitchAnchor
import com.vaiinilla.app.ui.components.assistantQueueOrderAnchor
import com.vaiinilla.app.ui.components.cashierAssistantGuides
import com.vaiinilla.app.ui.components.kitchenAssistantGuides
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberAssistantAnchorRegistry
import com.vaiinilla.app.ui.components.rememberOperationalAssistantController
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.counter.TicketAllergyBand
import com.vaiinilla.app.ui.operational.OperationalUiState
import com.vaiinilla.app.ui.operational.QueuePresentation
import com.vaiinilla.app.ui.operational.operationalLink
import com.vaiinilla.app.ui.operational.queuePresentation
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
import java.math.BigDecimal

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

/** Cambio de pedido protagonista: el nuevo entra con fade + escala suave; misma orden = sin animación. */
private fun heroSwapTransition(reduced: Boolean): ContentTransform =
    if (reduced) {
        fadeIn(snap()) togetherWith fadeOut(snap())
    } else {
        (
            fadeIn(spring(stiffness = 300f)) +
                scaleIn(spring(dampingRatio = 0.85f, stiffness = 400f), initialScale = 0.96f)
        ) togetherWith fadeOut(spring(stiffness = 600f))
    }

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
    /** Abre la caja con el efectivo que el cajero contó en el cajón. */
    onOpenCashSession: (initialAmount: String) -> Unit,
    onRequestCloseCash: () -> Unit = {},
    onDismissCloseCash: () -> Unit = {},
    onCloseCash: (String) -> Unit = {},
    onCollect: (orderId: String, amount: String, version: Int, tip: String?) -> Unit,
    onScanDeliver: (orderId: String, version: Int) -> Unit = { _, _ -> },
    /** Entregar sin escanear: el backend dispensa el QR de ese pedido (`entrega_sin_qr`). */
    onDeliver: (orderId: String, version: Int) -> Unit = { _, _ -> },
    /** Rechazar el pedido entero con motivo (B09); el cliente ve el motivo. */
    onRejectOrder: (orderId: String, version: Int, reason: String) -> Unit = { _, _, _ -> },
    /** Quitar un artículo con motivo (G07); el último no se quita, se rechaza el pedido. */
    onRejectItem: (orderId: String, itemId: Int, version: Int, reason: String) -> Unit = { _, _, _, _ -> },
    onActionNoticed: () -> Unit = {},
    onSearchWalletClients: (String) -> Unit = {},
    onOpenWalletUserQr: () -> Unit = {},
    onReloadWallet: (userId: String, amount: String) -> Unit = { _, _ -> },
    /** La recarga que quedó sin confirmar se vuelve a intentar con su misma llave... */
    onConfirmPendingReload: () -> Unit = {},
    /** ...o se descarta si el cajero sabe que no se abonó. */
    onDiscardPendingReload: () -> Unit = {},
    onCashChangeNoticed: () -> Unit = {},
    onChangeMode: (() -> Unit)? = null,
    onOpenAccounts: (() -> Unit)? = null,
    restrictedMode: RestrictedMode? = null,
    onToggleProductAvailable: (productId: Int, available: Boolean) -> Unit = { _, _ -> },
    onCreateCashierProduct: (CatalogProductDraft, ByteArray?, String?, String?, (String?) -> Unit) -> Unit =
        { _, _, _, _, _ -> },
    onUploadCashierProductImage: (Int, ByteArray, String, String) -> Unit = { _, _, _, _ -> },
    /** Subir el logo o la foto del negocio. */
    onUploadStoreImage: (StoreImageKind, ByteArray, String, String) -> Unit = { _, _, _, _ -> },
    onRemoveStoreImage: (StoreImageKind) -> Unit = {},
    assistantUserKey: String = "cashier",
    /** Abre el pedido de mostrador; null lo oculta. */
    onNewCounterOrder: (() -> Unit)? = null,
    /** Mesas a las que Caja toma pedido, como el mesero. */
    orderTables: List<TableSpace> = emptyList(),
    /** Estado del tablero de mesas, para que la hoja de Nuevo pedido diga por qué no hay mesas. */
    orderTablesLoading: Boolean = false,
    orderTablesError: String? = null,
    onRefreshOrderTables: () -> Unit = {},
    onNewTableOrder: ((TableSpace) -> Unit)? = null,
    /** Mesas y canchas con cuenta sin cobrar; null mientras no se sabe. */
    pendingSpaces: Int? = null,
    /** Caja de una escuela: la fila Alumnos (padres e hijos); null fuera de una escuela. */
    studentsEntry: (@Composable () -> Unit)? = null,
    /** Devoluciones pendientes (G08, H03); null las oculta. */
    refundsEntry: (@Composable () -> Unit)? = null,
    /** El negocio de esta caja, para que el cajero vea en cuál cobra. */
    placeName: String = "",
    /** Aviso que llega de fuera (por ejemplo, el folio del pedido de mostrador). */
    notice: String? = null,
    /** Si el negocio exige el QR del cliente para entregar; sin él, Caja entrega con un toque. */
    deliveryRequiresQr: Boolean = true,
    onDismissError: () -> Unit = {},
    onRefresh: () -> Unit = {},
) {
    val colors = rememberOperationalColors()
    val reduceMotion = reducedMotion()
    val themeMode = LocalVaiinillaThemeMode.current
    val themeChanger = LocalVaiinillaThemeModeChanger.current
    val haptics = rememberVaiinillaHaptics()
    var addProductSheetOpen by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var imageEditProductId by remember { mutableStateOf<Int?>(null) }
    var tick by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    // La foto de un producto existente se reemplaza aquí: el alta lleva su propia
    // selección de imagen dentro del sheet.
    val productImagePicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            val productId = imageEditProductId
            imageEditProductId = null
            if (uri == null || productId == null) return@rememberLauncherForActivityResult
            val prepared = runCatching { prepareProductImage(context, uri) }.getOrNull()
            if (prepared == null) {
                toastMessage = "No se pudo preparar la foto. Prueba otra imagen de hasta 16 MB."
            } else {
                onUploadCashierProductImage(
                    productId,
                    prepared.bytes,
                    prepared.filename,
                    prepared.mimeType,
                )
            }
        }
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
    LaunchedEffect(notice) { notice?.let { toastMessage = it } }
    LaunchedEffect(state.actionNotice) {
        state.actionNotice?.let {
            toastMessage = it
            onActionNoticed()
        }
    }
    var assistantPulse by remember { mutableIntStateOf(0) }
    var walletSheetOpen by remember { mutableStateOf(false) }
    var choosingOrderPlace by remember { mutableStateOf(false) }

    // Caja solo actúa sobre pedidos por cobrar o listos para entregar; el resto es de cocina.
    var selectedCashierOrderId by remember { mutableStateOf<String?>(null) }
    // Lo que el cajero lleva escrito (efectivo y propina) por pedido. Vive fuera de la lista: el sondeo
    // trae una copia nueva del pedido cada pocos segundos y la tarjeta se rehace, pero el cobro sigue.
    val cashDrafts = remember { mutableStateMapOf<String, CashierCashDraft>() }
    // El diálogo de rechazo apunta al pedido que se tocó, no al que esté arriba cuando se confirme.
    var rejectingOrderId by remember { mutableStateOf<String?>(null) }
    var rejectingItem by remember { mutableStateOf<Pair<String, OrderItem>?>(null) }
    var openCashSheet by remember { mutableStateOf(false) }
    val cashRegisterOpen = state.cashSessionOpen == true
    val canReloadWallet = cashRegisterOpen && restrictedMode != RestrictedMode.READ_ONLY
    val walletBlockedReason =
        when {
            restrictedMode == RestrictedMode.READ_ONLY -> "Esta sesión es de solo lectura."
            !cashRegisterOpen -> "Abre la caja para registrar recargas en efectivo."
            else -> null
        }
    val actionableCashierOrders = cashierQueueOrders(state.orders)
    // Bajo "Por cobrar" solo van pedidos por cobrar o por entregar; nunca uno ya entregado.
    val recentOrder =
        actionableCashierOrders.firstOrNull { it.summary.id == selectedCashierOrderId }
            ?: actionableCashierOrders.firstOrNull()
    val queuedCashierOrders =
        actionableCashierOrders.filterNot { it.summary.id == recentOrder?.summary?.id }
    val actionableIds = actionableCashierOrders.map { it.summary.id }.toSet()
    // Un pedido de mostrador que el cajero eligió cobrar ya llega aquí seleccionado: pasa al frente (G03).
    LaunchedEffect(state.selectedOrderId, actionableIds) {
        state.selectedOrderId?.takeIf { it in actionableIds }?.let { selectedCashierOrderId = it }
    }
    LaunchedEffect(actionableIds) {
        cashDrafts.keys.filterNot { it in actionableIds }.forEach(cashDrafts::remove)
        if (rejectingOrderId !in actionableIds) rejectingOrderId = null
        if (rejectingItem?.first !in actionableIds) rejectingItem = null
        if (selectedCashierOrderId !in actionableIds) selectedCashierOrderId = null
    }
    val ordersToCollect = actionableCashierOrders.count { it.summary.needsCashCollection }
    val cashierQueue = queuePresentation(actionableCashierOrders.isNotEmpty(), state.lastRefreshOk)
    val expiringOnClose =
        actionableCashierOrders.filter {
            it.summary.state == OrderState.PENDING_PAYMENT &&
                it.summary.needsCashCollection
        }
    val products = state.catalog?.products.orEmpty()
    val activeCount = products.count { it.available }
    val pausedCount = products.size - activeCount
    val canCreateProduct =
        state.catalog?.categories?.isNotEmpty() == true &&
            restrictedMode != RestrictedMode.READ_ONLY &&
            !state.catalogBusy
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
            openCashSheet = false
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
            item(key = "cashier-topline") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Con un nombre de negocio largo, el texto se corta y las iniciales siguen visibles (G09).
                    Row(
                        modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
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
                            // Antes de cobrar, el cajero confirma que la sesión está abierta y en qué negocio (G01).
                            Text(
                                cashierSessionCaption(state.cashSessionOpen, placeName),
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
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

                        StaffAvatarMenu(
                            initials = "CJ",
                            colors = colors,
                            themeMode = themeMode,
                            onThemeChange =
                                themeChanger?.let { change ->
                                    { mode ->
                                        haptics.selection()
                                        change(mode)
                                    }
                                },
                            onChangeMode = {
                                haptics.impact()
                                onChangeMode?.invoke() ?: onBack()
                            },
                            onCloseCash =
                                if (state.cashSessionOpen == true && restrictedMode != RestrictedMode.READ_ONLY) {
                                    {
                                        haptics.selection()
                                        onRequestCloseCash()
                                    }
                                } else {
                                    null
                                },
                        )
                    }
                }
            }

            // La hoja de alta y la fila sin cargar muestran su propio error; aquí no se repite.
            val cashierError =
                state.errorMessage.takeUnless {
                    addProductSheetOpen ||
                        walletSheetOpen ||
                        openCashSheet ||
                        cashierQueue == QueuePresentation.Error
                }
            if (cashierError != null) {
                item(key = "cashier-error") {
                    OperationalErrorCard(
                        message = cashierError,
                        colors = colors,
                        onDismiss = onDismissError,
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            item(key = "cashier-actions") {
                CashierQuickActions(
                    colors = colors,
                    enabled = restrictedMode != RestrictedMode.READ_ONLY,
                    onNewOrder =
                        onNewCounterOrder?.let { open ->
                            {
                                haptics.selection()
                                if (onNewTableOrder != null) {
                                    choosingOrderPlace = true
                                } else {
                                    open()
                                }
                            }
                        },
                    onReload = {
                        haptics.selection()
                        walletSheetOpen = true
                    },
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            // Session Alert if Closed
            if (state.cashSessionOpen == false) {
                item(key = "cash-session-closed") {
                    Surface(
                        modifier =
                            Modifier
                                .animateItem()
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
                                    openCashSheet = true
                                },
                                enabled = !state.acting && restrictedMode != RestrictedMode.READ_ONLY,
                                modifier =
                                    Modifier
                                        .heightIn(min = 48.dp)
                                        .assistantAnchor(assistantRegistry, ASSISTANT_ANCHOR_OPEN_CASH),
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

            // Sin leer el estado de la caja no hay "Abrir" ni "Cerrar": que no parezca que todo va bien.
            if (state.cashSessionOpen == null && state.cashSessionCheckFailed) {
                item(key = "cash-session-unknown") {
                    OperationalErrorCard(
                        title = "No se pudo saber si la caja está abierta",
                        message = "Revisa la conexión. Mientras tanto no se puede cobrar.",
                        colors = colors,
                        onRetry = onRefresh,
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            state.cashChangeNotice?.let { receipt ->
                item(key = "cash-change-notice") {
                    CashCollectedCard(
                        message = receipt,
                        colors = colors,
                        onDone = onCashChangeNoticed,
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            if (onOpenAccounts != null) {
                item(key = "spaces-entry") {
                    CashierSpacesEntry(
                        pendingSpaces = pendingSpaces,
                        colors = colors,
                        onClick = {
                            haptics.selection()
                            onOpenAccounts()
                        },
                    )
                }
            }

            if (studentsEntry != null) {
                item(key = "students-entry") {
                    Box(modifier = Modifier.animateItem()) { studentsEntry() }
                }
            }

            if (refundsEntry != null) {
                item(key = "refunds-entry") {
                    Box(modifier = Modifier.animateItem()) { refundsEntry() }
                }
            }

            item(key = "queue-header") {
                CashierQueueHeader(count = ordersToCollect, colors = colors)
            }

            // Con llave fija: si aparece un aviso arriba, la tarjeta no cambia de lugar ni se rehace.
            item(key = "cashier-hero") {
                AnimatedContent(
                    targetState = recentOrder,
                    contentKey = { it?.summary?.id },
                    transitionSpec = { heroSwapTransition(reduceMotion) },
                    label = "cashier-hero",
                ) { heroOrder ->
                    val recentOrder = heroOrder
                    if (recentOrder != null) {
                        val isOrderReady = recentOrder.summary.state == OrderState.READY
                        // Cocina puede arrancar el pedido antes de cobrarlo: el saldo sigue por cobrar en
                        // preparando y en listo, y no se entrega mientras falte.
                        val needsCash = recentOrder.summary.needsCashCollection
                        val amountDue = recentOrder.summary.cashAmountDue
                        val draftOrderId = recentOrder.summary.id
                        val orderTotal = runCatching { BigDecimal(amountDue) }.getOrDefault(BigDecimal.ZERO)
                        // Si lo que se cobra cambió (se quitó un artículo), lo escrito ya no aplica y se vuelve a
                        // sugerir; la propina elegida se conserva.
                        val cashDraft =
                            cashDrafts[draftOrderId]?.takeIf { it.orderTotal == amountDue }
                                ?: cashDrafts[draftOrderId].let { previous ->
                                    val tip = previous?.tip ?: TipChoice.None
                                    CashierCashDraft(
                                        orderTotal = amountDue,
                                        received = Money.format(orderTotal + tip.amountFor(orderTotal)),
                                        tip = tip,
                                    )
                                }
                        val cashReceivedInput = cashDraft.received
                        val cashTipChoice = cashDraft.tip
                        // El progreso es del cobro: otra acción en curso solo deshabilita el botón.
                        var collecting by remember(recentOrder.summary.id) { mutableStateOf(false) }
                        LaunchedEffect(state.acting, state.errorMessage) {
                            if (!state.acting) collecting = false
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
                                        // El sello se mide primero; el folio toma lo que sobra.
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "PEDIDO · ${elapsedShort(
                                                    recentOrder.summary.createdAt,
                                                    tick,
                                                ).uppercase()}${arrivalSuffix(recentOrder)}",
                                                color = TicketMuted,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 1.6.sp,
                                            )
                                            // Con la letra del sistema al doble el folio se partía en dos renglones.
                                            BasicText(
                                                "#${recentOrder.summary.folio}",
                                                style =
                                                    TextStyle(
                                                        fontWeight = FontWeight.Black,
                                                        letterSpacing = (-1.5).sp,
                                                        color = TicketInk,
                                                    ),
                                                maxLines = 1,
                                                autoSize =
                                                    TextAutoSize.StepBased(
                                                        minFontSize = 24.sp,
                                                        maxFontSize = 46.sp,
                                                        stepSize = 2.sp,
                                                    ),
                                            )
                                        }
                                        StampLabel(
                                            text = if (needsCash) "POR COBRAR" else "LISTO",
                                            live = true,
                                        )
                                    }

                                    if (needsCash && recentOrder.summary.state != OrderState.PENDING_PAYMENT) {
                                        Text(
                                            if (isOrderReady) {
                                                "YA ESTÁ LISTO · COBRA ANTES DE ENTREGAR"
                                            } else {
                                                "SE ESTÁ PREPARANDO · SIN COBRAR"
                                            },
                                            color = TicketMuted,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 1.2.sp,
                                            modifier = Modifier.padding(top = 6.dp),
                                        )
                                    }

                                    TicketAllergyBand(recentOrder.user?.allergies.orEmpty())

                                    Column(modifier = Modifier.padding(top = 8.dp)) {
                                        // Lo que Cocina o Caja quitaron ya no cuenta ni en el total ni en el ticket.
                                        // El último artículo no se quita: se rechaza el pedido entero.
                                        val canRemoveItems =
                                            restrictedMode != RestrictedMode.READ_ONLY &&
                                                recentOrder.activeItems.size > 1 &&
                                                !state.acting
                                        recentOrder.activeItems.forEach { item ->
                                            ReceiptLine(
                                                quantity = item.quantity,
                                                name = item.productName,
                                                detail =
                                                    item.options
                                                        .joinToString(" · ") { it.name }
                                                        .ifEmpty { null },
                                                price = moneyLabel(item.subtotal),
                                                onRemove =
                                                    if (canRemoveItems) {
                                                        { rejectingItem = recentOrder.summary.id to item }
                                                    } else {
                                                        null
                                                    },
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
                                            if (needsCash &&
                                                amountDue != recentOrder.summary.total
                                            ) {
                                                "PENDIENTE"
                                            } else {
                                                "TOTAL"
                                            },
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 1.4.sp,
                                            color = TicketMuted,
                                        )
                                        Text(
                                            moneyLabel(if (needsCash) amountDue else recentOrder.summary.total),
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = (-1).sp,
                                            color = TicketInk,
                                        )
                                    }

                                    if (needsCash) {
                                        val tipAmount = cashTipChoice.amountFor(orderTotal)
                                        val tipProblem = tipProblem(cashTipChoice, orderTotal)
                                        val required = orderTotal + tipAmount
                                        val receivedAmount = MoneyInput.parse(cashReceivedInput)
                                        val changeDue = receivedAmount?.minus(required)
                                        TicketTipRow(
                                            choice = cashTipChoice,
                                            tipAmount = tipAmount,
                                            problem = tipProblem,
                                            enabled = !state.acting,
                                            onChange = { next ->
                                                // El efectivo sugerido sigue a la propina; lo que el cajero ya tecleó no se pisa.
                                                val suggestion = Money.format(required)
                                                cashDrafts[draftOrderId] =
                                                    cashDraft.copy(
                                                        received =
                                                            if (cashReceivedInput == suggestion) {
                                                                Money.format(orderTotal + next.amountFor(orderTotal))
                                                            } else {
                                                                cashReceivedInput
                                                            },
                                                        tip = next,
                                                    )
                                            },
                                        )
                                        // Pedido pagado en efectivo: hay que cobrarlo antes de que exista
                                        // cualquier posibilidad de avanzarlo a cocina/entrega.
                                        BasicTextField(
                                            value = cashReceivedInput,
                                            onValueChange = { raw ->
                                                cashDrafts[draftOrderId] =
                                                    cashDraft.copy(received = MoneyInput.sanitize(raw))
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
                                                    .height(48.dp)
                                                    .semantics { contentDescription = "Efectivo recibido" }
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
                                                            "Efectivo recibido · ${moneyLabel(Money.format(required))}",
                                                            color = TicketMuted,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                        )
                                                    }
                                                    inner()
                                                }
                                            },
                                        )
                                        // El cambio se ve antes de confirmar, mientras el cajero cuenta los billetes.
                                        cashChangeHint(changeDue)?.let { hint ->
                                            Text(
                                                hint,
                                                color =
                                                    if (changeDue != null &&
                                                        changeDue.signum() < 0
                                                    ) {
                                                        CashShortInk
                                                    } else {
                                                        TicketInk
                                                    },
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier =
                                                    Modifier
                                                        .padding(top = 8.dp)
                                                        .semantics { liveRegion = LiveRegionMode.Polite },
                                            )
                                        }
                                        if (!cashRegisterOpen) {
                                            Text(
                                                if (state.cashSessionOpen == false) {
                                                    "Abre la caja para cobrar."
                                                } else {
                                                    "Revisando si la caja está abierta…"
                                                },
                                                color = TicketMuted,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(top = 6.dp),
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                haptics.impact()
                                                collecting = true
                                                onCollect(
                                                    recentOrder.summary.id,
                                                    cashReceivedInput,
                                                    recentOrder.summary.version,
                                                    tipAmount.asTipWire(),
                                                )
                                            },
                                            enabled =
                                                cashRegisterOpen &&
                                                    tipProblem == null &&
                                                    receivedAmount != null &&
                                                    receivedAmount >= required &&
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
                                            if (collecting && state.acting) {
                                                CircularProgressIndicator(
                                                    color = TicketMuted,
                                                    strokeWidth = 2.5.dp,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Outlined.Payments,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                if (collecting && state.acting) {
                                                    "Cobrando…"
                                                } else {
                                                    "Cobrar ${moneyLabel(Money.format(required))}"
                                                },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                softWrap = false,
                                            )
                                        }
                                    } else {
                                        // La entrega exige el QR del alumno, salvo que el backend lo dispense para este
                                        // pedido (`entrega_sin_qr`, una cuenta abierta), que el negocio no lo pida, o que
                                        // el pedido lo hiciera esta misma caja y su token ya esté en el dispositivo.
                                        val withoutQr =
                                            recentOrder.summary.deliveryWithoutQr ||
                                                !deliveryRequiresQr ||
                                                recentOrder.pickupToken != null
                                        Button(
                                            onClick = {
                                                haptics.impact()
                                                if (withoutQr) {
                                                    onDeliver(recentOrder.summary.id, recentOrder.summary.version)
                                                } else {
                                                    onScanDeliver(recentOrder.summary.id, recentOrder.summary.version)
                                                }
                                            },
                                            enabled =
                                                isOrderReady &&
                                                    !state.acting &&
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
                                                imageVector =
                                                    if (withoutQr) {
                                                        Icons.Rounded.Check
                                                    } else {
                                                        Icons.Outlined.QrCodeScanner
                                                    },
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                if (withoutQr) "Entregar" else "Escanear QR para entregar",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                softWrap = false,
                                            )
                                        }
                                    }
                                    if (restrictedMode != RestrictedMode.READ_ONLY) {
                                        TextButton(
                                            onClick = { rejectingOrderId = recentOrder.summary.id },
                                            enabled = !state.acting,
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                        ) {
                                            Text(
                                                "Rechazar pedido",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (state.acting) TicketMuted else Color(0xFFE5645A),
                                            )
                                        }
                                    }
                                }
                            }
                            TicketZigzag()
                        }
                    } else {
                        when (cashierQueue) {
                            QueuePresentation.Loading -> CashierQueueLoading(colors = colors)
                            QueuePresentation.Error ->
                                OperationalErrorCard(
                                    title = "No se pudieron cargar los pedidos",
                                    message = state.errorMessage ?: "Revisa la conexión e inténtalo otra vez.",
                                    colors = colors,
                                    onRetry = onRefresh,
                                )
                            else -> CashierQueueEmpty(colors = colors)
                        }
                    }
                }
            }

            // Pending-in-caja queue: every other order awaiting payment or pickup.
            if (queuedCashierOrders.isNotEmpty()) {
                items(queuedCashierOrders, key = { it.summary.id }) { order ->
                    val queueTitle =
                        order.activeItems
                            .joinToString(" · ") { "${it.quantity}x ${it.productName}" }
                            .ifEmpty { "Productos del menú" }
                    QueueTicketRow(
                        modifier = Modifier.animateItem(),
                        folio = "#${order.summary.folio}",
                        title = queueTitle,
                        subtitle =
                            customerPrefix(order) + (
                                if (order.summary.destination.name == "TAKE_AWAY") {
                                    "Para llevar"
                                } else {
                                    SpaceCopy.staffDestination(order.summary.destination, order.summary.space)
                                }
                            ) + arrivalSuffix(order),
                        time = if (order.summary.needsCashCollection) "Por cobrar" else "Por entregar",
                        colors = colors,
                        onClick = {
                            haptics.selection()
                            selectedCashierOrderId = order.summary.id
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
                                .physicalPress(enabled = canCreateProduct, scale = PhysicalPressScale.Small) {
                                    haptics.impact()
                                    addProductSheetOpen = true
                                }.size(48.dp)
                                .clip(CircleShape)
                                .background(colors.cardBackground)
                                .border(1.dp, colors.cardBorder, CircleShape)
                                .assistantAnchor(assistantRegistry, ASSISTANT_ANCHOR_ADD_PRODUCT)
                                .shadow(8.dp, CircleShape, spotColor = Color(0x1F171816)),
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
                        modifier = Modifier.animateItem().fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        pair.forEach { product ->
                            ProductGridCard(
                                product = product,
                                colors = colors,
                                assistantRegistry = assistantRegistry,
                                modifier = Modifier.weight(1f),
                                onEditImage =
                                    if (restrictedMode != RestrictedMode.READ_ONLY && !state.catalogBusy) {
                                        {
                                            imageEditProductId = product.id
                                            productImagePicker.launch("image/*")
                                        }
                                    } else {
                                        null
                                    },
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

            // Logo y foto del negocio: se ponen una vez, así que van al final, después del catálogo.
            state.storeImages?.let { images ->
                item(key = "store-images") {
                    StoreImagesCard(
                        images = images,
                        busy = state.storeImageBusy,
                        notice = state.storeImageNotice,
                        enabled = restrictedMode != RestrictedMode.READ_ONLY,
                        colors = colors,
                        onUpload = onUploadStoreImage,
                        onRemove = onRemoveStoreImage,
                        onPrepareFailed = { toastMessage = it },
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        }

        ToastPill(
            message = toastMessage,
            containerColor = colors.textPrimary,
            contentColor = colors.background,
            badgeColor = colors.accentLime,
            badgeContentColor = colors.accentInk,
        )

        if (choosingOrderPlace) {
            CashierNewOrderSheet(
                tables = orderTables,
                tablesLoading = orderTablesLoading,
                tablesError = orderTablesError,
                colors = colors,
                onDismiss = { choosingOrderPlace = false },
                onTakeAway = {
                    choosingOrderPlace = false
                    onNewCounterOrder?.invoke()
                },
                onTable = { table ->
                    choosingOrderPlace = false
                    onNewTableOrder?.invoke(table)
                },
                onRefreshTables = onRefreshOrderTables,
            )
        }

        if (walletSheetOpen) {
            WalletReloadSheet(
                clients = state.walletClients,
                searching = state.walletSearchLoading,
                receipt = state.walletReloadReceipt,
                canReload = canReloadWallet,
                blockedReason = walletBlockedReason,
                reloading = state.acting,
                errorMessage = state.errorMessage,
                pendingReload = state.pendingWalletReload,
                colors = colors,
                onSearch = onSearchWalletClients,
                onScan = onOpenWalletUserQr,
                onReload = onReloadWallet,
                onConfirmPending = onConfirmPendingReload,
                onDiscardPending = onDiscardPendingReload,
                onDismissError = onDismissError,
                onDismiss = { walletSheetOpen = false },
            )
        }
        if (openCashSheet && state.cashSessionOpen != true) {
            CashOpenSheet(
                opening = state.acting,
                suggestedAmount = state.suggestedCashFloat,
                errorMessage = state.errorMessage,
                colors = colors,
                onDismiss = { if (!state.acting) openCashSheet = false },
                onConfirm = onOpenCashSession,
            )
        }
        state.closeCash?.let { closeState ->
            CashCloseSheet(
                state = closeState,
                ordersToExpire = expiringOnClose.size,
                amountToExpire =
                    expiringOnClose.sumOf {
                        runCatching { BigDecimal(it.summary.cashAmountDue) }.getOrDefault(BigDecimal.ZERO)
                    },
                colors = colors,
                onDismiss = onDismissCloseCash,
                onConfirm = onCloseCash,
                onOpenAccounts = onOpenAccounts,
            )
        }
        // El pedido se resuelve por su id al confirmar: el que esté arriba cuando llegue el siguiente no cuenta.
        val rejectOrderTarget = state.orders.firstOrNull { it.summary.id == rejectingOrderId }
        val rejectItemTarget =
            rejectingItem?.let { (orderId, item) ->
                state.orders.firstOrNull { it.summary.id == orderId }?.let { it to item }
            }
        if (rejectItemTarget != null) {
            val (target, itemToReject) = rejectItemTarget
            KitchenRejectDialog(
                folio = target.summary.folio,
                itemName = itemToReject.productName,
                warning = paidRejectionWarning(target, itemToReject.subtotal),
                colors = colors,
                onDismiss = { rejectingItem = null },
                onConfirm = { reason ->
                    rejectingItem = null
                    haptics.impact()
                    onRejectItem(target.summary.id, itemToReject.id, target.summary.version, reason)
                },
            )
        }
        if (rejectOrderTarget != null) {
            KitchenRejectDialog(
                folio = rejectOrderTarget.summary.folio,
                warning = paidRejectionWarning(rejectOrderTarget, rejectOrderTarget.summary.total),
                colors = colors,
                onDismiss = { rejectingOrderId = null },
                onConfirm = { reason ->
                    rejectingOrderId = null
                    haptics.impact()
                    onRejectOrder(rejectOrderTarget.summary.id, rejectOrderTarget.summary.version, reason)
                },
            )
        }
        if (addProductSheetOpen) {
            val categoryId =
                state.catalog
                    ?.categories
                    ?.firstOrNull()
                    ?.id
            if (categoryId != null) {
                AddProductSheet(
                    saving = state.catalogBusy,
                    errorMessage = state.errorMessage,
                    onDismiss = {
                        if (!state.catalogBusy) addProductSheetOpen = false
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
                                counterPrice = price,
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

/** Propina en el ticket de Caja: píldoras con el trazo del ticket, la elegida en tinta. */
@Composable
private fun TicketTipRow(
    choice: TipChoice,
    tipAmount: BigDecimal,
    problem: String?,
    enabled: Boolean,
    onChange: (TipChoice) -> Unit,
) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(
            if (tipAmount.signum() > 0) "PROPINA ${moneyLabel(Money.format(tipAmount))}" else "PROPINA",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.4.sp,
            color = TicketMuted,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val options =
                listOf<Pair<String, TipChoice>>("Sin" to TipChoice.None) +
                    TipPercents.map { "$it%" to TipChoice.Percent(it) }
            options.forEach { (label, option) ->
                TicketChip(
                    label,
                    selected = choice == option,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                ) { onChange(option) }
            }
            TicketChip(
                "Otro",
                selected = choice is TipChoice.Custom,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            ) {
                onChange(TipChoice.Custom((choice as? TipChoice.Custom)?.amount.orEmpty()))
            }
        }
        val custom = choice as? TipChoice.Custom
        if (custom != null) {
            BasicTextField(
                value = custom.amount,
                onValueChange = { raw -> onChange(TipChoice.Custom(MoneyInput.sanitize(raw))) },
                singleLine = true,
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = TextStyle(color = TicketInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                cursorBrush = SolidColor(TicketInk),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TicketInk.copy(alpha = 0.05f))
                        .border(1.dp, TicketLine, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                decorationBox = { inner ->
                    if (custom.amount.isEmpty()) Text("Monto de la propina", color = TicketMuted, fontSize = 14.sp)
                    inner()
                },
            )
            if (problem != null) {
                Text(
                    problem,
                    color = CashShortInk,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp).semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
}

@Composable
private fun TicketChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(if (selected) TicketInk else Color.Transparent, label = "ticket-chip-bg")
    val fg by animateColorAsState(if (selected) Color.White else TicketInk, label = "ticket-chip-fg")
    Box(
        modifier =
            modifier
                // Es una opción de un grupo: el lector de pantalla dice cuál está elegida.
                .semantics {
                    role = Role.RadioButton
                    this.selected = selected
                }.physicalPress(enabled = enabled, scale = PhysicalPressScale.Small, onClick = onClick)
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bg)
                .border(1.dp, if (selected) TicketInk else TicketLine, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
    }
}

private val TicketInk = Color(0xFF1D1C18)
private val TicketMuted = Color(0xFF6B6656)
private val TicketLine = Color(0xFFD8CDB4)
private val StampGreen = Color(0xFF5A7A1E)

/** " · YA LLEGÓ" cuando el cliente (drive-thru) avisó que llegó por su pedido; si no, vacío. */
internal fun arrivalSuffix(order: OrderDetail): String = if (order.arrivedAt != null) " · YA LLEGÓ" else ""

/** Nombre del cliente de un pedido de mostrador para la fila de Caja o de Cocina; vacío en pedidos de la app. */
private fun customerPrefix(order: OrderDetail): String = staffCustomerName(order)?.let { "$it · " }.orEmpty()

/** El nombre a la vista del personal: el de mostrador o, si el servidor lo manda como cliente del pedido, ese. */
internal fun staffCustomerName(order: OrderDetail): String? =
    order.customerName?.takeIf { it.isNotBlank() } ?: order.user?.name?.takeIf { it.isNotBlank() }

internal fun elapsedSinceMs(iso: String): Long? =
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
            maxLines = 1,
            softWrap = false,
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
    onRemove: (() -> Unit)? = null,
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
            if (onRemove != null) {
                Text(
                    "Quitar",
                    color = Color(0xFFE5645A),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier =
                        Modifier
                            .padding(top = 2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClickLabel = "Quitar $name de la comanda", onClick = onRemove)
                            .padding(vertical = 4.dp),
                )
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
    onEditImage: (() -> Unit)? = null,
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
                if (onEditImage != null) {
                    // El círculo mide 30 dp, pero el toque llega a 48 dp.
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.TopStart)
                                .physicalPress(scale = PhysicalPressScale.Small) { onEditImage() }
                                .size(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(colors.cardBackground)
                                    .border(1.dp, colors.cardBorder, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoCamera,
                                contentDescription = "Cambiar foto de ${product.name}",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
                val switchSpec = if (reducedMotion()) snap<Color>() else spring(stiffness = Spring.StiffnessMedium)
                // Disponible se marca en tinta: el lima de Caja queda para cobrar.
                val switchBackground by animateColorAsState(
                    if (product.available) colors.textPrimary else colors.cardBackground,
                    switchSpec,
                    label = "availability-background",
                )
                val switchTint by animateColorAsState(
                    if (product.available) colors.background else colors.textMuted,
                    switchSpec,
                    label = "availability-tint",
                )
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .physicalPress(scale = PhysicalPressScale.Small) { onToggle(!product.available) }
                            .size(48.dp)
                            .then(
                                if (assistantRegistry != null) {
                                    Modifier.assistantAnchor(
                                        assistantRegistry,
                                        assistantProductSwitchAnchor(product.id),
                                    )
                                } else {
                                    Modifier
                                },
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier.size(30.dp).clip(CircleShape).background(switchBackground),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector =
                                if (product.available) Icons.Rounded.Check else Icons.Outlined.Close,
                            contentDescription =
                                if (product.available) {
                                    "${product.name}: disponible, toca para pausar"
                                } else {
                                    "${product.name}: pausado, toca para activar"
                                },
                            tint = switchTint,
                            modifier = Modifier.size(16.dp),
                        )
                    }
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
                    moneyLabel(product.digitalPrice),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary,
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
    onReject: (orderId: String, version: Int, reason: String) -> Unit = { _, _, _ -> },
    /** Quitar un solo artículo de la comanda (Cocina no puede hacerlo con el último). */
    onRejectItem: (orderId: String, itemId: Int, version: Int, reason: String) -> Unit = { _, _, _, _ -> },
    onChangeMode: (() -> Unit)? = null,
    restrictedMode: RestrictedMode? = null,
    assistantUserKey: String = "kitchen",
    /** Cocina de una escuela: "Para mañana" (padres e hijos); null fuera de una escuela. */
    reservedEntry: (@Composable () -> Unit)? = null,
    onActionNoticed: () -> Unit = {},
    onDismissError: () -> Unit = {},
    onRefresh: () -> Unit = {},
) {
    val colors = rememberOperationalColors()
    val reduceMotion = reducedMotion()
    val themeMode = LocalVaiinillaThemeMode.current
    val themeChanger = LocalVaiinillaThemeModeChanger.current
    val haptics = rememberVaiinillaHaptics()
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var assistantPulse by remember { mutableIntStateOf(0) }
    val assistantRegistry = rememberAssistantAnchorRegistry()
    val assistantController = rememberOperationalAssistantController(assistantUserKey, OperationalRole.KITCHEN)
    val assistantFocusRequester = remember { FocusRequester() }

    var selectedOrderId by remember { mutableStateOf<String?>(null) }
    var rejecting by remember { mutableStateOf(false) }
    var rejectingItem by remember { mutableStateOf<OrderItem?>(null) }
    val queueOrders = kitchenQueue(state.orders)
    val activeOrder = kitchenActiveOrder(queueOrders, selectedOrderId)
    // Tocar el push de una comanda la deja abierta; la selección del ViewModel también cuenta.
    LaunchedEffect(state.selectedOrderId) {
        if (state.selectedOrderId != null && queueOrders.any { it.summary.id == state.selectedOrderId }) {
            selectedOrderId = state.selectedOrderId
        }
    }
    val upcomingOrders = queueOrders.filterNot { it.summary.id == activeOrder?.summary?.id }
    val orderState = activeOrder?.summary?.state ?: OrderState.PAID
    val isPreparing = orderState == OrderState.PREPARING
    val isReady = orderState == OrderState.READY || orderState == OrderState.DELIVERED
    val nextGuidableOrder = upcomingOrders.firstOrNull()
    val kitchenQueue = queuePresentation(queueOrders.isNotEmpty(), state.lastRefreshOk)
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2_200)
            toastMessage = null
        }
    }
    // La confirmación llega cuando el servidor aceptó el cambio, no al tocar el botón.
    LaunchedEffect(state.actionNotice) {
        state.actionNotice?.let {
            haptics.success()
            toastMessage = it
            onActionNoticed()
        }
    }
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

                        StaffAvatarMenu(
                            initials = "CK",
                            colors = colors,
                            themeMode = themeMode,
                            onThemeChange =
                                themeChanger?.let { change ->
                                    { mode ->
                                        haptics.selection()
                                        change(mode)
                                    }
                                },
                            onChangeMode = {
                                haptics.impact()
                                onChangeMode?.invoke() ?: onBack()
                            },
                        )
                    }
                }
            }

            item(key = "kitchen-link") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    WaiterLivePill(link = operationalLink(state.lastRefreshOk), colors = colors)
                }
            }

            // Sin comandas cargadas, la tarjeta de la fila ya muestra el error.
            state.errorMessage?.takeUnless { kitchenQueue == QueuePresentation.Error }?.let { message ->
                item(key = "kitchen-error") {
                    OperationalErrorCard(
                        message = message,
                        colors = colors,
                        onDismiss = onDismissError,
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            if (reservedEntry != null) {
                item(key = "kitchen-reserved") {
                    Box(modifier = Modifier.animateItem()) { reservedEntry() }
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
                        val prepStatus =
                            when (kitchenQueue) {
                                QueuePresentation.Loading -> "Cargando…"
                                QueuePresentation.Error -> "Sin actualizar"
                                else -> "Al día"
                            }
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
                AnimatedContent(
                    targetState = activeOrder,
                    contentKey = { it?.summary?.id },
                    transitionSpec = { heroSwapTransition(reduceMotion) },
                    label = "kitchen-hero",
                ) { heroOrder ->
                    val activeOrder = heroOrder
                    if (activeOrder != null) {
                        val destLabel =
                            SpaceCopy
                                .staffDestination(activeOrder.summary.destination, activeOrder.summary.space)
                                .uppercase()
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
                                                "$destLabel${arrivalSuffix(activeOrder)}",
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
                                            staffCustomerName(activeOrder)?.let { name ->
                                                Text(
                                                    name,
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TicketInk,
                                                )
                                            }
                                            KitchenAgeText(
                                                createdAt = kitchenAgeStart(activeOrder.summary),
                                                ink = TicketInk,
                                                muted = TicketMuted,
                                                modifier = Modifier.padding(top = 2.dp),
                                            )
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

                                    TicketAllergyBand(activeOrder.user?.allergies.orEmpty())

                                    TicketPerf()

                                    Column {
                                        // Lo rechazado no se prepara: no aparece en la comanda.
                                        // Cocina quita artículos de un pedido cobrado o en preparación; el último ya
                                        // no se quita, se rechaza el pedido entero.
                                        val canRemoveItems =
                                            restrictedMode != RestrictedMode.READ_ONLY &&
                                                activeOrder.activeItems.size > 1 &&
                                                (orderState == OrderState.PAID || orderState == OrderState.PREPARING)
                                        activeOrder.activeItems.forEach { item ->
                                            // El servidor solo deja a Cocina quitar lo que se prepara en Cocina.
                                            val removable =
                                                canRemoveItems && item.preparationStation == PreparationStation.KITCHEN
                                            ReceiptLine(
                                                quantity = item.quantity,
                                                name = item.productName,
                                                detail =
                                                    item.options
                                                        .joinToString(" · ") { it.name }
                                                        .ifEmpty { null },
                                                price = moneyLabel(item.subtotal),
                                                onRemove =
                                                    if (removable && !state.acting) {
                                                        { rejectingItem = item }
                                                    } else {
                                                        null
                                                    },
                                            )
                                        }
                                    }

                                    KitchenNotesNote(activeOrder.kitchenNotes, ink = TicketInk, muted = TicketMuted)

                                    if (activeOrder.summary.state == OrderState.PENDING_PAYMENT &&
                                        !activeOrder.summary.payAtEnd
                                    ) {
                                        // Se puede preparar antes de cobrar (lo decide cada negocio); que se note.
                                        Text(
                                            "SIN COBRAR · CAJA LO COBRA",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 1.2.sp,
                                            color = TicketMuted,
                                            modifier = Modifier.padding(top = 8.dp),
                                        )
                                    }
                                    if (activeOrder.summary.payAtEnd) {
                                        Text(
                                            "A LA CUENTA · SE PAGA AL FINAL",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 1.2.sp,
                                            color = TicketMuted,
                                            modifier = Modifier.padding(top = 8.dp),
                                        )
                                    }

                                    // Una sola acción: la que aplica según el estado real.
                                    Button(
                                        onClick = {
                                            haptics.impact()
                                            if (isPreparing) {
                                                onReady(activeOrder.summary.id, activeOrder.summary.version)
                                            } else {
                                                onStart(activeOrder.summary.id, activeOrder.summary.version)
                                            }
                                        },
                                        enabled =
                                            !isReady &&
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
                                        if (state.acting && !isReady) {
                                            CircularProgressIndicator(
                                                color = TicketMuted,
                                                strokeWidth = 2.5.dp,
                                                modifier =
                                                    Modifier
                                                        .size(22.dp)
                                                        .semantics { contentDescription = "Guardando la comanda" },
                                            )
                                        } else {
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
                                    }

                                    // Sin cobrar, el servidor no deja rechazar: solo cobrado o en preparación.
                                    if (!isReady &&
                                        orderState != OrderState.PENDING_PAYMENT &&
                                        restrictedMode != RestrictedMode.READ_ONLY
                                    ) {
                                        TextButton(
                                            onClick = { rejecting = true },
                                            enabled = !state.acting,
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                        ) {
                                            Text(
                                                "Rechazar pedido",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (state.acting) TicketMuted else Color(0xFFE5645A),
                                            )
                                        }
                                    }

                                    upcomingOrders.firstOrNull()?.let { next ->
                                        val nextDest =
                                            SpaceCopy
                                                .staffDestination(next.summary.destination, next.summary.space)
                                                .uppercase()
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
                    } else if (kitchenQueue == QueuePresentation.Error) {
                        OperationalErrorCard(
                            title = "No se pudieron cargar las comandas",
                            message = state.errorMessage ?: "Revisa la conexión e inténtalo otra vez.",
                            colors = colors,
                            onRetry = onRefresh,
                        )
                    } else {
                        val loadingQueue = kitchenQueue == QueuePresentation.Loading
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
                                    if (loadingQueue) "Cargando comandas…" else "Cocina al día",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = colors.textPrimary,
                                )
                                if (!loadingQueue) {
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
                        if (upcomingOrders.size == 1) "1 comanda" else "${upcomingOrders.size} comandas",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary,
                    )
                }
            }

            // Upcoming Orders List
            items(upcomingOrders, key = { it.summary.id }) { order ->
                val orderDest = SpaceCopy.staffDestination(order.summary.destination, order.summary.space)
                val orderSummaryTitle =
                    order.activeItems
                        .joinToString(" · ") { "${it.quantity}x ${it.productName}" }
                        .ifEmpty { "Productos del menú" }
                QueueTicketRow(
                    folio = "#${order.summary.folio}",
                    title = orderSummaryTitle,
                    subtitle = customerPrefix(order) + orderDest + arrivalSuffix(order),
                    // La fila dice el mismo estado que la comanda abierta, no un "En espera" fijo.
                    time = kitchenQueueLabel(order),
                    colors = colors,
                    modifier =
                        Modifier
                            .animateItem()
                            .assistantAnchor(assistantRegistry, assistantQueueOrderAnchor(order.summary.id)),
                    onClick = {
                        haptics.selection()
                        selectedOrderId = order.summary.id
                        toastMessage = "Comanda #${order.summary.folio} seleccionada"
                        assistantController.onAnchorTapped(assistantQueueOrderAnchor(order.summary.id))
                    },
                )
            }
        }

        ToastPill(
            message = toastMessage,
            containerColor = colors.textPrimary,
            contentColor = colors.background,
            badgeColor = colors.accentLime,
            badgeContentColor = colors.accentInk,
        )

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

    val itemToReject = rejectingItem
    if (itemToReject != null && activeOrder != null) {
        KitchenRejectDialog(
            folio = activeOrder.summary.folio,
            itemName = itemToReject.productName,
            colors = colors,
            onDismiss = { rejectingItem = null },
            onConfirm = { reason ->
                rejectingItem = null
                haptics.impact()
                onRejectItem(activeOrder.summary.id, itemToReject.id, activeOrder.summary.version, reason)
            },
        )
    }

    if (rejecting && activeOrder != null) {
        KitchenRejectDialog(
            folio = activeOrder.summary.folio,
            colors = colors,
            onDismiss = { rejecting = false },
            onConfirm = { reason ->
                rejecting = false
                haptics.impact()
                onReject(activeOrder.summary.id, activeOrder.summary.version, reason)
            },
        )
    }
}

/** Error de una acción o de la carga, con la tarjeta del tablero de Mesero; se anuncia al lector de pantalla. */
@Composable
private fun OperationalErrorCard(
    message: String,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    title: String? = null,
    onDismiss: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.cardBackground,
        modifier =
            modifier
                .fillMaxWidth()
                .border(1.dp, colors.cardBorder, RoundedCornerShape(20.dp)),
    ) {
        Column(modifier = Modifier.padding(start = 20.dp, top = 14.dp, end = 10.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                // El aviso se lee completo (título y mensaje) al aparecer; cerrar y reintentar quedan aparte.
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(top = 6.dp, end = 8.dp)
                            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                ) {
                    title?.let {
                        Text(
                            it,
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        message,
                        color = if (title == null) colors.textPrimary else colors.textSecondary,
                        fontSize = 14.sp,
                        fontWeight = if (title == null) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(top = if (title == null) 0.dp else 4.dp),
                    )
                }
                if (onDismiss != null) {
                    Box(
                        modifier =
                            Modifier
                                .physicalPress(scale = PhysicalPressScale.Small, onClick = onDismiss)
                                .size(48.dp)
                                .clip(CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Cerrar aviso",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            if (onRetry != null) {
                EditorialAccentButton(
                    text = "Reintentar",
                    onClick = onRetry,
                    modifier = Modifier.padding(top = 12.dp, end = 10.dp),
                )
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
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Surface(
        modifier =
            modifier
                .physicalPress(onClick = onClick)
                .fillMaxWidth()
                .border(1.dp, colors.cardBorder, RoundedCornerShape(20.dp)),
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
        /** Con dos decimales, como lo pide el backend ("12.50"). */
        price: String,
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
                            .physicalPress(enabled = !saving, scale = PhysicalPressScale.Small) { onDismiss() }
                            .size(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colors.cardBackground)
                                .border(1.dp, colors.cardBorder, CircleShape),
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
            }

            // Photo drop-zone
            Box(
                modifier =
                    Modifier
                        .physicalPress(enabled = !saving) { photoPicker.launch("image/*") }
                        .fillMaxWidth()
                        .height(112.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.cardBackground),
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

            val stations =
                listOf(
                    PreparationStation.CASHIER to "Barra / Bebidas",
                    PreparationStation.KITCHEN to "Cocina caliente",
                )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Se prepara en", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                SlidingSegments(
                    labels = stations.map { it.second },
                    selectedIndex = stations.indexOfFirst { it.first == selectedStation }.coerceAtLeast(0),
                    onSelect = { index ->
                        haptics.selection()
                        selectedStation = stations[index].first
                    },
                    trackColor = colors.cardInner,
                    indicatorColor = colors.textPrimary,
                    selectedContentColor = colors.background,
                    idleContentColor = colors.textSecondary,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !saving,
                    verticalPadding = 11.dp,
                )
            }

            OperationalField(
                value = name,
                onValueChange = { name = it },
                label = "Nombre",
                colors = colors,
                placeholder = "Ej. Matcha frío con avena",
                enabled = !saving,
            )
            OperationalField(
                value = priceStr,
                onValueChange = { priceStr = MoneyInput.sanitize(it) },
                label = "Precio",
                colors = colors,
                placeholder = "Ej. 65 o 12.50",
                enabled = !saving,
                keyboardType = KeyboardType.Decimal,
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
            val price = MoneyInput.toWire(priceStr)
            val isValid = name.isNotBlank() && price != null && price.toBigDecimal().signum() > 0
            Button(
                onClick = {
                    haptics.impact()
                    submitted = true
                    if (price == null) return@Button
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

/** Motivos que Cocina da con más frecuencia: un toque los escribe. */
private val RejectionQuickReasons = listOf("Se acabó el producto", "Faltan ingredientes", "Cocina saturada")

/**
 * Cocina rechaza un pedido y explica por qué: el cliente ve el motivo en su seguimiento. Es una hoja,
 * como el resto de la app; el botón se vuelve su propio progreso y no aparece un modal aparte.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KitchenRejectDialog(
    folio: Int,
    colors: OperationalColors,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit,
    /** Con nombre se quita solo ese artículo; sin él se rechaza el pedido entero. */
    itemName: String? = null,
    /** Aviso de dinero ya cobrado que habrá que devolver. */
    warning: String? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        KitchenRejectContent(
            folio = folio,
            colors = colors,
            onConfirm = onConfirm,
            itemName = itemName,
            warning = warning,
        )
    }
}

/** El contenido de la hoja de rechazo, aparte de la hoja: así se puede probar y capturar. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun KitchenRejectContent(
    folio: Int,
    colors: OperationalColors,
    onConfirm: (reason: String) -> Unit,
    itemName: String? = null,
    warning: String? = null,
) {
    var reason by remember { mutableStateOf("") }
    val valid = reason.trim().length >= 3
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(modifier = Modifier.arrive(0)) {
            Text(
                if (itemName != null) "Quitar $itemName" else "Rechazar pedido",
                fontSize = 13.sp,
                color = colors.textSecondary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "#$folio",
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.4).sp,
                color = colors.textPrimary,
            )
            Text(
                if (itemName != null) {
                    "Solo se quita este artículo; el resto sigue. El cliente verá el motivo y se le descuenta."
                } else {
                    "El cliente verá el motivo. Si ya había pagado, se le devuelve el dinero."
                },
                fontSize = 13.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (warning != null) {
                Text(
                    warning,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CashShortInk,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        Column(modifier = Modifier.arrive(1), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                RejectionQuickReasons.forEach { quick ->
                    ChipButton(
                        label = quick,
                        enabled = true,
                        colors = colors,
                        selected = reason == quick,
                    ) { reason = quick }
                }
            }
            OperationalField(
                value = reason,
                onValueChange = { reason = it.take(240) },
                label = "Motivo",
                colors = colors,
                placeholder = "Escríbelo o toca uno de arriba",
                singleLine = false,
            )
        }
        Box(modifier = Modifier.arrive(2)) {
            ActionPill(
                label = if (itemName != null) "Quitar de la comanda" else "Rechazar pedido",
                enabled = valid,
                loading = false,
                colors = colors,
            ) { onConfirm(reason.trim()) }
        }
    }
}

/** Etiqueta de una comanda en la fila de cocina, igual al sello de la comanda abierta. */
internal fun kitchenQueueLabel(order: OrderDetail): String =
    when (order.summary.state) {
        OrderState.PENDING_PAYMENT -> if (order.summary.payAtEnd) "En espera" else "Sin cobrar"
        OrderState.PREPARING -> "Preparando"
        OrderState.READY -> "Lista"
        else -> "En espera"
    }

private val CashShortInk = Color(0xFFB3261E)

/**
 * La cola de Caja: lo que hay que cobrar (aunque Cocina ya lo haya empezado) y lo listo por entregar. El más
 * antiguo va primero y un pedido nuevo entra al final, así la tarjeta que se atiende no cambia sola.
 */
internal fun cashierQueueOrders(orders: List<OrderDetail>): List<OrderDetail> =
    orders
        .filter { it.summary.needsCashCollection || it.summary.state == OrderState.READY }
        .sortedBy { it.summary.createdAt }

/** Por qué la propina escrita no sirve; null si está bien o no es un monto libre. */
internal fun tipProblem(
    choice: TipChoice,
    base: BigDecimal,
): String? {
    val custom = choice as? TipChoice.Custom ?: return null
    val text = custom.amount.trim()
    if (text.isEmpty()) return null
    val value =
        text.toBigDecimalOrNull()?.takeIf { it.signum() >= 0 && it.scale() <= 2 }
            ?: return "Escribe un monto válido, con hasta dos decimales."
    return if (value > base) "La propina no puede ser mayor al pedido." else null
}

/** "Cambio: $50.00" con el efectivo ya escrito; "Faltan $X" si no alcanza; null si aún no hay monto. */
internal fun cashChangeHint(change: BigDecimal?): String? =
    when {
        change == null -> null
        change.signum() < 0 -> "Faltan ${moneyLabel(Money.format(change.negate()))}"
        else -> "Cambio: ${moneyLabel(Money.format(change))}"
    }

/** Aviso antes de rechazar un pedido que ya tiene dinero cobrado: el servidor registra la devolución. */
internal fun paidRejectionWarning(
    order: OrderDetail,
    removed: String,
): String? {
    val summary = order.summary
    val paid =
        summary.amountPaid?.toBigDecimalOrNull()
            ?: if (summary.state == OrderState.PENDING_PAYMENT) BigDecimal.ZERO else summary.total.toBigDecimalOrNull()
    val amount = removed.toBigDecimalOrNull()
    if (paid == null || amount == null || paid.signum() <= 0) return null
    return "Este pedido ya está pagado: hay que devolver ${moneyLabel(Money.format(paid.min(amount)))} al cliente."
}

/** El cobro que acaba de confirmarse, con el cambio, hasta que el cajero lo descarte: cuenta billetes con él a la vista. */
@Composable
private fun CashCollectedCard(
    message: String,
    colors: OperationalColors,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.cardBackground,
        modifier = modifier.fillMaxWidth().border(1.dp, colors.highlightBorder, RoundedCornerShape(20.dp)),
    ) {
        Row(
            modifier = Modifier.padding(start = 18.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                message,
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
            )
            TextButton(onClick = onDone, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Listo", fontWeight = FontWeight.Bold, color = colors.textPrimary)
            }
        }
    }
}

/** El cobro en efectivo que el cajero va armando: lo recibido y la propina elegida. */
internal data class CashierCashDraft(
    /** El total del pedido cuando se empezó a escribir. */
    val orderTotal: String,
    val received: String,
    val tip: TipChoice = TipChoice.None,
)

/** "Sesión de caja abierta · Usagi" con la caja abierta; "Cuenta de caja" mientras no se sabe o está cerrada. */
internal fun cashierSessionCaption(
    cashSessionOpen: Boolean?,
    placeName: String,
): String =
    when {
        cashSessionOpen != true -> "Cuenta de caja"
        placeName.isBlank() -> "Sesión de caja abierta"
        else -> "Sesión de caja abierta · ${placeName.trim()}"
    }
