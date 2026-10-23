package com.vaiinilla.app.ui.parentmode

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ProductDetailSheet
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.animateSelectionColor
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.order.trayItemKind
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.ProductIllustration
import com.vaiinilla.app.ui.screens.alternarOpcionMostrador
import com.vaiinilla.app.ui.screens.rememberOperationalColors
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

    /** La misma semana con la misma llave: un reintento no aparta dos veces. */
    private val sendKey = newKey()

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
    fun restriction(product: Product): ChildRestriction? = controls?.guardianRestriction(product.id)

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
        return withContext(io) { account.reserve(plan.orders, sendKey) }.fold(
            onSuccess = {
                phase = Phase.Done(it)
                it
            },
            onFailure = {
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
                        "Apartar ${if (days == 1) "1 día" else "$days días"} · ${compactMoney(total)}"
                    },
                )
        }
    val footerNote =
        (model.phase as? WeekPlannerModel.Phase.Failed)?.message
            ?: missing?.let { "Le faltan ${compactMoney(it)} de saldo. Recárgale primero." }
            ?: if (days == 0) "Elige un día y lo que va a comer." else "Tiene ${compactMoney(balance)}. ${model.policy}"
    val ready = model.loadError == null && model.calendar != null && model.dates.isNotEmpty()

    ParentSheet(
        title = "Apartar comida",
        subtitle = "Para ${child.firstName} · se paga con su saldo",
        colors = colors,
        // Con algo elegido no se cierra de un jalón: se perdería la semana armada.
        busy = model.phase == WeekPlannerModel.Phase.Sending || days > 0 && model.phase !is WeekPlannerModel.Phase.Done,
        onDismiss = onDismiss,
        footer =
            if (ready) {
                {
                    CenteredNote(
                        footerNote,
                        if (missing != null ||
                            model.phase is WeekPlannerModel.Phase.Failed
                        ) {
                            ErrorCoral
                        } else {
                            colors.textSecondary
                        },
                    )
                    MorphingButton(
                        state = buttonState,
                        enabled = days > 0 && missing == null,
                        colors = colors,
                        testTag = "week-reserve",
                    ) {
                        scope.launch {
                            val reserved = model.reserve() ?: return@launch
                            onReserved(reserved)
                            delay(if (reduced) 300 else 1000)
                            onDismiss()
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
                                    .padding(6.dp),
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
                                ink
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
                SchoolCalendar.longLabel(date, model.today),
                color = colors.textPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 19.sp,
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
            Text("Hora de recogerlo", color = colors.textSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
        // Dos columnas con foto, como el menú. Va en filas y no en una cuadrícula perezosa porque la hoja ya
        // se desplaza entera.
        Column(
            modifier = Modifier.fillMaxWidth().testTag("week-grid"),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            products.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    pair.forEach { product ->
                        PlannerProductCard(model, product, date, colors, onInfo, Modifier.weight(1f))
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * La misma tarjeta del menú: foto cuadrada, nombre y precio. Tocarla abre su ficha y abajo se elige cuántos.
 * Lo elegido se marca en tinta alrededor de la foto; lo que no se le puede apartar se apaga y el motivo
 * ocupa el lugar del precio.
 */
@Composable
private fun PlannerProductCard(
    model: WeekPlannerModel,
    product: Product,
    date: String,
    colors: OperationalColors,
    onInfo: (Product) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberVaiinillaHaptics()
    val reduced = reducedMotion()
    val quantity = model.plan.quantity(product.id, date)
    val restriction = model.restriction(product)
    val kind = remember(product.name) { trayItemKind(product.name) }
    val ring by animateDpAsState(
        targetValue = if (quantity > 0) 3.dp else 0.dp,
        animationSpec = if (reduced) snap() else spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "week-ring",
    )
    val photoShape = RoundedCornerShape(24.dp)
    Column(modifier = modifier.testTag("week-product-${product.id}")) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .physicalPress(scale = PhysicalPressScale.ProductCard) { onInfo(product) }
                    .semantics(mergeDescendants = true) {
                        contentDescription =
                            restriction?.let { "Ver ${product.name}. ${it.label}" } ?: "Ver ${product.name}"
                        selected = quantity > 0
                    }.padding(bottom = 14.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                ProductImage(
                    imageUrl = product.imageUrl,
                    contentDescription = product.name,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .clip(photoShape)
                            .background(colors.cardInner)
                            .then(if (restriction != null) Modifier.dimmed() else Modifier),
                    placeholder = { m -> ProductIllustration(kind, m) },
                )
                if (ring > 0.dp) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .border(ring, colors.textPrimary, photoShape)
                            .testTag("week-picked-${product.id}"),
                    )
                }
                restriction?.let {
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(colors.background),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            restrictionIcon(it),
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }
            Text(
                product.name,
                color = colors.textPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
                lineHeight = 21.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 2.dp, top = 12.dp, end = 2.dp),
            )
            Text(
                restriction?.label ?: moneyLabel(product.digitalPrice),
                color = colors.textSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = if (restriction == null) 15.sp else 13.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                modifier = Modifier.padding(start = 2.dp, top = 4.dp, end = 2.dp),
            )
        }
        model.choiceSummary(product, date)?.let {
            Text(
                it,
                color = colors.textSecondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        when {
            // El motivo ya está bajo la foto; sin "Agregar" no hay a qué tocar.
            restriction != null -> Unit
            quantity == 0 ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .physicalPress(scale = PhysicalPressScale.Small) {
                                haptics.selection()
                                if (model.needsChoice(product, date)) onInfo(product) else model.step(product, 1)
                            }.clip(RoundedCornerShape(22.dp))
                            .background(colors.cardInner)
                            .border(1.dp, colors.cardBorder, RoundedCornerShape(22.dp))
                            .semantics { contentDescription = "Agregar ${product.name}" },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        "Agregar",
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            else ->
                Stepper(
                    quantity,
                    colors,
                    onMinus = { model.step(product, -1) },
                    onPlus = { model.step(product, 1) },
                    modifier = Modifier.fillMaxWidth(),
                )
        }
    }
}

private fun restrictionIcon(restriction: ChildRestriction): ImageVector =
    when (restriction.reason) {
        ChildRestriction.Reason.BLOCKED -> Icons.Outlined.Lock
        ChildRestriction.Reason.ALLERGEN -> Icons.Outlined.ErrorOutline
        ChildRestriction.Reason.ALLERGENS_UNKNOWN -> Icons.Outlined.HelpOutline
        ChildRestriction.Reason.SOLD_OUT -> Icons.Outlined.Block
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

@Composable
private fun Stepper(
    quantity: Int,
    colors: OperationalColors,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(colors.textPrimary)
                .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier.size(34.dp).physicalPress(scale = PhysicalPressScale.Small, onClick = onMinus).semantics {
                    contentDescription =
                        "Uno menos"
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Remove,
                contentDescription = null,
                tint = colors.background,
                modifier = Modifier.size(16.dp),
            )
        }
        Text("$quantity", color = colors.background, fontWeight = FontWeight.Black, fontSize = 15.sp)
        Box(
            modifier =
                Modifier.size(34.dp).physicalPress(scale = PhysicalPressScale.Small, onClick = onPlus).semantics {
                    contentDescription =
                        "Uno más"
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = null,
                tint = colors.background,
                modifier = Modifier.size(16.dp),
            )
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
