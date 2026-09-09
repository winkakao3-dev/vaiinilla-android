package com.vaiinilla.app.ui.components

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ToggleOff
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.vaiinilla.app.domain.model.OperationalRole
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

const val ASSISTANT_ANCHOR_CASHIER_ORDER_CARD = "comanda-card"
const val ASSISTANT_ANCHOR_QR_SCAN = "qr-scan-button"
const val ASSISTANT_ANCHOR_DELIVER = "entregar-button"
const val ASSISTANT_ANCHOR_CHANGE_MODE = "cambiar-modo-button"
const val ASSISTANT_ANCHOR_ADD_PRODUCT = "agregar-producto-button"
const val ASSISTANT_ANCHOR_OPEN_CASH = "abrir-caja-button"
const val ASSISTANT_ANCHOR_KITCHEN_CARD = "comanda-cocina-card"

fun assistantProductSwitchAnchor(productId: Int): String = "producto-switch:$productId"

fun assistantKitchenStartAnchor(orderId: String): String = "comanda-iniciar:$orderId"

fun assistantKitchenReadyAnchor(orderId: String): String = "comanda-lista:$orderId"

fun assistantQueueOrderAnchor(orderId: String): String = "comanda-fila:$orderId"

data class OperationalAssistantPalette(
    val background: Color,
    val surface: Color,
    val surface2: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val lime: Color,
    val limeInk: Color,
    val isDark: Boolean,
)

sealed interface AssistantAdvance {
    data object Tap : AssistantAdvance

    data class State(
        val key: String,
    ) : AssistantAdvance

    data class Timeout(
        val millis: Long,
    ) : AssistantAdvance
}

data class OperationalAssistantStep(
    val anchorId: String?,
    val text: String,
    val advance: AssistantAdvance,
)

enum class OperationalManualVisual {
    CASH_OPEN,
    ORDER_STATES,
    QR_READY,
    QR_SCAN,
    QR_TROUBLESHOOT,
    PRODUCT_LIST,
    PRODUCT_ADD,
    PRODUCT_PHOTO,
    PRODUCT_FIELDS,
    PRODUCT_STATION,
    KITCHEN_TICKET,
    KITCHEN_NEW,
    KITCHEN_PREPARING,
    KITCHEN_READY,
    KITCHEN_QUEUE,
    CHANGE_MODE,
}

data class OperationalManualDemoStep(
    val title: String,
    val body: String,
    val visual: OperationalManualVisual,
)

data class OperationalAssistantGuide(
    val id: String,
    val title: String,
    val role: OperationalRole,
    val section: String,
    val icon: ImageVector,
    val demoSteps: List<OperationalManualDemoStep>,
    val steps: List<OperationalAssistantStep> = emptyList(),
    val completionMessage: String,
    val available: Boolean = true,
    val unavailableReason: String? = null,
)

data class OperationalAssistantOnboardingItem(
    val id: String,
    val label: String,
    val guideId: String? = null,
)

internal data class AssistantAnchorEntry(
    val bounds: Rect,
    val bringIntoViewRequester: BringIntoViewRequester,
)

@Stable
class AssistantAnchorRegistry internal constructor() {
    private val entries = mutableStateMapOf<String, AssistantAnchorEntry>()
    var version by mutableIntStateOf(0)
        private set

    internal fun register(
        id: String,
        bounds: Rect,
        requester: BringIntoViewRequester,
    ) {
        val previous = entries[id]
        if (previous?.bounds == bounds && previous.bringIntoViewRequester === requester) return
        entries[id] = AssistantAnchorEntry(bounds, requester)
        version += 1
    }

    internal fun unregister(id: String) {
        if (entries.remove(id) != null) version += 1
    }

    internal fun entry(id: String): AssistantAnchorEntry? = entries[id]

    fun bounds(id: String): Rect? = entries[id]?.bounds
}

@Composable
fun rememberAssistantAnchorRegistry(): AssistantAnchorRegistry = remember { AssistantAnchorRegistry() }

fun Modifier.assistantAnchor(
    registry: AssistantAnchorRegistry,
    anchorId: String,
): Modifier =
    composed {
        val requester = remember(anchorId) { BringIntoViewRequester() }
        DisposableEffect(registry, anchorId) { onDispose { registry.unregister(anchorId) } }
        this
            .bringIntoViewRequester(requester)
            .onGloballyPositioned { coordinates ->
                registry.register(anchorId, coordinates.boundsInWindow(), requester)
            }
    }

internal class OperationalAssistantProgressStore(
    context: Context,
    private val workerKey: String,
) {
    private val preferences =
        context.applicationContext.getSharedPreferences("vaiinilla_operational_help_v1", Context.MODE_PRIVATE)

    private fun key(kind: String): String = "$workerKey:$kind"

    fun completedGuides(): Set<String> = preferences.getStringSet(key("guides"), emptySet()).orEmpty().toSet()

    fun completedOnboarding(): Set<String> = preferences.getStringSet(key("onboarding"), emptySet()).orEmpty().toSet()

    fun saveCompletedGuides(value: Set<String>) {
        preferences.edit { putStringSet(key("guides"), value) }
    }

    fun saveCompletedOnboarding(value: Set<String>) {
        preferences.edit { putStringSet(key("onboarding"), value) }
    }
}

