package com.vaiinilla.app.ui.parentmode

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.guardianship.ChildRestriction
import com.vaiinilla.app.domain.guardianship.Dependent
import com.vaiinilla.app.domain.guardianship.DependentAccountRepository
import com.vaiinilla.app.domain.guardianship.MenuCalendar
import com.vaiinilla.app.domain.guardianship.MyGuardianControls
import com.vaiinilla.app.domain.guardianship.ScheduledDayOrder
import com.vaiinilla.app.domain.guardianship.ScheduledOrderRules
import com.vaiinilla.app.domain.guardianship.SchoolCalendar
import com.vaiinilla.app.domain.guardianship.WeekPlan
import com.vaiinilla.app.domain.guardianship.WeekPlannerRules
import com.vaiinilla.app.domain.guardianship.compactMoney
import com.vaiinilla.app.domain.guardianship.toMoneyOrNull
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.ContractRules
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.repository.CatalogRepository
import com.vaiinilla.app.ui.components.ClientPillButton
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ProductDetailSheet
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.animateSelectionColor
import com.vaiinilla.app.ui.components.minTouchTarget
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.order.trayItemKind
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.ProductIllustration
import com.vaiinilla.app.ui.screens.alternarOpcionMostrador
import com.vaiinilla.app.ui.screens.rememberOperationalColors
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.LocalTime

