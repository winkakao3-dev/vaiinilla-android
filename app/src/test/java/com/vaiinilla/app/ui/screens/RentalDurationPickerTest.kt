package com.vaiinilla.app.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class RentalDurationPickerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `elegir una duracion no cobra y el boton de abajo si`() {
        val confirmed = mutableListOf<Int>()
        composeTestRule.setContent {
            ScreenshotTheme {
                RentalDurationPicker(
                    title = "Rentar ahora",
                    verb = "Rentar",
                    pricePerHour = "150.00",
                    enabled = true,
                    colors = rememberOperationalColors(),
                    onConfirm = { confirmed += it },
                )
            }
        }

        composeTestRule.onNodeWithText("1 h", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        assertEquals(emptyList<Int>(), confirmed)

        composeTestRule.onNodeWithText("Rentar 1 h · $150").assertIsDisplayed().performClick()
        assertEquals(listOf(60), confirmed)
    }

    @Test
    fun `el precio de cada duracion sigue la regla del servidor`() {
        assertEquals("$150", rentalPrice("150.00", 60))
        assertEquals("$225", rentalPrice("150.00", 90))
        assertEquals("$187.50", rentalPrice("125.00", 90))
        assertNull(rentalPrice(null, 60))
        assertEquals("Rentar 1 h 30 · $225", rentalActionLabel("Rentar", 90, "150.00"))
        assertEquals("Renovar 2 h", rentalActionLabel("Renovar", 120, null))
    }

    @Test
    fun `el mosaico usa el numero o el nombre sin el tipo`() {
        assertEquals("4", waiterShortName("Mesa 4", "Mesa"))
        assertEquals("2", waiterShortName("Cancha 2", "Cancha"))
        assertEquals("Básquetbol", waiterShortName("Cancha Básquetbol", "Cancha"))
        assertEquals("Fútbol Rápido", waiterShortName("Cancha Fútbol Rápido", "Cancha"))
        assertEquals("Terraza", waiterShortName("mesa terraza", "Mesa"))
        assertEquals("Cancha", waiterShortName("Cancha", "Cancha"))
        assertEquals("Barra larga", waiterShortName("Barra larga", "Mesa"))
    }
}