@Stable
class OperationalAssistantController internal constructor(
    private val store: OperationalAssistantProgressStore,
) {
    var sheetOpen by mutableStateOf(false)
        private set
    var activeGuide by mutableStateOf<OperationalAssistantGuide?>(null)
        private set
    var stepIndex by mutableIntStateOf(0)
        private set
    var completionMessage by mutableStateOf<String?>(null)
        private set
    var completedGuideIds by mutableStateOf(store.completedGuides())
        private set
    var completedOnboardingIds by mutableStateOf(store.completedOnboarding())
        private set

    val currentStep: OperationalAssistantStep?
        get() = activeGuide?.steps?.getOrNull(stepIndex)

    fun open() {
        if (activeGuide == null) sheetOpen = true
    }

    fun closeSheet() {
        sheetOpen = false
    }

    fun startGuide(guide: OperationalAssistantGuide) {
        if (!guide.available || guide.steps.isEmpty()) return
        sheetOpen = false
        activeGuide = guide
        stepIndex = 0
        completionMessage = null
    }

    fun exitGuide() {
        activeGuide = null
        stepIndex = 0
    }

    fun onAnchorTapped(anchorId: String) {
        val step = currentStep ?: return
        if (step.anchorId == anchorId && step.advance is AssistantAdvance.Tap) advance()
    }

    fun onStateChanged(key: String) {
        val advance = currentStep?.advance as? AssistantAdvance.State ?: return
        if (advance.key == key) advance()
    }

    fun advanceTimeout() {
        if (currentStep?.advance is AssistantAdvance.Timeout) advance()
    }

    fun markOnboarding(id: String) {
        if (id in completedOnboardingIds) return
        completedOnboardingIds = completedOnboardingIds + id
        store.saveCompletedOnboarding(completedOnboardingIds)
    }

    fun clearCompletionMessage() {
        completionMessage = null
    }

    private fun advance() {
        val guide = activeGuide ?: return
        if (stepIndex + 1 < guide.steps.size) {
            stepIndex += 1
            return
        }
        completedGuideIds = completedGuideIds + guide.id
        store.saveCompletedGuides(completedGuideIds)
        when (guide.id) {
            "entregar-qr" -> markOnboarding("entregar-qr")
            "pausar-producto" -> markOnboarding("pausar-producto")
            "abrir-caja" -> markOnboarding("abrir-caja")
            "avanzar-comanda" -> markOnboarding("avanzar-comanda")
            "terminar-comanda" -> markOnboarding("terminar-comanda")
        }
        completionMessage = guide.completionMessage
        activeGuide = null
        stepIndex = 0
    }
}

