package com.vaiinilla.app.ui.screens

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** D07: con 4 duraciones a 360 dp, «+1 h 30» se lee completo en el botón de Renovar. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-normal-long-notround-any-xxhdpi", sdk = [33])
class RentalRenewChipsUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `las duraciones de renovar no se cortan`() {
        composeTestRule.setContent {
            ScreenshotTheme {
                RentalDurationPicker(
                    title = "Renovar",
                    verb = "Renovar",
                    pricePerHour = "200.00",
                    durations = listOf(90, 180, 270, 360),
                    enabled = true,
                    colors = rememberOperationalColors(),
                    renewal = true,
                    onConfirm = {},
                )
            }
        }

        listOf("+1 h 30", "+4 h 30").forEach { label ->
            val layouts = mutableListOf<TextLayoutResult>()
            composeTestRule
                .onNodeWithText(label)
                .fetchSemanticsNode()
                .config[SemanticsActions.GetTextLayoutResult]
                .action
                ?.invoke(layouts)
            assertFalse("$label se corta", layouts.single().hasVisualOverflow)
        }
    }
}
