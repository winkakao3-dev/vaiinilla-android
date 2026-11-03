package com.vaiinilla.app.ui.parentmode

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** La hoja del código de la escuela avisa de los intentos y enseña dónde viene el código. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi", sdk = [33])
class ParentUnlockSheetUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `avisa de los intentos y muestra la circular de muestra`() {
        composeTestRule.setContent {
            ScreenshotTheme {
                ParentUnlockSheet(FakeGuardianship(), onUnlocked = {}, onDismiss = {})
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Cuidado con los intentos", substring = true).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("DÓNDE VIENE").performScrollTo().assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("En la circular de la escuela viene como «Código para tutores en Vaiinilla»")
            .assertExists()
    }
}