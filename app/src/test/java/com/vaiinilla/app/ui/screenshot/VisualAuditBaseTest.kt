package com.vaiinilla.app.ui.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vaiinilla.app.ui.screens.WalletScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Comprueba que los detectores de la auditoría visual sí detectan, y corre la muestra sobre una pantalla real. */
class VisualAuditBaseTest : VisualAuditBase() {
    @Test
    fun `detecta elipsis`() {
        paint {
            Box(Modifier.fillMaxSize()) {
                Text(
                    "Apartado para el viernes",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(60.dp),
                )
            }
        }
        settle()
        assertTrue(textProblems().toString(), textProblems().any { "elipsis" in it })
    }

    @Test
    fun `detecta un texto cortado sin elipsis`() {
        paint { Box(Modifier.fillMaxSize()) { Text("Encargo", softWrap = false, modifier = Modifier.width(20.dp)) } }
        settle()
        assertTrue(textProblems().toString(), textProblems().any { "se corta" in it })
    }

    @Test
    fun `detecta una palabra partida como la de L01`() {
        paint { Box(Modifier.fillMaxSize()) { Text("Pregúntame", modifier = Modifier.width(40.dp)) } }
        settle()
        assertTrue(textProblems().toString(), textProblems().any { "media palabra" in it })
    }

    @Test
    fun `detecta textos encimados`() {
        paint {
            Box(Modifier.fillMaxSize()) {
                Text("Subtotal")
                Text("Propina")
            }
        }
        settle()
        assertTrue(textProblems().toString(), textProblems().any { "se encima" in it })
    }

    @Test
    fun `detecta la pantalla negra`() {
        paint { Box(Modifier.fillMaxSize().background(Color.Black)) }
        settle()
        assertEquals(listOf("pantalla negra"), imageProblems(capture("detector-negra")))
    }

    @Test
    fun `detecta una pantalla que no se refresca`() {
        val failure =
            runCatching {
                auditRefresh(
                    "detector-sin-refresco",
                    mutate = {},
                ) { Box(Modifier.fillMaxSize()) { Text("Saldo $100") } }
            }.exceptionOrNull()
        assertTrue(failure is AssertionError)
    }

    @Test
    fun `una pantalla que cambia pasa el refresco`() {
        var balance by mutableIntStateOf(100)
        auditRefresh(
            "muestra-refresco",
            mutate = { balance = 400 },
        ) { Box(Modifier.fillMaxSize()) { Text("Saldo $balance") } }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun `la letra grande llega a una hoja, que vive en otra ventana`() {
        paint { ModalBottomSheet(onDismissRequest = {}) { Text("Cobrar en efectivo") } }
        setScale(LARGE_SCALE)
        settle()
        val results = mutableListOf<TextLayoutResult>()
        composeTestRule
            .onNode(hasText("Cobrar en efectivo"))
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(results)
        assertEquals(
            LARGE_SCALE,
            results
                .single()
                .layoutInput.density.fontScale,
        )
        capture("muestra-hoja-130")
    }

    @Test
    fun `muestra - Cartera en escala normal y letra grande`() {
        audit("muestra-cartera") {
            WalletScreen(
                remoteState = BreakUiFixtures.wallet(BreakUiData.DEMO),
                userId = "u-1",
                venueName = "Usagi (Pruebas)",
                onMenu = {},
            )
        }
    }
}
