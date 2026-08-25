package com.vaiinilla.app.ui.order

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import com.vaiinilla.app.ui.components.SwipeToDeleteOrder
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Assert.assertEquals
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
class OrderSwipeDeleteUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `swipe left waits for confirmation before deleting`() {
        var deleteCount = 0
        setSwipeContent { deleteCount += 1 }

        composeTestRule.onNodeWithTag(SWIPE_TAG).performTouchInput { swipeLeft() }
        assertEquals(0, deleteCount)
        waitForDeleteDialog()

        composeTestRule.onNodeWithText("¿Eliminar este pedido?").assertIsDisplayed()
        composeTestRule.onNodeWithText("El pedido #4 se quitará de Mis pedidos.").assertIsDisplayed()

        composeTestRule.onNodeWithTag(CONFIRM_TAG).performClick()
        composeTestRule.waitUntil(timeoutMillis = 1_000) { deleteCount == 1 }

        assertEquals(1, deleteCount)
    }

    @Test
    fun `swipe right can be canceled and retried without deleting`() {
        var deleteCount = 0
        setSwipeContent { deleteCount += 1 }

        composeTestRule.onNodeWithTag(SWIPE_TAG).performTouchInput { swipeRight() }
        assertEquals(0, deleteCount)
        waitForDeleteDialog()

        composeTestRule.onNodeWithTag(CANCEL_TAG).performClick()
        composeTestRule.waitUntil(timeoutMillis = 1_000) {
            composeTestRule.onAllNodesWithTag(DIALOG_ROOT_TAG).fetchSemanticsNodes().isEmpty()
        }
        assertEquals(0, deleteCount)

        composeTestRule.onNodeWithTag(SWIPE_TAG).performTouchInput { swipeRight() }
        assertEquals(0, deleteCount)
        waitForDeleteDialog()

        composeTestRule.onNodeWithTag(CONFIRM_TAG).performClick()
        composeTestRule.waitUntil(timeoutMillis = 1_000) { deleteCount == 1 }
        assertEquals(1, deleteCount)
    }

    private fun waitForDeleteDialog() {
        composeTestRule.waitUntil(timeoutMillis = 1_500) {
            composeTestRule.onAllNodesWithTag(DIALOG_ROOT_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitUntil(timeoutMillis = 1_500) {
            composeTestRule.onAllNodesWithTag(CONFIRM_TAG).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun setSwipeContent(onDelete: () -> Unit) {
        composeTestRule.setContent {
            ScreenshotTheme {
                SwipeToDeleteOrder(
                    orderFolio = "4",
                    onDelete = onDelete,
                    modifier = Modifier.testTag(SWIPE_TAG),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .background(Color(0xFF1C1C1A)),
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    private companion object {
        const val SWIPE_TAG = "swipe-order"
        const val CONFIRM_TAG = "confirm-delete-order"
        const val CANCEL_TAG = "cancel-delete-order"
        const val DIALOG_ROOT_TAG = "delete-order-dialog-root"
    }
}