/** Apartar la comida de varios días de un hijo. */
class WeekPlannerModel(
    private val account: DependentAccountRepository,
    private val catalogRepository: CatalogRepository,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val nowMinutes: () -> Int = { LocalTime.now().let { it.hour * 60 + it.minute } },
) {
    sealed interface Phase {
        data object Editing : Phase

        data object Sending : Phase

        data class Done(
            val days: Int,
        ) : Phase

        data class Failed(
            val message: String,
        ) : Phase
    }

    var calendar by mutableStateOf<MenuCalendar?>(null)
        private set
    var catalog by mutableStateOf<Catalog?>(null)
        private set
    var controls by mutableStateOf<MyGuardianControls?>(null)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set
    var plan by mutableStateOf(WeekPlan())
    var selectedDate by mutableStateOf<String?>(null)
    var phase by mutableStateOf<Phase>(Phase.Editing)
        private set

    /**
     * La misma semana con la misma llave: un reintento no aparta dos veces. Editar el plan después de un envío
     * dudoso es otra petición y estrena llave, o el servidor la rechazaría por reutilizada con otro contenido.
     */
    private var sendKey = newKey()
    private var sentOrders: List<ScheduledDayOrder>? = null

    suspend fun load() {
        coroutineScope {
            val menu = async(io) { account.menuCalendar() }
            val products = async(io) { catalogRepository.getCatalog() }
            val childControls = async(io) { account.controls() }
            val menuResult = menu.await()
            val catalogResult = products.await()
            val error = menuResult.exceptionOrNull() ?: catalogResult.exceptionOrNull()
            if (error != null) {
                loadError = error.toUserFacingMessage("No pudimos traer el menú.")
                return@coroutineScope
            }
            calendar = menuResult.getOrNull()
            catalog = catalogResult.getOrNull()
            // Sin los controles se puede apartar igual: el servidor sigue rechazando lo que tenga alérgenos.
            controls = childControls.await().getOrNull()
            loadError = null
            if (selectedDate == null) selectedDate = calendar?.let { WeekPlannerRules.firstSelectable(dates, it) }
        }
    }

    /** Los días que se pueden apartar (fechas de la escuela). */
    val dates: List<String> get() = calendar?.let { WeekPlannerRules.dates(it, nowMinutes()) }.orEmpty()

    val today: String get() = calendar?.schedule?.today.orEmpty()

    val slots: List<String> get() = calendar?.schedule?.slots.orEmpty()

    fun closure(date: String): String? = calendar?.closure(date)

    fun hasMenu(date: String): Boolean = calendar?.menu(date) != null

    val policy: String get() = ScheduledOrderRules.policy(calendar?.schedule?.cancelCutoff)

    fun products(date: String): List<Product> {
        val catalog = catalog ?: return emptyList()
        val calendar = calendar ?: return emptyList()
        val ids =
            WeekPlannerRules
                .productIds(
                    calendar = calendar,
                    date = date,
                    availableIds = catalog.products.filter(Product::available).map(Product::id),
                    allIds = catalog.products.map(Product::id),
                ).toSet()
        return catalog.products.filter { it.id in ids }
    }

    /** Las alergias del hijo frenan también lo que su papá le aparta. */
    fun restriction(product: Product): ChildRestriction? =
        controls?.guardianRestriction(product)
            ?: product.blockReason
                ?.takeIf { it == "alergeno" || it == "sin_revisar" }
                ?.let { ChildRestriction.fromServer(it, forGuardian = true) }

    fun categoryName(product: Product): String =
        catalog?.categories?.firstOrNull { it.id == product.categoryId }?.name ?: "Producto"

    private val prices: Map<Int, BigDecimal>
        get() =
            catalog
                ?.products
                .orEmpty()
                .mapNotNull { p ->
                    p.digitalPrice.toMoneyOrNull()?.let { p.id to it }
                }.toMap()

    private val extras: Map<Int, BigDecimal>
        get() =
            catalog
                ?.products
                .orEmpty()
                .flatMap { p -> p.optionGroups.flatMap { it.options } }
                .mapNotNull { o -> o.extraPrice.toMoneyOrNull()?.let { o.id to it } }
                .toMap()

    val total: BigDecimal get() = plan.total(prices, extras)

    /** Lo que pide elegir opciones (tamaño, sabor) se agrega desde su ficha, no con el "+". */
    fun needsChoice(
        product: Product,
        date: String,
    ): Boolean = product.optionGroups.any { it.minimumSelections > 0 } && plan.quantity(product.id, date) == 0

    fun choiceSummary(
        product: Product,
        date: String,
    ): String? {
        val picked = plan.optionIds(product.id, date).toSet()
        return product.optionGroups
            .flatMap { it.options }
            .filter { it.id in picked }
            .joinToString(" · ") { it.name }
            .ifEmpty { null }
    }

    fun step(
        product: Product,
        delta: Int,
    ) {
        val date = selectedDate ?: return
        if (delta > 0 && (restriction(product) != null || closure(date) != null)) return
        plan = plan.set(plan.quantity(product.id, date) + delta, product.id, date)
        if (phase !is Phase.Sending) phase = Phase.Editing
    }

    /** Desde la ficha: con otras opciones, reemplaza lo elegido ese día; con las mismas, suma. */
    fun add(
        product: Product,
        optionIds: List<Int>,
        quantity: Int,
    ) {
        val date = selectedDate ?: return
        if (restriction(product) != null || closure(date) != null) return
        val same = plan.optionIds(product.id, date) == optionIds.sorted()
        val current = if (same) plan.quantity(product.id, date) else 0
        plan = plan.set(current + quantity, product.id, date, optionIds)
        if (phase !is Phase.Sending) phase = Phase.Editing
    }

    fun toggleSlot(
        date: String,
        slot: String,
    ) {
        plan = plan.withSlot(date, if (plan.slots[date] == slot) null else slot)
    }

    /** Devuelve cuántos días quedaron apartados, o `null` si falló. */
    suspend fun reserve(): Int? {
        if (plan.plannedDates.isEmpty() || phase is Phase.Sending) return null
        phase = Phase.Sending
        val orders = plan.orders
        if (sentOrders != null && sentOrders != orders) sendKey = newKey()
        sentOrders = orders
        return withContext(io) { account.reserve(orders, sendKey) }.fold(
            onSuccess = {
                phase = Phase.Done(it)
                it
            },
            onFailure = {
                sendKey = keyAfterFailure(it, sendKey)
                phase = Phase.Failed(it.toUserFacingMessage("No se pudo apartar."))
                null
            },
        )
    }
}

/**
 * Apartar la comida de los próximos días: tira de días arriba con la pastilla en tinta, lo que se puede
 * apartar ese día abajo y un solo botón para apartarlo todo con su saldo.
 */
