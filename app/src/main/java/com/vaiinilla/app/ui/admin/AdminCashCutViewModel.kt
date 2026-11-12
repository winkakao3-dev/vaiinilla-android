package com.vaiinilla.app.ui.admin

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.data.cashcut.AccountingReport
import com.vaiinilla.app.data.cashcut.CashCutRepository
import com.vaiinilla.app.data.cashcut.DayCut
import com.vaiinilla.app.data.cashcut.ShiftCut
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** Las dos pestañas de Negocio › Cortes y reportes. */
enum class CashCutTab(
    val label: String,
) {
    CUTS("Corte de caja"),
    REPORT("Reporte contable"),
}

/** Los periodos del reporte contable, como los pide el contador. */
enum class ReportPeriod(
    val label: String,
) {
    TODAY("Hoy"),
    YESTERDAY("Ayer"),
    WEEK("7 días"),
    THIS_MONTH("Este mes"),
    LAST_MONTH("Mes pasado"),
    ;

    fun range(today: LocalDate): Pair<LocalDate, LocalDate> =
        when (this) {
            TODAY -> today to today
            YESTERDAY -> today.minusDays(1) to today.minusDays(1)
            WEEK -> today.minusDays(6) to today
            THIS_MONTH -> today.withDayOfMonth(1) to today
            LAST_MONTH -> {
                val first = today.withDayOfMonth(1).minusMonths(1)
                first to first.withDayOfMonth(first.lengthOfMonth())
            }
        }
}

/** El CSV listo para compartirlo; la pantalla lo guarda en un archivo y abre el menú de compartir. */
data class CsvExport(
    val fileName: String,
    val content: String,
)

data class AdminCashCutUiState(
    val tab: CashCutTab = CashCutTab.CUTS,
    /** El día del corte; null mientras no se elige otro (hoy en la hora del negocio). */
    val day: LocalDate? = null,
    val dayCut: DayCut? = null,
    val dayLoading: Boolean = false,
    val dayError: String? = null,
    /** El turno abierto en detalle. */
    val shiftCut: ShiftCut? = null,
    val shiftLoading: Boolean = false,
    val period: ReportPeriod = ReportPeriod.TODAY,
    val report: AccountingReport? = null,
    val reportLoading: Boolean = false,
    val reportError: String? = null,
    /** A qué hora empieza el día del negocio ("05:00"); null si no se conoce o no se puede cambiar. */
    val dayStart: String? = null,
    val dayStartSaving: Boolean = false,
    val exporting: Boolean = false,
    val csv: CsvExport? = null,
    val notice: String? = null,
    val noticeIsError: Boolean = false,
) {
    /** El día que se ve: el elegido o el que dijo el servidor que es hoy. */
    val shownDay: LocalDate?
        get() = day ?: dayCut?.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
}

