package com.vaiinilla.app.data.admin

import com.vaiinilla.app.data.tape.ServerTape
import com.vaiinilla.app.data.tape.TapeFloor
import com.vaiinilla.app.domain.admin.AdminPeriod
import com.vaiinilla.app.domain.admin.AssistantJob
import com.vaiinilla.app.domain.admin.AssistantMessage
import com.vaiinilla.app.domain.admin.AssistantStep
import com.vaiinilla.app.domain.admin.CashbackRule
import com.vaiinilla.app.domain.admin.StoreFlow
import com.vaiinilla.app.domain.admin.steps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/** El repositorio real de los ajustes del negocio y su asistente contra lo que el servidor de pruebas contestó. */
class RemoteAdminStoreRepositoryTapeTest {
    @get:Rule
    val tape = ServerTape()

    private val floor by lazy { TapeFloor(tape) }

    private fun store() = RemoteAdminStoreRepository(floor.client("admin"), floor.venue)

    @Test
    fun `los ajustes del negocio y el cashback se guardan y se leen igual`() {
        val store = store()
        val config = store.config().getOrThrow()
        val flow =
            StoreFlow(
                typeWire = config.typeWire,
                deliveryRequiresQr = config.deliveryRequiresQr,
                allowsPayAtEnd = config.allowsPayAtEnd,
                allowsBalance = config.allowsBalance,
                releaseGraceMinutes = config.releaseGraceMinutes,
                orderWindows = config.orderWindows,
            )
        val saved = store.saveFlow(flow.copy(releaseGraceMinutes = 25)).getOrThrow()
        assertEquals(25, saved.releaseGraceMinutes)
        assertEquals(25, store.config().getOrThrow().releaseGraceMinutes)
        assertEquals(config.releaseGraceMinutes, store.saveFlow(flow).getOrThrow().releaseGraceMinutes)

        val rule = CashbackRule("Cinta", "5.00", null, null, null, null, null, active = true)
        val savedRule = store.saveCashbackRule(rule, floor.key()).getOrThrow()
        assertEquals(BigDecimal("5.00"), BigDecimal(savedRule.percent))
        assertTrue(savedRule.active)
        assertEquals("Cinta", store.cashbackRule().getOrThrow()?.name)
        assertFalse(store.saveCashbackRule(rule.copy(active = false), floor.key()).getOrThrow().active)

        val today = LocalDate.parse(tape.value("hoy") { LocalDate.now().toString() })
        val wallets = store.walletSummary(AdminPeriod(today.minusDays(1), today.plusDays(1))).getOrThrow()
        assertTrue(BigDecimal(wallets.reloads) >= BigDecimal.ZERO)
    }

    @Test
    fun `el asistente propone lugares y menu y al aplicar sus pasos quedan creados de verdad`() {
        val store = store()
        val suffix = tape.value("sufijo") { UUID.randomUUID().toString().take(4) }
        val ask =
            "Crea 2 mesas con el prefijo Terraza $suffix y agrega al menú la categoría Bebidas $suffix con el producto " +
                "Agua de jamaica $suffix a 25 pesos, que prepara cocina. Propónlo ya, no me preguntes nada más."
        val jobId =
            store
                .startAssistantJob(
                    listOf(AssistantMessage(fromOwner = true, text = ask)),
                    emptyList(),
                    emptyList(),
                ).getOrThrow()
        var job = store.assistantJob(jobId).getOrThrow()
        var tries = 0
        while (job is AssistantJob.Thinking && tries++ < 60) {
            tape.pause(3_000)
            job = store.assistantJob(jobId).getOrThrow()
        }
        val reply = (job as AssistantJob.Done).reply
        assertTrue(reply.message.isNotBlank())
        val steps = reply.proposal!!.steps()
        assertTrue("propone los lugares", steps.any { it is AssistantStep.Spaces })
        assertTrue("propone el menú", steps.any { it is AssistantStep.Menu })
        steps.forEach { store.applyAssistantStep(it, emptyMap(), floor.key()).getOrThrow() }

        val created =
            floor
                .spaces()
                .spaces()
                .getOrThrow()
                .filter { it.name.contains("Terraza $suffix") }
        assertEquals(2, created.size)
        val catalog = floor.catalog("admin").getCatalog().getOrThrow()
        val category = catalog.categories.single { it.name.contains(suffix) }
        val drink = catalog.products.single { it.categoryId == category.id }
        assertTrue(drink.name.contains("jamaica", ignoreCase = true))
        assertEquals(BigDecimal("25.00"), BigDecimal(drink.counterPrice))

        created.forEach { floor.spaces().delete(it.id, floor.key()).getOrThrow() }
        floor.admin().setProductArchived(drink.id, true, floor.key()).getOrThrow()
    }

    @Test
    fun `el asistente contesta una pregunta del dueño sin proponer cambios`() {
        val reply =
            store()
                .assistantReply(
                    listOf(AssistantMessage(true, "¿Cuántos lugares tengo dados de alta?")),
                    emptyList(),
                    emptyList(),
                ).getOrThrow()
        assertTrue(reply.message.isNotBlank())
        assertTrue(reply.remainingMessages >= 0)
    }
}
