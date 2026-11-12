package com.vaiinilla.app.data.guardianship

import com.vaiinilla.app.data.counter.RemoteStudentCounterRepository
import com.vaiinilla.app.data.school.RemoteSchoolAdminRepository
import com.vaiinilla.app.data.tape.ServerTape
import com.vaiinilla.app.data.tape.TapeFloor
import com.vaiinilla.app.data.wallet.RemoteWalletRepository
import com.vaiinilla.app.domain.counter.StudentCredentialKind
import com.vaiinilla.app.domain.counter.StudentPurchaseItem
import com.vaiinilla.app.domain.guardianship.Dependent
import com.vaiinilla.app.domain.guardianship.DependentControls
import com.vaiinilla.app.domain.guardianship.PlannedItem
import com.vaiinilla.app.domain.guardianship.RefundMethod
import com.vaiinilla.app.domain.guardianship.ScheduledDayOrder
import com.vaiinilla.app.domain.guardianship.StudentRequestKind
import com.vaiinilla.app.domain.guardianship.StudentRequestStatus
import com.vaiinilla.app.domain.school.RosterStudent
import com.vaiinilla.app.domain.school.SchoolAllergen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.util.UUID

/** Los repositorios reales de la escuela (tutor, Caja y dirección) contra lo que el servidor de pruebas contestó. */
class RemoteGuardianshipRepositoryTapeTest {
    @get:Rule
    val tape = ServerTape()

    private val floor by lazy { TapeFloor(tape, "escuela") }

    private fun guardian(account: String = "cliente") = RemoteGuardianshipRepository(floor.client(account))

    private fun school() = RemoteSchoolAdminRepository(floor.client("admin"))

    private fun counter() = RemoteStudentCounterRepository(floor.client("cajero"), floor.orderJson)

    private fun money(amount: String) = BigDecimal(amount)

    /** Deja al tutor sin hijos de grabaciones anteriores y con el aviso de privacidad aceptado. */
    private fun readyGuardian(account: String = "cliente"): String {
        val guardian = guardian(account)
        guardian.unlockParent(school().settings().getOrThrow().joinCode!!, floor.key()).getOrThrow()
        guardian.dependents().getOrThrow().forEach { guardian.unlink(it.userId, floor.key()).getOrThrow() }
        val notice = guardian.privacyNotice().getOrThrow()
        assertTrue(notice.paragraphs.isNotEmpty())
        guardian.acceptPrivacyNotice(notice.version, floor.key()).getOrThrow()
        return notice.version
    }

    /** El tutor da de alta a un alumno nuevo y la escuela lo aprueba. */
    private fun approvedChild(consent: String): Dependent {
        val studentId = tape.value("matricula") { "CINTA-${UUID.randomUUID().toString().take(6).uppercase()}" }
        val child = guardian().addDependent("Niña de cinta", studentId, "3B", consent, floor.key()).getOrThrow()
        assertEquals("Niña de cinta", child.name)
        assertEquals(studentId, child.studentId)
        assertTrue("la escuela todavía no lo aprueba", child.isAwaitingSchool)
        val request = school().guardianRequests().getOrThrow().single { it.studentId == studentId }
        assertEquals("Niña de cinta", request.studentName)
        school().reviewGuardianRequest(request.id, approve = true, floor.key()).getOrThrow()
        val approved = guardian().dependents().getOrThrow().single { it.userId == child.userId }
        assertFalse(approved.isAwaitingSchool)
        assertEquals(BigDecimal("0.00"), approved.balance)
        return approved
    }

