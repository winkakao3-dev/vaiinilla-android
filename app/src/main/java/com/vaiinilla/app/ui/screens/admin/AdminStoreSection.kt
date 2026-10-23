package com.vaiinilla.app.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.vaiinilla.app.domain.admin.BusinessType
import com.vaiinilla.app.domain.admin.StoreFlow
import com.vaiinilla.app.domain.admin.StoreRules
import com.vaiinilla.app.domain.admin.StoreTemplate
import com.vaiinilla.app.domain.admin.StoreVideo
import com.vaiinilla.app.domain.admin.StripeStep
import com.vaiinilla.app.domain.admin.stripeStep
import com.vaiinilla.app.domain.model.OrderWindow
import com.vaiinilla.app.ui.admin.AdminStoreViewModel
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.screens.ActionPill
import com.vaiinilla.app.ui.screens.ChipButton
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.OperationalField
import com.vaiinilla.app.ui.screens.StoreImagesCard

/** Flujo de mi tienda: cómo cobra, entrega y se presenta el negocio. */
@Composable
internal fun AdminStoreSection(
    viewModel: AdminStoreViewModel,
    colors: OperationalColors,
    readOnly: Boolean,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState
    // Al volver de Stripe (otra app) se vuelve a consultar el estado de la cuenta.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshStripe() }
    val enabled = !readOnly && !state.saving
    val notify: (String, Boolean) -> Unit = viewModel::notify

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
        item { StripeCard(viewModel, colors, enabled) }
        item {
            AdminCard(colors) {
                AdminSectionTitle("Directorio", colors)
                AdminChoice(
                    question = "¿Dónde te encuentran?",
                    options = listOf(false to "Solo con QR o código", true to "También en la lista de Vaiinilla"),
                    selected = config.visibleInDirectory,
                    colors = colors,
                    enabled = enabled,
                ) { if (it != config.visibleInDirectory) viewModel.setVisibleInDirectory(it) }
                config.storeUrl?.let { url ->
                    AdminHint("El QR de tu tienda abre tu menú directo.", colors)
                    AdminQrBlock(url = url, name = "tienda", colors = colors, onNotice = notify)
                }
            }
        }
        item {
            AdminCard(colors) {
                AdminSectionTitle("Escuela", colors)
                AdminChoice(
                    question = "¿Es una escuela?",
                    options = listOf(false to "No", true to "Sí, con tutores y alumnos"),
                    selected = config.isSchool,
                    colors = colors,
                    enabled = enabled,
                ) { if (it != config.isSchool) viewModel.setSchoolMode(it) }
                AdminHint(
                    "Los tutores recargan y apartan la comida de sus hijos, y Caja le cobra al alumno sin teléfono.",
                    colors,
                )
            }
        }
        item {
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
        item { VideosCard(config.videos, colors, enabled, viewModel::saveVideos) }
        item { FlowCard(config.flow(), colors, enabled, readOnly, viewModel) }
    }
}

