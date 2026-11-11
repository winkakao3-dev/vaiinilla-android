package com.vaiinilla.app.ui.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.repository.BoardOrder
import com.vaiinilla.app.ui.screens.PlayersStepper
import com.vaiinilla.app.ui.screens.WaiterSheetOrderRow
import com.vaiinilla.app.ui.screens.rememberOperationalColors
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** N10 y U02 de Miguel (8 nov): la mesa del mesero con bebida y platillo, y los jugadores de la pista. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi", sdk = [33])
class QaMiguelMesaYPistaScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val stations =
        mapOf(
            "refresco" to PreparationStation.CASHIER,
            "hamburguesa clásica" to PreparationStation.KITCHEN,
        )

    private fun order(
        state: OrderState,
        paymentPending: Boolean = false,
    ) = BoardOrder(
        id = "pedido-$state",
        folio = 12,
        state = state,
        version = 1,
        clientName = "Ana",
        itemsSummary = "1× Refresco, 1× Hamburguesa clásica",
        updatedAt = "2026-11-08T18:00:00Z",
        paymentPending = paymentPending,
    )

    private fun capture(
        name: String,
        content: @Composable () -> Unit,
    ) {
        composeTestRule.setContent {
            ScreenshotTheme {
                val colors = rememberOperationalColors()
                Column(Modifier.fillMaxWidth().background(colors.background).padding(16.dp)) { content() }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(name)
    }

    @Test
    fun n10_mesa_pagada_bebida_y_platillo() {
        capture("qa-miguel/n10_mesa_en_cocina.png") {
            WaiterSheetOrderRow(
                order = order(OrderState.PREPARING),
                readOnly = false,
                canDeliver = true,
                colors = rememberOperationalColors(),
                onDeliver = {},
                stations = stations,
            )
        }
    }

    @Test
    fun n10_mesa_por_cobrar_bebida_y_platillo() {
        capture("qa-miguel/n10_mesa_por_cobrar.png") {
            WaiterSheetOrderRow(
                order = order(OrderState.PENDING_PAYMENT, paymentPending = true),
                readOnly = false,
                canDeliver = true,
                colors = rememberOperationalColors(),
                onDeliver = {},
                stations = stations,
            )
        }
    }

    @Test
    fun u02_jugadores_de_la_pista() {
        capture("qa-miguel/u02_jugadores.png") {
            PlayersStepper(players = 4, enabled = true, colors = rememberOperationalColors(), onChange = {})
        }
    }
}