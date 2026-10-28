package com.vaiinilla.app.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Problema 2 del QA: si el negocio vuelve a cargar a media salida, la capa «Entrando» igual termina. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class StoreEntranceFinishUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `si outcome cambia durante el desvanecido onFinished se llama igual`() {
        val outcome = mutableStateOf(StoreEntranceOutcome.READY)
        var finished = false
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            val current by outcome
            ScreenshotTheme {
                StoreEntranceOverlay(
                    entrance = StoreEntrance(code = null),
                    outcome = current,
                    onFinished = { finished = true },
                )
            }
        }
        // El negocio se recarga una y otra vez mientras la puerta se abre y se desvanece: READY y WAITING se
        // alternan cuadro a cuadro. Antes, el primer cambio a media salida cancelaba el cierre para siempre.
        repeat(300) { frame ->
            outcome.value = if (frame % 2 == 0) StoreEntranceOutcome.READY else StoreEntranceOutcome.WAITING
            composeTestRule.mainClock.advanceTimeByFrame()
        }
        outcome.value = StoreEntranceOutcome.WAITING
        composeTestRule.mainClock.advanceTimeBy(3_000)

        assertTrue(finished)
    }
}
