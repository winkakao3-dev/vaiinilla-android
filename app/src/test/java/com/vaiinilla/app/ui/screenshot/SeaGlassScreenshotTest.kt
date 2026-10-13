package com.vaiinilla.app.ui.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.ui.theme.SeaGlassTone
import com.vaiinilla.app.ui.theme.seaGlass
import com.vaiinilla.app.ui.theme.seaGlassShades
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Las piezas de vidrio de mar en día y noche, en la misma composición que la referencia web. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w440dp-h1400dp-normal-long-notround-any-xxhdpi", sdk = [33])
class SeaGlassScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val tones =
        listOf(
            "lima" to SeaGlassTone.lima,
            "vainilla" to SeaGlassTone.vainilla,
            "horchata" to SeaGlassTone.horchata,
            "matcha" to SeaGlassTone.matcha,
            "menta" to SeaGlassTone.menta,
            "mango" to SeaGlassTone.mango,
            "cafe" to SeaGlassTone.cafe,
            "jamaica" to SeaGlassTone.jamaica,
            "fresa" to SeaGlassTone.fresa,
            "cielo" to SeaGlassTone.cielo,
            "lavanda" to SeaGlassTone.lavanda,
            "mora" to SeaGlassTone.mora,
        )

    @Test
    fun piecesDayAndNight() {
        composeTestRule.setContent {
            Row {
                Board(night = false, background = Color(0xFFF3F1F6))
                Board(night = true, background = Color(0xFF141217))
            }
        }
        composeTestRule.onRoot().captureRoboImage("vidrio/sea_glass_pieces.png")
    }

    @androidx.compose.runtime.Composable
    private fun Board(
        night: Boolean,
        background: Color,
    ) {
        Column(
            modifier = Modifier.width(220.dp).background(background).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            tones.forEach { (name, tone) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Piece(name, tone, night)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(
                    Modifier.size(56.dp).seaGlass(SeaGlassTone.vainilla, CircleShape, night),
                )
                Spacer(Modifier.width(16.dp))
                Spacer(
                    Modifier
                        .width(100.dp)
                        .height(56.dp)
                        .seaGlass(SeaGlassTone.lima, RoundedCornerShape(22.dp), night),
                )
            }
            Spacer(Modifier.height(10.dp))
        }
    }

    @androidx.compose.runtime.Composable
    private fun Piece(
        name: String,
        tone: SeaGlassTone,
        night: Boolean,
    ) {
        val ink = seaGlassShades(tone, night).ink
        Row(
            modifier =
                Modifier
                    .width(180.dp)
                    .height(56.dp)
                    .seaGlass(tone, RoundedCornerShape(percent = 50), night),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                name,
                color = Color(ink.red.toFloat(), ink.green.toFloat(), ink.blue.toFloat()),
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}
