package com.vaiinilla.app.ui.admin

import com.vaiinilla.app.core.venue.VenueContextStore
import com.vaiinilla.app.domain.admin.ForcedCloseResult
import com.vaiinilla.app.domain.admin.ManagedSpace
import com.vaiinilla.app.domain.admin.ReservationSchedule
import com.vaiinilla.app.domain.admin.SpaceBatchResult
import com.vaiinilla.app.domain.admin.SpacePatch
import com.vaiinilla.app.domain.admin.SpaceType
import com.vaiinilla.app.domain.repository.AdminSpacesRepository
import com.vaiinilla.app.ui.operational.FakeReservations
import com.vaiinilla.app.ui.operational.StaffHarness
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

internal class FakeSpaces : AdminSpacesRepository {
    @Volatile var batchResult: Result<SpaceBatchResult> = Result.success(SpaceBatchResult(10, 10, emptyList()))
    val batches = CopyOnWriteArrayList<List<Any?>>()

    private fun <T> no(): Result<T> = Result.failure(IllegalStateException("no"))

    @Volatile var spaceList: List<ManagedSpace> = emptyList()
    val patches = CopyOnWriteArrayList<Pair<Int, SpacePatch>>()
    val savedSchedules = CopyOnWriteArrayList<ReservationSchedule>()

    @Volatile var schedule: ReservationSchedule? = null

    override fun spaces(): Result<List<ManagedSpace>> = Result.success(spaceList)

    override fun create(
        name: String,
        type: SpaceType,
        idempotencyKey: String,
        capacity: Int?,
        floor: Int?,
        category: String?,
    ): Result<ManagedSpace> = no()

    override fun createBatch(
        type: SpaceType,
        prefix: String,
        from: Int,
        to: Int,
        idempotencyKey: String,
        capacity: Int?,
        floor: Int?,
        category: String?,
    ): Result<SpaceBatchResult> {
        batches += listOf(type, prefix, from, to, idempotencyKey, capacity)
        return batchResult
    }

    override fun update(
        spaceId: Int,
        patch: SpacePatch,
        idempotencyKey: String,
    ): Result<ManagedSpace> {
        patches += spaceId to patch
        return spaceList.firstOrNull { it.id == spaceId }?.let { Result.success(it) } ?: no()
    }

    override fun delete(
        spaceId: Int,
        idempotencyKey: String,
    ): Result<Unit> = no()

    override fun uploadImage(
        spaceId: Int,
        bytes: ByteArray,
        filename: String,
        mimeType: String,
        idempotencyKey: String,
    ): Result<ManagedSpace> = no()

    override fun removeImage(
        spaceId: Int,
        idempotencyKey: String,
    ): Result<ManagedSpace> = no()

    override fun reservationSchedule(): Result<ReservationSchedule> = schedule?.let { Result.success(it) } ?: no()

    override fun saveReservationSchedule(
        schedule: ReservationSchedule,
        idempotencyKey: String,
    ): Result<ReservationSchedule> {
        savedSchedules += schedule
        return Result.success(schedule)
    }

    override fun closeWithoutCharging(
        spaceId: Int,
        reason: String,
        idempotencyKey: String,
    ): Result<ForcedCloseResult> = no()
}

/**
 * Mesas del admin dentro del ViewModel (T05): crear varias de una vez, con sus avisos y sus límites.
 * Corre en la JVM; no cuenta como recorrido en teléfono.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecorridosMesasAdminViewModelTest {
    private val repo = FakeSpaces()
    private lateinit var vm: AdminSpacesViewModel

    private val ui get() = vm.uiState.value

    @Before
    fun setUp() {
        StaffHarness.installMain()
        vm = AdminSpacesViewModel(repo, FakeReservations(), VenueContextStore())
        vm.onVisible("negocio-1")
        StaffHarness.awaitUntil("las mesas cargadas") { ui.spaces != null }
    }

    @Test
    fun t05_crearDiezMesasDeUnaVezMandaElRangoYAvisaCuantasSeCrearon() {
        var done = false
        vm.createBatch(SpaceType.TABLE, "Mesa", 1, 10, 4) { done = true }
        StaffHarness.awaitUntil("el aviso") { ui.notice == "Se crearon 10 de 10" && done }
        val sent = repo.batches.single()
        assertEquals(listOf(SpaceType.TABLE, "Mesa", 1, 10), sent.take(4))
        assertEquals(4, sent[5])
    }

    @Test
    fun t05_siAlgunasYaExistianElAvisoLoDice() {
        repo.batchResult = Result.success(SpaceBatchResult(10, 7, listOf("Mesa 1", "Mesa 2", "Mesa 3")))
        vm.createBatch(SpaceType.TABLE, "Mesa", 1, 10) {}
        StaffHarness.awaitUntil("el aviso") { ui.notice == "Se crearon 7 de 10 (3 ya existían)" }
    }

    @Test
    fun t05_unRangoInvalidoNoLlegaAlServidor() {
        vm.createBatch(SpaceType.TABLE, "Mesa", 10, 1) {}
        StaffHarness.awaitUntil("el aviso") { ui.notice == "El último número debe ser mayor o igual al primero." && ui.noticeIsError }
        vm.createBatch(SpaceType.TABLE, "  ", 1, 5) {}
        StaffHarness.awaitUntil("el aviso del nombre") { ui.notice == "El nombre base va de 1 a 70 letras." }
        vm.createBatch(SpaceType.TABLE, "Mesa", 1, 600) {}
        StaffHarness.awaitUntil("el aviso del tope") { ui.notice == "Se crean hasta ${SpaceRulesMax} a la vez." }
        assertTrue(repo.batches.isEmpty())
    }

    @Test
    fun t05_siElServidorFallaElReintentoUsaLaMismaLlave() {
        repo.batchResult = Result.failure(java.io.IOException("sin red"))
        vm.createBatch(SpaceType.TABLE, "Mesa", 1, 10) {}
        StaffHarness.awaitUntil("primer intento") { repo.batches.size == 1 && !ui.saving }
        vm.createBatch(SpaceType.TABLE, "Mesa", 1, 10) {}
        StaffHarness.awaitUntil("segundo intento") { repo.batches.size == 2 && !ui.saving }
        assertEquals(repo.batches[0][4], repo.batches[1][4])
    }

    private val SpaceRulesMax get() = com.vaiinilla.app.domain.admin.SpaceRules.MAX_BATCH
}