@Composable
internal fun WeekPlannerSheet(
    child: Dependent,
    account: DependentAccountRepository,
    catalog: CatalogRepository,
    balance: BigDecimal,
    onReserved: (Int) -> Unit,
    onDismiss: () -> Unit,
    /** Abre la recarga encima, sin perder lo elegido; sin ella, no hay botón de Recargar. */
    onReload: (() -> Unit)? = null,
) {
    val colors = rememberOperationalColors()
    val reduced = reducedMotion()
    val scope = rememberCoroutineScope()
    val model = remember { WeekPlannerModel(account, catalog) }
    var infoProduct by remember { mutableStateOf<Product?>(null) }
    LaunchedEffect(Unit) { model.load() }

    val days = model.plan.plannedDates.size
    val total = model.total
    val missing = if (total > balance) total - balance else null
    val buttonState =
        when (val phase = model.phase) {
            WeekPlannerModel.Phase.Sending -> MorphState.Working("Apartando…")
            is WeekPlannerModel.Phase.Done ->
                MorphState.Done(
                    if (phase.days ==
                        1
                    ) {
                        "Apartado 1 día"
                    } else {
                        "Apartados ${phase.days} días"
                    },
                )
            else ->
                MorphState.Idle(
                    if (days ==
                        0
                    ) {
                        "Apartar"
                    } else {
                        "Apartar ${if (days == 1) "1 día" else "$days días"}"
                    },
                )
        }
    val footerNote =
        (model.phase as? WeekPlannerModel.Phase.Failed)?.message
            ?: if (days == 0) "Elige un día y lo que va a comer." else null
    val ready = model.loadError == null && model.calendar != null && model.dates.isNotEmpty()

    ParentSheet(
        title = "Apartar para ${child.firstName}",
        subtitle = "Se paga con su saldo",
        colors = colors,
        // Con algo elegido no se cierra de un jalón: se perdería la semana armada.
        busy = model.phase == WeekPlannerModel.Phase.Sending || days > 0 && model.phase !is WeekPlannerModel.Phase.Done,
        onDismiss = onDismiss,
        footer =
            if (ready) {
                {
                    footerNote?.let {
                        CenteredNote(
                            it,
                            if (model.phase is WeekPlannerModel.Phase.Failed) ErrorCoral else colors.textSecondary,
                        )
                    }
                    if (days > 0) {
                        Row(modifier = Modifier.fillMaxWidth().testTag("week-summary"), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (days == 1) "1 día elegido" else "$days días elegidos",
                                color = colors.textSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f),
                            )
                            Text(compactMoney(total), color = colors.textPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (onReload != null) {
                            ClientPillButton(
                                text = "Recargar",
                                onClick = onReload,
                                primary = false,
                                modifier = Modifier.weight(1f).testTag("week-reload"),
                            )
                        }
                        MorphingButton(
                            state = buttonState,
                            enabled = days > 0 && missing == null,
                            colors = colors,
                            testTag = "week-reserve",
                            modifier = Modifier.weight(1.4f),
                        ) {
                            scope.launch {
                                val reserved = model.reserve() ?: return@launch
                                onReserved(reserved)
                                delay(if (reduced) 300 else 1000)
                                onDismiss()
                            }
                        }
                    }
                    if (days > 0 && buttonState is MorphState.Idle) {
                        Text(
                            "Cerrar sin apartar",
                            color = colors.textSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier =
                                Modifier
                                    .align(
                                        Alignment.CenterHorizontally,
                                    ).physicalPress(scale = PhysicalPressScale.Small, onClick = onDismiss)
                                    .padding(6.dp)
                                    .minTouchTarget(),
                        )
                    }
                }
            } else {
                null
            },
    ) {
        val loadError = model.loadError
        when {
            loadError != null ->
                ParentErrorCard(message = loadError, colors = colors, title = "Sin menú", onRetry = {
                    scope.launch { model.load() }
                })
            model.calendar == null -> SkeletonRows(5)
            model.dates.isEmpty() -> {
                Text(
                    "Hoy ya no se puede apartar",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                )
                Text(
                    "La escuela no abrió días para apartar o ya pasó la hora de corte.",
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                )
            }
            else -> {
                DayStrip(model, colors)
                model.selectedDate?.let { DayContent(model, it, colors) { product -> infoProduct = product } }
                AnimatedVisibility(
                    visible = missing != null,
                    enter = if (reduced) fadeIn(snap()) else fadeIn() + expandVertically(spring(dampingRatio = 0.86f, stiffness = 400f)),
                    exit = if (reduced) fadeOut(snap()) else fadeOut() + shrinkVertically(spring(stiffness = 500f)),
                ) {
                    val vColors = LocalVaiinillaColors.current
                    Text(
                        "${if (days == 1) "El día suma" else "Los $days días suman"} ${compactMoney(total)} y ${child.firstName} " +
                            "tiene ${compactMoney(balance)}. " +
                            if (onReload != null) "Recárgale sin salir de aquí." else "Recárgale primero.",
                        color = vColors.yolk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(vColors.yolk.copy(alpha = 0.12f))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("week-missing"),
                    )
                }
                if (days > 0) CenteredNote(model.policy, colors.textSecondary)
            }
        }
    }

    infoProduct?.let { product ->
        PlannerProductSheet(model = model, product = product, onDismiss = { infoProduct = null })
    }
}

