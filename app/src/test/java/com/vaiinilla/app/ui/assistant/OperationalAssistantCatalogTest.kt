package com.vaiinilla.app.ui.assistant

import com.vaiinilla.app.ui.components.ASSISTANT_ANCHOR_QR_SCAN
import com.vaiinilla.app.ui.components.AssistantAdvance
import com.vaiinilla.app.ui.components.assistantKitchenReadyAnchor
import com.vaiinilla.app.ui.components.assistantKitchenStartAnchor
import com.vaiinilla.app.ui.components.assistantQueueOrderAnchor
import com.vaiinilla.app.ui.components.cashierAssistantGuides
import com.vaiinilla.app.ui.components.kitchenAssistantGuides
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OperationalAssistantCatalogTest {
    @Test
    fun `manual de caja siempre expone todas las preguntas aunque no haya contexto`() {
        val guides =
            cashierAssistantGuides(
                readyOrderId = null,
                readyOrderFolio = null,
                firstProductId = null,
                cashSessionOpen = true,
                canCreateProduct = false,
            )

        assertEquals(8, guides.size)
        assertTrue(guides.all { it.demoSteps.size >= 2 })
        assertTrue("entregar-qr" in guides.map { it.id })
        assertTrue("qr-no-lee" in guides.map { it.id })
        assertTrue("estados-pedido" in guides.map { it.id })
        assertTrue("estacion-producto" in guides.map { it.id })
        assertFalse("entregar-manual" in guides.map { it.id })
    }

    @Test
    fun `entrega de caja solo enseña qr y la practica exige pedido listo`() {
        val noContext =
            cashierAssistantGuides(
                readyOrderId = null,
                readyOrderFolio = null,
                firstProductId = 7,
                cashSessionOpen = true,
                canCreateProduct = true,
            ).first { it.id == "entregar-qr" }
        assertFalse(noContext.available)
        assertEquals(4, noContext.demoSteps.size)

        val ready =
            cashierAssistantGuides(
                readyOrderId = "38",
                readyOrderFolio = 38,
                firstProductId = 7,
                cashSessionOpen = true,
                canCreateProduct = true,
            ).first { it.id == "entregar-qr" }
        assertTrue(ready.available)
        assertEquals(ASSISTANT_ANCHOR_QR_SCAN, ready.steps.single().anchorId)
        assertEquals(AssistantAdvance.State("pedido-entregado:38"), ready.steps.single().advance)
    }

    @Test
    fun `manual de cocina siempre se puede estudiar sin comandas`() {
        val guides =
            kitchenAssistantGuides(
                orderId = null,
                orderFolio = null,
                isPreparing = false,
                isReady = false,
                nextOrderId = null,
                nextOrderFolio = null,
            )

        assertEquals(6, guides.size)
        assertTrue(guides.all { it.demoSteps.size >= 2 })
        assertTrue("leer-comanda" in guides.map { it.id })
        assertTrue("estados-cocina" in guides.map { it.id })
        assertTrue(guides.filter { it.steps.isNotEmpty() }.none { it.available })
    }

    @Test
    fun `cocina nueva practica preparando y no lista directa`() {
        val guides =
            kitchenAssistantGuides(
                orderId = "k-12",
                orderFolio = 12,
                isPreparing = false,
                isReady = false,
                nextOrderId = null,
                nextOrderFolio = null,
            )
        val start = guides.first { it.id == "avanzar-comanda" }
        val ready = guides.first { it.id == "terminar-comanda" }

        assertTrue(start.available)
        assertFalse(ready.available)
        assertEquals(assistantKitchenStartAnchor("k-12"), start.steps.single().anchorId)
    }

    @Test
    fun `cocina preparando habilita marcar lista`() {
        val guide =
            kitchenAssistantGuides(
                orderId = "k-12",
                orderFolio = 12,
                isPreparing = true,
                isReady = false,
                nextOrderId = null,
                nextOrderFolio = null,
            ).first { it.id == "terminar-comanda" }

        assertTrue(guide.available)
        assertEquals(assistantKitchenReadyAnchor("k-12"), guide.steps.single().anchorId)
        assertEquals(AssistantAdvance.State("comanda-lista:k-12"), guide.steps.single().advance)
    }

    @Test
    fun `cocina puede practicar cambiar a otra comanda cuando existe`() {
        val guide =
            kitchenAssistantGuides(
                orderId = "k-12",
                orderFolio = 12,
                isPreparing = true,
                isReady = false,
                nextOrderId = "k-13",
                nextOrderFolio = 13,
            ).first { it.id == "cambiar-comanda" }

        assertTrue(guide.available)
        assertEquals(assistantQueueOrderAnchor("k-13"), guide.steps.single().anchorId)
        assertEquals(AssistantAdvance.Tap, guide.steps.single().advance)
    }
}
