package com.vaiinilla.app.ui.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.ui.components.StudentNavPillMotion
import com.vaiinilla.app.ui.components.StudentTab
import com.vaiinilla.app.ui.components.VaiinillaBottomNav
import com.vaiinilla.app.ui.theme.Cream
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class UberNavScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun resetPillMotion() {
        StudentNavPillMotion.index = 0f
        StudentNavPillMotion.lastTab = StudentTab.MENU
    }

    @Test
    fun `24_uber_nav_menu_on_cream`() {
        composeTestRule.setContent {
            ScreenshotTheme {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(Cream),
                ) {
                    VaiinillaBottomNav(
                        activeTab = StudentTab.MENU,
                        cartCount = 0,
                        onTabSelected = {},
                        enableDrag = false,
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth(),
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("24_uber_nav_menu_on_cream.png")
    }

    @Test
    fun `25_uber_nav_reference_black`() {
        composeTestRule.setContent {
            ScreenshotTheme(mode = VaiinillaThemeMode.Dark) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0C0C0C)),
                    contentAlignment = Alignment.Center,
                ) {
                    VaiinillaBottomNav(
                        activeTab = StudentTab.MENU,
                        cartCount = 0,
                        onTabSelected = {},
                        enableDrag = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("25_uber_nav_reference_black.png")
    }

    @Test
    fun `26_uber_nav_cart_active_badge`() {
        StudentNavPillMotion.index = StudentTab.CART.ordinal.toFloat()
        StudentNavPillMotion.lastTab = StudentTab.CART
        composeTestRule.setContent {
            ScreenshotTheme {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(Cream),
                ) {
                    VaiinillaBottomNav(
                        activeTab = StudentTab.CART,
                        cartCount = 2,
                        onTabSelected = {},
                        enableDrag = false,
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth(),
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("26_uber_nav_cart_active_badge.png")
    }

    @Test
    fun `27_uber_nav_slide_settled_orders`() {
        val tab = mutableStateOf(StudentTab.MENU)
        composeTestRule.setContent {
            ScreenshotTheme {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(Cream),
                ) {
                    VaiinillaBottomNav(
                        activeTab = tab.value,
                        cartCount = 0,
                        onTabSelected = { tab.value = it },
                        enableDrag = false,
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth(),
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        tab.value = StudentTab.ORDERS
        composeTestRule.mainClock.advanceTimeBy(800)
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("27_uber_nav_slide_settled_orders.png")
    }
}
