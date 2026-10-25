package com.vaiinilla.app.ui.counter

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.counter.StudentAllergens
import com.vaiinilla.app.domain.counter.StudentCard
import com.vaiinilla.app.domain.counter.StudentPurchaseRules
import com.vaiinilla.app.domain.counter.StudentTodayOrder
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.ContractRules
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ProductDetailSheet
import com.vaiinilla.app.ui.components.ToastPill
import com.vaiinilla.app.ui.components.VaiinillaQrCode
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.discovery.QrScannerDialog
import com.vaiinilla.app.ui.screens.ActionPill
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.admin.AdminSheet
import com.vaiinilla.app.ui.screens.admin.shareQr
import com.vaiinilla.app.ui.screens.rememberOperationalColors
import kotlinx.coroutines.delay
import java.math.BigDecimal

/** El coral de la app: alergias, rechazos y lo que la cajera tiene que ver antes de cobrar. */
internal val CounterCoral = Color(0xFFE5645A)

/** Lo que la hoja de Alumnos le pide al modelo; con valores vacíos para las pruebas de pantalla. */
data class StudentCounterActions(
    val onQueryChange: (String) -> Unit = {},
    val onRetrySearch: () -> Unit = {},
    val onSelect: (StudentCard) -> Unit = {},
    val onCloseStudent: () -> Unit = {},
    val onCredentialRead: (value: String, fromNfc: Boolean) -> Unit = { _, _ -> },
    val onStep: (productId: Int, delta: Int) -> Unit = { _, _ -> },
    val onAddWithOptions: (productId: Int, optionIds: List<Int>, quantity: Int) -> Unit = { _, _, _ -> },
    val onPinChange: (String) -> Unit = {},
    val onCharge: (BigDecimal) -> Unit = {},
    val onDeliver: (StudentTodayOrder) -> Unit = {},
    val onIssueQr: () -> Unit = {},
    val onLinkNfc: (String) -> Unit = {},
    val onSettleOffline: (com.vaiinilla.app.domain.counter.OfflineStudentPurchase) -> Unit = {},
    val onNoticeShown: () -> Unit = {},
)

fun StudentCounterViewModel.actions(): StudentCounterActions =
    StudentCounterActions(
        onQueryChange = ::updateQuery,
        onRetrySearch = ::retrySearch,
        onSelect = ::select,
        onCloseStudent = ::closeStudent,
        onCredentialRead = ::openCredential,
        onStep = { id, delta -> step(id, delta) },
        onAddWithOptions = ::add,
        onPinChange = ::updatePin,
        onCharge = ::charge,
        onDeliver = ::deliver,
        onIssueQr = ::issueQrCredential,
        onLinkNfc = ::linkNfcCard,
        onSettleOffline = offline::dismiss,
        onNoticeShown = ::consumeNotice,
    )

