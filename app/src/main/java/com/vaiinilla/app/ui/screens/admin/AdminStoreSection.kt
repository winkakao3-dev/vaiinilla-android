package com.vaiinilla.app.ui.screens.admin

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.vaiinilla.app.core.location.HomeAddressLocator
import com.vaiinilla.app.core.location.MapsLink
import com.vaiinilla.app.domain.admin.BusinessType
import com.vaiinilla.app.domain.admin.StoreFlow
import com.vaiinilla.app.domain.admin.StoreRules
import com.vaiinilla.app.domain.admin.StoreTemplate
import com.vaiinilla.app.domain.admin.StoreVideo
import com.vaiinilla.app.domain.admin.StripeStep
import com.vaiinilla.app.domain.admin.stripeStep
import com.vaiinilla.app.domain.model.AssistantMode
import com.vaiinilla.app.domain.model.OrderWindow
import com.vaiinilla.app.domain.model.SpaceDelivery
import com.vaiinilla.app.ui.admin.AdminStoreViewModel
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.screens.ChipButton
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.OperationalField
import com.vaiinilla.app.ui.screens.StoreImagesCard
import com.vaiinilla.app.ui.theme.Yolk
import kotlinx.coroutines.launch

/** Las páginas que abre la portada de Flujo de mi tienda (marcos 02a, 42, 43 y 44). */
private enum class StorePage { PAYMENTS, FLOW, PRESENCE, DELIVERY }

/**
 * Flujo de mi tienda como el lienzo de Doop: arriba la foto y el logo, después cómo funciona la tienda y los pagos
 * con tarjeta, que guardan al tocar; cada parte abre su página para el detalle y la entrega a domicilio va al final.
 */
