package com.vaiinilla.app.ui.hotel.admin

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vaiinilla.app.core.network.RetryKeys
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.admin.SpaceRules
import com.vaiinilla.app.domain.hotel.HotelRepository
import com.vaiinilla.app.domain.hotel.HotelRoom
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.admin.AdminCapsLabel
import com.vaiinilla.app.ui.screens.admin.AdminLoadError
import com.vaiinilla.app.ui.screens.admin.AdminPanel
import com.vaiinilla.app.ui.screens.admin.AdminPrimaryButton
import com.vaiinilla.app.ui.screens.admin.AdminRow
import com.vaiinilla.app.ui.screens.admin.AdminRowDivider
import com.vaiinilla.app.ui.screens.admin.AdminSectionPage
import com.vaiinilla.app.ui.screens.admin.AdminTextField
import com.vaiinilla.app.ui.screens.admin.adminRise
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Lo que el dueño escribe para crear habitaciones en lote; todo como texto hasta que se manda. */
data class RoomBatchDraft(
    val prefix: String = "Habitación",
    val from: String = "",
    val to: String = "",
    val floor: String = "",
    val category: String = "",
) {
    val fromNumber: Int? get() = from.toIntOrNull()
    val toNumber: Int? get() = to.toIntOrNull()
    val floorNumber: Int? get() = floor.toIntOrNull()

    /** Cuántas se crean; 0 si los números no cuadran. */
    val count: Int
        get() {
            val a = fromNumber ?: return 0
            val b = toNumber ?: return 0
            return if (b >= a) b - a + 1 else 0
        }
}

data class HotelRoomsUiState(
    val rooms: List<HotelRoom>? = null,
    val error: String? = null,
    val saving: Boolean = false,
    val notice: String? = null,
    val noticeIsError: Boolean = false,
)

/** A01: las habitaciones del hotel y su alta en lote con piso y categoría. */
@HiltViewModel
class HotelRoomsAdminViewModel
    @Inject
    constructor(
        private val repository: HotelRepository,
    ) : ViewModel() {
        private val _uiState = mutableStateOf(HotelRoomsUiState())
        val uiState: State<HotelRoomsUiState> = _uiState
        private val retryKeys = RetryKeys()
        private var noticeJob: Job? = null

        fun refresh() {
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { repository.rooms() }
                _uiState.value =
                    result.fold(
                        onSuccess = {
                            _uiState.value.copy(
                                rooms =
                                    it.sortedBy { room ->
                                        room.name.naturalKey()
                                    },
                                error = null,
                            )
                        },
                        onFailure = {
                            _uiState.value.copy(
                                error = it.toUserFacingMessage("No se pudieron cargar las habitaciones."),
                            )
                        },
                    )
            }
        }

        fun create(
            draft: RoomBatchDraft,
            onDone: () -> Unit,
        ) {
            SpaceRules
                .batchProblem(
                    draft.prefix,
                    draft.fromNumber,
                    draft.toNumber,
                )?.let { return showNotice(it, isError = true) }
            if (_uiState.value.saving) return
            _uiState.value = _uiState.value.copy(saving = true)
            viewModelScope.launch {
                val category = draft.category.trim().ifEmpty { null }
                val result =
                    withContext(Dispatchers.IO) {
                        retryKeys.withKey(
                            "rooms",
                            draft.prefix.trim(),
                            draft.fromNumber,
                            draft.toNumber,
                            draft.floorNumber,
                            category,
                        ) { key ->
                            repository.createRooms(
                                draft.prefix.trim(),
                                draft.fromNumber!!,
                                draft.toNumber!!,
                                draft.floorNumber,
                                category,
                                key,
                            )
                        }
                    }
                _uiState.value = _uiState.value.copy(saving = false)
                result.fold(
                    onSuccess = { batch ->
                        val skipped = if (batch.omitted.isEmpty()) "" else " (${batch.omitted.size} ya existían)"
                        val converted =
                            if (batch.converted ==
                                0
                            ) {
                                ""
                            } else {
                                ", ${batch.converted} mesas pasaron a habitación"
                            }
                        showNotice("Se crearon ${batch.created} habitaciones$skipped$converted", isError = false)
                        onDone()
                        refresh()
                    },
                    onFailure = {
                        showNotice(
                            it.toUserFacingMessage("No se pudieron crear las habitaciones."),
                            isError = true,
                        )
                    },
                )
            }
        }

        private fun showNotice(
            text: String,
            isError: Boolean,
        ) {
            noticeJob?.cancel()
            _uiState.value = _uiState.value.copy(notice = text, noticeIsError = isError)
            noticeJob =
                viewModelScope.launch {
                    delay(NOTICE_MILLIS)
                    _uiState.value = _uiState.value.copy(notice = null)
                }
        }

        private companion object {
            const val NOTICE_MILLIS = 3_500L
        }
    }

