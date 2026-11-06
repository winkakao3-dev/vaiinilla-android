package com.vaiinilla.app.ui.screens

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import com.vaiinilla.app.domain.repository.AccountOrder
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** R13: se cobra lo que falta de cada pedido. R34: el sondeo no vuelve a marcar lo que se desmarcó. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class CollectAccountDueUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun order(
        id: String,
        folio: Int,
        name: String,
        total: String,
        due: String = total,
    ) = AccountOrder(
        id = id,
        folio = folio,
        state = "entregado",
        total = total,
        clientName = name,
        itemsSummary = "",
        payAtEnd = true,
        pending = true,
        due = due,
    )

    @Test
    fun `the charge uses what is left of each order, not its gross total`() {
        composeTestRule.setContent {
            ScreenshotTheme {
                CollectAccountContent(
                    total = "110.00",
                    payable = listOf(order("a", 1, "Ana", "100.00", due = "60.00"), order("b", 2, "Beto", "50.00")),
                    confirming = false,
                    colors = rememberOperationalColors(),
                    onConfirm = { _, _, _, _, _ -> },
                    remaining = "110.00",
                )
            }
        }
        composeTestRule.onAllNodesWithText("Cobrar $110.00", substring = true).onFirst().assertExists()

        composeTestRule.onAllNodesWithText("#2 · Beto", substring = true).onFirst().performClick()

        composeTestRule.onAllNodesWithText("Cobrar $60.00", substring = true).onFirst().assertExists()
    }

    @Test
    fun `a new order from polling keeps the partial selection`() {
        val payable = mutableStateOf(listOf(order("a", 1, "Ana", "60.00"), order("b", 2, "Beto", "50.00")))
        composeTestRule.setContent {
            ScreenshotTheme {
                CollectAccountContent(
                    total = "110.00",
                    payable = payable.value,
                    confirming = false,
                    colors = rememberOperationalColors(),
                    onConfirm = { _, _, _, _, _ -> },
                    selectionKey = "sesion-1",
                )
            }
        }
        composeTestRule.onAllNodesWithText("#2 · Beto", substring = true).onFirst().performClick()
        composeTestRule.onAllNodesWithText("Cobrar $60.00", substring = true).onFirst().assertExists()

        payable.value = payable.value + order("c", 3, "Caro", "30.00")
        composeTestRule.waitForIdle()

        // Lo de Ana sigue solo; el pedido nuevo llega sin marcar.
        composeTestRule.onAllNodesWithText("Cobrar $60.00", substring = true).onFirst().assertExists()
    }
}
