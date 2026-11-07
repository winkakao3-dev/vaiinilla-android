package com.vaiinilla.app.ui.screens

import android.net.Uri
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.model.SpaceCopy
import com.vaiinilla.app.domain.model.activeItems
import com.vaiinilla.app.domain.model.cashAmountDue
import com.vaiinilla.app.domain.model.needsCashCollection
import com.vaiinilla.app.domain.model.offlineCaptureLabel
import com.vaiinilla.app.domain.repository.CashDrawerMovementType
import com.vaiinilla.app.domain.repository.StoreImageKind
import com.vaiinilla.app.domain.repository.TableSpace
import com.vaiinilla.app.domain.time.BusinessTime
import com.vaiinilla.app.ui.components.ASSISTANT_ANCHOR_ADD_PRODUCT
import com.vaiinilla.app.ui.components.ASSISTANT_ANCHOR_CASHIER_ORDER_CARD
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
import com.vaiinilla.app.ui.components.animateSelectionColor
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.components.assistantAnchor
import com.vaiinilla.app.ui.components.assistantProductSwitchAnchor
import com.vaiinilla.app.ui.components.cashierAssistantGuides
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberAssistantAnchorRegistry
import com.vaiinilla.app.ui.components.rememberOperationalAssistantController
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.components.rentalChargeLabel
import com.vaiinilla.app.ui.notices.NoticePanel
import com.vaiinilla.app.ui.operational.CashSession
import com.vaiinilla.app.ui.operational.CollectedToday
import com.vaiinilla.app.ui.operational.OperationalUiState
import com.vaiinilla.app.ui.operational.QueuePresentation
import com.vaiinilla.app.ui.operational.queuePresentation
import com.vaiinilla.app.ui.screens.admin.AdminPrimaryButton
import com.vaiinilla.app.ui.screens.admin.AdminSecondaryButton
import com.vaiinilla.app.ui.screens.admin.moneyLabelCents
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.LocalVaiinillaThemeMode
import com.vaiinilla.app.ui.theme.LocalVaiinillaThemeModeChanger
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.time.Instant
import java.time.format.DateTimeFormatter

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
    /** Lee el resumen y los renglones del cajón (N11, R37). */
    onLoadCashDrawer: () -> Unit = {},
    /** Meter o sacar efectivo del cajón con su motivo (R7). */
    onSaveDrawerMovement: (CashDrawerMovementType, String, String) -> Unit = { _, _, _ -> },
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
    /** R1: «Ver cuentas por cobrar» del cierre bloqueado abre esta lista; sin ella, el tablero de mesas. */
    onOpenUnpaidAccounts: (() -> Unit)? = null,
    restrictedMode: RestrictedMode? = null,
    onToggleProductAvailable: (productId: Int, available: Boolean) -> Unit = { _, _ -> },
    onCreateCashierProduct: (CatalogProductDraft, ByteArray?, String?, String?, (String?) -> Unit) -> Unit =
        { _, _, _, _, _ -> },
    onUploadCashierProductImage: (Int, ByteArray, String, String) -> Unit = { _, _, _, _ -> },
    /** Subir el logo o la foto del negocio. */
    onUploadStoreImage: (StoreImageKind, ByteArray, String, String) -> Unit = { _, _, _, _ -> },
    onRemoveStoreImage: (StoreImageKind) -> Unit = {},
    assistantUserKey: String = "cashier",
    staffName: String? = null,
    /** Abre el pedido de mostrador; null lo oculta. */
    onNewCounterOrder: (() -> Unit)? = null,
    /** Mesas a las que Caja toma pedido, como el mesero. */
    orderTables: List<TableSpace> = emptyList(),
    /** Estado del tablero de mesas, para que la hoja de Nuevo pedido diga por qué no hay mesas. */
    orderTablesLoading: Boolean = false,
    orderTablesError: String? = null,
    onRefreshOrderTables: () -> Unit = {},
    onNewTableOrder: ((TableSpace) -> Unit)? = null,
    /** Doop CR5: el corte de caja del día y de cada turno, a pantalla completa; null lo oculta del menú. */
    cashCutContent: (@Composable (onClose: () -> Unit) -> Unit)? = null,
    /** Mesas y canchas con cuenta sin cobrar; null mientras no se sabe. */
    pendingSpaces: Int? = null,
    /** Caja de una escuela: la fila Alumnos (padres e hijos); null fuera de una escuela. */
    studentsEntry: (@Composable () -> Unit)? = null,
    /** Pedidos de mostrador guardados sin internet (por mandar y con problema); null si no hay. */
    offlineOrdersEntry: (@Composable () -> Unit)? = null,
    /** Avisos de comandas que no llegaron o llevan rato sin avanzar; null los oculta. */
    alertsEntry: (@Composable () -> Unit)? = null,
    /** Devoluciones pendientes (G08, H03); null las oculta. */
    refundsEntry: (@Composable () -> Unit)? = null,
    /** El negocio de esta caja, para que el cajero vea en cuál cobra. */
    placeName: String = "",
    /** Aviso que llega de fuera (por ejemplo, el folio del pedido de mostrador). */
    notice: String? = null,
    /** Si el negocio exige el QR del cliente para entregar; sin él, Caja entrega con un toque. */
    deliveryRequiresQr: Boolean = true,
    /** Sube cada vez que el visor pide abrir la búsqueda («¿No lee?» de C08). */
    openSearchRequest: Int = 0,
    onDismissError: () -> Unit = {},
    onRefresh: () -> Unit = {},
) {
    val colors = rememberOperationalColors()
    val themeMode = LocalVaiinillaThemeMode.current
    val themeChanger = LocalVaiinillaThemeModeChanger.current
    val haptics = rememberVaiinillaHaptics()
    var addProductSheetOpen by remember { mutableStateOf(false) }
    var cashDrawerOpen by remember { mutableStateOf(false) }
    var cashCutOpen by remember { mutableStateOf(false) }
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
    var searchOpen by remember { mutableStateOf(false) }
    LaunchedEffect(openSearchRequest) { if (openSearchRequest > 0) searchOpen = true }
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
    var browsingClosed by rememberSaveable { mutableStateOf(false) }
    // El conteo del cierre vive en la ruta y no en la hoja: ir a «Ver cuentas por cobrar» cierra la hoja, y al
    // volver el cajero no tiene que contar el cajón otra vez. Se borra cuando la caja ya quedó cerrada.
    val closeCount = rememberSaveable(stateSaver = CashCountDraft.Saver) { mutableStateOf(CashCountDraft()) }
    LaunchedEffect(state.cashSessionOpen) {
        if (state.cashSessionOpen == false) closeCount.value = CashCountDraft()
    }
    val cashRegisterOpen = state.cashSessionOpen == true
    val canReloadWallet = cashRegisterOpen && restrictedMode != RestrictedMode.READ_ONLY
    val walletBlockedReason =
        when {
            restrictedMode == RestrictedMode.READ_ONLY -> "Esta sesión es de solo lectura."
            !cashRegisterOpen -> "Abre la caja para registrar recargas en efectivo."
            else -> null
        }
    val actionableCashierOrders = cashierQueueOrders(state.orders)
    // La hoja de cobro (C02) solo se abre con un pedido elegido y se cierra sola cuando ese pedido sale de la fila.
    val chargeOrder = actionableCashierOrders.firstOrNull { it.summary.id == selectedCashierOrderId }
    val scanTarget = cashierScanTarget(actionableCashierOrders, deliveryRequiresQr)
    val actionableIds = actionableCashierOrders.map { it.summary.id }.toSet()
    // Un pedido de mostrador que el cajero eligió cobrar ya llega aquí seleccionado y abre su hoja (G03). Cada
    // selección abre la hoja una sola vez: si el cajero la cierra, que no vuelva a abrirse con el siguiente sondeo.
    var handledSelectedOrderId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.selectedOrderId, actionableIds) {
        val wanted = state.selectedOrderId ?: return@LaunchedEffect
        if (wanted != handledSelectedOrderId && wanted in actionableIds) {
            handledSelectedOrderId = wanted
            selectedCashierOrderId = wanted
        }
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
            readyOrderId = scanTarget?.summary?.id,
            readyOrderFolio = scanTarget?.summary?.folio,
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
        val unreadableOrders = rememberUnreadableOrders()
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Topline
            item(key = "cashier-topline") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CashierStatusPill(
                        open = state.cashSessionOpen,
                        modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
                    )

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
                            modeLabel = "Caja",
                            placeName = placeName,
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
                            onOpenCashDrawer =
                                if (state.cashSessionOpen == true) {
                                    {
                                        haptics.selection()
                                        cashDrawerOpen = true
                                    }
                                } else {
                                    null
                                },
                            onOpenCashCut =
                                cashCutContent?.let {
                                    {
                                        haptics.selection()
                                        cashCutOpen = true
                                    }
                                },
                        )
                    }
                }
            }

            if (state.lastRefreshOk == false && state.orders.isNotEmpty()) {
                item(key = "cashier-offline") {
                    StaffOfflineBanner(offline = true, modifier = Modifier.animateItem(), savesOrders = true)
                }
            }

            if (alertsEntry != null) {
                item(key = "cashier-alerts") {
                    Box(modifier = Modifier.animateItem()) { alertsEntry() }
                }
            }

            if (offlineOrdersEntry != null) {
                item(key = "cashier-offline-orders") {
                    Box(modifier = Modifier.animateItem()) { offlineOrdersEntry() }
                }
            }

            if (unreadableOrders > 0) {
                item(key = "cashier-update-app") {
                    StaffUpdateAppBanner(count = unreadableOrders, modifier = Modifier.animateItem())
                }
            }

            if (state.cashSessionOpen == false && !browsingClosed && restrictedMode != RestrictedMode.READ_ONLY) {
                item(key = "cash-welcome") {
                    CashWelcome(
                        staffName = staffName,
                        suggested = state.suggestedCashFloat,
                        enabled = !state.acting,
                        colors = colors,
                        onOpen = onOpenCashSession,
                        onOtherAmount = {
                            haptics.impact()
                            openCashSheet = true
                        },
                        onSkip = { browsingClosed = true },
                    )
                }
                return@LazyColumn
            }

            item(key = "cashier-title") {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Por cobrar",
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp,
                            color = colors.textPrimary,
                            maxLines = 1,
                            // L01: con letra grande «Por cobrar» se partía junto al buscador; se encoge.
                            autoSize = TextAutoSize.StepBased(minFontSize = 22.sp, maxFontSize = 34.sp),
                            modifier = Modifier.weight(1f),
                        )
                        CashierSearchButton(
                            colors = colors,
                            onClick = {
                                haptics.selection()
                                searchOpen = true
                            },
                        )
                    }
                    // Antes de cobrar, el cajero confirma que la sesión está abierta y en qué negocio (G01).
                    Text(
                        cashierSessionCaption(
                            state.cashSessionOpen,
                            placeName,
                            openingAmount = state.cashOpeningAmount,
                            openedAt = state.cashOpenedAt,
                        ),
                        fontSize = 12.5.sp,
                        color = colors.textSecondary,
                        fontWeight = FontWeight.SemiBold,
                        // L01: con letra grande el negocio, el fondo y la hora no caben en un renglón.
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
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
                    pendingSpaces = pendingSpaces,
                    onSpaces =
                        onOpenAccounts?.let { open ->
                            {
                                haptics.selection()
                                open()
                            }
                        },
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
            if (state.cashSession == CashSession.CheckFailed) {
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
                CashierQueueHeader(count = actionableCashierOrders.size, colors = colors)
            }

            // Toda la fila en una tarjeta, como en C01: cada renglón dice cómo va y si ya pagó, y al tocarlo se abre
            // la hoja para cobrarlo o entregarlo.
            item(key = "cashier-queue") {
                if (actionableCashierOrders.isNotEmpty()) {
                    CashierQueueCard(
                        orders = actionableCashierOrders,
                        tick = tick,
                        colors = colors,
                        modifier = Modifier.assistantAnchor(assistantRegistry, ASSISTANT_ANCHOR_CASHIER_ORDER_CARD),
                        onOpen = { order ->
                            haptics.selection()
                            selectedCashierOrderId = order.summary.id
                        },
                    )
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

            if (actionableCashierOrders.isNotEmpty()) {
                item(key = "cashier-queue-note") {
                    Text(
                        "Cada pedido dice dos cosas: cómo va y si ya pagó. Toca uno para cobrarlo o entregarlo.",
                        fontSize = 12.5.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.animateItem().padding(horizontal = 4.dp),
                    )
                }
            }

            if (state.cashSessionOpen == true) {
                item(key = "cashier-today") {
                    // Cada cobro en efectivo mueve el esperado: se relee el cajón para que lo cobrado no se quede en «—».
                    // Un cobro con tarjeta o saldo no lo mueve: también se relee cuando un pedido cambia de estado,
                    // para que «Cobrado hoy» (todos los métodos) siga al día.
                    val orderStates = state.orders.map { it.summary.id to it.summary.state }
                    LaunchedEffect(state.expectedCash, orderStates) { onLoadCashDrawer() }
                    CashierTodayCard(
                        cashSales =
                            state.cashDrawer.report
                                ?.summary
                                ?.cashSales,
                        collectedToday = state.collectedToday,
                        expectedCash = state.expectedCash,
                        colors = colors,
                        modifier = Modifier.animateItem(),
                        onOpenDrawer = {
                            haptics.selection()
                            cashDrawerOpen = true
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

        // C01: escanear queda fijo abajo y entrega el pedido listo más viejo que pide el QR; con la hoja abierta,
        // el botón es el de la hoja.
        val pinnedScan = scanTarget.takeIf { chargeOrder == null && restrictedMode != RestrictedMode.READ_ONLY }
        AnimatedVisibility(
            visible = pinnedScan != null,
            enter = slideInVertically(spring(dampingRatio = 0.8f, stiffness = 380f)) { it } + fadeIn(spring()),
            exit = slideOutVertically(spring(stiffness = 500f)) { it } + fadeOut(spring()),
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            CashierScanButton(
                enabled = !state.acting,
                colors = colors,
                modifier = Modifier.assistantAnchor(assistantRegistry, ASSISTANT_ANCHOR_QR_SCAN),
                onClick = {
                    scanTarget?.let { target ->
                        haptics.impact()
                        onScanDeliver(target.summary.id, target.summary.version)
                    }
                },
            )
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

        if (searchOpen) {
            CashierOrderSearchSheet(
                orders = state.orders,
                colors = colors,
                canOpen = { it.summary.id in actionableIds },
                onDismiss = { searchOpen = false },
                onOpen = { order ->
                    haptics.selection()
                    searchOpen = false
                    selectedCashierOrderId = order.summary.id
                },
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
        if (cashDrawerOpen) {
            LaunchedEffect(Unit) { onLoadCashDrawer() }
            CashDrawerSheet(
                expectedCash = state.expectedCash,
                colors = colors,
                onDismiss = { cashDrawerOpen = false },
                drawer = state.cashDrawer,
                onSave = onSaveDrawerMovement.takeIf { restrictedMode != RestrictedMode.READ_ONLY },
                onRetry = onLoadCashDrawer,
            )
        }
        if (cashCutOpen && cashCutContent != null) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { cashCutOpen = false },
                properties =
                    androidx.compose.ui.window.DialogProperties(
                        usePlatformDefaultWidth = false,
                        decorFitsSystemWindows = false,
                    ),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(colors.background)
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                ) { cashCutContent { cashCutOpen = false } }
            }
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
                onOpenAccounts = onOpenUnpaidAccounts ?: onOpenAccounts,
                expectedCash = state.expectedCash,
                openingAmount = state.cashOpeningAmount,
                countState = closeCount,
                unsyncedCashOrders = state.unsyncedOfflineCash,
                drawerSummary = state.cashDrawer.report?.summary,
            )
        }
        chargeOrder?.let { order ->
            key(order.summary.id) {
                CashierChargeSheet(
                    order = order,
                    draft = cashDrafts[order.summary.id],
                    onDraftChange = { cashDrafts[order.summary.id] = it },
                    acting = state.acting,
                    errorMessage = state.errorMessage,
                    cashSessionOpen = state.cashSessionOpen,
                    readOnly = restrictedMode == RestrictedMode.READ_ONLY,
                    deliveryRequiresQr = deliveryRequiresQr,
                    colors = colors,
                    onCollect = onCollect,
                    onDeliver = onDeliver,
                    onScanDeliver = onScanDeliver,
                    onRemoveItem = { item -> rejectingItem = order.summary.id to item },
                    onReject = { rejectingOrderId = order.summary.id },
                    onDismiss = { selectedCashierOrderId = null },
                )
            }
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

/**
 * " · YA LLEGÓ 14:32" cuando el cliente (drive-thru) avisó que llegó por su pedido, con la hora del negocio para
 * saber cuánto lleva esperando (W25; QAIOS-26 en iOS); si no, vacío.
 */
internal fun arrivalSuffix(order: OrderDetail): String =
    order.arrivedAt
        ?.let { " · YA LLEGÓ ${BusinessTime.format(it, ArrivalHour)}" }
        .orEmpty()

private val ArrivalHour: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * El lugar del pedido para Caja: «Para llevar» o la mesa, barra o cancha. Una renta no se lleva: es «Renta de
 * Pista 3» (en vivo, D03).
 */
internal fun cashierPlaceLabel(order: OrderDetail): String =
    when {
        order.reservation != null -> rentalChargeLabel(order)
        order.summary.destination.name == "TAKE_AWAY" -> "Para llevar"
        else -> SpaceCopy.staffDestination(order.summary.destination, order.summary.space, order.summary.pickUpAtBar)
    }

// Una venta de mostrador no tiene cliente: el servidor manda «Mostrador» como nombre y eso ya dice de dónde viene.
// Una renta apartada en Caja también puede llegar a nombre de «Mostrador», pero se dice como renta.
private fun isCounterSale(order: OrderDetail): Boolean =
    order.reservation == null && staffCustomerName(order).equals("Mostrador", ignoreCase = true)

/** «Mostrador» o el lugar del pedido, para la hoja de cobro. */
internal fun cashierOrderOrigin(order: OrderDetail): String =
    if (isCounterSale(order)) "Mostrador" else cashierPlaceLabel(order)

/** «Para llevar · Sofía» en la fila de Caja: el lugar y, si se sabe, el nombre del cliente. */
private fun cashierRowTitle(order: OrderDetail): String =
    if (isCounterSale(order)) {
        "Mostrador"
    } else {
        listOfNotNull(cashierPlaceLabel(order), staffCustomerName(order)).joinToString(" · ")
    } + arrivalSuffix(order) + (order.offlineCaptureLabel()?.let { " · $it" } ?: "")

/** «2 Concha, Café de olla»: lo que lleva el pedido, con la cantidad solo cuando es más de uno. */
private fun cashierItemsLine(order: OrderDetail): String =
    order.activeItems
        .joinToString(", ") { if (it.quantity > 1) "${it.quantity} ${it.productName}" else it.productName }
        .ifEmpty { "Productos del menú" }

/**
 * Si Caja entrega este pedido sin escanear: el backend lo dispensa (`entrega_sin_qr`, una cuenta abierta), el
 * negocio no pide el QR, o el pedido lo hizo esta misma caja y su token ya está en el dispositivo.
 */
internal fun cashierDeliversWithoutQr(
    order: OrderDetail,
    deliveryRequiresQr: Boolean,
): Boolean = order.summary.deliveryWithoutQr || !deliveryRequiresQr || order.pickupToken != null

/** El pedido que entrega el botón fijo de escanear: el listo y pagado más viejo que sí pide el QR del cliente. */
internal fun cashierScanTarget(
    orders: List<OrderDetail>,
    deliveryRequiresQr: Boolean,
): OrderDetail? =
    orders.firstOrNull {
        it.summary.state == OrderState.READY &&
            !it.summary.needsCashCollection &&
            !cashierDeliversWithoutQr(it, deliveryRequiresQr)
    }

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
        ms < 7 * 86_400_000L -> "hace ${ms / 86_400_000} d"
        // Después de una semana, la fecha: «hace 340 d» no dice nada.
        else -> NoticePanel.shortDate(System.currentTimeMillis() - ms)
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
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp,
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }
}

/** Error de una acción o de la carga, con la tarjeta del tablero de Mesero; se anuncia al lector de pantalla. */
@Composable
internal fun OperationalErrorCard(
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

/** C01: la fila de Caja en una sola tarjeta, un renglón por pedido, del más viejo al más nuevo. */
@Composable
private fun CashierQueueCard(
    orders: List<OrderDetail>,
    tick: Int,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    onOpen: (OrderDetail) -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth().border(1.dp, colors.cardBorder, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        color = colors.cardBackground,
    ) {
        Column(
            modifier =
                Modifier.animateContentSize(
                    if (reducedMotion()) snap() else spring(dampingRatio = 0.85f, stiffness = 380f),
                ),
        ) {
            orders.forEachIndexed { index, order ->
                key(order.summary.id) {
                    if (index > 0) {
                        HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(horizontal = 14.dp))
                    }
                    QueueTicketRow(order = order, tick = tick, colors = colors, onClick = { onOpen(order) })
                }
            }
        }
    }
}

@Composable
private fun QueueTicketRow(
    order: OrderDetail,
    tick: Int,
    colors: OperationalColors,
    onClick: () -> Unit,
) {
    val summary = order.summary
    Row(
        modifier =
            Modifier
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .defaultMinSize(minWidth = 40.dp, minHeight = 40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.cardInner)
                    .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("#${summary.folio}", color = colors.textPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                cashierRowTitle(order),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                cashierItemsLine(order),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                elapsedShort(summary.createdAt, tick),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                maxLines = 1,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                moneyLabel(if (summary.needsCashCollection) summary.cashAmountDue else summary.total),
                color = colors.textPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
                letterSpacing = (-0.3).sp,
                maxLines = 1,
            )
            CashierOrderStatus(summary = summary)
        }
    }
}

/** Caja cerrada, como el marco C06: saluda, propone el fondo de siempre y abrir es un toque si cuadra. */
@Composable
private fun CashWelcome(
    staffName: String?,
    suggested: String?,
    enabled: Boolean,
    colors: OperationalColors,
    onOpen: (String) -> Unit,
    onOtherAmount: () -> Unit,
    onSkip: () -> Unit,
) {
    val hour =
        java.time.LocalTime
            .now(BusinessTime.zone)
            .hour
    val hello =
        when {
            hour < 12 -> "Buenos días"
            hour < 19 -> "Buenas tardes"
            else -> "Buenas noches"
        }
    val first = staffName?.trim()?.substringBefore(' ')?.takeIf { it.isNotEmpty() }
    Column(
        Modifier.fillMaxWidth().padding(top = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(22.dp)).background(colors.cardBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Lock,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(26.dp),
            )
        }
        Text(
            if (first != null) "$hello,\n$first" else hello,
            fontSize = 34.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1).sp,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 18.dp),
        )
        Text(
            "Cuenta el efectivo que hay en el cajón antes de abrir. Así al cerrar sabes si cuadra.",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp, start = 8.dp, end = 8.dp),
        )
        if (suggested != null) {
            Text(
                "EFECTIVO EN EL CAJÓN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.3.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 26.dp, bottom = 8.dp),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.cardBackground)
                    .border(1.5.dp, colors.accentLime, RoundedCornerShape(20.dp))
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    moneyLabelCents(suggested),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.textPrimary,
                )
            }
            Text(
                "Es el fondo de siempre. Si cuadra, solo confirma.",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 10.dp, bottom = 16.dp),
            )
            AdminPrimaryButton(
                title = "Sí, hay ${moneyLabel(suggested)} · Abrir caja",
                colors = colors,
                enabled = enabled,
                fill = colors.accentLime,
                contentColor = colors.accentInk,
            ) { onOpen(suggested) }
            Spacer(Modifier.height(10.dp))
            AdminSecondaryButton(
                title = "Conté otra cantidad",
                colors = colors,
                outlined = true,
                enabled = enabled,
                onClick = onOtherAmount,
            )
        } else {
            Spacer(Modifier.height(26.dp))
            AdminPrimaryButton(
                title = "Abrir caja",
                colors = colors,
                enabled = enabled,
                fill = colors.accentLime,
                contentColor = colors.accentInk,
                onClick = onOtherAmount,
            )
        }
        Text(
            "Ver la fila sin abrir",
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colors.textMuted,
            modifier =
                Modifier
                    .padding(top = 8.dp)
                    .physicalPress(scale = PhysicalPressScale.Small, onClick = onSkip)
                    .padding(12.dp),
        )
    }
}

