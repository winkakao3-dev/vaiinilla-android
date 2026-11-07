package com.vaiinilla.app.ui.admin

import com.vaiinilla.app.domain.guardianship.RefundMethod
import com.vaiinilla.app.domain.guardianship.StudentRequestKind
import com.vaiinilla.app.domain.repository.CatalogRepositoryException
import com.vaiinilla.app.domain.school.DayClosure
import com.vaiinilla.app.domain.school.GuardianRequest
import com.vaiinilla.app.domain.school.MenuCalendar
import com.vaiinilla.app.domain.school.ReservedDay
import com.vaiinilla.app.domain.school.RosterStudent
import com.vaiinilla.app.domain.school.SchoolAdminRepository
import com.vaiinilla.app.domain.school.SchoolGuardianship
import com.vaiinilla.app.domain.school.SchoolSettings
import com.vaiinilla.app.domain.school.SchoolStudentRequest
import com.vaiinilla.app.ui.admin.school.AdminSchoolViewModel
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.operational.StaffHarness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.util.concurrent.CopyOnWriteArrayList

private class FakeSchool : SchoolAdminRepository {
    val today: LocalDate = LocalDate.of(2026, 11, 9)

    @Volatile var settings =
        SchoolSettings("ABC123", true, true, "50.00", "2000.00", 5, "09:00", "08:00")

    @Volatile var requests =
        listOf(GuardianRequest("g1", "Marta López", null, "Ana López", "A-001", "3°B", false, true, 0))

    @Volatile var studentRequests =
        listOf(
            SchoolStudentRequest(
                "s1",
                StudentRequestKind.REFUND,
                RefundMethod.CASH,
                null,
                "150.00",
                "150.00",
                "Marta López",
                null,
                "Ana López",
                "A-001",
                "3°B",
            ),
            SchoolStudentRequest(
                "s2",
                StudentRequestKind.DATA_REMOVAL,
                null,
                null,
                "0.00",
                "0.00",
                "Luis Pérez",
                null,
                "Beto Pérez",
                "A-002",
                "4°A",
            ),
        )

    @Volatile var guardianships =
        listOf(SchoolGuardianship("t1", "Marta López", null, false, "u-ana", "Ana López", "A-001", "3°B"))

    @Volatile var failWith: Throwable? = null
    val saved = CopyOnWriteArrayList<Triple<SchoolSettings, Boolean, String>>()
    val reviews = CopyOnWriteArrayList<Pair<String, Boolean>>()
    val resolved = CopyOnWriteArrayList<String>()
    val rejected = CopyOnWriteArrayList<Pair<String, String>>()
    val principals = CopyOnWriteArrayList<String>()
    val closedDays = CopyOnWriteArrayList<Pair<LocalDate, String>>()
    val rosters = CopyOnWriteArrayList<List<RosterStudent>>()
    val menus = CopyOnWriteArrayList<Pair<LocalDate, List<Int>>>()

    private fun <T> answer(ok: () -> T): Result<T> = failWith?.let { Result.failure(it) } ?: Result.success(ok())

    override fun settings() = Result.success(settings)

    override fun save(
        settings: SchoolSettings,
        regenerateJoinCode: Boolean,
        idempotencyKey: String,
    ): Result<SchoolSettings> {
        saved += Triple(settings, regenerateJoinCode, idempotencyKey)
        return answer { settings.also { this.settings = it } }
    }

    override fun removeJoinCode(
        settings: SchoolSettings,
        idempotencyKey: String,
    ) = answer { settings.copy(joinCode = null, joinRequiresCode = false) }

    override fun menuCalendar(
        from: LocalDate?,
        to: LocalDate?,
    ) = Result.success(MenuCalendar(today, emptyMap(), emptyList()))

    override fun saveMenu(
        date: LocalDate,
        productIds: List<Int>,
        idempotencyKey: String,
    ) = answer {
        menus.add(date to productIds)
        Unit
    }

    override fun closeDay(
        date: LocalDate,
        reason: String,
        idempotencyKey: String,
    ) = answer {
        closedDays.add(date to reason)
        DayClosure(3, "100.00", 0)
    }

    override fun reopenDay(
        date: LocalDate,
        idempotencyKey: String,
    ) = answer { Unit }

    override fun reservedDay(date: LocalDate) = Result.success(ReservedDay(date, 0, null, emptyList()))