/** Caja con un alumno que no trae teléfono: se busca por nombre o matrícula, o se lee su credencial. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentCounterSheet(
    state: StudentCounterUiState,
    offline: OfflineCounterState,
    catalog: Catalog?,
    canCharge: Boolean,
    actions: StudentCounterActions,
    onDismiss: () -> Unit,
) {
    val colors = rememberOperationalColors()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var scanning by remember { mutableStateOf(false) }
    var linkingNfc by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.notice) {
        state.notice?.let {
            toast = it
            actions.onNoticeShown()
        }
    }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2_600)
            toast = null
        }
    }
    // Con la hoja abierta, acercar la tarjeta del alumno lo abre; en su credencial, la liga.
    NfcCardReaderEffect(enabled = !scanning) { uid ->
        if (linkingNfc) {
            linkingNfc = false
            actions.onLinkNfc(uid)
        } else {
            actions.onCredentialRead(uid, true)
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.94f).imePadding()) {
            StudentCounterContent(
                state = state,
                offline = offline,
                catalog = catalog,
                canCharge = canCharge,
                colors = colors,
                actions = actions,
                onScan = { scanning = true },
                linkingNfc = linkingNfc,
                onLinkNfcChange = { linkingNfc = it },
                onClose = onDismiss,
            )
            ToastPill(
                message = toast,
                containerColor = colors.textPrimary,
                contentColor = colors.background,
                badgeColor = colors.accentLime,
                badgeContentColor = colors.accentInk,
            )
        }
    }
    if (scanning) {
        QrScannerDialog(
            onClose = { scanning = false },
            helperText = "La credencial impresa del alumno, con su código de Vaiinilla.",
            onPayload = { raw ->
                scanning = false
                actions.onCredentialRead(raw, false)
            },
        )
    }
}

/** El cuerpo de la hoja, sin la hoja: buscar y la ficha. Aparte para probarlo en la JVM. */
@Composable
internal fun StudentCounterContent(
    state: StudentCounterUiState,
    offline: OfflineCounterState,
    catalog: Catalog?,
    canCharge: Boolean,
    colors: OperationalColors,
    actions: StudentCounterActions,
    onScan: () -> Unit = {},
    linkingNfc: Boolean = false,
    onLinkNfcChange: (Boolean) -> Unit = {},
    onClose: () -> Unit = {},
) {
    val reduced = reducedMotion()
    val requiresCredential = state.policy?.deliveryRequiresQr != false
    AnimatedContent(
        targetState = state.selected?.userId,
        transitionSpec = {
            if (reduced) {
                fadeIn(snap()) togetherWith fadeOut(snap())
            } else if (targetState != null) {
                (slideInHorizontally(spring(dampingRatio = 0.9f, stiffness = 500f)) { it / 3 } + fadeIn()) togetherWith
                    fadeOut(spring(stiffness = 700f))
            } else {
                (slideInHorizontally(spring(dampingRatio = 0.9f, stiffness = 500f)) { -it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { it / 3 } + fadeOut())
            }
        },
        label = "student-counter",
    ) { selectedId ->
        val card = state.selected?.takeIf { it.userId == selectedId }
        if (card == null) {
            StudentSearchPane(state, offline, colors, actions, onScan, onClose)
        } else {
            StudentCardPane(
                card = card,
                state = state,
                catalog = catalog,
                canCharge = canCharge,
                requiresCredential = requiresCredential,
                colors = colors,
                actions = actions,
                onScan = onScan,
                linkingNfc = linkingNfc,
                onLinkNfcChange = onLinkNfcChange,
            )
        }
    }
}

@Composable
private fun SheetHeader(
    title: String,
    subtitle: String?,
    colors: OperationalColors,
    onBack: (() -> Unit)?,
    onClose: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null) RoundIcon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver", colors, onBack)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Black, fontSize = 22.sp, color = colors.textPrimary, maxLines = 1)
            subtitle?.let {
                Text(it, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary, maxLines = 1)
            }
        }
        if (onClose != null) RoundIcon(Icons.Outlined.Close, "Cerrar", colors, onClose)
    }
}

@Composable
private fun RoundIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    colors: OperationalColors,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.cardBackground)
                .border(1.dp, colors.cardBorder, CircleShape)
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
    }
}

