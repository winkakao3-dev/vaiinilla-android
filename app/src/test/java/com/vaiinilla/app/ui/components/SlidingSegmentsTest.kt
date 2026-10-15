package com.vaiinilla.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SlidingSegmentsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `talkback hears which segment is chosen`() {
        composeTestRule.setContent {
            ScreenshotTheme {
                SlidingSegments(
                    labels = listOf("Barra / Bebidas", "Cocina caliente"),
                    selectedIndex = 1,
                    onSelect = {},
                    trackColor = Color.White,
                    indicatorColor = Color.Black,
                    selectedContentColor = Color.White,
                    idleContentColor = Color.Black,
                )
            }
        }

        composeTestRule.onNodeWithText("Cocina caliente").assertIsSelected()
        composeTestRule.onNodeWithText("Barra / Bebidas").assertIsNotSelected()
    }
}
