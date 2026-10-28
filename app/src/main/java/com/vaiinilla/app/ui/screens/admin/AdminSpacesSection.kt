package com.vaiinilla.app.ui.screens.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.admin.ManagedSpace
import com.vaiinilla.app.domain.admin.ReservationSchedule
import com.vaiinilla.app.domain.admin.SpaceRules
import com.vaiinilla.app.domain.admin.SpaceType
import com.vaiinilla.app.domain.model.MoneyInput
import com.vaiinilla.app.domain.model.Reservation
import com.vaiinilla.app.domain.model.ReservationState
import com.vaiinilla.app.domain.time.BusinessTime
import com.vaiinilla.app.ui.admin.AdminSpacesViewModel
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.screens.ActionPill
import com.vaiinilla.app.ui.screens.ChipButton
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.OperationalField
import com.vaiinilla.app.ui.screens.prepareProductImage
import java.time.format.DateTimeFormatter

private sealed interface SpacesSheet {
    data object Create : SpacesSheet

    data object Batch : SpacesSheet

    data class Edit(
        val spaceId: Int,
    ) : SpacesSheet

    data class Qr(
        val spaceId: Int,
    ) : SpacesSheet

    data class Delete(
        val space: ManagedSpace,
    ) : SpacesSheet

    data class Charge(
        val reservation: Reservation,
    ) : SpacesSheet

    data class Release(
        val reservation: Reservation,
    ) : SpacesSheet
}

/** Mesas y espacios: alta, QR y código de cada uno, fichas de cancha y reservas del día. */
@Composable
internal fun AdminSpacesSection(
    viewModel: AdminSpacesViewModel,
    colors: OperationalColors,
    readOnly: Boolean,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState
    var sheet by remember { mutableStateOf<SpacesSheet?>(null) }
    val close = { sheet = null }
    val enabled = !readOnly && !state.saving

    AdminSectionPage(
        title = "Mesas y espacios",
        colors = colors,
        notice = state.notice,
        noticeIsError = state.noticeIsError,
        onBack = onBack,
    ) {
        if (!readOnly) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionPill(
                        label = "Nuevo espacio",
                        enabled = enabled,
                        loading = false,
                        colors = colors,
                        modifier = Modifier.weight(1f),
                    ) {
                        sheet = SpacesSheet.Create
                    }
                    ActionPill(
                        label = "Crear varios",
                        enabled = enabled,
                        loading = false,
                        colors = colors,
                        primary = false,
                        modifier = Modifier.weight(1f),
                    ) { sheet = SpacesSheet.Batch }
                }
            }
        }
        val spaces = state.spaces
        when {
            spaces == null && state.error != null ->
                item {
                    AdminLoadError(
                        state.error.orEmpty(),
                        colors,
                        viewModel::refresh,
                    )
                }
            spaces == null -> item { AdminHint("Cargando espacios…", colors) }
            spaces.isEmpty() -> item { AdminHint("Aún no hay mesas ni espacios. Crea uno para tener su QR.", colors) }
            else ->
                items(
                    spaces.sortedWith(compareBy(naturalOrder<String>()) { naturalKey(it.name) }),
                    key = { "s${it.id}" },
                ) { space ->
                    SpaceCard(
                        space = space,
                        colors = colors,
                        busy = state.busySpaceId == space.id,
                        readOnly = readOnly,
                        onQr = { sheet = SpacesSheet.Qr(space.id) },
                        onEdit = { sheet = SpacesSheet.Edit(space.id) },
                    )
                }
        }
        if (state.hasCourts) {
            item {
                ReservationsCard(state.reservations, colors, enabled, onCharge = {
                    sheet =
                        SpacesSheet.Charge(
                            it,
                        )
                }, onRelease = {
                    sheet = SpacesSheet.Release(it)
                })
            }
            item { ScheduleCard(state.schedule, colors, enabled, viewModel::saveSchedule) }
        }
    }

    val error = state.notice.takeIf { state.noticeIsError }
    when (val current = sheet) {
        SpacesSheet.Create ->
            CreateSheet(
                colors,
                state.saving,
                error,
                close,
            ) { name, type -> viewModel.create(name, type, close) }
        SpacesSheet.Batch ->
            BatchSheet(colors, state.saving, error, close) { type, prefix, from, to ->
                viewModel.createBatch(type, prefix, from, to, close)
            }
        is SpacesSheet.Edit -> {
            val space = state.spaces?.firstOrNull { it.id == current.spaceId }
            if (space == null) {
                close()
            } else {
                EditSheet(space, colors, state.saving, error, viewModel, onDelete = {
                    sheet = SpacesSheet.Delete(space)
                }, onDismiss = close)
            }
        }
        is SpacesSheet.Qr -> {
            val space = state.spaces?.firstOrNull { it.id == current.spaceId }
            if (space == null) {
                close()
            } else {
                AdminSheet(title = space.name, colors = colors, busy = false, onDismiss = close) {
                    AdminHint("El cliente escanea este QR o escribe el código para pedir desde ${space.name}.", colors)
                    AdminQrBlock(
                        url = space.qrUrl,
                        name = space.name,
                        colors = colors,
                        onNotice = viewModel::notify,
                        code = space.code,
                    )
                }
            }
        }
        is SpacesSheet.Delete ->
            AdminConfirmSheet(
                title = "Eliminar ${current.space.name}",
                message =
                    "Su QR deja de funcionar. Si tiene una cuenta abierta o reservas pendientes, " +
                        "no se podrá eliminar.",
                confirmLabel = "Eliminar",
                colors = colors,
                busy = state.saving,
                onDismiss = close,
            ) {
                viewModel.delete(current.space)
                close()
            }
        is SpacesSheet.Charge ->
            ChargeSheet(current.reservation, colors, state.saving, error, close) { received ->
                viewModel.payReservation(current.reservation, received, close)
            }
        is SpacesSheet.Release ->
            AdminConfirmSheet(
                title = "Liberar cancha",
                message =
                    "Se cancela la reserva de ${current.reservation.customerName ?: "este cliente"} " +
                        "y la cancha queda libre.",
                confirmLabel = "Liberar cancha",
                colors = colors,
                busy = state.saving,
                onDismiss = close,
            ) {
                viewModel.cancelReservation(current.reservation)
                close()
            }
        null -> Unit
    }
}