/** «Habitación 2» antes que «Habitación 10»: los números se comparan como números. */
internal fun String.naturalKey(): String = replace(Regex("\\d+")) { it.value.padStart(8, '0') }.lowercase()

/** El texto de una fila: «Piso 3 · Doble · QR activo» o, con huésped, «ocupada». */
internal fun roomDetail(room: HotelRoom): String =
    listOfNotNull(
        room.floor?.let { "Piso $it" },
        room.category
            ?.takeIf { it.isNotBlank() }
            ?.trim()
            ?.replaceFirstChar { it.uppercase() },
        if (room.occupied) "ocupada" else "QR activo",
    ).joinToString(" · ")

/** El inicio de la pantalla de habitaciones lo pinta [HotelRoomsAdminPage]; esta lo conecta con el servidor. */
@Composable
internal fun HotelRoomsAdminSection(
    colors: OperationalColors,
    readOnly: Boolean,
    onBack: () -> Unit,
    onOpenRoom: (Int) -> Unit,
    viewModel: HotelRoomsAdminViewModel = viewModel(),
) {
    val state by viewModel.uiState
    val context = LocalContext.current
    var draft by rememberSaveable(stateSaver = RoomBatchDraftSaver) {
        mutableStateOf(RoomBatchDraft(from = "101", to = "110", floor = "1"))
    }
    LaunchedEffect(Unit) { viewModel.refresh() }
    HotelRoomsAdminPage(
        state = state,
        draft = draft,
        colors = colors,
        readOnly = readOnly,
        onDraft = { draft = it },
        onCreate = { viewModel.create(draft) {} },
        onOpenRoom = { onOpenRoom(it.id) },
        onPrintAll = {
            val lines = state.rooms.orEmpty().mapNotNull { room -> room.qrUrl?.let { "${room.name}: $it" } }
            if (lines.isNotEmpty()) {
                val send =
                    Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, lines.joinToString("\n"))
                runCatching {
                    context.startActivity(
                        Intent.createChooser(send, "QR de las habitaciones").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            }
        },
        onRetry = viewModel::refresh,
        onBack = onBack,
    )
}

private val RoomBatchDraftSaver =
    androidx.compose.runtime.saveable.listSaver<RoomBatchDraft, String>(
        save = { listOf(it.prefix, it.from, it.to, it.floor, it.category) },
        restore = { RoomBatchDraft(it[0], it[1], it[2], it[3], it[4]) },
    )

/** A01 como el marco del lienzo: crear en lote con piso y categoría, la lista con su QR y el botón de imprimir. */
@Composable
internal fun HotelRoomsAdminPage(
    state: HotelRoomsUiState,
    draft: RoomBatchDraft,
    colors: OperationalColors,
    readOnly: Boolean,
    onDraft: (RoomBatchDraft) -> Unit,
    onCreate: () -> Unit,
    onOpenRoom: (HotelRoom) -> Unit,
    onPrintAll: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    val enabled = !readOnly && !state.saving
    AdminSectionPage(
        title = "Habitaciones",
        eyebrow = "Administración",
        colors = colors,
        notice = state.notice,
        noticeIsError = state.noticeIsError,
        onBack = onBack,
    ) {
        if (!readOnly) {
            item(key = "lote") {
                AdminPanel(
                    colors,
                    Modifier.adminRise(1),
                    padding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                ) {
                    AdminCapsLabel("Crear en lote", colors, inset = false)
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AdminTextField(
                            "Prefijo",
                            draft.prefix,
                            { onDraft(draft.copy(prefix = it.take(70))) },
                            colors,
                            Modifier.weight(1.5f),
                            enabled = enabled,
                        )
                        AdminTextField(
                            "Desde",
                            draft.from,
                            { onDraft(draft.copy(from = it.filter(Char::isDigit).take(5))) },
                            colors,
                            Modifier.weight(1f),
                            keyboardType = KeyboardType.Number,
                            enabled = enabled,
                        )
                        AdminTextField(
                            "Hasta",
                            draft.to,
                            { onDraft(draft.copy(to = it.filter(Char::isDigit).take(5))) },
                            colors,
                            Modifier.weight(1f),
                            keyboardType = KeyboardType.Number,
                            enabled = enabled,
                        )
                    }
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AdminTextField(
                            "Piso",
                            draft.floor,
                            { onDraft(draft.copy(floor = it.filter(Char::isDigit).take(3))) },
                            colors,
                            Modifier.weight(1f),
                            keyboardType = KeyboardType.Number,
                            enabled = enabled,
                        )
                        AdminTextField(
                            "Categoría",
                            draft.category,
                            { onDraft(draft.copy(category = it.take(30))) },
                            colors,
                            Modifier.weight(1.5f),
                            placeholder = "Doble, suite…",
                            enabled = enabled,
                        )
                    }
                    AdminPrimaryButton(
                        title = createLabel(draft.count),
                        colors = colors,
                        modifier = Modifier.padding(top = 12.dp),
                        busy = state.saving,
                        enabled = enabled && draft.count > 0,
                        fill = colors.accentLime,
                        contentColor = colors.accentInk,
                        onClick = onCreate,
                    )
                }
            }
        }
        val rooms = state.rooms
        when {
            rooms == null && state.error != null -> item(key = "error") { AdminLoadError(state.error, colors, onRetry) }
            rooms == null -> item(key = "cargando") { HotelRoomsHint("Cargando habitaciones…", colors) }
            rooms.isEmpty() ->
                item(key = "vacio") {
                    HotelRoomsHint("Todavía no hay habitaciones. Crea la primera arriba.", colors)
                }
            else -> {
                item(key = "lista") {
                    AdminPanel(
                        colors,
                        Modifier.adminRise(2),
                        padding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                    ) {
                        rooms.forEachIndexed { index, room ->
                            if (index > 0) AdminRowDivider(colors)
                            AdminRow(
                                title = room.name,
                                colors = colors,
                                detail = roomDetail(room),
                                onClick = { onOpenRoom(room) },
                            ) { QrChip(colors) }
                        }
                    }
                }
                item(key = "imprimir") {
                    AdminPrimaryButton(
                        title = "Imprimir QR de todas",
                        colors = colors,
                        modifier = Modifier.adminRise(3),
                        enabled = rooms.any { it.qrUrl != null },
                        onClick = onPrintAll,
                    )
                }
            }
        }
    }
}

internal fun createLabel(count: Int): String =
    when (count) {
        0 -> "Crear habitaciones con QR"
        1 -> "Crear 1 habitación con QR"
        else -> "Crear $count habitaciones con QR"
    }

@Composable
private fun QrChip(colors: OperationalColors) {
    Box(
        Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(colors.textPrimary.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center,
    ) { Text("QR", fontSize = 10.sp, fontWeight = FontWeight.Black, color = colors.textPrimary) }
}

@Composable
private fun HotelRoomsHint(
    text: String,
    colors: OperationalColors,
) {
    Text(
        text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = colors.textMuted,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 12.dp),
    )
}
