package com.vaiinilla.app.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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

    @Test
    fun `every tab keeps a 48dp touch target on a narrow screen with large text`() {
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                ScreenshotTheme {
                    VaiinillaBottomNav(
                        activeTab = StudentTab.MENU,
                        cartCount = 3,
                        onTabSelected = {},
                        modifier = Modifier.width(320.dp),
                    )
                }
            }
        }

        listOf("Menú", "Pedidos", "Cartera", "Carrito").forEach { label ->
            composeTestRule
                .onNodeWithContentDescription(label)
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
        }
    }
}