@Composable
internal fun StudentSearchPane(
    state: StudentCounterUiState,
    offline: OfflineCounterState,
    colors: OperationalColors,
    actions: StudentCounterActions,
    onScan: () -> Unit,
    onClose: () -> Unit,
) {
    val haptics = rememberVaiinillaHaptics()
    Column(modifier = Modifier.fillMaxSize()) {
        SheetHeader("Alumnos", "Para el que no trae teléfono", colors, onBack = null, onClose = onClose)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val shape = RoundedCornerShape(16.dp)
            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(shape)
                        .background(colors.cardInner)
                        .border(1.dp, colors.cardBorder, shape)
                        .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp),
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (state.query.isEmpty()) {
                        Text(
                            "Nombre o matrícula",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textMuted,
                        )
                    }
                    BasicTextField(
                        value = state.query,
                        onValueChange = actions.onQueryChange,
                        singleLine = true,
                        textStyle =
                            TextStyle(
                                color = colors.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        cursorBrush = SolidColor(colors.textPrimary),
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Buscar alumno" },
                    )
                }
                if (state.isSearching) {
                    CircularProgressIndicator(
                        color = colors.textSecondary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Box(
                modifier =
                    Modifier
                        .physicalPress(scale = PhysicalPressScale.Small) {
                            haptics.selection()
                            onScan()
                        }.size(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.cardInner)
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
                        .semantics { contentDescription = "Escanear credencial" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.QrCodeScanner,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        OfflineCounterSection(
            state = offline,
            colors = colors,
            onSettle = actions.onSettleOffline,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 8.dp,
                    bottom = 24.dp,
                ),
        ) {
            item(key = "status") {
                val status =
                    when {
                        state.searchError != null -> null
                        state.searchedOffline ->
                            if (state.results.isEmpty()) {
                                "Sin conexión. Solo salen los alumnos que ya abriste hoy."
                            } else {
                                "Sin conexión: de los que ya abriste hoy."
                            }
                        state.hasSearched && state.results.isEmpty() -> "Nadie con ese nombre o matrícula."
                        !state.hasSearched -> "Escribe al menos dos letras, o escanea o acerca su credencial."
                        else -> null
                    }
                state.searchError?.let { error ->
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Text(error, fontSize = 14.sp, color = colors.textPrimary)
                        Text(
                            "Intentar otra vez",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(top = 6.dp).physicalPress(onClick = actions.onRetrySearch),
                        )
                    }
                }
                if (status != null) {
                    Row(
                        modifier = Modifier.padding(top = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (state.searchedOffline) {
                            Icon(
                                Icons.Outlined.WifiOff,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Text(status, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                    }
                }
            }
            items(state.results, key = { it.userId }) { card ->
                StudentResultRow(card, colors) {
                    haptics.impact()
                    actions.onSelect(card)
                }
            }
        }
    }
}

@Composable
private fun StudentResultRow(
    card: StudentCard,
    colors: OperationalColors,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().physicalPress(scale = PhysicalPressScale.Nav, onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            InitialsBubble(card.initials, colors)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    card.name,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(card.identity, fontSize = 12.sp, color = colors.textSecondary, maxLines = 1)
            }
            Text(moneyLabel(card.balance), fontWeight = FontWeight.Black, fontSize = 16.sp, color = colors.textPrimary)
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary)
        }
        HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(start = 57.dp))
    }
}

@Composable
private fun InitialsBubble(
    initials: String,
    colors: OperationalColors,
) {
    Box(
        modifier =
            Modifier
                .size(
                    44.dp,
                ).clip(CircleShape)
                .background(colors.cardBackground)
                .border(1.dp, colors.cardBorder, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, fontWeight = FontWeight.Black, fontSize = 15.sp, color = colors.textPrimary)
    }
}

/** La ficha del alumno en Caja: cuánto puede gastar hoy, sus alergias, lo de hoy y la venta. */
@Composable
internal fun StudentCardPane(
    card: StudentCard,
    state: StudentCounterUiState,
    catalog: Catalog?,
    canCharge: Boolean,
    requiresCredential: Boolean,
    colors: OperationalColors,
    actions: StudentCounterActions,
    onScan: () -> Unit = {},
    linkingNfc: Boolean = false,
    onLinkNfcChange: (Boolean) -> Unit = {},
) {
    var showsCredential by rememberSaveable(card.userId) { mutableStateOf(false) }
    var choosing by remember(card.userId) { mutableStateOf<Product?>(null) }
    val products = catalog?.products.orEmpty()
    val sellable = products.filter(StudentPurchaseRules::isSellable)
    // La venta a un alumno se cobra a precio de mostrador, no al digital (CAJA-15).
    val prices = products.associate { it.id to (it.counterPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO) }
    val extras = StudentPurchaseRules.extras(products)
    val total = StudentPurchaseRules.total(state.cart, prices, state.cartChoices, extras)

    Column(modifier = Modifier.fillMaxSize()) {
        SheetHeader(
            title = card.name,
            subtitle = card.identity.ifEmpty { null },
            colors = colors,
            onBack = actions.onCloseStudent,
            onClose = null,
        )
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding =
                androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    bottom = 20.dp,
                ),
        ) {
            item(key = "head") { SpendableHeader(card, colors) { showsCredential = true } }
            if (card.allergens.isNotEmpty()) {
                item(key = "allergy") { StudentAllergyBanner(card.allergens, colors, Modifier.padding(top = 16.dp)) }
            }
            if (card.todayOrders.isNotEmpty()) {
                item(key = "today-title") { SectionTitle("Hoy", colors, Modifier.padding(top = 26.dp)) }
                items(card.todayOrders, key = { "order-${it.id}" }) { order ->
                    TodayOrderRow(
                        order = order,
                        needsScan = requiresCredential && state.scannedCredential == null,
                        delivering = state.deliveringOrderId == order.id,
                        busy = state.deliveringOrderId != null,
                        colors = colors,
                        onScan = onScan,
                        onDeliver = { actions.onDeliver(order) },
                    )
                }
            }
            item(key = "sell-title") { SectionTitle("Vender", colors, Modifier.padding(top = 26.dp)) }
            when {
                catalog == null ->
                    item(key = "sell-loading") {
                        Text(
                            "Cargando productos…",
                            fontSize = 14.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                sellable.isEmpty() ->
                    item(key = "sell-empty") {
                        Text(
                            "No hay productos disponibles para vender.",
                            fontSize = 14.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                else ->
                    items(sellable, key = { "product-${it.id}" }) { product ->
                        SellRow(
                            product = product,
                            card = card,
                            quantity = state.quantity(product.id),
                            choice =
                                state.cartChoices[product.id]?.let { picked ->
                                    product.optionGroups
                                        .flatMap { it.options }
                                        .filter { it.id in picked }
                                        .joinToString(" · ") { it.name }
                                        .ifEmpty { null }
                                },
                            colors = colors,
                            onAdd = {
                                if (StudentPurchaseRules.needsChoice(
                                        product,
                                    )
                                ) {
                                    choosing = product
                                } else {
                                    actions.onStep(product.id, 1)
                                }
                            },
                            onMinus = { actions.onStep(product.id, -1) },
                            onPlus = { actions.onStep(product.id, 1) },
                        )
                    }
            }
        }
        StudentChargeFooter(card, state, total, canCharge, colors, actions)
    }

    if (showsCredential) {
        StudentCredentialSheet(
            student = card,
            state = state,
            colors = colors,
            linkingNfc = linkingNfc,
            onLinkNfcChange = onLinkNfcChange,
            onIssueQr = actions.onIssueQr,
            onDismiss = {
                onLinkNfcChange(false)
                showsCredential = false
            },
        )
    }
    choosing?.let { product ->
        StudentProductChoice(
            product = product,
            categoryName = catalog?.categories?.firstOrNull { it.id == product.categoryId }?.name ?: "Producto",
            onDismiss = { choosing = null },
            onAdd = { optionIds, quantity ->
                actions.onAddWithOptions(product.id, optionIds, quantity)
                choosing = null
            },
        )
    }
}

@Composable
private fun SpendableHeader(
    card: StudentCard,
    colors: OperationalColors,
    onCredential: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Puede gastar hoy",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                modifier = Modifier.weight(1f),
            )
            Text(
                "Credencial",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                modifier =
                    Modifier
                        .physicalPress(
                            scale = PhysicalPressScale.Small,
                            onClick = onCredential,
                        ).padding(6.dp),
            )
        }
        Text(
            moneyLabel(card.spendableToday),
            fontSize = 46.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1.4).sp,
            color = colors.textPrimary,
        )
        val limitLine =
            buildList {
                add("Saldo ${moneyLabel(card.balance)}")
                card.dailyLimit?.let { add("lleva ${moneyLabel(card.spentToday)} de ${moneyLabel(it)} hoy") }
            }.joinToString(" · ")
        Text(limitLine, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
    }
}