@Composable
private fun DayStrip(
    model: WeekPlannerModel,
    colors: OperationalColors,
) {
    val haptics = rememberVaiinillaHaptics()
    val listState = rememberLazyListState()
    LaunchedEffect(model.selectedDate) {
        val index = model.dates.indexOf(model.selectedDate)
        if (index >= 0) listState.animateScrollToItem(maxOf(0, index - 1))
    }
    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.testTag("week-days"),
    ) {
        items(model.dates, key = { it }) { date ->
            val selected = date == model.selectedDate
            val count = model.plan.itemCount(date)
            val closed = model.closure(date) != null
            val background = animateSelectionColor(if (selected) colors.textPrimary else colors.cardInner, "day-bg")
            val ink = animateSelectionColor(if (selected) colors.background else colors.textPrimary, "day-ink")
            Column(
                modifier =
                    Modifier
                        .width(56.dp)
                        .physicalPress(scale = PhysicalPressScale.Small) {
                            haptics.selection()
                            model.selectedDate = date
                        }.clip(RoundedCornerShape(18.dp))
                        .background(background)
                        .border(
                            1.dp,
                            if (selected) colors.textPrimary else colors.cardBorder,
                            RoundedCornerShape(18.dp),
                        ).semantics {
                            role = Role.Tab
                            this.selected = selected
                            contentDescription =
                                SchoolCalendar.longLabel(date, model.today) +
                                when {
                                    closed -> ", sin servicio"
                                    count > 0 -> ", $count apartados"
                                    else -> ""
                                }
                        }.padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (date ==
                        model.today
                    ) {
                        "Hoy"
                    } else {
                        SchoolCalendar.weekdayShort(date)
                    },
                    color = ink.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
                Text(
                    SchoolCalendar.dayNumber(date),
                    color = ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 19.sp,
                    textDecoration = if (closed) TextDecoration.LineThrough else null,
                )
                Box(
                    modifier =
                        Modifier.padding(top = 3.dp).size(5.dp).clip(CircleShape).background(
                            if (count >
                                0
                            ) {
                                LocalVaiinillaColors.current.accent
                            } else {
                                background
                            },
                        ),
                )
            }
        }
    }
}

