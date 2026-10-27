package com.vaiinilla.app.ui.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.vaiinilla.app.ui.components.StudentNavPillMotion
import com.vaiinilla.app.ui.components.StudentTab
import com.vaiinilla.app.ui.components.VaiinillaBottomNav
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Barra inferior de vidrio sobre un fondo de colores. Robolectric puede no ejecutar los shaders
 * AGSL: estas capturas prueban el diseño y el montaje, no la refracción real.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h300dp-normal-long-notround-any-xxxhdpi", sdk = [33])
class LiquidGlassNavScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun resetPillMotion() {
        StudentNavPillMotion.index = 0f
        StudentNavPillMotion.lastTab = StudentTab.MENU
    }

    @Test
    fun `light four tabs menu selected`() = capture("liquid_nav_light_4_menu", tab = StudentTab.MENU)

    @Test
    fun `light four tabs orders selected`() = capture("liquid_nav_light_4_orders", tab = StudentTab.ORDERS)

    @Test
    fun `light four tabs wallet selected`() = capture("liquid_nav_light_4_wallet", tab = StudentTab.WALLET)

    @Test
    fun `light four tabs cart selected`() = capture("liquid_nav_light_4_cart", tab = StudentTab.CART, cartCount = 2)

    @Test
    fun `dark four tabs`() =
        capture("liquid_nav_dark_4_orders", tab = StudentTab.ORDERS, mode = VaiinillaThemeMode.Dark)

    @Test
    fun `light three tabs`() = capture("liquid_nav_light_3_menu", tab = StudentTab.MENU, tabs = ThreeTabs)

    @Test
    fun `dark three tabs`() =
        capture("liquid_nav_dark_3_wallet", tab = StudentTab.WALLET, tabs = ThreeTabs, mode = VaiinillaThemeMode.Dark)

    @Test
    fun `solid fallback without backdrop`() {
        capture("liquid_nav_fallback_light_4_orders", tab = StudentTab.ORDERS, glass = false)
    }

    @Test
    fun `large font on a narrow screen`() {
        capture(
            "liquid_nav_light_4_320dp_font2x",
            tab = StudentTab.CART,
            cartCount = 12,
            width = 320.dp,
            fontScale = 2f,
        )
    }

    private fun capture(
        name: String,
        tab: StudentTab,
        tabs: List<StudentTab> = listOf(StudentTab.MENU, StudentTab.ORDERS, StudentTab.WALLET, StudentTab.CART),
        mode: VaiinillaThemeMode = VaiinillaThemeMode.Light,
        glass: Boolean = true,
        cartCount: Int = 0,
        width: Dp? = null,
        fontScale: Float = 1f,
    ) {
        StudentNavPillMotion.index = tabs.indexOf(tab).toFloat()
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                ScreenshotTheme(mode = mode) {
                    ColorfulScreen(
                        modifier =
                            if (width !=
                                null
                            ) {
                                Modifier.width(width)
                            } else {
                                Modifier.fillMaxWidth()
                            },
                    ) { backdrop ->
                        VaiinillaBottomNav(
                            activeTab = tab,
                            cartCount = cartCount,
                            onTabSelected = {},
                            backdrop = if (glass) backdrop else null,
                            visibleTabs = tabs,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("$name.png")
    }
}

private val ThreeTabs = listOf(StudentTab.MENU, StudentTab.ORDERS, StudentTab.WALLET)

@Composable
private fun ColorfulScreen(
    modifier: Modifier,
    nav: @Composable BoxScope.(LayerBackdrop) -> Unit,
) {
    val backdrop = rememberLayerBackdrop()
    Box(modifier = modifier.height(190.dp)) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
                    .background(Brush.verticalGradient(listOf(Color(0xFFBFE3D6), Color(0xFFF2D9B8)))),
        ) {
            listOf(16.dp, 8.dp).forEach { top ->
                Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = top)) {
                    listOf(Color(0xFFE5484D), Color(0xFF111111), Color(0xFFFF6A3D), Color(0xFF3B82F6)).forEach {
                        Box(
                            Modifier
                                .weight(1f)
                                .height(72.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(it),
                        )
                    }
                }
            }
        }
        nav(backdrop)
    }
}
