package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.vaiinilla.app.domain.repository.AccountOrder
import com.vaiinilla.app.domain.repository.SpaceAccount
import com.vaiinilla.app.domain.repository.SpaceAvailability
import com.vaiinilla.app.domain.repository.SpaceAvailabilityState
import com.vaiinilla.app.domain.repository.SpaceSessionDetail
import com.vaiinilla.app.domain.repository.SpaceSessionInfo
import com.vaiinilla.app.domain.repository.TableSpace
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

/** Mesas y canchas (G06): "Falta cobrar" descuenta lo abonado y el mosaico dice desde cuándo está abierta la cuenta. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi", sdk = [33])
class SpaceAccountRemainingUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val mesa = TableSpace(id = 5, name = "Mesa 5", type = "mesa")
    private val startedAt = Instant.now().minusSeconds(45 * 60).toString()

    private fun availability(settled: Boolean) =
        SpaceAvailability(
            space = mesa,
            state = SpaceAvailabilityState.OCUPADA,
            settled = settled,
            endsAt = null,
            releasesAt = null,
            remainingSeconds = null,
            graceMinutes = 5,
            startedAt = startedAt,
        )

    @Test
    fun `falta cobrar es lo pendiente menos lo abonado`() {
        val detail =
            SpaceSessionDetail(
                availability = availability(settled = false),
                session = SpaceSessionInfo("s-5", startedAt, null, 2),
                account =
                    SpaceAccount(
                        orders =
                            listOf(
                                AccountOrder("o-1", 71, "entregado", "300.00", "Ana", "2× Pizza", true, true),
                            ),
                        total = "300.00",
                        pending = "300.00",
                        paid = "0.00",
                        settled = false,
                        paidIn = "120.00",
                        remaining = "180.00",
                    ),
            )
        composeTestRule.setContent {
            ScreenshotTheme {
                val colors = rememberOperationalColors()
                Box(Modifier.fillMaxSize().background(colors.background)) {
                    Column {
                        WaiterSpaceSection(
                            space = mesa,
                            detail = detail,
                            loading = false,
                            lastCollection = null,
                            tick = 0,
                            acting = false,
                            readOnly = false,
                            colors = colors,
                            onOpenTurn = {},
                            onExtend = {},
                            onRelease = {},
                            onDismissCollection = {},
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Abonado $120.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("Total").assertIsDisplayed()
    }

    @Test
    fun `una cuenta sin turno dice desde cuándo está abierta`() {
        val caption = spaceAvailabilityCaption(availability(settled = false), tick = 0)!!
        assertTrue(caption, caption.startsWith("Desde las "))
        assertTrue(caption, caption.endsWith("\nPor cobrar"))
        assertEquals("Cuenta abierta", spaceAvailabilityCaption(availability(true).copy(startedAt = null), 0))
    }
}