    override fun roster() = Result.success(emptyList<RosterStudent>())

    override fun saveRoster(
        students: List<RosterStudent>,
        idempotencyKey: String,
    ) = answer {
        rosters.add(students)
        students.size
    }

    override fun guardianRequests() = Result.success(requests)

    override fun reviewGuardianRequest(
        id: String,
        approve: Boolean,
        idempotencyKey: String,
    ) = answer {
        reviews.add(id to approve)
        Unit
    }

    override fun searchGuardianships(query: String) = Result.success(guardianships)

    override fun removeGuardianship(
        id: String,
        idempotencyKey: String,
    ) = answer { Unit }

    override fun makePrincipal(
        id: String,
        idempotencyKey: String,
    ) = answer {
        principals.add(id)
        Unit
    }

    override fun studentRequests() = Result.success(studentRequests)

    override fun resolveStudentRequest(
        id: String,
        idempotencyKey: String,
    ) = answer {
        resolved.add(id)
        Unit
    }

    override fun rejectStudentRequest(
        id: String,
        reason: String,
        idempotencyKey: String,
    ) = answer {
        rejected.add(id to reason)
        Unit
    }
}

/**
 * La escuela del admin dentro del ViewModel (T09, F01, F02, F10, F13 y F14): ajustes, padrón, menú del día, tutores
 * que esperan, quién cuida a quién y devoluciones y bajas. Corre en la JVM; no cuenta como recorrido en teléfono.
 */
class RecorridosEscuelaAdminViewModelTest {
    private val repo = FakeSchool()
    private lateinit var vm: AdminSchoolViewModel

    private val ui get() = vm.uiState.value

    @Before
    fun setUp() {
        StaffHarness.installMain()
        vm = AdminSchoolViewModel(repo)
        vm.onVisible("escuela-1")
        StaffHarness.awaitUntil("la escuela cargada") {
            ui.settings != null && ui.requests != null && ui.studentRequests != null && ui.menu != null &&
                ui.roster != null
        }
    }

    private fun waitFree() = StaffHarness.awaitUntil("sin tarea en curso") { !ui.saving }

    @Test
    fun t09_guardarAjustesValidosAvisaYUnDiaDeMasNoSale() {
        vm.save(repo.settings.copy(daysAhead = 7))
        StaffHarness.awaitUntil("guardado") { ui.notice == "Guardado" && ui.settings?.daysAhead == 7 }
        waitFree()
        vm.save(repo.settings.copy(daysAhead = 20))
        StaffHarness.awaitUntil("el aviso") { ui.notice == "Se aparta de 0 a 14 días antes." && ui.noticeIsError }
        assertEquals(1, repo.saved.size)
    }

    @Test
    fun t09_unCodigoNuevoSePideAlServidorYSeAvisa() {
        vm.save(repo.settings, regenerateJoinCode = true)
        StaffHarness.awaitUntil("código nuevo") { ui.notice == "Código nuevo listo" }
        assertEquals(true, repo.saved.single().second)
    }

    @Test
    fun t09_elPadronYElMenuDelDiaSeGuardan() {
        vm.saveRoster(listOf(RosterStudent("A-001", "Ana", "3°B"), RosterStudent("A-002", "Beto", "4°A")))
        StaffHarness.awaitUntil("padrón guardado") { ui.notice == "Padrón guardado: 2 alumnos" }
        waitFree()
        vm.saveMenu(repo.today, emptySet())
        StaffHarness.awaitUntil("menú del día") {
            ui.notice == "Ese día se vende todo el menú" &&
                ui.savingMenuDate == null
        }
        assertEquals(listOf(repo.today to emptyList<Int>()), repo.menus.toList())
    }

    @Test
    fun t09_cerrarUnDiaDiceCuantosPedidosSeCancelaronYCuantoSeDevolvio() {
        vm.closeDay(repo.today, "  Junta de maestros ")
        StaffHarness.awaitUntil("día cerrado") { ui.notice?.contains("3") == true && repo.closedDays.isNotEmpty() }
        assertEquals(repo.today to "Junta de maestros", repo.closedDays.single())
    }

