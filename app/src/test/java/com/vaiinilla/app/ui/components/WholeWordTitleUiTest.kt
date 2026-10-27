package com.vaiinilla.app.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** EQUIPO-16: con letra al doble, «AUTOSERVICIO» no se parte a media palabra. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WholeWordTitleUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `con letra grande ninguna palabra se parte en dos renglones`() {
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                WholeWordTitle(
                    text = "AUTOSERVICIO EL COMEDOR",
                    color = Color.Black,
                    fontSize = 46.sp,
                    lineHeight = 44.sp,
                    letterSpacing = (-1.2).sp,
                    maxLines = 3,
                    modifier = Modifier.width(320.dp),
                )
            }
        }
        val results = mutableListOf<TextLayoutResult>()
        composeTestRule
            .onNodeWithText("AUTOSERVICIO EL COMEDOR")
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(results)
        val layout = results.single()
        val text = "AUTOSERVICIO EL COMEDOR"
        for (line in 0 until layout.lineCount) {
            val end = layout.getLineEnd(line, visibleEnd = true)
            if (end < text.length) {
                assertTrue("se partió en «${text.substring(0, end)}»", text[end] == ' ' || text[end - 1] == ' ')
            }
        }
    }
}