@Composable
private fun DayContent(
    model: WeekPlannerModel,
    date: String,
    colors: OperationalColors,
    onInfo: (Product) -> Unit,
) {
    val reduced = reducedMotion()
    val closure = model.closure(date)
    Column(
        modifier = Modifier.animateContentSize(if (reduced) snap() else spring(dampingRatio = 0.86f, stiffness = 400f)),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                SchoolCalendar.longLabel(date, model.today).uppercase(),
                color = colors.textSecondary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                letterSpacing = 2.2.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                when {
                    closure != null -> "Sin servicio"
                    model.hasMenu(date) -> "Menú del día"
                    else -> "Todo el menú"
                },
                color = colors.textSecondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        }
        if (closure != null) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(16.dp),
                        ).background(colors.cardInner)
                        .padding(14.dp)
                        .testTag("week-closed-day"),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    Icons.Outlined.Bedtime,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        "La escuela no da servicio este día",
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                    )
                    Text(closure, color = colors.textPrimary, fontSize = 14.sp)
                    Text(
                        "Si le habías apartado algo, ya regresó a su saldo.",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                    )
                }
            }
            return@Column
        }
        if (model.slots.isNotEmpty()) {
            Text("LO RECOGE A LAS", color = colors.textSecondary, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, letterSpacing = 2.2.sp)
            ChipFlow {
                model.slots.forEach { slot ->
                    ToggleChip(
                        slot.replace("-", "–"),
                        model.plan.slots[date] == slot,
                        colors,
                    ) { model.toggleSlot(date, slot) }
                }
            }
        }
        val products = model.products(date)
        if (products.isEmpty()) {
            Text(
                "No hay nada para apartar este día.",
                color = colors.textSecondary,
                fontSize = 14.sp,
            )
        }
        // En renglones y no en una lista perezosa porque la hoja ya se desplaza entera.
        Column(
            modifier = Modifier.fillMaxWidth().testTag("week-grid"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            products.forEach { product -> PlannerProductRow(model, product, date, colors, onInfo) }
        }
    }
}

/**
 * Un renglón del menú como el dibujo de Doop: foto, nombre, precio y un círculo que se llena al elegirlo. Tocar
 * la foto abre su ficha; lo que no se le puede apartar se apaga y el motivo ocupa el lugar del precio.
 */
@Composable
private fun PlannerProductRow(
    model: WeekPlannerModel,
    product: Product,
    date: String,
    colors: OperationalColors,
    onInfo: (Product) -> Unit,
) {
    val vColors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val reduced = reducedMotion()
    val quantity = model.plan.quantity(product.id, date)
    val restriction = model.restriction(product)
    val picked = quantity > 0
    val kind = remember(product.name) { trayItemKind(product.name) }
    val shape = RoundedCornerShape(18.dp)
    val border by animateColorAsState(
        if (picked) vColors.accent else vColors.accent.copy(alpha = 0f),
        if (reduced) snap() else spring(stiffness = 600f),
        label = "week-border",
    )
    val radio by animateDpAsState(
        if (picked) 7.dp else 2.dp,
        if (reduced) snap() else spring(dampingRatio = 0.55f, stiffness = 600f),
        label = "week-radio",
    )
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("week-product-${product.id}")
                .alpha(if (restriction != null) 0.5f else 1f)
                .physicalPress(enabled = restriction == null, scale = PhysicalPressScale.Small) {
                    haptics.selection()
                    when {
                        picked -> model.step(product, -quantity)
                        model.needsChoice(product, date) -> onInfo(product)
                        else -> model.step(product, 1)
                    }
                }.clip(shape)
                .background(colors.cardInner)
                .border(1.5.dp, border, shape)
                .semantics {
                    role = Role.Checkbox
                    selected = picked
                    contentDescription =
                        when {
                            restriction != null -> "${product.name}. ${restriction.label}"
                            picked -> "Quitar ${product.name}"
                            else -> "Agregar ${product.name}"
                        }
                }.padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProductImage(
            imageUrl = product.imageUrl,
            contentDescription = "Ver ${product.name}",
            modifier =
                Modifier
                    .size(62.dp)
                    .physicalPress(scale = PhysicalPressScale.Small) { onInfo(product) }
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.background)
                    .then(if (restriction != null) Modifier.dimmed() else Modifier),
            placeholder = { m -> ProductIllustration(kind, m) },
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                product.name,
                color = colors.textPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                lineHeight = 19.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                restriction?.label
                    ?: listOfNotNull(moneyLabel(product.digitalPrice), model.choiceSummary(product, date)).joinToString(" · "),
                color = if (restriction != null) vColors.errorText else colors.textSecondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            AnimatedVisibility(
                visible = picked,
                enter = if (reduced) fadeIn(snap()) else fadeIn() + expandVertically(spring(dampingRatio = 0.86f, stiffness = 450f)),
                exit = if (reduced) fadeOut(snap()) else fadeOut() + shrinkVertically(spring(stiffness = 600f)),
            ) {
                MiniStepper(
                    quantity = quantity,
                    onMinus = { model.step(product, -1) },
                    onPlus = { model.step(product, 1) },
                )
            }
        }
        if (restriction == null) {
            Box(
                modifier =
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .border(radio, if (picked) vColors.accent else vColors.line, CircleShape)
                        .then(if (picked) Modifier.testTag("week-picked-${product.id}") else Modifier),
            )
        }
    }
}