@Composable
internal fun AdminStoreSection(
    viewModel: AdminStoreViewModel,
    colors: OperationalColors,
    readOnly: Boolean,
    onBack: () -> Unit,
    /** Abre el asistente de configuración (marco AS2); null lo esconde, como en solo consulta. */
    onOpenAssistant: (() -> Unit)? = null,
) {
    val state by viewModel.uiState
    // Al volver de Stripe (otra app) se vuelve a consultar el estado de la cuenta.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshStripe() }
    var page by rememberSaveable { mutableStateOf<StorePage?>(null) }
    val reduced = reducedMotion()
    AnimatedContent(
        targetState = page,
        transitionSpec = {
            if (reduced) {
                fadeIn(snap()) togetherWith fadeOut(snap())
            } else {
                val forward = targetState != null
                (
                    slideInHorizontally(
                        spring(stiffness = 500f),
                    ) { if (forward) it / 4 else -it / 4 } + fadeIn(spring())
                ) togetherWith
                    (
                        slideOutHorizontally(spring(stiffness = 500f)) { if (forward) -it / 4 else it / 4 } +
                            fadeOut(spring())
                    )
            }
        },
        label = "store-page",
    ) { current ->
        when (current) {
            null -> StoreCover(viewModel, colors, readOnly, onBack, onOpenAssistant) { page = it }
            StorePage.PAYMENTS -> StorePaymentsPage(viewModel, colors, readOnly) { page = null }
            StorePage.FLOW -> StoreFlowPage(viewModel, colors, readOnly) { page = null }
            StorePage.PRESENCE -> StorePresencePage(viewModel, colors, readOnly) { page = null }
            StorePage.DELIVERY -> StoreDeliveryPage(viewModel, colors, readOnly) { page = null }
        }
    }
    LaunchedEffect(state.config == null) { if (state.config == null && state.configError == null) viewModel.refresh() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StoreCover(
    viewModel: AdminStoreViewModel,
    colors: OperationalColors,
    readOnly: Boolean,
    onBack: () -> Unit,
    onOpenAssistant: (() -> Unit)?,
    onOpen: (StorePage) -> Unit,
) {
    val state by viewModel.uiState
    val enabled = !readOnly && !state.saving
    AdminSectionPage(
        title = "Flujo de mi tienda",
        colors = colors,
        notice = state.notice,
        noticeIsError = state.noticeIsError,
        onBack = onBack,
    ) {
        val config = state.config
        if (config == null) {
            item {
                val error = state.configError
                if (error !=
                    null
                ) {
                    AdminLoadError(error, colors, viewModel::refresh)
                } else {
                    AdminHint("Cargando tu tienda…", colors)
                }
            }
            return@AdminSectionPage
        }
        item {
            Text(
                "Pagos, logo, entrega y cómo se pide",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMuted,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        onOpenAssistant?.let { open ->
            item(key = "asistente") { AssistantEntryCard(colors, Modifier.adminRise(1), open) }
        }
        item { StoreHero(state.images, colors, Modifier.adminRise(1)) { onOpen(StorePage.PRESENCE) } }
        item {
            val flow = config.flow()
            AdminPanel(colors, Modifier.adminRise(2)) {
                PanelTitle("Cómo funciona tu tienda", colors) { onOpen(StorePage.FLOW) }
                StoreQuestion("Tipo de negocio", colors, Modifier.padding(top = 12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BusinessType.offered(config.availableTypes).forEach { type ->
                        AdminFilterChip(
                            label = type.label,
                            selected = flow.typeWire == type.wire,
                            colors = colors,
                            onClick = { if (enabled) saveNow(viewModel, flow, flow.copy(typeWire = type.wire)) },
                            height = 40.dp,
                        )
                    }
                }
                StoreQuestion("¿Cuándo pagan?", colors, Modifier.padding(top = 14.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AdminFilterChip(
                        label = "Al pedir",
                        selected = !flow.allowsPayAtEnd,
                        colors = colors,
                        onClick = { if (enabled) saveNow(viewModel, flow, flow.copy(allowsPayAtEnd = false)) },
                        height = 40.dp,
                    )
                    AdminFilterChip(
                        label = "Al final, con cuenta abierta",
                        selected = flow.allowsPayAtEnd,
                        colors = colors,
                        onClick = { if (enabled) saveNow(viewModel, flow, flow.copy(allowsPayAtEnd = true)) },
                        height = 40.dp,
                    )
                }
                val spaceDelivery = flow.spaceDelivery
                if (spaceDelivery != null) {
                    StoreQuestion("En mesa o lugar, ¿cómo les llega?", colors, Modifier.padding(top = 14.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SpaceDelivery.entries.forEach { option ->
                            AdminFilterChip(
                                label = spaceDeliveryLabel(option),
                                selected = spaceDelivery == option,
                                colors = colors,
                                onClick = { if (enabled) saveNow(viewModel, flow, flow.copy(spaceDelivery = option)) },
                                height = 40.dp,
                            )
                        }
                    }
                }
                val assistantMode = config.assistantMode
                if (assistantMode != null) {
                    StoreQuestion("¿Quién usa el asistente de IA?", colors, Modifier.padding(top = 14.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AssistantMode.entries.forEach { option ->
                            AdminFilterChip(
                                label = option.label,
                                selected = assistantMode == option,
                                colors = colors,
                                onClick = {
                                    if (enabled && option != assistantMode) {
                                        saveNow(viewModel, flow, flow.copy(assistantMode = option))
                                    }
                                },
                                height = 40.dp,
                            )
                        }
                    }
                    Text(
                        assistantModeHint(assistantMode),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textMuted,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
        item {
            AdminPanel(colors, Modifier.adminRise(3)) {
                PanelTitle("Pagos con tarjeta", colors) { onOpen(StorePage.PAYMENTS) }
                StripeQuick(viewModel, colors, enabled)
            }
        }
        if (showsDelivery(config)) {
            item {
                AdminPrimaryButton(
                    title = "Seguir: entrega a domicilio",
                    colors = colors,
                    modifier = Modifier.adminRise(4).padding(top = 6.dp),
                    icon = Icons.AutoMirrored.Outlined.ArrowForward,
                ) { onOpen(StorePage.DELIVERY) }
            }
        }
    }
}

/** Qué hace cada opción del asistente, en palabras del dueño. */
internal fun assistantModeHint(mode: AssistantMode): String =
    when (mode) {
        AssistantMode.BOTH -> "Te ayuda a configurar la tienda y tus clientes le preguntan qué pedir."
        AssistantMode.STAFF -> "Solo te ayuda a configurar la tienda; tus clientes no lo ven."
        AssistantMode.NONE -> "Nadie lo ve: configuras paso a paso y los clientes piden desde el menú."
    }

/** Las opciones de entrega en mesa o lugar, como las lee el dueño. */
internal fun spaceDeliveryLabel(option: SpaceDelivery): String =
    when (option) {
        SpaceDelivery.TABLE -> "Te lo llevamos"
        SpaceDelivery.BAR -> "Pasa por él"
        SpaceDelivery.BOTH -> "Las dos"
    }

/** Los chips de la portada guardan al tocarlos; lo demás del flujo se edita en su página. */
private fun saveNow(
    viewModel: AdminStoreViewModel,
    saved: StoreFlow,
    next: StoreFlow,
) {
    if (next == saved) return
    val problem = StoreRules.problem(next)
    if (problem != null) viewModel.notify(problem, true) else viewModel.saveFlow(next)
}

/** La entrega a domicilio y los encargos salen para la repostería y para quien ya reparte. */
private fun showsDelivery(config: com.vaiinilla.app.domain.admin.StoreConfig): Boolean {
    val bakery = config.type == BusinessType.HOME_BAKERY
    if (config.homeDelivery?.let { it.enabled || bakery } == true) return true
    return config.preorders?.let { bakery || it.requiresAcceptance || it.depositPercent != null } == true
}

/** La foto del lugar con el logo encima; abre la página de logo, videos y directorio. */
@Composable
private fun StoreHero(
    images: com.vaiinilla.app.domain.repository.StoreImages,
    colors: OperationalColors,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val detail =
        when {
            images.logoUrl != null && images.photoUrl != null -> "Logo y foto listos"
            images.logoUrl != null -> "Falta la foto del lugar"
            images.photoUrl != null -> "Falta tu logo"
            else -> "Sube tu logo y una foto"
        }
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .fillMaxWidth()
            .physicalPress(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "Logo, foto, videos y directorio. $detail" }
            .clip(shape)
            .background(colors.cardBackground),
    ) {
        Box(Modifier.fillMaxWidth().height(96.dp)) {
            val photo = images.photoUrl
            if (photo != null) {
                ProductImage(imageUrl = photo, contentDescription = "", modifier = Modifier.fillMaxSize())
            } else {
                Box(
                    Modifier.fillMaxSize().background(colors.textPrimary.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Image,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            Row(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .height(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    Icons.Outlined.PhotoCamera,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
                Text("Foto", fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 14.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val logo = images.logoUrl
            val circle =
                Modifier
                    .offset(y = (-26).dp)
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(colors.background)
                    .border(4.dp, colors.cardBackground, CircleShape)
            if (logo != null) {
                ProductImage(imageUrl = logo, contentDescription = "", modifier = circle)
            } else {
                Box(circle, contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Storefront,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Column(Modifier.weight(1f).offset(y = (-6).dp)) {
                Text("Tu negocio", fontSize = 16.sp, fontWeight = FontWeight.Black, color = colors.textPrimary)
                Text(detail, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = colors.textMuted)
            }
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.offset(y = (-10).dp),
            )
        }
    }
}

@Composable
private fun PanelTitle(
    title: String,
    colors: OperationalColors,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .physicalPress(
                scale = PhysicalPressScale.Small,
                onClick = onClick,
            ).semantics { heading() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Text("Ver todo", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textMuted)
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.textMuted)
    }
}

@Composable
private fun StoreQuestion(
    text: String,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        fontSize = 14.sp,
        fontWeight = FontWeight.ExtraBold,
        color = colors.textPrimary,
        modifier = modifier.padding(bottom = 8.dp),
    )
}

/** Cómo está la cuenta de Stripe, en una franja de color: conectada (lima), pendiente (yema) o sin conectar. */

/** La misma información en un renglón, para la tarjeta del marco 02a. */
@Composable
private fun StripeCompactLine(
    step: StripeStep,
    colors: OperationalColors,
) {
    val lime = if (colors.isDark) colors.accentLime else colors.accentInk
    val (icon, text, tint) =
        when (step) {
            StripeStep.READY -> Triple(Icons.Outlined.Verified, "Cuenta de Stripe conectada", lime)
            StripeStep.FINISH -> Triple(Icons.Outlined.HourglassTop, "Stripe está revisando tus datos", Yolk)
            StripeStep.CONNECT -> Triple(Icons.Outlined.CreditCard, "Sin conectar", colors.textMuted)
        }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(tint.copy(alpha = 0.14f))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Text(text, fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold, color = tint)
    }
}

/** Sin conectar, en revisión o lista, con su ícono y lo que significa (marco 42). */
@Composable
private fun StripeStatusLine(
    step: StripeStep,
    colors: OperationalColors,
    action: (@Composable () -> Unit)? = null,
) {
    val lime = if (colors.isDark) colors.accentLime else colors.accentInk

    data class Look(
        val icon: ImageVector,
        val title: String,
        val detail: String,
        val tint: Color,
    )
    val look =
        when (step) {
            StripeStep.READY ->
                Look(
                    Icons.Outlined.Verified,
                    "Cuenta de Stripe conectada",
                    "Ya puedes cobrar con tarjeta.",
                    lime,
                )
            StripeStep.FINISH ->
                Look(
                    Icons.Outlined.HourglassTop,
                    "Pendiente",
                    "Stripe está revisando tus datos. Puede tardar unos minutos.",
                    Yolk,
                )
            StripeStep.CONNECT ->
                Look(
                    Icons.Outlined.CreditCard,
                    "Sin conectar",
                    "Conecta tu cuenta de Stripe para que tus clientes paguen con tarjeta en la app.",
                    colors.textPrimary,
                )
        }
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (step == StripeStep.CONNECT) colors.cardBackground else look.tint.copy(alpha = 0.12f))
            .then(
                if (step ==
                    StripeStep.CONNECT
                ) {
                    Modifier
                } else {
                    Modifier.border(1.dp, look.tint.copy(alpha = 0.4f), shape)
                },
            ).padding(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(look.tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) { Icon(look.icon, contentDescription = null, tint = look.tint, modifier = Modifier.size(20.dp)) }
            Column(Modifier.weight(1f)) {
                Text(look.title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
                Text(look.detail, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textMuted)
            }
        }
        action?.let {
            Spacer(Modifier.height(12.dp))
            it()
        }
    }
}

@Composable
private fun StripeQuick(
    viewModel: AdminStoreViewModel,
    colors: OperationalColors,
    enabled: Boolean,
) {
    val state by viewModel.uiState
    val context = LocalContext.current
    if (!state.stripeLoaded) {
        Box(Modifier.padding(top = 10.dp)) { AdminHint("Consultando tu cuenta de Stripe…", colors) }
        return
    }
    val step = stripeStep(state.stripe)
    Box(Modifier.padding(top = 12.dp)) { StripeCompactLine(step, colors) }
    when (step) {
        StripeStep.CONNECT ->
            AdminPrimaryButton(
                title = "Conectar con Stripe",
                colors = colors,
                modifier = Modifier.padding(top = 12.dp),
                busy = state.saving,
                enabled = enabled,
            ) { viewModel.openStripe { openLink(context, it) } }
        StripeStep.FINISH ->
            AdminSecondaryButton(
                title = "Completar en Stripe",
                colors = colors,
                modifier = Modifier.padding(top = 12.dp),
                outlined = true,
                enabled = enabled,
            ) { viewModel.openStripe { openLink(context, it) } }
        StripeStep.READY -> {
            val setup = state.stripe ?: return
            StoreQuestion("¿Quién paga la comisión de la tarjeta?", colors, Modifier.padding(top = 14.dp))
            FeeChips(setup.passFeeToCustomer, colors, enabled) { viewModel.setFeePaidByCustomer(it) }
        }
    }
}

@Composable
private fun FeeChips(
    customerPays: Boolean,
    colors: OperationalColors,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    AdminSegmented(
        options = listOf(true to "El cliente", false to "Yo"),
        selected = customerPays,
        colors = colors,
        enabled = enabled,
        onSelect = { if (it != customerPays) onChange(it) },
    )
}

/** Pagos con tarjeta (marco 42): el estado de Stripe, si se aceptan y quién paga la comisión. */
@Composable
private fun StorePaymentsPage(
    viewModel: AdminStoreViewModel,
    colors: OperationalColors,
    readOnly: Boolean,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState
    val context = LocalContext.current
    val enabled = !readOnly && !state.saving
    AdminSectionPage(
        title = "Pagos con tarjeta",
        eyebrow = "Flujo de mi tienda",
        colors = colors,
        notice = state.notice,
        noticeIsError = state.noticeIsError,
        onBack = onBack,
    ) {
        if (!state.stripeLoaded) {
            item { AdminHint("Consultando tu cuenta de Stripe…", colors) }
            return@AdminSectionPage
        }
        val step = stripeStep(state.stripe)
        item {
            Box(Modifier.adminRise(1)) {
                StripeStatusLine(
                    step,
                    colors,
                    action =
                        if (step == StripeStep.READY) {
                            null
                        } else {
                            {
                                AdminPrimaryButton(
                                    title =
                                        if (step ==
                                            StripeStep.CONNECT
                                        ) {
                                            "Conectar con Stripe"
                                        } else {
                                            "Completar en Stripe"
                                        },
                                    colors = colors,
                                    busy = state.saving,
                                    enabled = enabled,
                                ) { viewModel.openStripe { openLink(context, it) } }
                            }
                        },
                )
            }
        }
        val setup = state.stripe
        if (step == StripeStep.READY && setup != null) {
            item {
                AdminPanel(
                    colors,
                    Modifier.adminRise(2),
                    padding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                ) {
                    AdminToggleRow(
                        title = "Aceptar pagos con tarjeta",
                        checked = setup.enabled,
                        colors = colors,
                        onChange = { viewModel.setCardPayments(it) },
                        detail = "Tus clientes eligen tarjeta al pagar en la app.",
                        enabled = enabled,
                    )
                }
            }
            item {
                Column(Modifier.adminRise(3).padding(horizontal = 2.dp)) {
                    StoreQuestion("¿Quién paga la comisión de la tarjeta?", colors, Modifier.padding(top = 8.dp))
                    FeeChips(setup.passFeeToCustomer, colors, enabled) { viewModel.setFeePaidByCustomer(it) }
                    Box(Modifier.padding(top = 8.dp)) {
                        AdminHint(
                            if (setup.passFeeToCustomer) {
                                "Con tarjeta se suma la comisión al precio; con efectivo o saldo se cobra el precio normal."
                            } else {
                                "El mismo precio con efectivo, saldo o tarjeta. La comisión sale de lo que cobras."
                            },
                            colors,
                        )
                    }
                }
            }
        }
    }
}

/** Cómo funciona tu tienda (marco 43): plantilla, tipo, cobro, entrega, saldo, gracia y franjas. */
@Composable
private fun StoreFlowPage(
    viewModel: AdminStoreViewModel,
    colors: OperationalColors,
    readOnly: Boolean,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState
    AdminSectionPage(
        title = "Cómo funciona tu tienda",
        eyebrow = "Flujo de mi tienda",
        colors = colors,
        notice = state.notice,
        noticeIsError = state.noticeIsError,
        onBack = onBack,
    ) {
        val config = state.config ?: return@AdminSectionPage
        item {
            Box(Modifier.adminRise(1)) {
                FlowForm(
                    config.flow(),
                    config.availableTypes,
                    colors,
                    !readOnly && !state.saving,
                    readOnly,
                    state.saving,
                    viewModel,
                )
            }
        }
    }
}

/** Tu local (marco 44): logo y foto, videos y cómo te encuentran. */
@Composable
private fun StorePresencePage(
    viewModel: AdminStoreViewModel,
    colors: OperationalColors,
    readOnly: Boolean,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState
    val enabled = !readOnly && !state.saving
    val notify: (String, Boolean) -> Unit = viewModel::notify
    var editingImages by remember { mutableStateOf(false) }
    var editingVideos by remember { mutableStateOf(false) }
    AdminSectionPage(
        title = "Tu local",
        eyebrow = "Flujo de mi tienda",
        colors = colors,
        notice = state.notice,
        noticeIsError = state.noticeIsError,
        onBack = onBack,
    ) {
        val config = state.config ?: return@AdminSectionPage
        item { AdminH2("Logo y foto", colors) }
        item {
            PresenceHero(state.images, enabled, colors) { editingImages = true }
        }
        item { AdminH2("Videos del local", colors) }
        item { VideoStrip(config.videos, enabled, colors) { editingVideos = true } }
        item { AdminH2("Cómo te encuentran", colors) }
        item {
            AdminPanel(colors, padding = PaddingValues(horizontal = 16.dp, vertical = 2.dp)) {
                AdminToggleRow(
                    title = "Aparecer en el directorio",
                    checked = config.visibleInDirectory,
                    colors = colors,
                    onChange = { if (it != config.visibleInDirectory) viewModel.setVisibleInDirectory(it) },
                    detail = "Te encuentran en «Dónde pedir»",
                    enabled = enabled,
                )
                AdminRowDivider(colors)
                AdminToggleRow(
                    title = "Cocina prepara antes de cobrar",
                    checked = config.kitchenPreparesUnpaid,
                    colors = colors,
                    onChange = { viewModel.saveFlow(config.flow().copy(kitchenPreparesUnpaid = it)) },
                    detail = "Los pedidos por cobrar entran a cocina",
                    enabled = enabled,
                )
                AdminRowDivider(colors)
                AdminToggleRow(
                    title = "Es una escuela",
                    checked = config.isSchool,
                    colors = colors,
                    onChange = { if (it != config.isSchool) viewModel.setSchoolMode(it) },
                    detail = "Activa alumnos, tutores y menú del día",
                    enabled = enabled,
                )
            }
        }
        config.storeUrl?.let { url ->
            item {
                AdminPanel(colors, Modifier.padding(top = 10.dp)) {
                    AdminHint("El QR de tu tienda abre tu menú directo.", colors)
                    Box(Modifier.padding(top = 10.dp)) {
                        // Con un mensaje listo, el QR se pega tal cual en WhatsApp o en el estado (CASA-10).
                        AdminQrBlock(
                            url = url,
                            name = "tienda",
                            colors = colors,
                            onNotice = notify,
                            shareMessage = "Haz tu pedido aquí: $url",
                        )
                    }
                }
            }
        }
    }
    if (editingImages) {
        AdminSheet(title = "Logo y foto", colors = colors, busy = false, onDismiss = { editingImages = false }) {
            StoreImagesCard(
                images = state.images,
                busy = state.imageBusy,
                notice = null,
                enabled = !readOnly,
                colors = colors,
                onUpload = viewModel::uploadImage,
                onRemove = viewModel::removeImage,
                onPrepareFailed = viewModel::imageProblem,
            )
        }
    }
    if (editingVideos) {
        AdminSheet(title = "Videos del local", colors = colors, busy = false, onDismiss = { editingVideos = false }) {
            VideosCard(state.config?.videos.orEmpty(), colors, enabled, viewModel::saveVideos)
        }
    }
}

/** La foto del lugar con el logo encima y "Cambiar", como el marco 44. */
@Composable
private fun PresenceHero(
    images: com.vaiinilla.app.domain.repository.StoreImages,
    enabled: Boolean,
    colors: OperationalColors,
    onChange: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(colors.cardBackground)
            .physicalPress(enabled = enabled, onClick = onChange)
            .semantics(mergeDescendants = true) { contentDescription = "Cambiar logo y foto" },
    ) {
        val photo = images.photoUrl
        if (photo != null) {
            ProductImage(imageUrl = photo, contentDescription = "", modifier = Modifier.fillMaxSize())
        } else {
            Icon(
                Icons.Outlined.Image,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.align(Alignment.Center).size(30.dp),
            )
        }
        val logoShape = RoundedCornerShape(14.dp)
        val logo =
            Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
                .size(52.dp)
                .clip(logoShape)
        val logoUrl = images.logoUrl
        if (logoUrl != null) {
            ProductImage(
                imageUrl = logoUrl,
                contentDescription = "",
                modifier = logo.border(2.dp, Color.White.copy(alpha = 0.8f), logoShape),
            )
        } else {
            Box(logo.background(colors.background), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.Storefront,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Row(
            Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .height(30.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Outlined.PhotoCamera,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(13.dp),
            )
            Text("Cambiar", fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }
    }
}

/** Los videos en miniatura y al final uno para agregar; se editan en una hoja porque son enlaces. */
@Composable
private fun VideoStrip(
    videos: List<StoreVideo>,
    enabled: Boolean,
    colors: OperationalColors,
    onEdit: () -> Unit,
) {
    val tile = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        videos.forEach { video ->
            Box(
                Modifier
                    .size(width = 100.dp, height = 130.dp)
                    .clip(tile)
                    .background(colors.cardBackground)
                    .physicalPress(enabled = enabled, scale = PhysicalPressScale.Small, onClick = onEdit)
                    .semantics(mergeDescendants = true) { contentDescription = "Video ${video.title}" },
            ) {
                youtubeThumbnail(video.url)?.let { thumb ->
                    ProductImage(imageUrl = thumb, contentDescription = "", modifier = Modifier.fillMaxSize())
                }
                Box(
                    Modifier
                        .align(
                            Alignment.Center,
                        ).size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    video.title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                )
            }
        }
        if (videos.size < StoreRules.MAX_VIDEOS) {
            Column(
                Modifier
                    .size(width = 100.dp, height = 130.dp)
                    .clip(tile)
                    .dashedBorder(colors.cardBorder, 16.dp)
                    .physicalPress(enabled = enabled, scale = PhysicalPressScale.Small, onClick = onEdit)
                    .semantics(mergeDescendants = true) {},
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    "Agregar video",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

private fun youtubeThumbnail(url: String): String? =
    Regex("""(?:youtu\.be/|v=|shorts/)([\w-]{6,})""")
        .find(url)
        ?.groupValues
        ?.get(1)
        ?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }

private fun Modifier.dashedBorder(
    color: Color,
    radius: androidx.compose.ui.unit.Dp,
): Modifier =
    drawBehind {
        drawRoundRect(
            color = color,
            cornerRadius = CornerRadius(radius.toPx()),
            style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
        )
    }

/** Entrega a domicilio y encargos: la zona, el envío y cómo se aceptan. */
@Composable
private fun StoreDeliveryPage(
    viewModel: AdminStoreViewModel,
    colors: OperationalColors,
    readOnly: Boolean,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState
    val enabled = !readOnly && !state.saving
    val notify: (String, Boolean) -> Unit = viewModel::notify
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var locatingStore by remember { mutableStateOf(false) }

    // La ubicación del negocio sale de un enlace de Maps, una dirección o el teléfono (CASA-02).
    fun locateStore(find: suspend () -> MapsLink.Point?) {
        if (locatingStore) return
        locatingStore = true
        scope.launch {
            val point = runCatching { find() }.getOrNull()
            locatingStore = false
            if (point == null) {
                notify(
                    "No encontramos ese lugar. Pega el enlace de Compartir de Google Maps o escribe la dirección completa.",
                    true,
                )
            } else {
                viewModel.saveStoreLocation(point.latitude, point.longitude)
            }
        }
    }
    val locationPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                locateStore { HomeAddressLocator.currentLocation(context) }
            } else {
                notify("Sin permiso de ubicación. Pega el enlace de Google Maps de tu negocio.", true)
            }
        }
    AdminSectionPage(
        title = "Entrega a domicilio",
        eyebrow = "Flujo de mi tienda",
        colors = colors,
        notice = state.notice,
        noticeIsError = state.noticeIsError,
        onBack = onBack,
    ) {
        val config = state.config ?: return@AdminSectionPage
        val worksFromHome = config.type == BusinessType.HOME_BAKERY
        val delivery = config.homeDelivery?.takeIf { it.enabled || worksFromHome }
        val preorders =
            config.preorders?.takeIf { worksFromHome || it.requiresAcceptance || it.depositPercent != null }
        item {
            AdminDeliveryForm(
                savedDelivery = delivery,
                savedPreorders = preorders,
                location = config.location,
                worksFromHome = worksFromHome,
                locating = locatingStore,
                saving = state.saving,
                enabled = enabled,
                readOnly = readOnly,
                colors = colors,
                onLocate = { text -> locateStore { HomeAddressLocator.locate(context, text) } },
                onUseCurrentLocation = {
                    if (HomeAddressLocator.hasLocationPermission(context)) {
                        locateStore { HomeAddressLocator.currentLocation(context) }
                    } else {
                        locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }
                },
                onSave = { newDelivery, newPreorders ->
                    newDelivery?.let(viewModel::saveHomeDelivery)
                    newPreorders?.let(viewModel::savePreorders)
                },
            )
        }
    }
}

@Composable
private fun VideosCard(
    saved: List<StoreVideo>,
    colors: OperationalColors,
    enabled: Boolean,
    onSave: (List<StoreVideo>) -> Unit,
) {
    val videos = remember(saved) { mutableStateListOf<StoreVideo>().apply { addAll(saved) } }
    AdminCard(colors) {
        AdminSectionTitle("Videos del local", colors)
        AdminHint(
            "Hasta ${StoreRules.MAX_VIDEOS} videos que el cliente ve mientras espera. Solo enlaces https.",
            colors,
        )
        videos.forEachIndexed { index, video ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.foundation.layout.Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    OperationalField(
                        value = video.title,
                        onValueChange = { videos[index] = video.copy(title = it.take(60)) },
                        label = "Título",
                        colors = colors,
                        enabled = enabled,
                    )
                    OperationalField(
                        value = video.url,
                        onValueChange = { videos[index] = video.copy(url = it.trim().take(500)) },
                        label = "Enlace",
                        colors = colors,
                        placeholder = "https://",
                        enabled = enabled,
                        keyboardType = KeyboardType.Uri,
                    )
                }
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier =
                        Modifier
                            .physicalPress(
                                enabled = enabled,
                                scale = PhysicalPressScale.Small,
                            ) { videos.removeAt(index) }
                            .size(44.dp)
                            .semantics { contentDescription = "Quitar video ${video.title}" },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (videos.size < StoreRules.MAX_VIDEOS) {
                ChipButton(
                    label = "Agregar video",
                    enabled = enabled,
                    colors = colors,
                ) { videos.add(StoreVideo("", "")) }
            }
            ChipButton(label = "Guardar videos", enabled = enabled && videos.toList() != saved, colors = colors) {
                onSave(videos.toList())
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowForm(
    saved: StoreFlow,
    availableTypes: List<String>?,
    colors: OperationalColors,
    enabled: Boolean,
    readOnly: Boolean,
    saving: Boolean,
    viewModel: AdminStoreViewModel,
) {
    var flow by remember { mutableStateOf(saved) }
    // Sin plantilla tocada se marca la primera del tipo guardado: el tipo de negocio sale de aquí (CASA-01).
    var template by remember { mutableStateOf<StoreTemplate?>(null) }
    var editingWindow by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(saved) {
        flow = saved
        template = null
        editingWindow = null
    }
    val windows = flow.orderWindows
    val chips = Arrangement.spacedBy(8.dp)
    Column(Modifier.animateContentSize(spring(stiffness = 500f))) {
        StoreQuestion("Plantilla", colors)
        FlowRow(horizontalArrangement = chips, verticalArrangement = chips) {
            StoreTemplate.offered(availableTypes).forEach { option ->
                AdminFilterChip(
                    label = option.label,
                    selected =
                        option ==
                            (
                                template
                                    ?: StoreTemplate
                                        .offered(
                                            availableTypes,
                                        ).firstOrNull { it.type.wire == flow.typeWire }
                            ),
                    colors = colors,
                    onClick = {
                        if (enabled) {
                            template = option
                            flow = flow.applying(option)
                        }
                    },
                    height = 40.dp,
                )
            }
        }
        StoreQuestion("¿Cuándo pagan?", colors, Modifier.padding(top = 18.dp))
        FlowRow(horizontalArrangement = chips, verticalArrangement = chips) {
            AdminFilterChip(
                label = "Al pedir",
                selected = !flow.allowsPayAtEnd,
                colors = colors,
                onClick = { if (enabled) flow = flow.copy(allowsPayAtEnd = false) },
                height = 40.dp,
            )
            AdminFilterChip(
                label = "Al final, con cuenta abierta",
                selected = flow.allowsPayAtEnd,
                colors = colors,
                onClick = { if (enabled) flow = flow.copy(allowsPayAtEnd = true) },
                height = 40.dp,
            )
        }
        StoreQuestion("¿Cómo se entrega lo de llevar?", colors, Modifier.padding(top = 18.dp))
        FlowRow(horizontalArrangement = chips, verticalArrangement = chips) {
            AdminFilterChip(
                label = "Escaneando su QR",
                selected = flow.deliveryRequiresQr,
                colors = colors,
                onClick = { if (enabled) flow = flow.copy(deliveryRequiresQr = true) },
                height = 40.dp,
            )
            AdminFilterChip(
                label = "Sin QR",
                selected = !flow.deliveryRequiresQr,
                colors = colors,
                onClick = { if (enabled) flow = flow.copy(deliveryRequiresQr = false) },
                height = 40.dp,
            )
        }
        Box(Modifier.padding(top = 8.dp, start = 2.dp)) {
            AdminHint("En una mesa con cuenta abierta nunca se pide QR.", colors)
        }
        AdminPanel(
            colors,
            Modifier.padding(top = 16.dp),
            padding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
        ) {
            AdminToggleRow(
                title = "Pagar con saldo",
                checked = flow.allowsBalance,
                colors = colors,
                onChange = { flow = flow.copy(allowsBalance = it) },
                detail = "El cliente usa su wallet al pagar",
                enabled = enabled,
            )
        }
        AdminPanel(
            colors,
            Modifier.padding(top = 10.dp),
            padding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
        ) {
            AdminRow(
                title = "Tiempo de gracia",
                colors = colors,
                detail = "Al terminar un turno, antes de liberar la mesa",
            ) {
                StepButton(Icons.Outlined.Remove, "Menos minutos", colors, enabled && flow.releaseGraceMinutes > 0) {
                    flow = flow.copy(releaseGraceMinutes = (flow.releaseGraceMinutes - 5).coerceAtLeast(0))
                }
                Text(
                    "${flow.releaseGraceMinutes} min",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.textPrimary,
                    modifier = Modifier.widthIn(min = 52.dp),
                    textAlign = TextAlign.Center,
                )
                StepButton(
                    Icons.Outlined.Add,
                    "Más minutos",
                    colors,
                    enabled && flow.releaseGraceMinutes < StoreRules.MAX_GRACE_MINUTES,
                ) {
                    flow =
                        flow.copy(
                            releaseGraceMinutes =
                                (flow.releaseGraceMinutes + 5).coerceAtMost(
                                    StoreRules.MAX_GRACE_MINUTES,
                                ),
                        )
                }
            }
        }
        StoreQuestion("¿Cuándo se reciben pedidos?", colors, Modifier.padding(top = 18.dp))
        FlowRow(horizontalArrangement = chips, verticalArrangement = chips) {
            windows.forEachIndexed { index, window ->
                AdminFilterChip(
                    label = "${window.from}–${window.to}",
                    selected = true,
                    colors = colors,
                    onClick = { editingWindow = if (editingWindow == index) null else index },
                    height = 40.dp,
                )
            }
            if (windows.size < StoreRules.MAX_WINDOWS) {
                Row(
                    Modifier
                        .physicalPress(enabled = enabled, scale = PhysicalPressScale.Small) {
                            flow = flow.copy(orderWindows = windows + StoreRules.nextWindow(windows))
                            editingWindow = windows.size
                        }.height(40.dp)
                        .border(1.dp, colors.cardBorder, CircleShape)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text("Franja", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                }
            }
        }
        if (windows.isEmpty()) {
            Box(Modifier.padding(top = 8.dp, start = 2.dp)) { AdminHint("Siempre que la tienda esté abierta.", colors) }
        }
        val index = editingWindow
        if (index != null && index in windows.indices) {
            val window = windows[index]
            AdminPanel(colors, Modifier.padding(top = 10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminTextField(
                        label = "Desde",
                        value = window.from,
                        onValueChange = {
                            flow =
                                flow.copy(orderWindows = windows.replace(index, window.copy(from = clock(it))))
                        },
                        colors = colors,
                        modifier = Modifier.weight(1f),
                        placeholder = "12:00",
                        keyboardType = KeyboardType.Number,
                        enabled = enabled,
                    )
                    AdminTextField(
                        label = "Hasta",
                        value = window.to,
                        onValueChange = {
                            flow =
                                flow.copy(orderWindows = windows.replace(index, window.copy(to = clock(it))))
                        },
                        colors = colors,
                        modifier = Modifier.weight(1f),
                        placeholder = "15:00",
                        keyboardType = KeyboardType.Number,
                        enabled = enabled,
                    )
                }
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { AdminHint("Si termina antes de empezar, cruza la medianoche.", colors) }
                    Text(
                        "Quitar",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.coralText,
                        modifier =
                            Modifier
                                .physicalPress(enabled = enabled, scale = PhysicalPressScale.Small) {
                                    flow = flow.copy(orderWindows = windows.filterIndexed { i, _ -> i != index })
                                    editingWindow = null
                                }.padding(8.dp),
                    )
                }
            }
        }
        // Solo si el servidor los conoce (vienen en la configuración, aunque sea vacíos).
        val pause = flow.pauseWhenOffline
        if (pause != null) {
            AdminPanel(
                colors,
                Modifier.padding(top = 18.dp),
                padding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
            ) {
                AdminToggleRow(
                    title = "Pausar si se va el internet",
                    checked = pause,
                    colors = colors,
                    onChange = { flow = flow.copy(pauseWhenOffline = it) },
                    detail = "Si Caja y el personal pierden la conexión, se pausan los pedidos por QR y las reservas",
                    enabled = enabled,
                )
            }
        }
        val fund = flow.suggestedFloat
        val phone = flow.contactPhone
        if (fund != null || phone != null) {
            StoreQuestion("Caja y contacto", colors, Modifier.padding(top = 18.dp))
            AdminPanel(colors) {
                if (fund != null) {
                    AdminTextField(
                        label = "Fondo sugerido para abrir caja",
                        value = fund,
                        onValueChange = { flow = flow.copy(suggestedFloat = it.take(12)) },
                        colors = colors,
                        placeholder = "500",
                        keyboardType = KeyboardType.Decimal,
                        enabled = enabled,
                        prefix = "$",
                    )
                    Box(Modifier.padding(top = 6.dp, start = 2.dp)) {
                        AdminHint(
                            "Caja lo ve ya escrito al abrir el turno. Déjalo vacío si cambia cada día.",
                            colors,
                        )
                    }
                }
                if (phone != null) {
                    AdminTextField(
                        label = "Teléfono de contacto",
                        value = phone,
                        onValueChange = { flow = flow.copy(contactPhone = it.take(20)) },
                        colors = colors,
                        modifier = if (fund != null) Modifier.padding(top = 14.dp) else Modifier,
                        placeholder = "55 1234 5678",
                        keyboardType = KeyboardType.Phone,
                        enabled = enabled,
                    )
                    Box(Modifier.padding(top = 6.dp, start = 2.dp)) {
                        AdminHint("Aparece en tu ficha pública para que tus clientes te llamen.", colors)
                    }
                }
            }
        }
        val problem = StoreRules.problem(flow)
        if (flow != saved) {
            Box(Modifier.padding(top = 12.dp)) { AdminSheetError(problem) }
        }
        if (!readOnly && flow != saved) {
            Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminSecondaryButton(
                    title = "Descartar",
                    colors = colors,
                    modifier = Modifier.weight(1f),
                    outlined = true,
                ) {
                    flow = saved
                    template = null
                    editingWindow = null
                }
                AdminPrimaryButton(
                    title = "Guardar flujo",
                    colors = colors,
                    modifier = Modifier.weight(1f),
                    busy = saving,
                    enabled = problem == null && enabled,
                ) { viewModel.saveFlow(flow) }
            }
        }
    }
}

@Composable
private fun StepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    colors: OperationalColors,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val haptics = rememberVaiinillaHaptics()
    Box(
        Modifier
            .physicalPress(enabled = enabled, scale = PhysicalPressScale.Small) {
                haptics.selection()
                onClick()
            }.alpha(if (enabled) 1f else 0.4f)
            .size(34.dp)
            .clip(CircleShape)
            .background(colors.textPrimary.copy(alpha = 0.08f))
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(14.dp)) }
}

private fun List<OrderWindow>.replace(
    index: Int,
    window: OrderWindow,
): List<OrderWindow> = mapIndexed { i, current -> if (i == index) window else current }

private fun clock(raw: String): String = raw.filter { it.isDigit() || it == ':' }.take(5)