    @Test
    fun f01_aprobarAlTutorLoQuitaDeLasSolicitudesYDiceQuienCuidaA() {
        val request = ui.requests!!.single()
        vm.review(request, approve = true)
        StaffHarness.awaitUntil("aprobado") {
            ui.notice == "Marta López ya cuida a Ana López" &&
                ui.requests?.isEmpty() == true
        }
        assertEquals(listOf("g1" to true), repo.reviews.toList())
    }

    @Test
    fun f02_rechazarAlTutorLoQuitaYAvisaQueSeRechazo() {
        vm.review(ui.requests!!.single(), approve = false)
        StaffHarness.awaitUntil("rechazado") { ui.notice == "Solicitud rechazada" && ui.requests?.isEmpty() == true }
        assertEquals(listOf("g1" to false), repo.reviews.toList())
    }

    @Test
    fun f02_siElServidorDiceQueYaSeRevisoSeExplicaYSeVuelveAPedirLaLista() {
        repo.failWith = CatalogRepositoryException("GUARDIANSHIP_NOT_PENDING", "x")
        vm.review(ui.requests!!.single(), approve = true)
        StaffHarness.awaitUntil("el aviso") {
            ui.notice == "Esa solicitud ya se revisó. Actualiza la lista." &&
                ui.noticeIsError
        }
    }

    @Test
    fun f10_elTutorPrincipalSeCambiaYSeDiceQuienPoneLosControles() {
        vm.searchGuardianships("An")
        StaffHarness.awaitUntil("la búsqueda") { ui.guardianships?.size == 1 }
        vm.makePrincipal(ui.guardianships!!.single())
        StaffHarness.awaitUntil("principal") { ui.notice == "Ahora Marta López pone los controles de Ana López" }
        assertEquals(listOf("t1"), repo.principals.toList())
    }

    @Test
    fun f10_unaBusquedaDeUnaLetraNoBuscaNada() {
        vm.searchGuardianships("A")
        Thread.sleep(500)
        assertEquals(null, ui.guardianships)
    }

    @Test
    fun f13_devolverElSaldoEnEfectivoDiceCuantoSaleDeLaCajaYQuitaLaSolicitud() {
        val refund = ui.studentRequests!!.first { it.id == "s1" }
        vm.resolve(refund)
        StaffHarness.awaitUntil("devuelto") {
            ui.notice == "Listo: salieron ${moneyLabel("150.00")} de la caja para Marta López" &&
                ui.studentRequests?.none { it.id == "s1" } == true
        }
        assertEquals(listOf("s1"), repo.resolved.toList())
    }

    @Test
    fun f13_sinAbrirLaCajaElEfectivoNoSaleYSeExplica() {
        repo.failWith = CatalogRepositoryException("CASH_SESSION_REQUIRED", "CASH_SESSION_REQUIRED")
        vm.resolve(ui.studentRequests!!.first { it.id == "s1" })
        StaffHarness.awaitUntil("el aviso") {
            ui.notice == "Abre la caja primero: el efectivo se registra en ella." &&
                ui.noticeIsError
        }
        assertTrue(ui.studentRequests!!.any { it.id == "s1" })
    }

    @Test
    fun f13_rechazarPideUnMotivoDeAlMenosTresLetras() {
        val refund = ui.studentRequests!!.first { it.id == "s1" }
        vm.reject(refund, "no")
        StaffHarness.awaitUntil("el aviso") { ui.notice == "Escribe por qué no procede." && ui.noticeIsError }
        assertTrue(repo.rejected.isEmpty())
        vm.reject(refund, "  Ya se entregó en mano ")
        StaffHarness.awaitUntil("rechazada") { ui.notice == "Le avisamos a Marta López" }
        assertEquals(listOf("s1" to "Ya se entregó en mano"), repo.rejected.toList())
    }

    @Test
    fun f14_laBajaDeDatosDejaAlAlumnoDadoDeBajaYSoloSeResuelveConSaldoEnCero() {
        val removal = ui.studentRequests!!.first { it.id == "s2" }
        assertTrue(removal.canResolve)
        // La devolución con saldo no se resuelve como baja: ese caso pide saldo en cero.
        assertEquals(
            false,
            ui.studentRequests!!
                .first { it.id == "s1" }
                .canResolve
                .not(),
        )
        vm.resolve(removal)
        StaffHarness.awaitUntil("baja") { ui.notice == "Beto Pérez quedó dado de baja" }
        assertEquals(listOf("s2"), repo.resolved.toList())
    }
}