/** Arriba de la ficha, en coral: lo primero que ve la cajera antes de venderle. */
@Composable
internal fun StudentAllergyBanner(
    allergens: List<String>,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
) {
    val text = "Alérgico a " + allergens.joinToString(", ") { StudentAllergens.label(it) }.lowercase()
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(CounterCoral.copy(alpha = 0.12f))
                .border(1.dp, CounterCoral.copy(alpha = 0.5f), shape)
                .padding(14.dp)
                .clearAndSetSemantics { contentDescription = text },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.Outlined.WarningAmber,
            contentDescription = null,
            tint = CounterCoral,
            modifier = Modifier.size(18.dp),
        )
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
    }
}

@Composable
private fun SectionTitle(
    text: String,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        fontWeight = FontWeight.Black,
        fontSize = 19.sp,
        color = colors.textPrimary,
        modifier = modifier.padding(bottom = 4.dp),
    )
}

/** Con QR obligatorio y la ficha abierta por nombre, primero se escanea su credencial. */
@Composable
private fun TodayOrderRow(
    order: StudentTodayOrder,
    needsScan: Boolean,
    delivering: Boolean,
    busy: Boolean,
    colors: OperationalColors,
    onScan: () -> Unit,
    onDeliver: () -> Unit,
) {
    val haptics = rememberVaiinillaHaptics()
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "#${order.folio} · ${order.statusLabel}",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = colors.textPrimary,
                )
                Text(order.summary, fontSize = 12.sp, color = colors.textSecondary, maxLines = 2)
            }
            if (order.isReady) {
                Row(
                    modifier =
                        Modifier
                            .physicalPress(enabled = !busy, scale = PhysicalPressScale.Small) {
                                haptics.impact()
                                if (needsScan) onScan() else onDeliver()
                            }.height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.textPrimary)
                            .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (delivering) {
                        CircularProgressIndicator(
                            color = colors.background,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp),
                        )
                    } else if (needsScan) {
                        Icon(
                            Icons.Outlined.QrCodeScanner,
                            contentDescription = null,
                            tint = colors.background,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        if (needsScan) "Escanear" else "Entregar",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = colors.background,
                    )
                }
            } else {
                Text(
                    moneyLabel(order.total),
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = colors.textPrimary,
                )
            }
        }
        HorizontalDivider(color = colors.cardBorder)
    }
}

