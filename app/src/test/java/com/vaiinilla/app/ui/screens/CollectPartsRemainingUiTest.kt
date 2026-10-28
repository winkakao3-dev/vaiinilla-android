package com.vaiinilla.app.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** I06: $100 entre 3; tras el primer abono quedan 2 personas y la hoja propone $33.33, no $22.22. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class CollectPartsRemainingUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `tras un abono en partes la hoja sigue en partes con las personas que faltan`() {
        composeTestRule.setContent {
            ScreenshotTheme {
                CollectAccountContent(
                    total = "100.00",
                    payable = emptyList(),
                    confirming = false,
                    colors = rememberOperationalColors(),
                    onConfirm = { _, _, _, _, _ -> },
                    remaining = "66.67",
                    paidIn = "33.33",
                    onAbono = { _, _, _, _, _, _, _ -> },
                    initialSplit = SplitMode.PARTS,
                    initialPeople = 2,
                )
            }
        }

        composeTestRule
            .onAllNodesWithText("Paga $33.33 cada uno", substring = true)
            .onFirst()
            .assertExists()
    }
}
