package com.vaiinilla.app.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.vaiinilla.app.domain.model.DeliveryAddress
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderHomeDelivery
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.ui.order.JourneyVenue
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * «¿Qué pasó?» cuando algo sale mal en un pedido a domicilio (S06): el enlace aparece mientras el pedido puede
 * cambiar, la hoja da los cuatro casos y, sin WhatsApp del negocio, lo dice. Corre en la JVM con Robolectric y
 * Compose; no cuenta como recorrido en teléfono.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class RecorridosAyudaPedidoTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun homeOrder(state: OrderState): OrderDetail {
        val base = ScreenshotFixtures.sampleOrder(state = state)
        return base.copy(
            summary = base.summary.copy(destination = OrderDestination.HOME_DELIVERY, folio = 1),
            homeDelivery =
                OrderHomeDelivery(
                    address = DeliveryAddress(address = "Aldama 1408, Centro", phone = "6140000000"),
                    deposit = "535.15",
                    dueOnArrival = "535.14",
                ),
        )
    }

    private fun show(
        order: OrderDetail,
        venue: JourneyVenue,
        readOnly: Boolean = false,
    ) {
        composeTestRule.setContent {
            ScreenshotTheme {
                OrderJourneyPage(order = order, qrPayload = null, onBack = {}, venue = venue, readOnly = readOnly)
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun s06_mientrasElPedidoPuedeCambiarSaleAlgoCambioONecesitoAyuda() {
        show(homeOrder(OrderState.PREPARING), JourneyVenue(name = "Pastelería Vaini"))
        composeTestRule.onNodeWithText("Algo cambió o necesito ayuda").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun s06_enCaminoElEnlaceDiceNoEstareEnCasa() {
        show(homeOrder(OrderState.ON_THE_WAY), JourneyVenue(name = "Pastelería Vaini"))
        composeTestRule.onNodeWithText("No estaré en casa · Necesito ayuda").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun s06_laHojaDaLosCuatroCasosYSinWhatsAppDiceQueElNegocioNoLoDejo() {
        show(homeOrder(OrderState.PAID), JourneyVenue(name = "Pastelería Vaini", whatsappUrl = null))
        composeTestRule.onNodeWithText("Algo cambió o necesito ayuda").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("¿Qué pasó?").assertIsDisplayed()
        composeTestRule.onNodeWithText("No voy a estar en casa").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cambiar la hora de entrega").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ya pasó la hora y no llega").assertIsDisplayed()
        composeTestRule.onNodeWithText("Llegó dañado o no es lo que pedí").assertIsDisplayed()
        composeTestRule.onNodeWithText(HELP_WITHOUT_WHATSAPP).assertIsDisplayed()
    }

    @Test
    fun s06_conWhatsAppDelNegocioNoSaleElAvisoDeQueNoLoDejo() {
        show(homeOrder(OrderState.PAID), JourneyVenue(name = "Pastelería Vaini", whatsappUrl = "https://wa.me/5216140000000"))
        composeTestRule.onNodeWithText("Algo cambió o necesito ayuda").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("¿Qué pasó?").assertIsDisplayed()
        composeTestRule.onNodeWithText(HELP_WITHOUT_WHATSAPP).assertDoesNotExist()
    }

    @Test
    fun s06_elTutorQueSoloMiraNoVeElEnlaceDeAyuda() {
        show(homeOrder(OrderState.PAID), JourneyVenue(name = "Pastelería Vaini"), readOnly = true)
        composeTestRule.onNodeWithText("Algo cambió o necesito ayuda").assertDoesNotExist()
    }

    @Test
    fun s06_unPedidoYaEntregadoNoOfreceAyuda() {
        show(homeOrder(OrderState.DELIVERED), JourneyVenue(name = "Pastelería Vaini"))
        composeTestRule.onNodeWithText("Algo cambió o necesito ayuda").assertDoesNotExist()
    }
}
