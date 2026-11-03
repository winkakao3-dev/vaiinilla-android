package com.vaiinilla.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.domain.repository.CallReason
import com.vaiinilla.app.domain.repository.CallStatus
import com.vaiinilla.app.domain.repository.TableCall
import com.vaiinilla.app.domain.repository.TableSpace
import com.vaiinilla.app.ui.order.MenuWaiterCallState
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** La hoja de llamar al mesero (Doop L05), antes de llamar y con la llamada ya hecha. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class WaiterCallSheetUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun call() =
        TableCall(
            id = "call-1",
            space = TableSpace(id = 4, name = "Mesa 4", type = "mesa"),
            orderId = null,
            reason = CallReason.CUENTA,
            status = CallStatus.PENDIENTE,
            clientName = "Kao",
            takenBy = null,
            createdAt = "2026-11-03T16:54:08.777Z",
            takenAt = null,
            closedAt = null,
            version = 1,
        )

    private fun sheet(open: Boolean) {
        val state =
            MenuWaiterCallState(
                spaceId = 4,
                onCurrent = { Result.success(if (open) call() else null) },
                onCall = { _, _, _ -> Result.success(call()) },
                onCancel = { Result.success(it) },
                onJoinTable = null,
            )
        composeTestRule.setContent {
            ScreenshotTheme(mode = VaiinillaThemeMode.Dark) {
                LaunchedEffect(Unit) { state.refresh() }
                val colors = LocalVaiinillaColors.current
                Box(Modifier.fillMaxSize().background(Color(0xFF0E0F0D))) {
                    Box(Modifier.fillMaxSize().background(colors.paper).alpha(0.38f)) {
                        Column(Modifier.padding(start = 20.dp, top = 56.dp)) {
                            Text("Pastelería Vaini", color = colors.muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Mesa 4", color = colors.ink, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Column(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                            .background(colors.paper),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ClientSheetHandle()
                        WaiterCallContent(state = state, place = "la Mesa 4", modifier = Modifier.padding(bottom = 16.dp))
                    }
                }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(1_000)
    }

    @Test
    fun `llamada hecha como el dibujo de Doop`() {
        sheet(open = true)
        composeTestRule.onRoot().captureRoboImage("lugar/l05_llamar_mesero.png")
    }

    @Test
    fun `antes de llamar elige que necesita`() {
        sheet(open = false)
        composeTestRule.onRoot().captureRoboImage("lugar/l05_llamar_mesero_motivos.png")
    }
}