@Composable
private fun SellRow(
    product: Product,
    card: StudentCard,
    quantity: Int,
    choice: String?,
    colors: OperationalColors,
    onAdd: () -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    val haptics = rememberVaiinillaHaptics()
    val warnings = StudentPurchaseRules.allergenWarnings(product, card.allergens)
    val price = moneyLabel(product.counterPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.name,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = colors.textPrimary,
                    maxLines = 2,
                )
                choice?.let {
                    Text(
                        it,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary,
                        maxLines = 1,
                    )
                }
                if (warnings.isEmpty()) {
                    Text(price, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                } else {
                    Text(
                        "$price · tiene ${warnings.joinToString(", ") { StudentAllergens.label(it) }.lowercase()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CounterCoral,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            if (quantity == 0) {
                StepButton(Icons.Outlined.Add, "Agregar ${product.name}", colors) {
                    haptics.selection()
                    onAdd()
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StepButton(Icons.Outlined.Remove, "Quitar uno de ${product.name}", colors) {
                        haptics.selection()
                        onMinus()
                    }
                    Text(
                        "$quantity",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(min = 20.dp),
                    )
                    StepButton(Icons.Outlined.Add, "Otro ${product.name}", colors) {
                        haptics.selection()
                        onPlus()
                    }
                }
            }
        }
        HorizontalDivider(color = colors.cardBorder)
    }
}