    @Test
    fun `el tutor da de alta a su hijo, le pone limites y Caja le cobra con su saldo respetandolos`() {
        floor.open()
        val product = floor.product()
        val child = approvedChild(readyGuardian())
        val guardian = guardian()

        val controls =
            DependentControls(
                dailyLimit = BigDecimal("60.00"),
                notes = "Sin refresco",
                blockedCategoryIds = emptySet(),
                allergens = setOf(SchoolAllergen.HUEVO),
            )
        val limited = guardian.updateControls(child.userId, controls, floor.key()).getOrThrow().controls!!
        assertEquals(BigDecimal("60.00"), limited.dailyLimit)
        assertEquals("Sin refresco", limited.notes)
        assertEquals(setOf(SchoolAllergen.HUEVO), limited.allergens)
        val alerted = guardian.updateLowBalanceAlert(child.userId, BigDecimal("50.00"), floor.key()).getOrThrow()
        assertEquals(BigDecimal("50.00"), alerted.controls?.lowBalanceAlert)
        assertTrue("con saldo 0 y aviso en 50 está bajo", alerted.isLowBalance)

        val receipt =
            RemoteWalletRepository(
                floor.client("cajero"),
            ).reloadCash(child.userId, "100.00", floor.key()).getOrThrow()
        assertEquals(money("100.00"), money(receipt.newBalance))
        val wallets = RemoteWalletRepository(floor.client("cajero"))
        assertTrue(wallets.searchClients("Niña de cinta").getOrThrow().any { it.userId == child.userId })
        assertEquals(money("100.00"), money(wallets.lookupClient(child.userId).getOrThrow().balance!!))
        val counter = counter()
        val card = counter.search(child.studentId!!).getOrThrow().single { it.userId == child.userId }
        assertEquals(BigDecimal("100.00"), card.balance)
        assertEquals(BigDecimal("60.00"), card.dailyLimit)
        assertEquals("Sin refresco", card.guardianNote)

        val unreviewed = counter.purchase(child.userId, listOf(StudentPurchaseItem(product.id, 1)), null, floor.key())
        assertTrue("a un alumno con alergias no se le vende lo que no tiene alérgenos revisados", unreviewed.isFailure)
        guardian.updateControls(child.userId, controls.copy(allergens = emptySet()), floor.key()).getOrThrow()
        val bought =
            counter
                .purchase(
                    child.userId,
                    listOf(StudentPurchaseItem(product.id, 1)),
                    null,
                    floor.key(),
                ).getOrThrow()
        assertEquals(money(TapeFloor.PRICE), money(bought.summary.total))
        val after = counter.search(child.studentId!!).getOrThrow().single { it.userId == child.userId }
        assertEquals(BigDecimal("100.00") - money(TapeFloor.PRICE), after.balance)
        assertEquals(money(TapeFloor.PRICE), after.spentToday)
        val overLimit = counter.purchase(child.userId, listOf(StudentPurchaseItem(product.id, 1)), null, floor.key())
        assertTrue("45 + 45 pasa del límite diario de 60", overLimit.isFailure)

        val blocking = controls.copy(allergens = emptySet(), blockedProductIds = setOf(product.id))
        val blocked = guardian.updateControls(child.userId, blocking, floor.key())
        assertEquals(setOf(product.id), blocked.getOrThrow().controls?.blockedProductIds)
        assertEquals(
            setOf(product.id),
            counter
                .search(child.studentId!!)
                .getOrThrow()
                .single {
                    it.userId ==
                        child.userId
                }.blockedProducts,
        )

        guardian.unlink(child.userId, floor.key()).getOrThrow()
        assertTrue(guardian.dependents().getOrThrow().none { it.userId == child.userId })
    }