@Composable
fun rememberOperationalAssistantController(
    workerKey: String,
    role: OperationalRole,
): OperationalAssistantController {
    val context = LocalContext.current
    val scopedKey = "$workerKey:${role.name.lowercase()}"
    return remember(scopedKey) {
        OperationalAssistantController(OperationalAssistantProgressStore(context, scopedKey))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationalAssistantHost(
    controller: OperationalAssistantController,
    registry: AssistantAnchorRegistry,
    guides: List<OperationalAssistantGuide>,
    manualTitle: String,
    palette: OperationalAssistantPalette,
    modifier: Modifier = Modifier,
    buttonFocusRequester: FocusRequester? = null,
) {
    val reduceMotion = reducedMotion()
    val currentGuide = controller.activeGuide
    val currentStep = controller.currentStep
    val registryVersion = registry.version

    LaunchedEffect(currentGuide?.id, controller.stepIndex) {
        val step = controller.currentStep ?: return@LaunchedEffect
        val anchorId = step.anchorId
        if (anchorId != null) {
            repeat(5) {
                val entry = registry.entry(anchorId)
                if (entry != null) {
                    runCatching { entry.bringIntoViewRequester.bringIntoView() }
                    delay(if (reduceMotion) 20 else 120)
                    return@repeat
                }
                delay(80)
            }
        }
        val timeout = step.advance as? AssistantAdvance.Timeout
        if (timeout != null) {
            delay(timeout.millis)
            if (controller.currentStep === step) controller.advanceTimeout()
        }
    }

    LaunchedEffect(currentGuide?.id, controller.stepIndex, registryVersion) {
        val step = controller.currentStep ?: return@LaunchedEffect
        val anchorId = step.anchorId ?: return@LaunchedEffect
        delay(420)
        if (controller.currentStep === step && registry.bounds(anchorId) == null) {
            controller.exitGuide()
        }
    }

    LaunchedEffect(controller.completionMessage) {
        if (controller.completionMessage != null) {
            delay(2_600)
            controller.clearCompletionMessage()
        }
    }

    BackHandler(enabled = currentGuide != null) { controller.exitGuide() }

    if (controller.sheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.74f
        ModalBottomSheet(
            onDismissRequest = {
                controller.closeSheet()
                buttonFocusRequester?.requestFocus()
            },
            sheetState = sheetState,
            containerColor = palette.surface,
            contentColor = palette.ink,
            scrimColor = Color.Black.copy(alpha = if (palette.isDark) 0.58f else 0.48f),
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            dragHandle = {
                Box(
                    Modifier
                        .padding(top = 10.dp, bottom = 4.dp)
                        .size(width = 38.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(palette.muted.copy(alpha = 0.34f)),
                )
            },
        ) {
            AssistantSheetContent(
                controller = controller,
                guides = guides,
                manualTitle = manualTitle,
                palette = palette,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxHeight)
                        .windowInsetsPadding(WindowInsets.navigationBars),
            )
        }
    }

    if (currentGuide != null && currentStep != null) {
        GuideOverlay(
            guide = currentGuide,
            step = currentStep,
            stepIndex = controller.stepIndex,
            anchorBounds = currentStep.anchorId?.let(registry::bounds),
            palette = palette,
            reduceMotion = reduceMotion,
            onExit = controller::exitGuide,
            modifier = modifier,
        )
    }

    AnimatedVisibility(
        visible = controller.completionMessage != null,
        enter = fadeIn(tween(180)) + slideInVertically(tween(240, easing = FastOutSlowInEasing)) { it / 2 },
        exit = fadeOut(tween(160)) + slideOutVertically(tween(200)) { it / 2 },
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 18.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = palette.lime,
            shadowElevation = 12.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = palette.limeInk,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = controller.completionMessage.orEmpty(),
                    color = palette.limeInk,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun AssistantSheetContent(
    controller: OperationalAssistantController,
    guides: List<OperationalAssistantGuide>,
    manualTitle: String,
    palette: OperationalAssistantPalette,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var selectedGuideId by remember { mutableStateOf<String?>(null) }
    var demoStepIndex by remember { mutableIntStateOf(0) }
    val selectedGuide = guides.firstOrNull { it.id == selectedGuideId }

    if (selectedGuide != null) {
        ManualGuideDetail(
            guide = selectedGuide,
            stepIndex = demoStepIndex,
            palette = palette,
            onBack = {
                selectedGuideId = null
                demoStepIndex = 0
            },
            onClose = controller::closeSheet,
            onPrevious = { if (demoStepIndex > 0) demoStepIndex -= 1 },
            onNext = {
                if (demoStepIndex + 1 < selectedGuide.demoSteps.size) {
                    demoStepIndex += 1
                } else {
                    selectedGuideId = null
                    demoStepIndex = 0
                }
            },
            onPractice = {
                if (selectedGuide.available && selectedGuide.steps.isNotEmpty()) {
                    controller.startGuide(selectedGuide)
                }
            },
            modifier = modifier,
        )
        return
    }

    val normalizedQuery = query.trim()
    val filteredGuides =
        guides.filter { guide ->
            normalizedQuery.isBlank() ||
                guide.title.contains(normalizedQuery, ignoreCase = true) ||
                guide.section.contains(normalizedQuery, ignoreCase = true)
        }
    val groupedGuides = filteredGuides.groupBy { it.section }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = manualTitle,
                    color = palette.ink,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Procedimientos completos, aunque no haya una operación activa.",
                    color = palette.muted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            IconButton(onClick = controller::closeSheet, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.Close, contentDescription = "Cerrar manual", tint = palette.muted)
            }
        }

        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 14.dp, bottom = 24.dp),
        ) {
            SearchField(value = query, onValueChange = { query = it }, palette = palette)
            if (groupedGuides.isEmpty()) {
                Text(
                    "No encontramos esa pregunta en este manual.",
                    color = palette.muted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 18.dp),
                )
            } else {
                groupedGuides.forEach { (section, sectionGuides) ->
                    GroupLabel(section, palette)
                    sectionGuides.forEach { guide ->
                        AssistantTaskRow(
                            guide = guide,
                            palette = palette,
                            onClick = {
                                selectedGuideId = guide.id
                                demoStepIndex = 0
                            },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun ManualGuideDetail(
    guide: OperationalAssistantGuide,
    stepIndex: Int,
    palette: OperationalAssistantPalette,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPractice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeIndex = stepIndex.coerceIn(0, (guide.demoSteps.size - 1).coerceAtLeast(0))
    val step = guide.demoSteps.getOrNull(safeIndex)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver al manual", tint = palette.ink)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(guide.section.uppercase(), color = palette.muted, fontSize = 10.sp, letterSpacing = 1.1.sp)
                Text(
                    guide.title,
                    color = palette.ink,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.Close, contentDescription = "Cerrar manual", tint = palette.muted)
            }
        }

        if (step != null) {
            key(safeIndex) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(180)) + slideInVertically(tween(240, easing = FastOutSlowInEasing)) { it / 7 },
                ) {
                    val animatedStep = guide.demoSteps[safeIndex]
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                        ManualDemoStage(animatedStep.visual, palette)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "PASO ${safeIndex + 1} DE ${guide.demoSteps.size}",
                            color = palette.lime,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp,
                        )
                        Text(
                            animatedStep.title,
                            color = palette.ink,
                            fontSize = 18.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            animatedStep.body,
                            color = palette.muted,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(top = 5.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            guide.demoSteps.forEachIndexed { index, _ ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(if (index <= safeIndex) palette.lime else palette.line),
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (safeIndex > 0) {
                Surface(
                    modifier = Modifier.weight(0.42f).height(48.dp).clickable(onClick = onPrevious),
                    shape = RoundedCornerShape(14.dp),
                    color = palette.surface2,
                    border = androidx.compose.foundation.BorderStroke(1.dp, palette.line),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Anterior", color = palette.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Surface(
                modifier = Modifier.weight(1f).height(48.dp).clickable(onClick = onNext),
                shape = RoundedCornerShape(14.dp),
                color = palette.lime,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        if (safeIndex + 1 < guide.demoSteps.size) "Siguiente" else "Volver al manual",
                        color = palette.limeInk,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        if (guide.available && guide.steps.isNotEmpty()) {
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .height(44.dp)
                        .clickable(onClick = onPractice),
                shape = RoundedCornerShape(13.dp),
                color = Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.line),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "Practicar en esta pantalla",
                        color = palette.ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        } else {
            Text(
                text = "Esta demostración no necesita datos reales y no modifica ninguna operación.",
                color = palette.muted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun ManualDemoStage(
    visual: OperationalManualVisual,
    palette: OperationalAssistantPalette,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(205.dp),
        shape = RoundedCornerShape(22.dp),
        color = palette.surface2,
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.line),
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            when (visual) {
                OperationalManualVisual.CASH_OPEN -> DemoCashOpen(palette)
                OperationalManualVisual.ORDER_STATES -> DemoOrderStates(palette)
                OperationalManualVisual.QR_READY -> DemoReadyOrder(palette)
                OperationalManualVisual.QR_SCAN -> DemoQrScanner(palette, error = false)
                OperationalManualVisual.QR_TROUBLESHOOT -> DemoQrScanner(palette, error = true)
                OperationalManualVisual.PRODUCT_LIST -> DemoProductList(palette)
                OperationalManualVisual.PRODUCT_ADD -> DemoProductAdd(palette)
                OperationalManualVisual.PRODUCT_PHOTO -> DemoProductForm(palette, stage = 0)
                OperationalManualVisual.PRODUCT_FIELDS -> DemoProductForm(palette, stage = 1)
                OperationalManualVisual.PRODUCT_STATION -> DemoProductForm(palette, stage = 2)
                OperationalManualVisual.KITCHEN_TICKET -> DemoKitchenTicket(palette, state = 0)
                OperationalManualVisual.KITCHEN_NEW -> DemoKitchenTicket(palette, state = 0)
                OperationalManualVisual.KITCHEN_PREPARING -> DemoKitchenTicket(palette, state = 1)
                OperationalManualVisual.KITCHEN_READY -> DemoKitchenTicket(palette, state = 2)
                OperationalManualVisual.KITCHEN_QUEUE -> DemoKitchenQueue(palette)
                OperationalManualVisual.CHANGE_MODE -> DemoChangeMode(palette)
            }
        }
    }
}

@Composable
private fun DemoCashOpen(palette: OperationalAssistantPalette) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Sesión de caja cerrada", color = palette.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("Ábrela para recibir pedidos y operar el turno.", color = palette.muted, fontSize = 12.sp)
        DemoButton("Abrir caja", palette, highlighted = true)
    }
}

@Composable
private fun DemoOrderStates(palette: OperationalAssistantPalette) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Estados del pedido", color = palette.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DemoPill("EN ESPERA", palette, false)
            DemoPill("LISTO", palette, true)
            DemoPill("ENTREGADO", palette, false)
        }
        Text("Caja entrega únicamente cuando el pedido está LISTO.", color = palette.muted, fontSize = 12.sp)
    }
}

@Composable
private fun DemoReadyOrder(palette: OperationalAssistantPalette) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Pedido #38", color = palette.ink, fontSize = 17.sp, fontWeight = FontWeight.Black)
            DemoPill("LISTO", palette, true)
        }
        Text("1 × Torta · Para llevar", color = palette.muted, fontSize = 12.sp)
        DemoButton("Escanear QR", palette, highlighted = true)
    }
}

