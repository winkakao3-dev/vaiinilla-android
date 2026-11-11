package com.vaiinilla.app.ui.admin

import com.vaiinilla.app.data.cashcut.AccountingReport
import com.vaiinilla.app.data.cashcut.CashCutJson
import com.vaiinilla.app.data.cashcut.CashCutRepository
import com.vaiinilla.app.data.cashcut.DayCut
import com.vaiinilla.app.data.cashcut.ShiftCut
import com.vaiinilla.app.ui.operational.StaffHarness
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.util.concurrent.CopyOnWriteArrayList

private const val BREAKDOWN =
    """
    "resumen": {"ventas": "350.00", "pedidos": 4, "ticket_promedio": "87.50", "articulos": 6,
      "importe_articulos": "350.00", "descuentos": "0.00", "envios": "0.00", "rentas": "100.00"},
    "formas_pago": [{"metodo": "efectivo", "pedidos": 2, "monto": "220.00"}],
    "propinas": {"total": "25.00", "efectivo": "0.00", "terminal": "25.00"},
    "cancelados": {"pedidos": 1, "monto": "120.00"},
    "por_categoria": [], "por_producto": [], "por_personal": [], "por_espacio": []
    """

private const val SHIFT =
    """
    {"id": "s-1", "fecha_operativa": "2026-11-09", "abierta_en": "2026-11-09T16:00:00.000Z",
     "cerrada_en": "2026-11-09T22:00:00.000Z", "cierre_automatico": false,
     "abierta_por": {"usuario_id": "u-2", "nombre": "Carla Caja"},
     "cerrada_por": {"usuario_id": "u-2", "nombre": "Carla Caja"},
     "efectivo": {"fondo": "500.00", "cobros_efectivo": "220.00", "propinas": "0.00", "recargas": "0.00",
       "entradas": "0.00", "salidas": "50.00", "devoluciones_efectivo": "0.00", "efectivo_esperado": "670.00",
       "contado": "665.00", "diferencia": "-5.00", "arqueo": "falta"}}
    """

private class FakeCashCuts : CashCutRepository {
    @Volatile var csv: Result<String> = Result.success("fecha,ventas\n2026-11-09,350.00")
    val dayRequests = CopyOnWriteArrayList<LocalDate?>()
    val shiftRequests = CopyOnWriteArrayList<String>()
    val accountingRequests = CopyOnWriteArrayList<Pair<LocalDate, LocalDate>>()
    val csvRequests = CopyOnWriteArrayList<Pair<LocalDate, LocalDate>>()
    val dayStarts = CopyOnWriteArrayList<String>()

    override fun shiftCut(sessionId: String): Result<ShiftCut> {
        shiftRequests += sessionId
        return Result.success(CashCutJson.shiftCut("""{"data": {"tipo": "final", "turno": $SHIFT, $BREAKDOWN}}"""))
    }

    override fun dayCut(date: LocalDate?): Result<DayCut> {
        dayRequests += date
        val day = (date ?: LocalDate.of(2026, 11, 9)).toString()
        return Result.success(
            CashCutJson.dayCut(
                """{"data": {"fecha": "$day", "tipo": "parcial", "turnos": [$SHIFT],
                  "efectivo": {"fondo": "500.00", "efectivo_esperado": "670.00"}, $BREAKDOWN}}""",
            ),
        )
    }

    override fun accounting(
        from: LocalDate,
        to: LocalDate,
    ): Result<AccountingReport> {
        accountingRequests += from to to
        return Result.success(
            CashCutJson.accounting(
                """{"data": {"negocio": "Boliche", "periodo": {"desde": "$from", "hasta": "$to", "dias": 1}, $BREAKDOWN,
                  "por_dia": [{"fecha": "$to", "pedidos": 4, "ventas": "350.00"}]}}""",
            ),
        )
    }

    override fun accountingCsv(
        from: LocalDate,
        to: LocalDate,
    ): Result<String> {
        csvRequests += from to to
        return csv
    }

    override fun dayStart(): Result<String?> = Result.success("05:00")

    override fun setDayStart(hhmm: String): Result<String> {
        dayStarts += hhmm
        return Result.success(hhmm)
    }
}