@Composable
private fun StepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    colors: OperationalColors,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.cardInner)
                .border(1.dp, colors.cardBorder, CircleShape)
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun StudentChargeFooter(
    card: StudentCard,
    state: StudentCounterUiState,
    total: BigDecimal,
    canCharge: Boolean,
    colors: OperationalColors,
    actions: StudentCounterActions,
) {
    val haptics = rememberVaiinillaHaptics()
    val overBudget = total > card.spendableToday
    val phase = state.chargePhase
    LaunchedEffect(phase) {
        if (phase is ChargePhase.Charged || phase == ChargePhase.Queued) haptics.success()
    }
    val note =
        when {
            !canCharge -> "Abre la caja para cobrar."
            phase is ChargePhase.Failed -> phase.message
            phase is ChargePhase.Charged -> "Pedido #${phase.folio} a cocina. Le quedan ${moneyLabel(
                card.spendableToday,
            )} hoy."
            phase == ChargePhase.Queued -> "Sin conexión: quedó guardado y se cobra solo cuando vuelva la red."
            overBudget -> "Hoy solo puede gastar ${moneyLabel(card.spendableToday)}."
            card.requiresPin && state.itemCount > 0 && !state.pinReady -> "Su tutor le puso PIN: pídeselo."
            else -> null
        }
    val label =
        when (phase) {
            ChargePhase.Charging -> "Cobrando…"
            is ChargePhase.Charged -> "Cobrado"
            ChargePhase.Queued -> "Guardado"
            else -> if (state.itemCount > 0) "Cobrar ${moneyLabel(total)} de su saldo" else "Cobrar de su saldo"
        }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    colors.background,
                ).padding(horizontal = 20.dp)
                .padding(top = 10.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AnimatedVisibility(visible = note != null) {
            Text(
                note.orEmpty(),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (overBudget || phase is ChargePhase.Failed) CounterCoral else colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (card.requiresPin) {
            StudentPinField(
                pin = state.pin,
                firstName = card.firstName,
                colors = colors,
                onChange = actions.onPinChange,
            )
        }
        ActionPill(
            label = label,
            enabled = canCharge && state.itemCount > 0 && !overBudget && state.pinReady,
            loading = phase == ChargePhase.Charging,
            colors = colors,
            onClick = {
                haptics.impact()
                actions.onCharge(total)
            },
        )
    }
}

/** Lo dicta el alumno; va oculto y el lector de pantalla no lo lee en voz alta. */
@Composable
internal fun StudentPinField(
    pin: String,
    firstName: String,
    colors: OperationalColors,
    onChange: (String) -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(shape)
                .background(colors.cardInner)
                .border(1.dp, colors.cardBorder, shape)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Outlined.Lock,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(18.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            if (pin.isEmpty()) {
                Text("PIN de $firstName", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.textMuted)
            }
            BasicTextField(
                value = pin,
                onValueChange = { typed -> onChange(typed.filter { it in '0'..'9' }.take(4)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                textStyle =
                    TextStyle(
                        color = colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 6.sp,
                    ),
                cursorBrush = SolidColor(colors.textPrimary),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "PIN de $firstName" },
            )
        }
    }
}

/** La ficha del producto con opciones obligatorias: lo elegido viaja en `opcion_ids`. */
@Composable
private fun StudentProductChoice(
    product: Product,
    categoryName: String,
    onDismiss: () -> Unit,
    onAdd: (optionIds: List<Int>, quantity: Int) -> Unit,
) {
    val defaults =
        remember(product.id) {
            product.optionGroups.flatMapTo(
                linkedSetOf(),
            ) { group -> group.options.take(group.minimumSelections).map { it.id } }
        }
    var selected by remember(product.id) { mutableStateOf<Set<Int>>(defaults) }
    var quantity by remember(product.id) { mutableStateOf(1) }
    var error by remember(product.id) { mutableStateOf<String?>(null) }
    val unit = runCatching { Money.counterUnitPreview(product, selected) }.getOrDefault(product.counterPrice)
    val lineTotal = runCatching { Money.format(Money.parse(unit) * quantity.toBigDecimal()) }.getOrDefault(unit)
    ProductDetailSheet(
        product = product,
        categoryName = categoryName,
        selectedOptionIds = selected,
        defaultOptionIds = defaults,
        quantity = quantity,
        previewPrice = unit,
        previewTotal = lineTotal,
        canAdd = true,
        errorMessage = error,
        onDismiss = onDismiss,
        onToggleOption = { groupId, optionId ->
            val group = product.optionGroups.firstOrNull { it.id == groupId } ?: return@ProductDetailSheet
            val groupIds = group.options.mapTo(mutableSetOf()) { it.id }
            val next = selected.toMutableSet()
            val isSelected = optionId in next
            when {
                isSelected && group.minimumSelections == 0 -> next.remove(optionId)
                isSelected -> Unit
                group.maximumSelections == 1 -> {
                    next.removeAll(groupIds)
                    next.add(optionId)
                }
                next.count { it in groupIds } < group.maximumSelections -> next.add(optionId)
            }
            selected = next
            error = null
        },
        onClearOptionalGroup = { groupId ->
            val group = product.optionGroups.firstOrNull { it.id == groupId }
            if (group != null && group.minimumSelections == 0) selected = selected - group.options.map { it.id }.toSet()
        },
        onQuantityChange = { delta -> quantity = (quantity + delta).coerceIn(1, StudentPurchaseRules.MAX_PER_PRODUCT) },
        onAdd = {
            val valid = runCatching { ContractRules.validateSelections(product, selected) }
            if (valid.isFailure) {
                error = valid.exceptionOrNull()?.message ?: "Elige las opciones obligatorias."
            } else {
                onAdd(selected.toList(), quantity)
            }
        },
    )
}

/** La credencial impresa o la tarjeta: el QR se genera aquí y solo se ve esta vez. Reponerla apaga la anterior. */
@Composable
private fun StudentCredentialSheet(
    student: StudentCard,
    state: StudentCounterUiState,
    colors: OperationalColors,
    linkingNfc: Boolean,
    onLinkNfcChange: (Boolean) -> Unit,
    onIssueQr: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var confirmsReplace by remember { mutableStateOf(false) }
    AdminSheet(
        title = "Credencial de ${student.firstName}",
        colors = colors,
        busy = state.issuingCredential,
        onDismiss = onDismiss,
    ) {
        val printable = state.credential?.printableValue
        if (printable != null) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                VaiinillaQrCode(value = printable, qrSize = 240.dp)
            }
            Text(
                "Imprímela o tómale foto ahora: este código no se vuelve a mostrar.",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            ActionPill(
                label = "Compartir o imprimir",
                enabled = true,
                loading = false,
                primary = false,
                colors = colors,
                onClick = { shareQr(context, printable, "Credencial de ${student.name}") },
            )
        } else if (confirmsReplace) {
            Text(
                "Si ${student.name} ya tenía una, deja de servir.",
                fontSize = 14.sp,
                color = colors.textPrimary,
            )
            ActionPill(
                label = "Hacer credencial",
                enabled = true,
                loading = state.issuingCredential,
                colors = colors,
                onClick = onIssueQr,
            )
            ActionPill(
                label = "No",
                enabled = !state.issuingCredential,
                loading = false,
                primary = false,
                colors = colors,
            ) {
                confirmsReplace = false
            }
        } else {
            Text(
                "Con su credencial, Caja lo encuentra al escanearla. Si ya tenía una, la anterior deja de servir.",
                fontSize = 14.sp,
                color = colors.textSecondary,
            )
            ActionPill(label = "Hacer credencial QR", enabled = true, loading = false, colors = colors) {
                confirmsReplace =
                    true
            }
            ActionPill(
                label = if (linkingNfc) "Acerca la tarjeta al teléfono…" else "Ligar una tarjeta NFC",
                enabled = !state.issuingCredential,
                loading = false,
                primary = false,
                colors = colors,
            ) { onLinkNfcChange(!linkingNfc) }
        }
    }
}