    @Test
    fun `el hijo recibe PIN y credencial y otro tutor se suma con un codigo que la escuela aprueba o rechaza`() {
        val consent = readyGuardian()
        val child = approvedChild(consent)
        val guardian = guardian()

        guardian.setPin(child.userId, "4821", floor.key()).getOrThrow()
        assertTrue(
            guardian
                .dependents()
                .getOrThrow()
                .single { it.userId == child.userId }
                .hasPin,
        )
        assertTrue(
            counter()
                .search(child.studentId!!)
                .getOrThrow()
                .single { it.userId == child.userId }
                .requiresPin,
        )
        val credential =
            counter()
                .assignCredential(
                    child.userId,
                    StudentCredentialKind.QR,
                    null,
                    floor.key(),
                ).getOrThrow()
        assertEquals(StudentCredentialKind.QR, credential.kind)
        assertEquals(child.userId, counter().find(credential.printableValue!!).getOrThrow()?.userId)
        assertEquals(1, guardian.blockCredentials(child.userId, floor.key()).getOrThrow())
        guardian.setPin(child.userId, null, floor.key()).getOrThrow()
        assertFalse(
            guardian
                .dependents()
                .getOrThrow()
                .single { it.userId == child.userId }
                .hasPin,
        )

        val other = guardian("cliente2")
        readyGuardian("cliente2")
        val code = guardian.createGuardianCode(child.userId, floor.key()).getOrThrow()
        assertTrue(code.code.isNotBlank())
        other.link(code.code, consent, floor.key()).getOrThrow()
        school().guardianRequests().getOrThrow().filter { it.studentId == child.studentId }.forEach {
            school().reviewGuardianRequest(it.id, approve = true, floor.key()).getOrThrow()
        }
        val shared = guardian.dependents().getOrThrow().single { it.userId == child.userId }
        assertEquals(2, shared.guardians.size)
        val second = shared.guardians.single { !it.isYou }
        assertFalse(second.isPrincipal)

        val handed = guardian.makePrincipal(child.userId, second.id, floor.key()).getOrThrow()
        assertTrue(handed.guardians.single { it.id == second.id }.isPrincipal)
        val listed = school().searchGuardianships(child.studentId!!).getOrThrow()
        assertEquals(2, listed.size)
        val first = listed.single { !it.isPrincipal }
        school().makePrincipal(first.id, floor.key()).getOrThrow()
        assertTrue(
            guardian
                .dependents()
                .getOrThrow()
                .single { it.userId == child.userId }
                .isPrincipal,
        )
        guardian.removeGuardian(child.userId, second.id, floor.key()).getOrThrow()
        assertTrue(other.dependents().getOrThrow().none { it.userId == child.userId })

        other
            .link(
                guardian.createGuardianCode(child.userId, floor.key()).getOrThrow().code,
                consent,
                floor.key(),
            ).getOrThrow()
        school().guardianRequests().getOrThrow().filter { it.studentId == child.studentId }.forEach {
            school().reviewGuardianRequest(it.id, approve = true, floor.key()).getOrThrow()
        }
        assertEquals(2, school().searchGuardianships(child.studentId!!).getOrThrow().size)
        school().searchGuardianships(child.studentId!!).getOrThrow().filter { !it.isPrincipal }.forEach {
            school().removeGuardianship(it.id, floor.key()).getOrThrow()
        }
        assertEquals(1, school().searchGuardianships(child.studentId!!).getOrThrow().size)
        val third = guardian.createGuardianCode(child.userId, floor.key()).getOrThrow().code
        other.link(third, consent, floor.key()).getOrThrow()
        school().guardianRequests().getOrThrow().filter { it.studentId == child.studentId }.forEach {
            school().reviewGuardianRequest(it.id, approve = false, floor.key()).getOrThrow()
        }
        assertEquals(
            "un tutor rechazado no queda ligado",
            1,
            school().searchGuardianships(child.studentId!!).getOrThrow().size,
        )
        assertNotNull(guardian.createLinkCode(floor.key()).getOrThrow().code)
        assertNotNull(guardian.createAccountCode(child.userId, floor.key()).getOrThrow().code)
        guardian.unlink(child.userId, floor.key()).getOrThrow()
    }

