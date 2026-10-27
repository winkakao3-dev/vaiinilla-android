package com.vaiinilla.app.ui.components

import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class VaiinillaBottomNavSemanticsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `talkback hears which tab is open`() {
        composeTestRule.setContent {
            ScreenshotTheme {
                VaiinillaBottomNav(activeTab = StudentTab.ORDERS, cartCount = 0, onTabSelected = {})
            }
        }

        composeTestRule.onNodeWithContentDescription("Pedidos").assertIsSelected()
        composeTestRule.onNodeWithContentDescription("Menú").assertIsNotSelected()
    }
}