/**
 * «COBRADO HOY | Efectivo en el cajón» bajo la fila. Lo cobrado es lo del día con todos los métodos, del corte del día
 * (GET /sesiones-caja/cortes: `resumen.ventas` y `formas_pago`), con el desglose debajo si cabe. Sin corte todavía
 * (o si falla) queda lo de antes: las ventas en efectivo de esta caja, del resumen del cajón. El efectivo abre el
 * detalle del cajón.
 */
@Composable
private fun CashierTodayCard(
    cashSales: String?,
    expectedCash: String?,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    collectedToday: CollectedToday? = null,
    onOpenDrawer: () -> Unit,
) {
    val title = if (collectedToday != null) "COBRADO HOY" else "COBRADO EN EFECTIVO"
    val amount = collectedToday?.total ?: cashSales
    val breakdown = collectedToday?.breakdownLine(::moneyLabel)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = colors.cardBackground,
    ) {
        // L01: con letra grande las dos mitades se encimaban; cada una tiene su mitad y se parte entre palabras.
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                )
                Text(
                    amount?.let(::moneyLabel) ?: "—",
                    color = colors.textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 4.dp),
                )
                breakdown?.let {
                    Text(
                        it,
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .physicalPress(scale = PhysicalPressScale.Small, onClick = onOpenDrawer),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    "Efectivo en el cajón",
                    color = colors.textSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                )
                Text(
                    expectedCash?.let(::moneyLabel) ?: "—",
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** «Escanear QR para entregar», fijo abajo de Caja como en C01. */
@Composable
private fun CashierScanButton(
    enabled: Boolean,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            modifier
                .physicalPress(enabled = enabled, onClick = onClick)
                .shadow(14.dp, CircleShape, spotColor = Color(0x33000000))
                .clip(CircleShape)
                .background(colors.textPrimary)
                .heightIn(min = 56.dp)
                .padding(horizontal = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.QrCodeScanner,
            contentDescription = null,
            tint = colors.background,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        // L01: con letra grande se cortaba en «Escanear QR para…»: se encoge y, si hace falta, usa dos renglones.
        Text(
            "Escanear QR para entregar",
            color = colors.background,
            fontWeight = FontWeight.Black,
            maxLines = 2,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center,
            autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 15.sp, stepSize = 0.5.sp),
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

/** El estado de un pedido para Caja en dos partes, como en C01: cómo va en cocina y si ya pagó. */
internal data class CashierStatus(
    val progress: String,
    val ready: Boolean,
    val paid: Boolean,
)

internal fun cashierOrderStatus(summary: OrderSummary): CashierStatus =
    CashierStatus(
        progress =
            when (summary.state) {
                OrderState.READY -> "Listo"
                OrderState.PREPARING -> "En cocina"
                else -> "Sin empezar"
            },
        ready = summary.state == OrderState.READY,
        paid = !summary.needsCashCollection,
    )

// Lima, ámbar y coral oscuros en el tema claro; claros en el oscuro.
private val StatusReadyLight = Color(0xFF3B5310)
private val StatusWaitLight = Color(0xFF8A5A00)
private val StatusDueLight = Color(0xFFB3261E)
private val StatusIdleLight = Color(0xFF6B6656)
private val StatusReadyDark = Color(0xFFB7DE63)
private val StatusWaitDark = Color(0xFFF2C14E)
private val StatusDueDark = Color(0xFFFF7A6E)
private val StatusIdleDark = Color(0xFFA5A79E)

@Composable
internal fun CashierOrderStatus(
    summary: OrderSummary,
    modifier: Modifier = Modifier,
) {
    val dark = rememberOperationalColors().isDark
    val status = cashierOrderStatus(summary)
    val progressColor =
        when {
            status.ready -> if (dark) StatusReadyDark else StatusReadyLight
            summary.state == OrderState.PREPARING -> if (dark) StatusWaitDark else StatusWaitLight
            else -> if (dark) StatusIdleDark else StatusIdleLight
        }
    val payColor =
        if (status.paid) {
            if (dark) StatusReadyDark else StatusReadyLight
        } else {
            if (dark) StatusDueDark else StatusDueLight
        }
    val payLabel = if (status.paid) "Pagado" else "Falta pagar"
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(status.progress, color = progressColor, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold)
        Text(" · ", color = if (dark) StatusIdleDark else StatusIdleLight, fontSize = 12.5.sp)
        Text(payLabel, color = payColor, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/** «● CAJA ABIERTA» arriba a la izquierda; cerrada en coral para que nadie cobre sin sesión. */
@Composable
private fun CashierStatusPill(
    open: Boolean?,
    modifier: Modifier = Modifier,
) {
    val coral = LocalVaiinillaColors.current.coral
    val colors = rememberOperationalColors()
    val tint =
        animateSelectionColor(
            when (open) {
                true -> colors.accentLime
                false -> coral
                null -> colors.textSecondary
            },
            "cashier-pill",
        )
    val label =
        when (open) {
            true -> "CAJA ABIERTA"
            false -> "CAJA CERRADA"
            null -> "REVISANDO LA CAJA"
        }
    Row(
        modifier =
            modifier
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.16f))
                .padding(horizontal = 11.dp, vertical = 6.dp)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(tint))
        Text(label, color = tint, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 0.7.sp)
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
private val RejectionQuickReasons = listOf("Se acabó el producto", "Faltan ingredientes")

/**
 * Cocina rechaza un pedido y explica por qué: el cliente ve el motivo en su seguimiento. Es una hoja,
 * como el resto de la app; el botón se vuelve su propio progreso y no aparece un modal aparte.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KitchenRejectDialog(
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
            onCancel = onDismiss,
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
    onCancel: (() -> Unit)? = null,
) {
    var picked by remember { mutableStateOf(RejectionQuickReasons.first()) }
    var other by remember { mutableStateOf("") }
    val isOther = picked == REJECT_OTHER
    val reason = if (isOther) other.trim() else picked
    val valid = reason.length >= 3
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.arrive(0)) {
            Text(
                (if (itemName != null) "QUITAR ${itemName.uppercase()} · " else "PEDIDO ") + "#$folio",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                color = colors.textSecondary,
            )
            Text(
                "¿Por qué no se puede?",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.6).sp,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = 4.dp),
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
            (RejectionQuickReasons + REJECT_OTHER).forEach { option ->
                val on = picked == option
                val shape = RoundedCornerShape(18.dp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(if (on) Color(0xFFE5645A).copy(alpha = 0.14f) else colors.cardBackground)
                        .border(1.dp, if (on) Color(0xFFE5645A) else Color.Transparent, shape)
                        .selectable(selected = on, role = Role.RadioButton, onClick = { picked = option })
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier
                            .size(20.dp)
                            .border(2.dp, if (on) Color(0xFFE5645A) else colors.textMuted, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (on) Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFE5645A)))
                    }
                    Text(option, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
                }
            }
            if (isOther) {
                OperationalField(
                    value = other,
                    onValueChange = { other = it.take(240) },
                    label = "Motivo",
                    colors = colors,
                    placeholder = "Escríbelo para el cliente",
                    singleLine = false,
                )
            }
            if (itemName == null) {
                Text(
                    "¿Solo falta una cosa? Mejor quita ese artículo y lo demás sigue.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Box(modifier = Modifier.arrive(2)) {
            AdminPrimaryButton(
                title = if (itemName != null) "Quitar de la comanda" else "Rechazar pedido",
                colors = colors,
                enabled = valid,
                fill = Color(0xFFE5645A),
                contentColor = Color.White,
            ) { onConfirm(reason) }
        }
        if (onCancel != null) {
            ActionPill(
                label = "Cancelar",
                enabled = true,
                loading = false,
                colors = colors,
                outlined = true,
                modifier = Modifier.arrive(2),
                onClick = onCancel,
            )
        }
    }
}

private const val REJECT_OTHER = "Otro"

internal val CashShortInk = Color(0xFFB3261E)

/**
 * La cola de Caja: lo que hay que cobrar (aunque Cocina ya lo haya empezado) y lo listo por entregar. El más
 * antiguo va primero y un pedido nuevo entra al final, así la tarjeta que se atiende no cambia sola.
 */
internal fun cashierQueueOrders(orders: List<OrderDetail>): List<OrderDetail> =
    orders
        .filter { it.summary.needsCashCollection || it.summary.state == OrderState.READY }
        .sortedBy { it.summary.createdAt }

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

/**
 * «Pastelería Vaini · fondo $500 · desde 8:02» con la caja abierta, en la hora del negocio; «Cuenta de caja»
 * mientras no se sabe o está cerrada. Sin el nombre del negocio dice «Sesión de caja abierta».
 */
internal fun cashierSessionCaption(
    cashSessionOpen: Boolean?,
    placeName: String,
    openingAmount: String? = null,
    openedAt: String? = null,
): String {
    if (cashSessionOpen != true) return "Cuenta de caja"
    val opened = openedAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
    return listOfNotNull(
        placeName.trim().ifBlank { "Sesión de caja abierta" },
        openingAmount?.takeIf { it.isNotBlank() }?.let { "fondo ${moneyLabel(it)}" },
        opened?.let { "desde ${BusinessTime.format(it, OpeningHour)}" },
    ).joinToString(" · ")
}

private val OpeningHour: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")
