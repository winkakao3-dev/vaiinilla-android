package com.vaiinilla.app.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WalletSubflowTopBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `the first screen of the app has no back arrow that does nothing`() {
        composeTestRule.setContent {
            ScreenshotTheme { WalletSubflowTopBar(title = "Modos", onBack = null) }
        }

        composeTestRule.onNodeWithText("Modos").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Volver").assertDoesNotExist()
    }

    @Test
    fun `a screen with something behind keeps its back arrow`() {
        var backs = 0
        composeTestRule.setContent {
            ScreenshotTheme { WalletSubflowTopBar(title = "Modos", onBack = { backs++ }) }
        }

        composeTestRule.onNodeWithContentDescription("Volver").performClick()

        assertEquals(1, backs)
    }
}