/**
 * Cortes y reportes del dueño dentro del ViewModel (Q03 y Q04): el corte del día y de cada turno, el reporte
 * contable por periodo y su CSV. Corre en la JVM; no cuenta como recorrido en teléfono.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecorridosCortesViewModelTest {
    private val repo = FakeCashCuts()
    private val today = LocalDate.of(2026, 11, 9)
    private lateinit var vm: AdminCashCutViewModel

    private val ui get() = vm.uiState.value

    @Before
    fun setUp() {
        StaffHarness.installMain()
        vm = AdminCashCutViewModel(repo)
        vm.today = { today }
        vm.onVisible("negocio-1")
        StaffHarness.awaitUntil("el corte del día") { ui.dayCut != null && ui.dayStart == "05:00" }
    }

    @Test
    fun q03_alAbrirMuestraElCorteDeHoyConSusTurnos() {
        assertEquals("2026-11-09", ui.dayCut!!.date)
        assertEquals(1, ui.dayCut!!.shifts.size)
        assertEquals("670.00", ui.dayCut!!.cash.expected)
        assertEquals(
            "350.00",
            ui.dayCut!!
                .breakdown.summary.sales,
        )
    }

    @Test
    fun q03_ayerSePideConSuFechaYNoSePasaDeHoy() {
        vm.moveDay(-1)
        StaffHarness.awaitUntil("el corte de ayer") { ui.dayCut?.date == "2026-11-08" }
        vm.moveDay(1)
        StaffHarness.awaitUntil("el corte de hoy") { ui.dayCut?.date == "2026-11-09" }
        val requests = repo.dayRequests.size
        vm.moveDay(1)
        Thread.sleep(150)
        assertEquals(requests, repo.dayRequests.size)
    }

    @Test
    fun q03_abrirUnTurnoTraeSuCorteConElArqueoYCerrarloLoQuita() {
        vm.openShift("s-1")
        StaffHarness.awaitUntil("el corte del turno") { ui.shiftCut != null && !ui.shiftLoading }
        assertEquals(listOf("s-1"), repo.shiftRequests.toList())
        assertEquals(
            "665.00",
            ui.shiftCut!!
                .shift.cash.counted,
        )
        vm.closeShift()
        assertNull(ui.shiftCut)
    }

    @Test
    fun q03_cambiarLaHoraEnQueEmpiezaElDiaLaGuardaYVuelveAPedirElCorte() {
        vm.changeDayStart("04:00")
        StaffHarness.awaitUntil("la hora guardada") {
            ui.dayStart == "04:00" &&
                ui.notice == "El día empieza a las 04:00."
        }
        assertEquals(listOf("04:00"), repo.dayStarts.toList())
    }

    @Test
    fun q04_elReportePideElPeriodoElegidoYCambiaConEl() {
        vm.selectTab(CashCutTab.REPORT)
        StaffHarness.awaitUntil("el reporte de hoy") { ui.report != null }
        assertEquals(today to today, repo.accountingRequests.last())
        vm.selectPeriod(ReportPeriod.THIS_MONTH)
        StaffHarness.awaitUntil("el reporte del mes") { ui.report != null && repo.accountingRequests.size >= 2 }
        assertEquals(LocalDate.of(2026, 11, 1) to today, repo.accountingRequests.last())
        assertEquals("Boliche", ui.report!!.business)
    }

    @Test
    fun q04_elCsvSePideDelMismoPeriodoYSeEntregaConSuNombre() {
        vm.selectPeriod(ReportPeriod.LAST_MONTH)
        StaffHarness.awaitUntil("el reporte") { ui.report != null }
        vm.exportCsv()
        StaffHarness.awaitUntil("el archivo") { ui.csv != null && !ui.exporting }
        assertEquals(LocalDate.of(2026, 10, 1) to LocalDate.of(2026, 10, 31), repo.csvRequests.single())
        assertEquals("reporte-contable-2026-10-01-a-2026-10-31.csv", ui.csv!!.fileName)
        assertTrue(ui.csv!!.content.startsWith("fecha,ventas"))
        vm.csvShared(true)
        assertNull(ui.csv)
    }

    @Test
    fun q04_siElCsvFallaSeAvisaYNoQuedaArchivo() {
        repo.csv = Result.failure(java.io.IOException("sin red"))
        vm.exportCsv()
        StaffHarness.awaitUntil("el aviso") { ui.noticeIsError && ui.notice != null && !ui.exporting }
        assertNull(ui.csv)
    }

    @Test
    fun cajaVeSuCorteSinPedirLaHoraEnQueEmpiezaElDia() {
        val caja = AdminCashCutViewModel(repo)
        caja.today = { today }
        caja.onVisible("caja-1", canSetDayStart = false)
        StaffHarness.awaitUntil("el corte del día de Caja") { caja.uiState.value.dayCut != null }
        assertNull(caja.uiState.value.dayStart)
    }
}