/** Cuántos ese día, sin salir del renglón: casi siempre es uno, pero se pueden apartar dos. */
@Composable
private fun MiniStepper(
    quantity: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    val vColors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    Row(
        modifier = Modifier.padding(top = 6.dp).height(32.dp).clip(RoundedCornerShape(percent = 50)).background(vColors.line),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(Triple(Icons.Outlined.Remove, "Uno menos", onMinus), Triple(Icons.Outlined.Add, "Uno más", onPlus))
            .forEachIndexed { index, (icon, label, action) ->
                if (index == 1) {
                    Text("$quantity", color = vColors.ink, fontWeight = FontWeight.Black, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 4.dp))
                }
                Box(
                    modifier =
                        Modifier
                            .size(32.dp)
                            .physicalPress(scale = PhysicalPressScale.Small) {
                                haptics.selection()
                                action()
                            }.semantics { contentDescription = label },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = vColors.ink2, modifier = Modifier.size(14.dp))
                }
            }
    }
}
/** En gris y a medias, como la tarjeta de iOS: se ve qué es, pero no invita a tocarlo. */
private fun Modifier.dimmed(): Modifier =
    drawWithCache {
        val paint =
            Paint().apply {
                colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                alpha = 0.45f
            }
        onDrawWithContent {
            drawIntoCanvas { canvas ->
                canvas.saveLayer(Rect(Offset.Zero, size), paint)
                drawContent()
                canvas.restore()
            }
        }
    }

/** La ficha del producto de la app; aquí sus opciones viajan en `opcion_ids` del día elegido. */
@Composable
private fun PlannerProductSheet(
    model: WeekPlannerModel,
    product: Product,
    onDismiss: () -> Unit,
) {
    val date = model.selectedDate.orEmpty()
    val defaults =
        remember(product.id) {
            model.plan
                .optionIds(product.id, date)
                .toSet()
                .ifEmpty {
                    product.optionGroups
                        .flatMap { g ->
                            g.options.take(g.minimumSelections).map { it.id }
                        }.toSet()
                }
        }
    var selected by remember(product.id) { mutableStateOf(defaults) }
    var quantity by remember(product.id) { mutableStateOf(1) }
    val restriction = model.restriction(product)
    val valid = runCatching { ContractRules.validateSelections(product, selected) }.isSuccess
    val unit = runCatching { Money.productUnitPreview(product, selected) }.getOrDefault(product.digitalPrice)
    val lineTotal = runCatching { Money.format(Money.parse(unit) * quantity.toBigDecimal()) }.getOrDefault(unit)
    ProductDetailSheet(
        product = product,
        categoryName = model.categoryName(product),
        selectedOptionIds = selected,
        defaultOptionIds = defaults,
        quantity = quantity,
        previewPrice = unit,
        previewTotal = lineTotal,
        canAdd = restriction == null && model.closure(date) == null && valid,
        errorMessage = restriction?.label,
        onDismiss = onDismiss,
        onToggleOption = { groupId, optionId ->
            product.optionGroups.firstOrNull { it.id == groupId }?.let {
                selected =
                    alternarOpcionMostrador(selected, it, optionId)
            }
        },
        onClearOptionalGroup = { groupId ->
            val ids =
                product.optionGroups
                    .firstOrNull { it.id == groupId }
                    ?.options
                    ?.map { it.id }
                    .orEmpty()
            selected = selected - ids.toSet()
        },
        onQuantityChange = { delta -> quantity = (quantity + delta).coerceIn(1, WeekPlan.MAX_PER_PRODUCT) },
        onAdd = {
            model.add(product, selected.toList(), quantity)
            onDismiss()
        },
    )
}
