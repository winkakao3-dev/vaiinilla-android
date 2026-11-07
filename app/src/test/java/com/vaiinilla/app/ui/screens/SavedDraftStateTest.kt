package com.vaiinilla.app.ui.screens

import androidx.compose.runtime.saveable.SaverScope
import com.vaiinilla.app.domain.model.TipChoice
import org.junit.Assert.assertEquals
import org.junit.Test

/** Lo tecleado en la hoja de cobro (propina, pedidos marcados) sobrevive a que Android recree la actividad. */
class SavedDraftStateTest {
    private val scope = SaverScope { true }

    private fun roundTrip(choice: TipChoice): TipChoice? =
        with(TipChoiceSaver) { scope.save(choice) }?.let { TipChoiceSaver.restore(it) }

    @Test
    fun `la propina vuelve igual tras guardar y restaurar`() {
        listOf(TipChoice.None, TipChoice.Percent(15), TipChoice.Custom("32.50"), TipChoice.Custom("")).forEach {
            assertEquals(it, roundTrip(it))
        }
    }

    @Test
    fun `un guardado ilegible vuelve a sin propina en vez de romper`() {
        assertEquals(TipChoice.None, TipChoiceSaver.restore("percent-roto"))
        assertEquals(TipChoice.None, TipChoiceSaver.restore(42))
        assertEquals(TipChoice.Percent(0), TipChoiceSaver.restore("percent:xx"))
    }

    @Test
    fun `los pedidos marcados vuelven igual`() {
        val chosen = setOf("pedido-3", "pedido-7", "pedido-12")
        val saved = with(StringSetSaver) { scope.save(chosen) }!!

        assertEquals(chosen, StringSetSaver.restore(saved))
        assertEquals(emptySet<String>(), StringSetSaver.restore(with(StringSetSaver) { scope.save(emptySet()) }!!))
    }
}