@Composable
private fun SpaceCard(
    space: ManagedSpace,
    colors: OperationalColors,
    busy: Boolean,
    readOnly: Boolean,
    onQr: () -> Unit,
    onEdit: () -> Unit,
) {
    AdminCard(colors) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (space.isCourt && space.imageUrl != null) {
                ProductImage(
                    imageUrl = space.imageUrl,
                    contentDescription = space.name,
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(space.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Text(
                    listOfNotNull(
                        space.type?.label ?: space.typeWire,
                        space.code?.let { "código $it" },
                        space.hourlyPrice?.let { "${moneyLabel(it)}/hora" },
                    ).joinToString(" · "),
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                )
                if (!space.active) {
                    Text(
                        "Desactivado",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AdminCoral,
                    )
                }
            }
            if (busy) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = colors.textPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipButton(label = "QR y código", enabled = true, colors = colors, onClick = onQr)
            if (!readOnly) ChipButton(label = "Editar", enabled = !busy, colors = colors, onClick = onEdit)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateSheet(
    colors: OperationalColors,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onCreate: (String, SpaceType) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(SpaceType.TABLE) }
    AdminSheet(title = "Nuevo espacio", colors = colors, busy = saving, onDismiss = onDismiss) {
        OperationalField(value = name, onValueChange = {
            name = it.take(80)
        }, label = "Nombre", colors = colors, placeholder = "Ej. Mesa 5", enabled = !saving)
        Text("Tipo", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SpaceType.entries.forEach { option ->
                ChipButton(label = option.label, enabled = !saving, colors = colors, selected = option == type) {
                    type =
                        option
                }
            }
        }
        AdminSheetError(error)
        ActionPill(label = "Crear espacio", enabled = name.isNotBlank(), loading = saving, colors = colors) {
            onCreate(name, type)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BatchSheet(
    colors: OperationalColors,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onCreate: (SpaceType, String, Int?, Int?) -> Unit,
) {
    var type by remember { mutableStateOf(SpaceType.SEAT) }
    var prefix by remember { mutableStateOf("Asiento") }
    var from by remember { mutableStateOf("1") }
    var to by remember { mutableStateOf("100") }
    AdminSheet(title = "Crear varios a la vez", colors = colors, busy = saving, onDismiss = onDismiss) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SpaceType.batchTypes.forEach { option ->
                ChipButton(label = option.label, enabled = !saving, colors = colors, selected = option == type) {
                    if (prefix == type.label) prefix = option.label
                    type = option
                }
            }
        }
        OperationalField(value = prefix, onValueChange = {
            prefix = it.take(70)
        }, label = "Nombre base", colors = colors, enabled = !saving)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OperationalField(
                value = from,
                onValueChange = { from = it.filter(Char::isDigit).take(5) },
                label = "Del",
                colors = colors,
                enabled = !saving,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
            OperationalField(
                value = to,
                onValueChange = { to = it.filter(Char::isDigit).take(5) },
                label = "Al",
                colors = colors,
                enabled = !saving,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
        }
        val count = (to.toIntOrNull() ?: 0) - (from.toIntOrNull() ?: 0) + 1
        AdminHint(
            if (count >
                0
            ) {
                "Se crean \"${prefix.trim()} ${from}\" a \"${prefix.trim()} $to\" ($count). Los que ya existan se omiten."
            } else {
                "Revisa los números."
            },
            colors,
        )
        AdminSheetError(error)
        ActionPill(label = "Crear", enabled = count in 1..SpaceRules.MAX_BATCH, loading = saving, colors = colors) {
            onCreate(type, prefix, from.toIntOrNull(), to.toIntOrNull())
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditSheet(
    space: ManagedSpace,
    colors: OperationalColors,
    saving: Boolean,
    error: String?,
    viewModel: AdminSpacesViewModel,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var name by remember(space.id) { mutableStateOf(space.name) }
    var price by remember(
        space.id,
        space.hourlyPrice,
    ) { mutableStateOf(space.hourlyPrice?.removeSuffix(".00").orEmpty()) }
    var description by remember(space.id) { mutableStateOf(space.description.orEmpty()) }
    val features = remember(space.id) { mutableStateListOf<String>().apply { addAll(space.features) } }
    var newFeature by remember { mutableStateOf("") }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
            val prepared = runCatching { prepareProductImage(context, uri) }.getOrNull()
            if (prepared == null) {
                viewModel.imageProblem("No se pudo preparar la foto. Prueba otra imagen de hasta 16 MB.")
            } else {
                viewModel.uploadImage(space, prepared.bytes, prepared.filename, prepared.mimeType)
            }
        }
    AdminSheet(title = space.name, colors = colors, busy = saving, onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OperationalField(value = name, onValueChange = {
                name = it.take(80)
            }, label = "Nombre", colors = colors, enabled = !saving, modifier = Modifier.weight(1f))
            ChipButton(
                label = "Guardar",
                enabled = !saving && name.trim() != space.name && name.isNotBlank(),
                colors = colors,
            ) {
                viewModel.rename(space, name)
            }
        }
        Text("Tipo", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SpaceType.entries.forEach { option ->
                ChipButton(
                    label = option.label,
                    enabled = !saving,
                    colors = colors,
                    selected =
                        option.wire == space.typeWire,
                ) {
                    viewModel.setType(space, option)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Activo", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                AdminHint("Desactivado, su QR no recibe pedidos.", colors)
            }
            Switch(
                checked = space.active,
                onCheckedChange = { viewModel.setActive(space, it) },
                enabled = !saving,
                colors =
                    SwitchDefaults.colors(
                        checkedTrackColor = colors.accentLime,
                        checkedThumbColor = colors.accentInk,
                    ),
            )
        }
        if (space.isCourt) {
            AdminSectionTitle("Renta de la cancha", colors)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OperationalField(
                    value = price,
                    onValueChange = { price = MoneyInput.sanitize(it).take(8) },
                    label = "Precio por hora (MXN)",
                    colors = colors,
                    placeholder = "Vacío = no se renta",
                    enabled = !saving,
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                )
                ChipButton(
                    label = "Guardar",
                    enabled = !saving,
                    colors = colors,
                ) { viewModel.setHourlyPrice(space, price) }
            }
            AdminSectionTitle("Ficha de la cancha", colors)
            if (space.imageUrl != null) {
                ProductImage(
                    imageUrl = space.imageUrl,
                    contentDescription = space.name,
                    modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(18.dp)),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipButton(
                    label =
                        if (space.imageUrl ==
                            null
                        ) {
                            "Subir foto"
                        } else {
                            "Cambiar foto"
                        },
                    enabled = !saving,
                    colors = colors,
                ) {
                    picker.launch("image/*")
                }
                if (space.imageUrl !=
                    null
                ) {
                    ChipButton(
                        label = "Quitar foto",
                        enabled = !saving,
                        colors = colors,
                    ) { viewModel.removeImage(space) }
                }
            }
            OperationalField(
                value = description,
                onValueChange = { description = it.take(500) },
                label = "Descripción",
                colors = colors,
                enabled = !saving,
                singleLine = false,
                minLines = 2,
            )
            Text("Características", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                (
                    features +
                        SpaceRules.FEATURE_SUGGESTIONS.filter { s -> features.none { it.equals(s, ignoreCase = true) } }
                ).forEach { feature ->
                    val on = features.any { it.equals(feature, ignoreCase = true) }
                    ChipButton(
                        label = feature,
                        enabled = !saving && (on || features.size < SpaceRules.MAX_FEATURES),
                        colors = colors,
                        selected = on,
                    ) {
                        if (on) {
                            features.removeAll {
                                it.equals(
                                    feature,
                                    ignoreCase = true,
                                )
                            }
                        } else {
                            features.add(feature)
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OperationalField(
                    value = newFeature,
                    onValueChange = { newFeature = it.take(30) },
                    label = "Otra característica",
                    colors = colors,
                    enabled = !saving && features.size < SpaceRules.MAX_FEATURES,
                    modifier = Modifier.weight(1f),
                )
                ChipButton(
                    label = "Agregar",
                    enabled =
                        newFeature.isNotBlank() && features.size < SpaceRules.MAX_FEATURES,
                    colors = colors,
                ) {
                    features.add(newFeature.trim())
                    newFeature = ""
                }
            }
            ChipButton(label = "Guardar ficha", enabled = !saving, colors = colors) {
                viewModel.saveProfile(space, description, features.toList())
            }
        }
        AdminSheetError(error)
        ActionPill(
            label = "Eliminar espacio",
            enabled = !saving,
            loading = false,
            colors = colors,
            primary = false,
            onClick = onDelete,
        )
    }
}

@Composable
private fun ReservationsCard(
    reservations: List<Reservation>,
    colors: OperationalColors,
    enabled: Boolean,
    onCharge: (Reservation) -> Unit,
    onRelease: (Reservation) -> Unit,
) {
    AdminCard(colors) {
        AdminSectionTitle("Reservas de hoy", colors)
        if (reservations.isEmpty()) AdminHint("No hay canchas reservadas hoy.", colors)
        reservations.forEach { reservation ->
            AdminValueRow(
                label = "${reservationTime(reservation)} · ${reservation.courtName ?: "Cancha"}",
                value = moneyLabel(reservation.amount),
                colors = colors,
                detail =
                    listOfNotNull(
                        reservation.customerName ?: "Cliente",
                        reservationLabel(reservation.state),
                    ).joinToString(" · "),
            )
            if (reservation.state == ReservationState.PENDING_PAYMENT) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipButton(label = "Cobrar", enabled = enabled, colors = colors) { onCharge(reservation) }
                    ChipButton(label = "Liberar cancha", enabled = enabled, colors = colors) { onRelease(reservation) }
                }
            }
        }
    }
}

@Composable
private fun ChargeSheet(
    reservation: Reservation,
    colors: OperationalColors,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onCharge: (String) -> Unit,
) {
    var received by remember { mutableStateOf(reservation.amount.removeSuffix(".00")) }
    AdminSheet(title = "Cobrar renta", colors = colors, busy = saving, onDismiss = onDismiss) {
        AdminValueRow(
            "${reservation.courtName ?: "Cancha"} · ${reservationTime(reservation)}",
            moneyLabel(reservation.amount),
            colors,
        )
        OperationalField(
            value = received,
            onValueChange = { received = MoneyInput.sanitize(it).take(10) },
            label = "Efectivo recibido",
            colors = colors,
            enabled = !saving,
            keyboardType = KeyboardType.Decimal,
        )
        AdminHint("Se cobra en efectivo y necesita la caja abierta.", colors)
        AdminSheetError(error)
        ActionPill(label = "Cobrar", enabled = received.isNotBlank(), loading = saving, colors = colors) {
            onCharge(
                com.vaiinilla.app.domain.admin.AdminProductRules
                    .money(received) ?: received,
            )
        }
    }
}

@Composable
private fun ScheduleCard(
    saved: ReservationSchedule?,
    colors: OperationalColors,
    enabled: Boolean,
    onSave: (ReservationSchedule) -> Unit,
) {
    var opening by remember(saved) { mutableStateOf(saved?.opening.orEmpty()) }
    var closing by remember(saved) { mutableStateOf(saved?.closing.orEmpty()) }
    var days by remember(saved) { mutableStateOf(saved?.daysAhead?.toString().orEmpty()) }
    AdminCard(colors) {
        AdminSectionTitle("Horario de reservas", colors)
        if (saved == null) {
            AdminHint("Cargando…", colors)
            return@AdminCard
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OperationalField(
                value = opening,
                onValueChange = { opening = it.filter { c -> c.isDigit() || c == ':' }.take(5) },
                label = "Abre",
                colors = colors,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
            OperationalField(
                value = closing,
                onValueChange = { closing = it.filter { c -> c.isDigit() || c == ':' }.take(5) },
                label = "Cierra",
                colors = colors,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
        }
        AdminHint("Si cierra antes de abrir, el horario cruza la medianoche.", colors)
        OperationalField(
            value = days,
            onValueChange = { days = it.filter(Char::isDigit).take(2) },
            label = "Días de anticipación para reservar",
            colors = colors,
            supporting = "De 0 a 60.",
            enabled = enabled,
            keyboardType = KeyboardType.Number,
        )
        val draft = ReservationSchedule(opening, closing, days.toIntOrNull() ?: -1)
        ChipButton(label = "Guardar horario", enabled = enabled && draft != saved, colors = colors) { onSave(draft) }
    }
}

private val ReservationTimeFormat = DateTimeFormatter.ofPattern("HH:mm")

private fun reservationTime(reservation: Reservation): String {
    val zone = BusinessTime.zone
    return "${ReservationTimeFormat.format(
        reservation.start.atZone(zone),
    )}–${ReservationTimeFormat.format(reservation.end.atZone(zone))}"
}

private fun reservationLabel(state: ReservationState): String =
    when (state) {
        ReservationState.PENDING_PAYMENT -> "Por pagar en efectivo"
        ReservationState.CONFIRMED, ReservationState.IN_PROGRESS, ReservationState.FINISHED -> "Pagada"
        else -> state.label
    }

/** "Mesa 2" antes que "Mesa 10": los números se comparan como números. */
private fun naturalKey(name: String): String = name.replace(Regex("\\d+")) { it.value.padStart(8, '0') }.lowercase()