    @Test
    fun `el tutor aparta para su hijo, la escuela cierra el dia y devuelve, y la devolucion del saldo se resuelve`() {
        floor.open()
        val product = floor.product()
        val child = approvedChild(readyGuardian())
        val guardian = guardian()
        RemoteWalletRepository(floor.client("cajero")).reloadCash(child.userId, "200.00", floor.key()).getOrThrow()

        val school = school()
        val settings = school.settings().getOrThrow()
        school.save(settings.copy(daysAhead = 5), regenerateJoinCode = false, floor.key()).getOrThrow()
        val calendar = school.menuCalendar(null, null).getOrThrow()
        val day = calendar.today.plusDays(tape.value("dias") { "2" }.toLong())
        school.reopenDay(day, floor.key())
        school.saveMenu(day, listOf(product.id), floor.key()).getOrThrow()
        assertEquals(setOf(product.id), school.menuCalendar(day, day).getOrThrow().menu(day))

        val asChild =
            RemoteDependentAccountRepository(
                floor.client("cliente"),
                DependentContextSession(child.userId, guardian::issueContext),
            )
        assertEquals(BigDecimal("200.00"), asChild.wallet().getOrThrow().balance)
        assertTrue(
            "sin límites puestos, nada le queda bloqueado",
            asChild
                .controls()
                .getOrThrow()!!
                .blockedProductIds
                .isEmpty(),
        )
        val childCalendar = asChild.menuCalendar().getOrThrow()
        assertEquals(setOf(product.id), childCalendar.menu(day.toString()))
        val slot = childCalendar.schedule.slots.firstOrNull()
        val reserved =
            asChild.reserve(
                listOf(ScheduledDayOrder(day.toString(), slot, listOf(PlannedItem(product.id, 1, emptyList())))),
                floor.key(),
            )
        assertEquals(1, reserved.getOrThrow())
        val order = asChild.orders().getOrThrow().single { it.deliveryDate == day.toString() }
        assertEquals(money(TapeFloor.PRICE), order.total)
        assertEquals(BigDecimal("200.00") - money(TapeFloor.PRICE), asChild.wallet().getOrThrow().balance)

        val kitchenList = school.reservedDay(day).getOrThrow()
        assertEquals(1, kitchenList.orders)
        assertEquals(1, counter().reservedDay(day.toString()).getOrThrow().orders)
        val closure = school.closeDay(day, "Junta de consejo", floor.key()).getOrThrow()
        assertEquals(1, closure.cancelled)
        assertEquals(money(TapeFloor.PRICE), money(closure.refunded))
        assertEquals("cerrar el día devuelve lo apartado", BigDecimal("200.00"), asChild.wallet().getOrThrow().balance)
        assertEquals("Junta de consejo", school.reservedDay(day).getOrThrow().closedReason)
        school.reopenDay(day, floor.key()).getOrThrow()
        asChild
            .reserve(
                listOf(ScheduledDayOrder(day.toString(), slot, listOf(PlannedItem(product.id, 1, emptyList())))),
                floor.key(),
            ).getOrThrow()
        asChild
            .cancel(
                asChild.orders().getOrThrow().single {
                    it.deliveryDate == day.toString() &&
                        it.statusWire != "cancelado"
                },
                floor.key(),
            ).getOrThrow()
        assertEquals(BigDecimal("200.00"), asChild.wallet().getOrThrow().balance)

        val request =
            guardian
                .requestForStudent(
                    child.userId,
                    StudentRequestKind.REFUND,
                    RefundMethod.CASH,
                    "Nos cambiamos",
                    floor.key(),
                ).getOrThrow()
        assertEquals(StudentRequestStatus.PENDING, request.status)
        assertEquals(BigDecimal("200.00"), request.balanceAtRequest)
        val inbox = school.studentRequests().getOrThrow().single { it.id == request.id }
        assertEquals("Niña de cinta", inbox.studentName)
        school.rejectStudentRequest(request.id, "Falta la identificación", floor.key()).getOrThrow()
        val rejected = guardian.studentRequests(child.userId).getOrThrow().single { it.id == request.id }
        assertEquals(StudentRequestStatus.REJECTED, rejected.status)
        assertEquals("Falta la identificación", rejected.rejectionReason)
        val again =
            guardian
                .requestForStudent(
                    child.userId,
                    StudentRequestKind.REFUND,
                    RefundMethod.CASH,
                    null,
                    floor.key(),
                ).getOrThrow()
        school.resolveStudentRequest(again.id, floor.key()).getOrThrow()
        val resolved = guardian.studentRequests(child.userId).getOrThrow().single { it.id == again.id }
        assertEquals(StudentRequestStatus.RESOLVED, resolved.status)
        assertEquals(BigDecimal("200.00"), resolved.refunded)
        assertEquals(
            BigDecimal("0.00"),
            guardian
                .dependents()
                .getOrThrow()
                .single { it.userId == child.userId }
                .balance,
        )
        guardian.unlink(child.userId, floor.key()).getOrThrow()
        school.save(settings, regenerateJoinCode = false, floor.key()).getOrThrow()
    }

    @Test
    fun `la direccion guarda sus ajustes y el padron, y Caja sabe que es una escuela`() {
        val school = school()
        val settings = school.settings().getOrThrow()
        assertNotNull(settings.joinCode)
        val saved =
            school
                .save(
                    settings.copy(daysAhead = 9, orderCutoff = "08:30"),
                    regenerateJoinCode = false,
                    floor.key(),
                ).getOrThrow()
        assertEquals(9, saved.daysAhead)
        assertEquals("08:30", saved.orderCutoff)
        assertEquals(settings.joinCode, saved.joinCode)
        assertEquals(settings, school.save(settings, regenerateJoinCode = false, floor.key()).getOrThrow())

        val studentId = tape.value("matricula") { "PADRON-${UUID.randomUUID().toString().take(6).uppercase()}" }
        val roster = school.roster().getOrThrow()
        val added =
            school
                .saveRoster(
                    roster + RosterStudent(studentId, "Alumno del padrón", "1A"),
                    floor.key(),
                ).getOrThrow()
        assertEquals(roster.size + 1, added)
        assertEquals(
            "Alumno del padrón",
            school
                .roster()
                .getOrThrow()
                .single { it.studentId == studentId }
                .name,
        )
        school.saveRoster(roster, floor.key()).getOrThrow()

        val policy = counter().schoolPolicy().getOrThrow()
        assertTrue(policy.isSchool)
    }
}
