package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import com.vaiinilla.app.ui.sharedtable.SharedTableUiState
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** ¿Compartes la mesa? (Doop L03): la hoja que sale en el menú al entrar por el QR de una mesa con gente. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class SharedTableInviteUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private var joined = 0
    private var alone = 0

    private fun invite(alias: String = "Kao") {
        composeTestRule.setContent {
            ScreenshotTheme(mode = VaiinillaThemeMode.Dark) {
                val colors = LocalVaiinillaColors.current
                Box(Modifier.fillMaxSize().background(Color(0xFF0E0F0D))) {
                    // El menú de atrás, apagado como en el dibujo.
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
                        SharedTableInviteHandle()
                        SharedTableInvite(
                            state =
                                SharedTableUiState(
                                    loaded = true,
                                    aliasInput = alias,
                                    othersAtTable = listOf("Luis", "Mariana", "Andrés"),
                                ),
                            spaceName = "Mesa 4",
                            spaceType = "mesa",
                            onAliasChange = {},
                            onJoin = { joined++ },
                            onOrderAlone = { alone++ },
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                    }
                }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(1_000)
    }

    @Test
    fun `compartes la mesa como el dibujo de Doop`() {
        invite()
        composeTestRule.onRoot().captureRoboImage("lugar/l03_compartes_mesa.png")
    }

    @Test
    fun `dice quienes ya estan en la mesa del QR y deja unirse o pedir solo`() {
        invite()
        composeTestRule
            .onNodeWithText("Luis, Mariana y alguien más ya están en la Mesa 4.", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SHARED_TABLE_INVITE_JOIN_TAG).performClick()
        composeTestRule.onNodeWithTag(SHARED_TABLE_INVITE_ALONE_TAG).performClick()
        assertEquals(1, joined)
        assertEquals(1, alone)
    }

    @Test
    fun `sin nombre no se une`() {
        invite(alias = "")
        composeTestRule.onNodeWithTag(SHARED_TABLE_INVITE_JOIN_TAG).performClick()
        assertEquals(0, joined)
    }
}
