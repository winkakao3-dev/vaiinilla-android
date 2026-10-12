package com.vaiinilla.app.ui.sharedtable

import com.vaiinilla.app.domain.model.TableSession
import com.vaiinilla.app.domain.model.TableSessionParticipant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TableWhoUiStateTest {
    private val me = TableSessionParticipant(id = "p-2", alias = "Ana", isMe = true)
    private val session =
        TableSession(
            spaceId = 3,
            spaceName = "Mesa 3",
            spaceType = "mesa",
            sessionId = 12L,
            participants = listOf(TableSessionParticipant("p-1", "Luis", false), me),
            me = me,
        )

    @Test
    fun `con quien soy el servidor ofrece continuar, salvo que pida la lista`() {
        val offering = TableWhoUiState(loading = false, session = session)
        assertTrue(offering.offerContinue)
        assertFalse(offering.copy(showList = true).offerContinue)
    }

    @Test
    fun `sin quien soy no se ofrece continuar`() {
        assertFalse(TableWhoUiState(loading = false, session = session.copy(me = null)).offerContinue)
    }

    @Test
    fun `el alias en blanco no se puede enviar y el busy lo bloquea`() {
        assertFalse(TableWhoUiState(aliasInput = "   ").canSubmitAlias)
        assertFalse(TableWhoUiState(aliasInput = "Ana", busy = true).canSubmitAlias)
        assertTrue(TableWhoUiState(aliasInput = "Ana").canSubmitAlias)
        assertEquals(true, TableWhoUiState().loading)
    }
}
