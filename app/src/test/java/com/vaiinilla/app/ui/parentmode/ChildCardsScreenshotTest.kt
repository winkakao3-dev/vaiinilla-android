package com.vaiinilla.app.ui.parentmode

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.domain.guardianship.Dependent
import com.vaiinilla.app.domain.guardianship.DependentControls
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.math.BigDecimal

/** Cartera › Mis hijos en tarjetas de dos en dos, como el E02 de Doop. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class ChildCardsScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val mateo =
        Dependent(
            userId = "u-mateo",
            name = "Mateo Arreola",
            group = "5°A",
            photoUrl = null,
            studentId = null,
            hasOwnAccount = false,
            balance = BigDecimal("180"),
            controls = DependentControls(dailyLimit = BigDecimal("60"), availableToday = BigDecimal("60")),
        )

    @Test
    fun mis_hijos_tarjetas() {
        val children =
            ChildrenState(
                ParentFixtures.services(FakeGuardianship(listOf(ParentFixtures.ana, mateo, ParentFixtures.tilin))),
            )
        runBlocking { children.load() }
        composeTestRule.setContent {
            ScreenshotTheme {
                WalletFamilySection(
                    children = children,
                    onOpen = {},
                    onAdd = {},
                    onConsent = {},
                    entry = WalletSchoolEntry.PARENT,
                    onRole = {},
                    onLinkCode = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Mateo · 5°A").assertExists()
        composeTestRule.onNodeWithText("Hoy le quedan \$60").assertExists()
        composeTestRule.onRoot().captureRoboImage("mis_hijos_tarjetas.png")
    }
}