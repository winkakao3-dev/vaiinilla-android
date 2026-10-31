package com.vaiinilla.app.ui.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.core.notifications.NoticeLookup
import com.vaiinilla.app.core.notifications.NoticeMembership
import com.vaiinilla.app.core.notifications.NoticeOrderSummary
import com.vaiinilla.app.core.notifications.NoticePhoneContext
import com.vaiinilla.app.core.notifications.ReceivedNotice
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.ui.notices.ReceivedNoticesContent
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** «Avisos recibidos» (AVISOS-01): la lista y el detalle de un «por cobrar» que llegó a un mesero de otro negocio. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class ReceivedNoticesScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val phone =
        NoticePhoneContext(
            account = "kao.mesero@kakaowin.garden",
            openMode = "Mesero en USAGI",
            memberships =
                listOf(
                    NoticeMembership("m1", OperationalRole.WAITER, "e1", "USAGI"),
                    NoticeMembership("m2", OperationalRole.CASHIER, "e2", "Michi Market"),
                ),
        )

    private val notices =
        listOf(
            ReceivedNotice(
                id = "push-1",
                receivedAtMillis = 1_793_480_000_000,
                title = "Pedido #14 por cobrar",
                body = "Entro un pedido en efectivo.",
                payload = mapOf("type" to "staff_alert", "alert" to "por_cobrar", "orderId" to "p-14", "folio" to "14"),
                isRemote = true,
                arrivals = listOf(ReceivedNotice.Arrival.BACKGROUND, ReceivedNotice.Arrival.OPENED),
                phone = phone,
                lookup =
                    NoticeLookup.Found(
                        NoticeOrderSummary(
                            establishmentName = "Michi Market",
                            readAs = "Caja en Michi Market",
                            folio = 14,
                            status = "Por cobrar",
                            customerName = "Ana López",
                            total = "$95",
                            paymentMethod = "Efectivo",
                            destination = "Para llevar",
                            space = null,
                            items = listOf("1 × Tacos de birria", "1 × Agua de jamaica"),
                            createdAt = "2026-10-31T18:20:00Z",
                        ),
                        checkedAtMillis = 1_793_480_100_000,
                    ),
            ),
            ReceivedNotice(
                id = "push-2",
                receivedAtMillis = 1_793_479_000_000,
                title = "Mesa 5 te llama",
                body = "Pide cubiertos o servilletas",
                payload = mapOf("type" to "staff_alert", "alert" to "llamada_mesa", "espacio_nombre" to "Mesa 5"),
                isRemote = true,
                arrivals = listOf(ReceivedNotice.Arrival.SHOWN),
                phone = phone,
            ),
            ReceivedNotice(
                id = "app-3",
                receivedAtMillis = 1_793_478_000_000,
                title = "Pedido #9",
                body = "Tu pedido está listo. Recógelo en la barra.",
                payload = mapOf("type" to "order_state", "orderId" to "p-9", "folio" to "9", "state" to "listo"),
                isRemote = false,
                arrivals = listOf(ReceivedNotice.Arrival.SHOWN),
                phone = phone,
            ),
        )

    @Test
    fun `90 avisos recibidos lista`() {
        composeTestRule.setContent {
            ScreenshotTheme(mode = VaiinillaThemeMode.Light) {
                ReceivedNoticesContent(notices, emptySet(), onBack = {}, onLookUp = {}, onClear = {})
            }
        }
        composeTestRule.mainClock.advanceTimeBy(1_500)
        composeTestRule.onRoot().captureRoboImage("90_avisos_recibidos_lista.png")
    }

    @Test
    fun `91 avisos recibidos detalle`() {
        composeTestRule.setContent {
            ScreenshotTheme(mode = VaiinillaThemeMode.Light) {
                ReceivedNoticesContent(notices, emptySet(), onBack = {}, onLookUp = {}, onClear = {})
            }
        }
        composeTestRule.mainClock.advanceTimeBy(1_500)
        composeTestRule.onNodeWithText("Pedido #14 por cobrar").performClick()
        composeTestRule.mainClock.advanceTimeBy(1_500)
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("91_avisos_recibidos_detalle.png")
    }
}