/** Negocio › Cortes y reportes, y el «Corte de caja» del menú de Caja (backend PR #201). */
@HiltViewModel
class AdminCashCutViewModel
    @Inject
    constructor(
        private val repository: CashCutRepository,
    ) : ViewModel() {
        private val _uiState = mutableStateOf(AdminCashCutUiState())
        val uiState: State<AdminCashCutUiState> = _uiState
        private var scope: String? = null
        private var noticeJob: Job? = null
        private var dayJob: Job? = null
        private var reportJob: Job? = null
        private var shiftJob: Job? = null

        /** Para probar sin reloj; en la app es hoy del teléfono. */
        internal var today: () -> LocalDate = { LocalDate.now(ZoneId.systemDefault()) }

        private fun update(block: AdminCashCutUiState.() -> AdminCashCutUiState) {
            _uiState.value = _uiState.value.block()
        }

        /** [canSetDayStart] es de Administración: Caja ve su corte sin el ajuste de la hora en que empieza el día. */
        fun onVisible(
            scope: String,
            canSetDayStart: Boolean = true,
        ) {
            if (scope != this.scope) {
                this.scope = scope
                _uiState.value = AdminCashCutUiState()
            }
            refresh()
            if (canSetDayStart) loadDayStart()
        }

        /** Solo Administración puede leerla; a Caja el servidor se la niega y por eso no la pide. */
        private fun loadDayStart() {
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { repository.dayStart() }
                update { copy(dayStart = result.getOrNull()) }
            }
        }

        /** El dueño decide a qué hora empieza su día; el corte se vuelve a pedir con la hora nueva. */
        fun changeDayStart(hhmm: String) {
            if (_uiState.value.dayStartSaving || hhmm == _uiState.value.dayStart) return
            update { copy(dayStartSaving = true) }
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { repository.setDayStart(hhmm) }
                result
                    .onSuccess { saved ->
                        update { copy(dayStartSaving = false, dayStart = saved, day = null, dayCut = null) }
                        notify("El día empieza a las $saved.")
                        loadDay()
                    }.onFailure { error ->
                        update { copy(dayStartSaving = false) }
                        notify(error.toUserFacingMessage("No se pudo guardar la hora."), isError = true)
                    }
            }
        }

        fun refresh() {
            when (_uiState.value.tab) {
                CashCutTab.CUTS -> loadDay()
                CashCutTab.REPORT -> loadReport()
            }
            _uiState.value.shiftCut?.let { openShift(it.shift.id) }
        }

        fun selectTab(tab: CashCutTab) {
            update { copy(tab = tab) }
            if (tab == CashCutTab.REPORT && _uiState.value.report == null) loadReport()
            if (tab == CashCutTab.CUTS && _uiState.value.dayCut == null) loadDay()
        }

        /** Un día antes o después; no pasa de hoy. */
        fun moveDay(days: Long) {
            val base = _uiState.value.shownDay ?: today()
            val next = base.plusDays(days)
            if (next.isAfter(today())) return
            update { copy(day = next) }
            loadDay()
        }

        fun loadDay() {
            dayJob?.cancel()
            update { copy(dayLoading = true, dayError = null) }
            val day = _uiState.value.day
            dayJob =
                viewModelScope.launch {
                    val result = withContext(Dispatchers.IO) { repository.dayCut(day) }
                    update {
                        copy(
                            dayLoading = false,
                            dayCut = result.getOrNull() ?: dayCut.takeIf { it?.date == day?.toString() },
                            dayError = result.exceptionOrNull()?.toUserFacingMessage("No se pudo cargar el corte."),
                        )
                    }
                }
        }

        fun openShift(sessionId: String) {
            shiftJob?.cancel()
            update { copy(shiftLoading = true) }
            shiftJob =
                viewModelScope.launch {
                    val result = withContext(Dispatchers.IO) { repository.shiftCut(sessionId) }
                    result
                        .onSuccess { cut -> update { copy(shiftLoading = false, shiftCut = cut) } }
                        .onFailure { error ->
                            update { copy(shiftLoading = false) }
                            notify(error.toUserFacingMessage("No se pudo cargar el turno."), isError = true)
                        }
                }
        }

        fun closeShift() {
            shiftJob?.cancel()
            update { copy(shiftCut = null, shiftLoading = false) }
        }

        fun selectPeriod(period: ReportPeriod) {
            if (period == _uiState.value.period && _uiState.value.report != null) return
            update { copy(period = period, report = null) }
            loadReport()
        }

        fun loadReport() {
            reportJob?.cancel()
            val (from, to) = _uiState.value.period.range(today())
            update { copy(reportLoading = true, reportError = null) }
            reportJob =
                viewModelScope.launch {
                    val result = withContext(Dispatchers.IO) { repository.accounting(from, to) }
                    update {
                        copy(
                            reportLoading = false,
                            report = result.getOrNull() ?: report,
                            reportError =
                                result.exceptionOrNull()?.toUserFacingMessage("No se pudo cargar el reporte."),
                        )
                    }
                }
        }

        /** Pide el CSV al servidor; la pantalla lo comparte cuando llega en [AdminCashCutUiState.csv]. */
        fun exportCsv() {
            if (_uiState.value.exporting) return
            val (from, to) = _uiState.value.period.range(today())
            update { copy(exporting = true) }
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { repository.accountingCsv(from, to) }
                result
                    .onSuccess { csv ->
                        update { copy(exporting = false, csv = CsvExport("reporte-contable-$from-a-$to.csv", csv)) }
                    }.onFailure { error ->
                        update { copy(exporting = false) }
                        notify(error.toUserFacingMessage("No se pudo descargar el reporte."), isError = true)
                    }
            }
        }

        fun csvShared(ok: Boolean) {
            update { copy(csv = null) }
            if (!ok) notify("No se pudo compartir el archivo.", isError = true)
        }

        private fun notify(
            message: String,
            isError: Boolean = false,
        ) {
            noticeJob?.cancel()
            update { copy(notice = message, noticeIsError = isError) }
            noticeJob =
                viewModelScope.launch {
                    delay(3_000)
                    update { copy(notice = null) }
                }
        }
    }