@Composable
private fun DemoQrScanner(
    palette: OperationalAssistantPalette,
    error: Boolean,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF111311)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(112.dp)
                .border(2.dp, if (error) Color(0xFFFF8A78) else palette.lime, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.QrCodeScanner,
                null,
                tint = if (error) Color(0xFFFF8A78) else palette.lime,
                modifier = Modifier.size(48.dp),
            )
        }
        Text(
            if (error) "QR inválido · vuelve a enfocar" else "Coloca el QR del alumno dentro del marco",
            color = Color(0xFFF7F3E7),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
        )
    }
}

@Composable
private fun DemoProductList(palette: OperationalAssistantPalette) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Productos", color = palette.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        listOf("Waffles", "Burrito").forEachIndexed { index, name ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(palette.surface)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(name, color = palette.ink, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Box(
                    Modifier
                        .size(width = 44.dp, height = 24.dp)
                        .clip(CircleShape)
                        .background(if (index == 0) palette.lime else palette.line)
                        .padding(3.dp),
                    contentAlignment = if (index == 0) Alignment.CenterEnd else Alignment.CenterStart,
                ) { Box(Modifier.size(18.dp).clip(CircleShape).background(palette.surface)) }
            }
        }
    }
}

@Composable
private fun DemoProductAdd(palette: OperationalAssistantPalette) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Productos", color = palette.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("12 activos · 1 pausado", color = palette.muted, fontSize = 11.sp)
            }
            Box(
                Modifier.size(46.dp).clip(CircleShape).background(palette.lime),
                contentAlignment = Alignment.Center,
            ) { Text("+", color = palette.limeInk, fontSize = 26.sp, fontWeight = FontWeight.Bold) }
        }
        Text("El botón + abre el formulario de alta.", color = palette.muted, fontSize = 12.sp)
    }
}

@Composable
private fun DemoProductForm(
    palette: OperationalAssistantPalette,
    stage: Int,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("Nuevo producto", color = palette.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        when (stage) {
            0 -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier
                            .size(
                                58.dp,
                            ).clip(RoundedCornerShape(14.dp))
                            .background(palette.line.copy(alpha = 0.45f)),
                    )
                    DemoButton("Elegir de galería", palette, highlighted = true, modifier = Modifier.weight(1f))
                }
                Text("Usa una foto clara del producto.", color = palette.muted, fontSize = 12.sp)
            }
            1 -> {
                DemoField("Nombre", "Torta de pollo", palette)
                DemoField("Precio", "$65", palette)
            }
            else -> {
                Text("Estación de preparación", color = palette.muted, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DemoPill("Barra / Bebidas", palette, false)
                    DemoPill("Cocina caliente", palette, true)
                }
                DemoButton("Guardar producto", palette, highlighted = true)
            }
        }
    }
}