@Composable
private fun StripeCard(
    viewModel: AdminStoreViewModel,
    colors: OperationalColors,
    enabled: Boolean,
) {
    val state by viewModel.uiState
    val context = LocalContext.current
    AdminCard(colors) {
        AdminSectionTitle("Pagos con tarjeta", colors)
        if (!state.stripeLoaded) {
            AdminHint("Consultando tu cuenta de Stripe…", colors)
            return@AdminCard
        }
        val setup = state.stripe
        when (stripeStep(setup)) {
            StripeStep.CONNECT -> {
                AdminHint("Conecta tu cuenta de Stripe para que tus clientes paguen con tarjeta desde la app.", colors)
                ActionPill(
                    label = "Conectar mi cuenta de Stripe",
                    enabled = enabled,
                    loading = state.saving,
                    colors = colors,
                ) {
                    viewModel.openStripe { openLink(context, it) }
                }
            }
            StripeStep.FINISH -> {
                AdminHint(
                    "Stripe aún necesita datos o está revisando tu cuenta. Termina la configuración para cobrar con tarjeta.",
                    colors,
                )
                ActionPill(label = "Completar en Stripe", enabled = enabled, loading = state.saving, colors = colors) {
                    viewModel.openStripe { openLink(context, it) }
                }
            }
            StripeStep.READY -> {
                val ready = setup ?: return@AdminCard
                AdminChoice(
                    question = "¿Aceptas pagos con tarjeta?",
                    options = listOf(true to "Sí, acepto tarjeta", false to "No por ahora"),
                    selected = ready.enabled,
                    colors = colors,
                    enabled = enabled,
                ) { if (it != ready.enabled) viewModel.setCardPayments(it) }
                AdminChoice(
                    question = "¿Quién paga la comisión de la tarjeta?",
                    options = listOf(false to "Yo, el negocio", true to "El cliente"),
                    selected = ready.passFeeToCustomer,
                    colors = colors,
                    enabled = enabled,
                ) { if (it != ready.passFeeToCustomer) viewModel.setFeePaidByCustomer(it) }
                AdminHint(
                    if (ready.passFeeToCustomer) {
                        "Con tarjeta se suma la comisión al precio; con efectivo o saldo se cobra el precio normal."
                    } else {
                        "El mismo precio con efectivo, saldo o tarjeta."
                    },
                    colors,
                )
            }
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
private fun FlowCard(
    saved: StoreFlow,
    colors: OperationalColors,
    enabled: Boolean,
    readOnly: Boolean,
    viewModel: AdminStoreViewModel,
) {
    var flow by remember { mutableStateOf(saved) }
    var grace by remember { mutableStateOf(saved.releaseGraceMinutes.toString()) }
    LaunchedEffect(saved) {
        flow = saved
        grace = saved.releaseGraceMinutes.toString()
    }
    val windows = flow.orderWindows
    AdminCard(colors) {
        AdminSectionTitle("Cómo funciona tu tienda", colors)
        Text("Empieza con una plantilla", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StoreTemplate.entries.forEach { template ->
                ChipButton(label = template.label, enabled = enabled, colors = colors) {
                    flow = flow.applying(template)
                    grace = "5"
                }
            }
        }
        AdminChoice(
            question = "Tipo de negocio",
            options = BusinessType.entries.map { it.wire to it.label },
            selected = flow.typeWire,
            colors = colors,
            enabled = enabled,
        ) { flow = flow.copy(typeWire = it) }
        AdminChoice(
            question = "¿Cuándo pagan?",
            options = listOf(false to "Al pedir", true to "Al final, con cuenta abierta"),
            selected = flow.allowsPayAtEnd,
            colors = colors,
            enabled = enabled,
        ) { flow = flow.copy(allowsPayAtEnd = it) }
        AdminChoice(
            question = "¿Permitir pagos con saldo?",
            options = listOf(true to "Sí", false to "No"),
            selected = flow.allowsBalance,
            colors = colors,
            enabled = enabled,
        ) { flow = flow.copy(allowsBalance = it) }
        AdminChoice(
            question = "¿Cómo se entrega?",
            options = listOf(true to "Escanear QR para pedidos para llevar", false to "Entregar sin QR"),
            selected = flow.deliveryRequiresQr,
            colors = colors,
            enabled = enabled,
        ) { flow = flow.copy(deliveryRequiresQr = it) }
        AdminHint("En una mesa con cuenta abierta nunca se pide QR.", colors)
        OperationalField(
            value = grace,
            onValueChange = { grace = it.filter(Char::isDigit).take(2) },
            label = "Gracia al terminar un turno (minutos)",
            colors = colors,
            supporting = "De 0 a ${StoreRules.MAX_GRACE_MINUTES}.",
            enabled = enabled,
            keyboardType = KeyboardType.Number,
        )
        Text("¿Cuándo se reciben pedidos?", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        if (windows.isEmpty()) AdminHint("Siempre que la tienda esté abierta.", colors)
        windows.forEachIndexed { index, window ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OperationalField(
                    value = window.from,
                    onValueChange = { value ->
                        flow =
                            flow.copy(orderWindows = windows.replace(index, window.copy(from = clock(value))))
                    },
                    label = "Desde",
                    colors = colors,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                )
                OperationalField(
                    value = window.to,
                    onValueChange = { value ->
                        flow =
                            flow.copy(orderWindows = windows.replace(index, window.copy(to = clock(value))))
                    },
                    label = "Hasta",
                    colors = colors,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier =
                        Modifier
                            .physicalPress(enabled = enabled, scale = PhysicalPressScale.Small) {
                                flow = flow.copy(orderWindows = windows.filterIndexed { i, _ -> i != index })
                            }.size(44.dp)
                            .semantics { contentDescription = "Quitar franja" },
                )
            }
        }
        if (windows.size < StoreRules.MAX_WINDOWS) {
            ChipButton(label = "Agregar franja", enabled = enabled, colors = colors) {
                flow = flow.copy(orderWindows = windows + StoreRules.nextWindow(windows))
            }
        }
        AdminHint("Si una franja termina antes de empezar, cruza la medianoche.", colors)
        val ready = flow.copy(releaseGraceMinutes = grace.toIntOrNull() ?: -1)
        val problem = StoreRules.problem(ready)
        AdminSheetError(problem.takeIf { ready != saved })
        if (!readOnly) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionPill(
                    label = "Descartar",
                    enabled = enabled && ready != saved,
                    loading = false,
                    colors = colors,
                    primary = false,
                    modifier = Modifier.weight(1f),
                ) {
                    flow = saved
                    grace = saved.releaseGraceMinutes.toString()
                }
                ActionPill(
                    label = "Guardar flujo",
                    enabled = enabled && ready != saved && problem == null,
                    loading = false,
                    colors = colors,
                    modifier = Modifier.weight(1f),
                ) { viewModel.saveFlow(ready) }
            }
        }
    }
}

private fun List<OrderWindow>.replace(
    index: Int,
    window: OrderWindow,
): List<OrderWindow> = mapIndexed { i, current -> if (i == index) window else current }

private fun clock(raw: String): String = raw.filter { it.isDigit() || it == ':' }.take(5)
