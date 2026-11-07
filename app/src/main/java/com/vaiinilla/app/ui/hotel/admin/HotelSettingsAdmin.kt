package com.vaiinilla.app.ui.hotel.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.RetryKeys
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.hotel.HotelConfig
import com.vaiinilla.app.domain.hotel.HotelRepository
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.hotel.hotelViewModel
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.admin.AdminCapsLabel
import com.vaiinilla.app.ui.screens.admin.AdminLoadError
import com.vaiinilla.app.ui.screens.admin.AdminPanel
import com.vaiinilla.app.ui.screens.admin.AdminPrimaryButton
import com.vaiinilla.app.ui.screens.admin.AdminRow
import com.vaiinilla.app.ui.screens.admin.AdminRowDivider
import com.vaiinilla.app.ui.screens.admin.AdminSectionPage
import com.vaiinilla.app.ui.screens.admin.AdminTextField
import com.vaiinilla.app.ui.screens.admin.AdminToggleRow
import com.vaiinilla.app.ui.screens.admin.adminRise
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class HotelSettingsUiState(
    val config: HotelConfig? = null,
    val error: String? = null,
    val saving: Boolean = false,
    val notice: String? = null,
    val noticeIsError: Boolean = false,
)

/** A02: lo que el dueño decide de su hotel; cada interruptor es del cliente, la app no fija ningún precio. */
@HiltViewModel
class HotelSettingsAdminViewModel
    @Inject
    constructor(
        private val repository: HotelRepository,
    ) : ViewModel() {
        private val _uiState = mutableStateOf(HotelSettingsUiState())
        val uiState: State<HotelSettingsUiState> = _uiState
        private val retryKeys = RetryKeys()
        private var noticeJob: Job? = null

        fun refresh() {
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { repository.hotelConfig() }
                _uiState.value =
                    result.fold(
                        onSuccess = { _uiState.value.copy(config = it, error = null) },
                        onFailure = {
                            _uiState.value.copy(
                                error = it.toUserFacingMessage("No se pudo cargar la configuración del hotel."),
                            )
                        },
                    )
            }
        }

        fun save(config: HotelConfig) {
            if (_uiState.value.saving) return
            _uiState.value = _uiState.value.copy(saving = true)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        retryKeys.withKey("hotel-config", config) { key -> repository.saveHotelConfig(config, key) }
                    }
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(config = it, saving = false)
                        showNotice("Cambios guardados", isError = false)
                    },
                    onFailure = {
                        _uiState.value = _uiState.value.copy(saving = false)
                        showNotice(it.toUserFacingMessage("No se pudieron guardar los cambios."), isError = true)
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

/** La hora que se escribe: solo dígitos y los dos puntos, hasta «HH:mm». */
internal fun clockInput(raw: String): String = raw.filter { it.isDigit() || it == ':' }.take(5)

/** «HH:mm» válida; vacío no cuenta. */
internal fun isClock(value: String): Boolean = Regex("""^([01]?\d|2[0-3]):[0-5]\d$""").matches(value)

@Composable
internal fun HotelSettingsAdminSection(
    colors: OperationalColors,
    readOnly: Boolean,
    onBack: () -> Unit,
    viewModel: HotelSettingsAdminViewModel = hotelViewModel(),
) {
    val state by viewModel.uiState
    LaunchedEffect(Unit) { viewModel.refresh() }
    val config = state.config
    if (config == null) {
        AdminSectionPage(
            title = "Hotel",
            eyebrow = "Flujo de mi tienda",
            colors = colors,
            notice = state.notice,
            noticeIsError = state.noticeIsError,
            onBack = onBack,
        ) {
            item(key = "cargando") {
                val error = state.error
                if (error != null) {
                    AdminLoadError(error, colors, viewModel::refresh)
                } else {
                    Text(
                        "Cargando tu hotel…",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textMuted,
                    )
                }
            }
        }
        return
    }
    HotelSettingsPage(
        saved = config,
        colors = colors,
        readOnly = readOnly,
        saving = state.saving,
        notice = state.notice,
        noticeIsError = state.noticeIsError,
        onSave = viewModel::save,
        onBack = onBack,
    )
}

private enum class SettingsEdit { KITCHEN, CHECKOUT, LATE_TIME, LATE_PRICE }

/** A02 como el marco del lienzo: plantilla, room service, salida y huéspedes; todo se guarda con un botón. */
@Composable
internal fun HotelSettingsPage(
    saved: HotelConfig,
    colors: OperationalColors,
    readOnly: Boolean,
    saving: Boolean,
    notice: String?,
    noticeIsError: Boolean,
    onSave: (HotelConfig) -> Unit,
    onBack: () -> Unit,
    startEditing: Int? = null,
) {
    var config by remember(saved) { mutableStateOf(saved) }
    var editing by remember { mutableStateOf(startEditing?.let { SettingsEdit.entries.getOrNull(it) }) }
    val enabled = !readOnly && !saving
    val lateOn = config.lateCheckoutTime != null
    AdminSectionPage(
        title = "Hotel",
        eyebrow = "Flujo de mi tienda",
        colors = colors,
        notice = notice,
        noticeIsError = noticeIsError,
        onBack = onBack,
    ) {
        item(key = "plantilla") {
            AdminPanel(
                colors,
                Modifier.adminRise(1),
                highlighted = true,
                padding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Text(
                    "Plantilla: Hotel con room service",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.textPrimary,
                )
                Text(
                    "Los espacios se llaman habitaciones. Los pedidos van al cuarto y los cargos se juntan en la cuenta del huésped.",
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMuted,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        item(key = "room-service") {
            Group("Room service", colors) {
                AdminToggleRow(
                    "Cargar a la habitación",
                    config.chargeToRoom,
                    colors,
                    { config = config.copy(chargeToRoom = it) },
                    detail = "El huésped paga todo al salir",
                    enabled = enabled,
                )
                AdminRowDivider(colors)
                AdminToggleRow(
                    "Pago con tarjeta al pedir",
                    config.cardAtOrderEnabled,
                    colors,
                    { config = config.copy(cardAtOrderEnabled = it) },
                    detail = "Opcional para quien lo prefiera",
                    enabled = enabled,
                )
                AdminRowDivider(colors)
                AdminRow(
                    title = "Horario de cocina",
                    colors = colors,
                    detail = kitchenLabel(config),
                    onClick =
                        if (enabled) {
                            (
                                {
                                    editing =
                                        if (editing == SettingsEdit.KITCHEN) null else SettingsEdit.KITCHEN
                                }
                            )
                        } else {
                            null
                        },
                ) { EditLabel(colors) }
                if (editing == SettingsEdit.KITCHEN) {
                    Row(Modifier.padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AdminTextField(
                            "Desde",
                            config.kitchenFrom.orEmpty(),
                            { config = config.copy(kitchenFrom = clockInput(it).ifEmpty { null }) },
                            colors,
                            Modifier.weight(1f),
                            placeholder = "07:00",
                            keyboardType = KeyboardType.Number,
                        )
                        AdminTextField(
                            "Hasta",
                            config.kitchenTo.orEmpty(),
                            { config = config.copy(kitchenTo = clockInput(it).ifEmpty { null }) },
                            colors,
                            Modifier.weight(1f),
                            placeholder = "23:00",
                            keyboardType = KeyboardType.Number,
                        )
                    }
                }
            }
        }
        item(key = "salida") {
            Group("Salida", colors) {
                AdminRow(
                    title = "Hora de salida",
                    colors = colors,
                    detail = "Después se pide salida tardía",
                    onClick =
                        if (enabled) {
                            (
                                {
                                    editing =
                                        if (editing == SettingsEdit.CHECKOUT) null else SettingsEdit.CHECKOUT
                                }
                            )
                        } else {
                            null
                        },
                ) { Value(config.checkoutTime ?: "—", colors) }
                if (editing == SettingsEdit.CHECKOUT) {
                    AdminTextField(
                        "Hora de salida",
                        config.checkoutTime.orEmpty(),
                        {
                            val time = clockInput(it).ifEmpty { null }
                            config = config.copy(checkoutTime = time)
                        },
                        colors,
                        Modifier.padding(bottom = 12.dp),
                        placeholder = "12:00",
                        keyboardType = KeyboardType.Number,
                    )
                }
                AdminRowDivider(colors)
                AdminToggleRow(
                    "Salida tardía",
                    lateOn,
                    colors,
                    { on ->
                        config =
                            if (on) {
                                config.copy(lateCheckoutTime = config.lateCheckoutTime ?: DEFAULT_LATE_CHECKOUT)
                            } else {
                                config.copy(lateCheckoutTime = null)
                            }
                    },
                    detail =
                        if (lateOn) {
                            "Hasta las ${config.lateCheckoutTime} · el precio lo pones tú"
                        } else {
                            "No se ofrece"
                        },
                    enabled = enabled,
                )
                if (lateOn) {
                    AdminRowDivider(colors)
                    AdminRow(
                        title = "Precio de la salida tardía",
                        colors = colors,
                        detail = "Por solicitud aprobada",
                        onClick =
                            if (enabled) {
                                (
                                    {
                                        editing =
                                            if (editing == SettingsEdit.LATE_PRICE) null else SettingsEdit.LATE_PRICE
                                    }
                                )
                            } else {
                                null
                            },
                    ) { Value(config.lateCheckoutPrice?.let(::moneyLabel) ?: "Sin precio", colors) }
                    if (editing == SettingsEdit.LATE_PRICE) {
                        AdminTextField(
                            "Precio",
                            config.lateCheckoutPrice.orEmpty(),
                            { config = config.copy(lateCheckoutPrice = priceInput(it).ifEmpty { null }) },
                            colors,
                            Modifier.padding(bottom = 12.dp),
                            placeholder = "0.00",
                            keyboardType = KeyboardType.Decimal,
                            prefix = "$",
                        )
                    }
                }
            }
        }
        item(key = "huespedes") {
            Group("Huéspedes", colors) {
                AdminToggleRow(
                    "Pedir al hotel",
                    config.requestsEnabled,
                    colors,
                    { config = config.copy(requestsEnabled = it) },
                    detail = "Toallas, limpieza y cargadores",
                    enabled = enabled,
                )
                AdminRowDivider(colors)
                AdminToggleRow(
                    "Sin molestar",
                    config.doNotDisturbEnabled,
                    colors,
                    { config = config.copy(doNotDisturbEnabled = it) },
                    detail = "Avisa al personal de limpieza",
                    enabled = enabled,
                )
            }
        }
        if (!readOnly) {
            item(key = "guardar") {
                AdminPrimaryButton(
                    title = "Guardar cambios",
                    colors = colors,
                    modifier = Modifier.adminRise(4),
                    busy = saving,
                    enabled = config != saved && settingsValid(config),
                ) { onSave(config.withPaddedClocks()) }
            }
        }
    }
}

private const val DEFAULT_LATE_CHECKOUT = "15:00"

/** Cada hora escrita debe ser «HH:mm»; la de cocina va completa o no va. */
internal fun settingsValid(config: HotelConfig): Boolean {
    val kitchenOk =
        (config.kitchenFrom == null) == (config.kitchenTo == null) &&
            listOfNotNull(config.kitchenFrom, config.kitchenTo).all(::isClock)
    val checkoutOk = config.checkoutTime?.let(::isClock) ?: true
    val lateOk = config.lateCheckoutTime?.let(::isClock) ?: true
    return kitchenOk && checkoutOk && lateOk
}

/** El servidor pide «HH:mm» con dos cifras en la hora: «7:30» se manda como «07:30». */
internal fun HotelConfig.withPaddedClocks(): HotelConfig {
    fun pad(value: String?): String? = value?.let { if (Regex("""^\d:[0-5]\d$""").matches(it)) "0$it" else it }
    return copy(
        kitchenFrom = pad(kitchenFrom),
        kitchenTo = pad(kitchenTo),
        lateCheckoutTime = pad(lateCheckoutTime),
        checkoutTime = pad(checkoutTime),
    )
}

internal fun kitchenLabel(config: HotelConfig): String {
    val from = config.kitchenFrom
    val to = config.kitchenTo
    return if (from != null && to != null) "$from a $to" else "Sin horario propio"
}

/** Un monto en el campo: dígitos y un punto con hasta dos decimales. */
internal fun priceInput(raw: String): String {
    val clean = raw.filter { it.isDigit() || it == '.' }
    val dot = clean.indexOf('.')
    if (dot < 0) return clean.take(6)
    return clean.substring(0, dot).take(6) + "." + clean.substring(dot + 1).replace(".", "").take(2)
}

@Composable
private fun Group(
    title: String,
    colors: OperationalColors,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth().adminRise(2)) {
        AdminCapsLabel(title, colors, Modifier.padding(top = 6.dp, bottom = 8.dp))
        AdminPanel(colors, padding = PaddingValues(horizontal = 18.dp, vertical = 2.dp), content = content)
    }
}

@Composable
private fun EditLabel(colors: OperationalColors) {
    Text(
        "Editar",
        fontSize = 13.sp,
        fontWeight = FontWeight.ExtraBold,
        color = if (colors.isDark) colors.accentLime else colors.accentInk,
    )
}

@Composable
private fun Value(
    text: String,
    colors: OperationalColors,
) {
    Text(text, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary, maxLines = 1)
}