@Composable
private fun DemoKitchenTicket(
    palette: OperationalAssistantPalette,
    state: Int,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("#42", color = palette.ink, fontSize = 23.sp, fontWeight = FontWeight.Black)
            DemoPill(
                when (state) {
                    0 -> "NUEVA"
                    1 -> "PREPARANDO"
                    else -> "LISTA"
                },
                palette,
                state == 2,
            )
        }
        Text("2 × Waffles · 1 × Burrito", color = palette.ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text("Para llevar", color = palette.muted, fontSize = 11.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DemoButton("Preparando", palette, highlighted = state == 0, modifier = Modifier.weight(1f))
            DemoButton("Ya se preparó", palette, highlighted = state == 1, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun DemoKitchenQueue(palette: OperationalAssistantPalette) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Siguientes", color = palette.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        listOf("#43 · 2x Torta", "#44 · 1x Waffles").forEachIndexed { index, label ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(13.dp),
                color = if (index == 0) palette.lime.copy(alpha = 0.17f) else palette.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (index == 0) palette.lime else palette.line),
            ) {
                Text(
                    label,
                    color = palette.ink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}

@Composable
private fun DemoChangeMode(palette: OperationalAssistantPalette) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf("Caja", "Cocina", "Cliente").forEachIndexed { index, mode ->
            Surface(
                modifier = Modifier.weight(1f).height(74.dp),
                shape = RoundedCornerShape(16.dp),
                color = if (index == 0) palette.lime.copy(alpha = 0.18f) else palette.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (index == 0) palette.lime else palette.line),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(mode, color = palette.ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DemoPill(
    text: String,
    palette: OperationalAssistantPalette,
    highlighted: Boolean,
) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(if (highlighted) palette.lime else palette.surface)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text,
            color = if (highlighted) palette.limeInk else palette.muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DemoButton(
    text: String,
    palette: OperationalAssistantPalette,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (highlighted) palette.lime else palette.surface)
            .border(1.dp, if (highlighted) palette.lime else palette.line, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (highlighted) palette.limeInk else palette.ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DemoField(
    label: String,
    value: String,
    palette: OperationalAssistantPalette,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = palette.muted, fontSize = 10.sp)
        Box(
            Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(palette.surface)
                .border(1.dp, palette.line, RoundedCornerShape(11.dp))
                .padding(horizontal = 11.dp),
            contentAlignment = Alignment.CenterStart,
        ) { Text(value, color = palette.ink, fontSize = 12.sp) }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    palette: OperationalAssistantPalette,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(palette.surface2)
                .border(1.dp, palette.line, RoundedCornerShape(13.dp))
                .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = palette.muted, modifier = Modifier.size(19.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle =
                androidx.compose.ui.text
                    .TextStyle(color = palette.ink, fontSize = 14.sp),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (value.isEmpty()) Text("Buscar una pregunta", color = palette.muted, fontSize = 14.sp)
                inner()
            },
        )
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun GroupLabel(
    text: String,
    palette: OperationalAssistantPalette,
) {
    Text(
        text = text,
        color = palette.muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun AssistantTaskRow(
    guide: OperationalAssistantGuide,
    palette: OperationalAssistantPalette,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = palette.surface2,
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.line.copy(alpha = 0.55f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                guide.icon,
                contentDescription = null,
                tint = palette.ink,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    guide.title,
                    color = palette.ink,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "${guide.demoSteps.size} pasos · Ver demostración",
                    color = palette.lime,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
                tint = palette.muted.copy(alpha = 0.7f),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun FirstStepsContent(
    items: List<OperationalAssistantOnboardingItem>,
    completed: Set<String>,
    guides: List<OperationalAssistantGuide>,
    controller: OperationalAssistantController,
    palette: OperationalAssistantPalette,
) {
    val done = items.count { it.id in completed }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(palette.surface2),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(if (items.isEmpty()) 1f else done.toFloat() / items.size.toFloat())
                        .background(palette.lime),
                )
            }
            Text(
                "$done de ${items.size}",
                color = palette.muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.height(12.dp))
        items.forEach { item ->
            val isDone = item.id in completed
            val guide = item.guideId?.let { id -> guides.firstOrNull { it.id == id } }
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .then(
                                if (isDone) {
                                    Modifier.background(palette.lime)
                                } else {
                                    Modifier.border(1.dp, palette.line, RoundedCornerShape(7.dp))
                                },
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isDone) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = palette.limeInk,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Text(
                    item.label,
                    color = if (isDone) palette.muted else palette.ink,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                )
                if (!isDone && guide != null) {
                    Text(
                        "Mostrar",
                        color = palette.lime,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier =
                            Modifier
                                .clickable {
                                    controller.startGuide(
                                        guide,
                                    )
                                }.padding(horizontal = 4.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideOverlay(
    guide: OperationalAssistantGuide,
    step: OperationalAssistantStep,
    stepIndex: Int,
    anchorBounds: Rect?,
    palette: OperationalAssistantPalette,
    reduceMotion: Boolean,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var rootBounds by remember { mutableStateOf(Rect.Zero) }
    val density = LocalDensity.current
    val rawBounds =
        anchorBounds?.let { bounds ->
            val pad = with(density) { 8.dp.toPx() }
            Rect(
                left = bounds.left - rootBounds.left - pad,
                top = bounds.top - rootBounds.top - pad,
                right = bounds.right - rootBounds.left + pad,
                bottom = bounds.bottom - rootBounds.top + pad,
            )
        }
    val duration = if (reduceMotion) 0 else 280
    val left by animateFloatAsState(
        rawBounds?.left ?: 0f,
        tween(duration, easing = FastOutSlowInEasing),
        label = "assistant-hole-left",
    )
    val top by animateFloatAsState(
        rawBounds?.top ?: 0f,
        tween(duration, easing = FastOutSlowInEasing),
        label = "assistant-hole-top",
    )
    val right by animateFloatAsState(
        rawBounds?.right ?: 0f,
        tween(duration, easing = FastOutSlowInEasing),
        label = "assistant-hole-right",
    )
    val bottom by animateFloatAsState(
        rawBounds?.bottom ?: 0f,
        tween(duration, easing = FastOutSlowInEasing),
        label = "assistant-hole-bottom",
    )
    val pulseTransition = rememberInfiniteTransition(label = "assistant-guide-pulse")
    val ringAlpha by
        if (reduceMotion) {
            remember { mutableFloatStateOf(1f) }
        } else {
            pulseTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0.42f,
                animationSpec = infiniteRepeatable(tween(820), RepeatMode.Reverse),
                label = "assistant-guide-ring-alpha",
            )
        }

    Box(
        modifier = modifier.fillMaxSize().onGloballyPositioned { rootBounds = it.boundsInWindow() },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (rawBounds != null) {
                val hole = Rect(left, top, right, bottom)
                val radius = (hole.height / 2f).coerceIn(12.dp.toPx(), 20.dp.toPx())
                val path =
                    Path().apply {
                        fillType = PathFillType.EvenOdd
                        addRect(Rect(0f, 0f, size.width, size.height))
                        addRoundRect(RoundRect(hole, CornerRadius(radius, radius)))
                    }
                drawPath(path, Color.Black.copy(alpha = if (palette.isDark) 0.74f else 0.64f))
                drawRoundRect(
                    color = palette.lime.copy(alpha = ringAlpha),
                    topLeft =
                        androidx.compose.ui.geometry
                            .Offset(hole.left - 2.dp.toPx(), hole.top - 2.dp.toPx()),
                    size =
                        androidx.compose.ui.geometry
                            .Size(hole.width + 4.dp.toPx(), hole.height + 4.dp.toPx()),
                    cornerRadius = CornerRadius(radius + 2.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx()),
                )
            } else {
                drawRect(Color.Black.copy(alpha = if (palette.isDark) 0.52f else 0.44f))
            }
        }

        if (rawBounds != null && rootBounds.height > 0f) {
            val hole = Rect(left, top, right, bottom)
            val bubbleHeightPx = with(density) { 76.dp.toPx() }
            val bottomBarPx = with(density) { 82.dp.toPx() }
            val marginPx = with(density) { 14.dp.toPx() }
            val belowTop = hole.bottom + marginPx
            val placeAbove = belowTop + bubbleHeightPx > rootBounds.height - bottomBarPx
            val bubbleTop =
                if (placeAbove) {
                    (hole.top - bubbleHeightPx - marginPx).coerceAtLeast(with(density) { 18.dp.toPx() })
                } else {
                    belowTop
                }
            Surface(
                modifier =
                    Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .offset { IntOffset(0, bubbleTop.roundToInt()) }
                        .semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = "Paso ${stepIndex + 1} de ${guide.steps.size}. ${step.text}"
                        },
                shape = RoundedCornerShape(16.dp),
                color = palette.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.line),
                shadowElevation = 12.dp,
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {
                    Text(
                        "Paso ${stepIndex + 1} de ${guide.steps.size}",
                        color = palette.lime,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        step.text,
                        color = palette.ink,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(140)) + slideInVertically(tween(220, easing = FastOutSlowInEasing)) { it },
            exit = fadeOut(tween(120)) + slideOutVertically(tween(180)) { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = palette.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.line),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(horizontal = 20.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            guide.title,
                            color = palette.ink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text("Paso ${stepIndex + 1} de ${guide.steps.size}", color = palette.muted, fontSize = 12.sp)
                    }
                    Surface(
                        modifier = Modifier.height(42.dp).clickable(onClick = onExit),
                        shape = RoundedCornerShape(11.dp),
                        color = palette.surface2,
                        border = androidx.compose.foundation.BorderStroke(1.dp, palette.line),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "Salir",
                                color = palette.ink,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 17.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

fun cashierAssistantGuides(
    readyOrderId: String?,
    readyOrderFolio: Int?,
    firstProductId: Int?,
    cashSessionOpen: Boolean?,
    canCreateProduct: Boolean,
): List<OperationalAssistantGuide> {
    val hasReadyOrder = readyOrderId != null && readyOrderFolio != null
    return listOf(
        OperationalAssistantGuide(
            id = "abrir-caja",
            title = "¿Cómo abro la caja para empezar el turno?",
            role = OperationalRole.CASHIER,
            section = "Turno",
            icon = Icons.Outlined.Payments,
            demoSteps =
                listOf(
                    OperationalManualDemoStep(
                        "Busca el aviso de sesión cerrada",
                        "Cuando la caja está cerrada aparece un aviso al inicio de la pantalla. Sin una sesión abierta no debes iniciar la operación normal del turno.",
                        OperationalManualVisual.CASH_OPEN,
                    ),
                    OperationalManualDemoStep(
                        "Toca Abrir caja",
                        "Usa el botón verde Abrir caja. La aplicación registra la sesión operativa del turno.",
                        OperationalManualVisual.CASH_OPEN,
                    ),
                    OperationalManualDemoStep(
                        "Espera la confirmación",
                        "Cuando la sesión abre correctamente desaparece el aviso de caja cerrada y la operación queda habilitada.",
                        OperationalManualVisual.CASH_OPEN,
                    ),
                ),
            steps =
                listOf(
                    OperationalAssistantStep(
                        anchorId = ASSISTANT_ANCHOR_OPEN_CASH,
                        text = "Toca Abrir caja para habilitar la operación del turno.",
                        advance = AssistantAdvance.State("caja-abierta"),
                    ),
                ),
            completionMessage = "Caja abierta para el turno",
            available = cashSessionOpen == false,
            unavailableReason = "La caja ya está abierta o el estado todavía no está disponible.",
        ),
        OperationalAssistantGuide(
            id = "estados-pedido",
            title = "¿Qué significan EN ESPERA, LISTO y ENTREGADO?",
            role = OperationalRole.CASHIER,
            section = "Pedidos y entrega",
            icon = Icons.Outlined.Inventory2,
            demoSteps =
                listOf(
                    OperationalManualDemoStep(
                        "EN ESPERA todavía no se entrega",
                        "La comanda aún no está lista para recogerse. No intentes finalizar la entrega desde Caja.",
                        OperationalManualVisual.ORDER_STATES,
                    ),
                    OperationalManualDemoStep(
                        "LISTO significa que ya puede recogerse",
                        "Cuando Cocina termina, el pedido pasa a LISTO. Este es el momento en que Caja puede escanear el QR del alumno.",
                        OperationalManualVisual.ORDER_STATES,
                    ),
                    OperationalManualDemoStep(
                        "ENTREGADO cierra el flujo",
                        "Después de validar el QR, el backend marca el pedido como ENTREGADO. Ya no requiere otra acción de Caja.",
                        OperationalManualVisual.ORDER_STATES,
                    ),
                ),
            completionMessage = "Estados de Caja revisados",
            available = false,
        ),
        OperationalAssistantGuide(
            id = "entregar-qr",
            title = "¿Cómo entrego un pedido con el QR del alumno?",
            role = OperationalRole.CASHIER,
            section = "Pedidos y entrega",
            icon = Icons.Outlined.QrCodeScanner,
            demoSteps =
                listOf(
                    OperationalManualDemoStep(
                        "Confirma que el pedido diga LISTO",
                        "La entrega solo corresponde cuando Cocina ya terminó y el pedido aparece como LISTO.",
                        OperationalManualVisual.QR_READY,
                    ),
                    OperationalManualDemoStep(
                        "Toca Escanear QR",
                        "Abre el lector desde la tarjeta del pedido. No existe una entrega manual alternativa: el token del QR es obligatorio.",
                        OperationalManualVisual.QR_READY,
                    ),
                    OperationalManualDemoStep(
                        "Escanea el QR del pedido",
                        "Pide al alumno que muestre el QR de su pedido actual y mantenlo dentro del marco hasta que la app lo valide.",
                        OperationalManualVisual.QR_SCAN,
                    ),
                    OperationalManualDemoStep(
                        "Comprueba que quedó ENTREGADO",
                        "La aplicación envía el token al backend. Solo una validación correcta cambia el pedido a ENTREGADO.",
                        OperationalManualVisual.ORDER_STATES,
                    ),
                ),
            steps =
                listOf(
                    OperationalAssistantStep(
                        anchorId = ASSISTANT_ANCHOR_QR_SCAN,
                        text = "Toca Escanear QR y apunta la cámara al código del alumno.",
                        advance = AssistantAdvance.State("pedido-entregado:${readyOrderId.orEmpty()}"),
                    ),
                ),
            completionMessage = "Pedido entregado con QR",
            available = hasReadyOrder,
            unavailableReason = "Necesitas un pedido LISTO para practicar sobre la pantalla real.",
        ),
        OperationalAssistantGuide(
            id = "qr-no-lee",
            title = "¿Qué hago si el QR no se puede leer o es inválido?",
            role = OperationalRole.CASHIER,
            section = "Pedidos y entrega",
            icon = Icons.Outlined.QrCodeScanner,
            demoSteps =
                listOf(
                    OperationalManualDemoStep(
                        "Vuelve a encuadrar el código",
                        "Mantén el QR completo dentro del marco, evita reflejos y acerca o aleja el teléfono hasta que la cámara pueda enfocarlo.",
                        OperationalManualVisual.QR_SCAN,
                    ),
                    OperationalManualDemoStep(
                        "Si aparece inválido, no marques el pedido manualmente",
                        "Un QR inválido no debe saltarse. La entrega necesita el token correcto del pedido.",
                        OperationalManualVisual.QR_TROUBLESHOOT,
                    ),
                    OperationalManualDemoStep(
                        "Pide al alumno abrir su pedido actual",
                        "Que vuelva a la pantalla de su pedido y muestre el QR vigente. Después intenta el escaneo otra vez.",
                        OperationalManualVisual.QR_SCAN,
                    ),
                ),
            completionMessage = "Recuperación de QR revisada",
            available = false,
        ),
        OperationalAssistantGuide(
            id = "pausar-producto",
            title = "¿Cómo pauso o reactivo un producto del menú?",
            role = OperationalRole.CASHIER,
            section = "Productos",
            icon = Icons.Outlined.ToggleOff,
            demoSteps =
                listOf(
                    OperationalManualDemoStep(
                        "Busca el producto en la lista",
                        "Cada producto tiene un interruptor de disponibilidad a la derecha.",
                        OperationalManualVisual.PRODUCT_LIST,
                    ),
                    OperationalManualDemoStep(
                        "Apaga el interruptor si se agotó",
                        "Al pausarlo deja de estar disponible para nuevas compras. No elimina el producto del catálogo.",
                        OperationalManualVisual.PRODUCT_LIST,
                    ),
                    OperationalManualDemoStep(
                        "Vuélvelo a activar cuando regrese",
                        "Enciende el mismo interruptor para publicarlo otra vez.",
                        OperationalManualVisual.PRODUCT_LIST,
                    ),
                ),
            steps =
                listOf(
                    OperationalAssistantStep(
                        anchorId = firstProductId?.let(::assistantProductSwitchAnchor),
                        text = "Toca el interruptor del producto para cambiar su disponibilidad.",
                        advance = AssistantAdvance.Tap,
                    ),
                ),
            completionMessage = "Disponibilidad actualizada",
            available = firstProductId != null,
            unavailableReason = "Necesitas al menos un producto para practicar sobre la pantalla real.",
        ),
        OperationalAssistantGuide(
            id = "agregar-producto",
            title = "¿Cómo agrego un producto nuevo?",
            role = OperationalRole.CASHIER,
            section = "Productos",
            icon = Icons.Outlined.AddCircleOutline,
            demoSteps =
                listOf(
                    OperationalManualDemoStep(
                        "Toca + en Productos",
                        "El botón + abre el formulario de alta de producto.",
                        OperationalManualVisual.PRODUCT_ADD,
                    ),
                    OperationalManualDemoStep(
                        "Elige una foto de galería",
                        "Selecciona una imagen clara del producto. La vista previa debe aparecer antes de guardar.",
                        OperationalManualVisual.PRODUCT_PHOTO,
                    ),
                    OperationalManualDemoStep(
                        "Escribe nombre y precio",
                        "Usa un nombre reconocible y captura el precio mostrado al cliente.",
                        OperationalManualVisual.PRODUCT_FIELDS,
                    ),
                    OperationalManualDemoStep(
                        "Elige estación y guarda",
                        "Selecciona dónde se prepara el producto y termina con Guardar producto.",
                        OperationalManualVisual.PRODUCT_STATION,
                    ),
                ),
            steps =
                listOf(
                    OperationalAssistantStep(
                        anchorId = ASSISTANT_ANCHOR_ADD_PRODUCT,
                        text = "Toca + para abrir el formulario de nuevo producto.",
                        advance = AssistantAdvance.State("alta-producto-abierta"),
                    ),
                ),
            completionMessage = "Formulario de nuevo producto abierto",
            available = canCreateProduct,
            unavailableReason = "El catálogo debe estar disponible para practicar el alta real.",
        ),
        OperationalAssistantGuide(
            id = "estacion-producto",
            title = "¿Qué estación debo elegir al crear un producto?",
            role = OperationalRole.CASHIER,
            section = "Productos",
            icon = Icons.Outlined.AddCircleOutline,
            demoSteps =
                listOf(
                    OperationalManualDemoStep(
                        "Usa Barra / Bebidas para preparación de Caja",
                        "Selecciona Barra / Bebidas cuando el producto no necesita entrar a la cola de Cocina caliente.",
                        OperationalManualVisual.PRODUCT_STATION,
                    ),
                    OperationalManualDemoStep(
                        "Usa Cocina caliente cuando debe prepararlo Cocina",
                        "Esta opción hace que los productos correspondientes aparezcan en las comandas de Cocina.",
                        OperationalManualVisual.PRODUCT_STATION,
                    ),
                    OperationalManualDemoStep(
                        "Revisa la estación antes de guardar",
                        "La estación define quién recibe el trabajo. Confírmala junto con nombre, precio y foto antes de guardar.",
                        OperationalManualVisual.PRODUCT_STATION,
                    ),
                ),
            completionMessage = "Estaciones revisadas",
            available = false,
        ),
        OperationalAssistantGuide(
            id = "cambiar-modo",
            title = "¿Cómo regreso a los otros modos de trabajo?",
            role = OperationalRole.CASHIER,
            section = "Cuenta y modo",
            icon = Icons.Outlined.Inventory2,
            demoSteps =
                listOf(
                    OperationalManualDemoStep(
                        "Toca el chip de tu cuenta en el encabezado",
                        "El control con tus iniciales abre el cambio de modo cuando tu cuenta tiene más de un acceso.",
                        OperationalManualVisual.CHANGE_MODE,
                    ),
                    OperationalManualDemoStep(
                        "Elige el modo autorizado",
                        "Solo aparecerán los modos que el backend haya asignado a tu membresía.",
                        OperationalManualVisual.CHANGE_MODE,
                    ),
                ),
            completionMessage = "Cambio de modo revisado",
            available = false,
        ),
    )
}

fun kitchenAssistantGuides(
    orderId: String?,
    orderFolio: Int?,
    isPreparing: Boolean,
    isReady: Boolean,
    nextOrderId: String?,
    nextOrderFolio: Int?,
): List<OperationalAssistantGuide> {
    val hasOrder = orderId != null && orderFolio != null
    val hasNextOrder = nextOrderId != null && nextOrderFolio != null
    return listOf(
        OperationalAssistantGuide(
            id = "leer-comanda",
            title = "¿Cómo leo una comanda de Cocina?",
            role = OperationalRole.KITCHEN,
            section = "Comandas",
            icon = Icons.Outlined.Inventory2,
            demoSteps =
                listOf(
                    OperationalManualDemoStep(
                        "Empieza por folio y destino",
                        "El número identifica la comanda. Debajo se indica si es para llevar o para consumir en el establecimiento.",
                        OperationalManualVisual.KITCHEN_TICKET,
                    ),
                    OperationalManualDemoStep(
                        "Revisa cantidades y productos",
                        "Cada línea muestra cuántas unidades debes preparar y las opciones elegidas por el cliente.",
                        OperationalManualVisual.KITCHEN_TICKET,
                    ),
                    OperationalManualDemoStep(
                        "Mira el estado antes de tocar nada",
                        "NUEVA espera inicio, PREPARANDO ya está en trabajo y LISTA ya terminó la preparación.",
                        OperationalManualVisual.KITCHEN_TICKET,
                    ),
                ),
            completionMessage = "Lectura de comanda revisada",
            available = false,
        ),
        OperationalAssistantGuide(
            id = "estados-cocina",
            title = "¿Qué significan NUEVA, PREPARANDO y LISTA?",
            role = OperationalRole.KITCHEN,
            section = "Comandas",
            icon = Icons.Outlined.Inventory2,
            demoSteps =
                listOf(
                    OperationalManualDemoStep(
                        "NUEVA todavía no se ha iniciado",
                        "Es una comanda cobrada que Cocina ya puede comenzar.",
                        OperationalManualVisual.KITCHEN_NEW,
                    ),
                    OperationalManualDemoStep(
                        "PREPARANDO significa trabajo en curso",
                        "Usa este estado solamente cuando alguien realmente empezó a preparar la comanda.",
                        OperationalManualVisual.KITCHEN_PREPARING,
                    ),
                    OperationalManualDemoStep(
                        "LISTA significa preparación terminada",
                        "Cuando todo está terminado, marca Ya se preparó. Caja podrá ver el pedido listo para recogerse.",
                        OperationalManualVisual.KITCHEN_READY,
                    ),
                ),
            completionMessage = "Estados de Cocina revisados",
            available = false,
        ),
        OperationalAssistantGuide(
            id = "avanzar-comanda",
            title = "¿Cómo empiezo a preparar una comanda?",
            role = OperationalRole.KITCHEN,
            section = "Preparación",
            icon = Icons.Outlined.PlayCircleOutline,
            demoSteps =
                listOf(
                    OperationalManualDemoStep(
                        "Confirma que la comanda esté NUEVA",
                        "Antes de iniciar, revisa folio, productos y opciones.",
                        OperationalManualVisual.KITCHEN_NEW,
                    ),
                    OperationalManualDemoStep(
                        "Toca Preparando",
                        "Hazlo cuando el trabajo real haya comenzado. La app actualiza el estado para el resto del equipo.",
                        OperationalManualVisual.KITCHEN_NEW,
                    ),
                    OperationalManualDemoStep(
                        "Comprueba el estado PREPARANDO",
                        "La comanda debe mostrar PREPARANDO mientras se trabaja en ella.",
                        OperationalManualVisual.KITCHEN_PREPARING,
                    ),
                ),
            steps =
                listOf(
                    OperationalAssistantStep(
                        anchorId = orderId?.let(::assistantKitchenStartAnchor),
                        text = "Toca Preparando cuando empieces a trabajar esta comanda.",
                        advance = AssistantAdvance.State("comanda-preparando:${orderId.orEmpty()}"),
                    ),
                ),
            completionMessage = "Comanda en preparación",
            available = hasOrder && !isPreparing && !isReady,
            unavailableReason = "Necesitas una comanda NUEVA para practicar sobre la pantalla real.",
        ),
        OperationalAssistantGuide(
            id = "terminar-comanda",
            title = "¿Cómo marco una comanda como lista?",
            role = OperationalRole.KITCHEN,
            section = "Preparación",
            icon = Icons.Outlined.Inventory2,
            demoSteps =
                listOf(
                    OperationalManualDemoStep(
                        "Asegúrate de que todo esté terminado",
                        "No marques LISTA hasta que todos los productos de la comanda estén preparados.",
                        OperationalManualVisual.KITCHEN_PREPARING,
                    ),
                    OperationalManualDemoStep(
                        "Toca Ya se preparó",
                        "Ese botón cambia la comanda de PREPARANDO a LISTA.",
                        OperationalManualVisual.KITCHEN_PREPARING,
                    ),
                    OperationalManualDemoStep(
                        "Comprueba que aparezca LISTA",
                        "Caja recibirá el nuevo estado y podrá entregar el pedido cuando corresponda.",
                        OperationalManualVisual.KITCHEN_READY,
                    ),
                ),
            steps =
                listOf(
                    OperationalAssistantStep(
                        anchorId = orderId?.let(::assistantKitchenReadyAnchor),
                        text = "Toca Ya se preparó cuando todos los productos estén listos.",
                        advance = AssistantAdvance.State("comanda-lista:${orderId.orEmpty()}"),
                    ),
                ),
            completionMessage = "Comanda lista para entrega",
            available = hasOrder && isPreparing && !isReady,
            unavailableReason = "Necesitas una comanda PREPARANDO para practicar sobre la pantalla real.",
        ),
        OperationalAssistantGuide(
            id = "cambiar-comanda",
            title = "¿Cómo cambio a otra comanda de la fila?",
            role = OperationalRole.KITCHEN,
            section = "Fila de comandas",
            icon = Icons.Outlined.Inventory2,
            demoSteps =
                listOf(
                    OperationalManualDemoStep(
                        "Busca la sección Siguientes",
                        "Las otras comandas pendientes aparecen debajo de la comanda activa.",
                        OperationalManualVisual.KITCHEN_QUEUE,
                    ),
                    OperationalManualDemoStep(
                        "Toca la comanda que quieres revisar",
                        "Seleccionarla la convierte en la comanda mostrada arriba. Todavía no cambia su estado.",
                        OperationalManualVisual.KITCHEN_QUEUE,
                    ),
                    OperationalManualDemoStep(
                        "Revisa antes de actualizar su estado",
                        "Una vez activa, lee sus productos y usa Preparando o Ya se preparó según corresponda.",
                        OperationalManualVisual.KITCHEN_TICKET,
                    ),
                ),
            steps =
                listOf(
                    OperationalAssistantStep(
                        anchorId = nextOrderId?.let(::assistantQueueOrderAnchor),
                        text = "Toca una comanda de Siguientes para convertirla en la comanda activa.",
                        advance = AssistantAdvance.Tap,
                    ),
                ),
            completionMessage = "Comanda seleccionada",
            available = hasNextOrder,
            unavailableReason = "Necesitas otra comanda en la fila para practicar sobre la pantalla real.",
        ),
        OperationalAssistantGuide(
            id = "cambiar-modo-cocina",
            title = "¿Cómo regreso a los otros modos de trabajo?",
            role = OperationalRole.KITCHEN,
            section = "Cuenta y modo",
            icon = Icons.Outlined.Inventory2,
            demoSteps =
                listOf(
                    OperationalManualDemoStep(
                        "Toca el chip de tu cuenta en el encabezado",
                        "El control con tus iniciales permite volver a la selección de modos cuando tienes más de un acceso.",
                        OperationalManualVisual.CHANGE_MODE,
                    ),
                    OperationalManualDemoStep(
                        "Elige otro modo autorizado",
                        "La aplicación solo muestra roles realmente asignados por el backend.",
                        OperationalManualVisual.CHANGE_MODE,
                    ),
                ),
            completionMessage = "Cambio de modo revisado",
            available = false,
        ),
    )
}
