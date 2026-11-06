package com.vaiinilla.app.ui.screenshot

import android.content.res.Configuration
import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Pinta una pantalla u hoja en la JVM y la revisa como la revisaría una persona. Corre con `vbuild test`; los PNG quedan
 * en `app/build/outputs/roborazzi/auditoria/` para abrirlos con Read. Los problemas hacen fallar la prueba.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
abstract class VisualAuditBase {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private var paintedScale by mutableFloatStateOf(NORMAL_SCALE)

    /** La pinta en cada escala. [afterPaint] corre tras cada pintada, para abrir la hoja o llegar al estado que importa. */
    protected fun audit(
        name: String,
        mode: VaiinillaThemeMode = VaiinillaThemeMode.Light,
        scales: List<Float> = listOf(NORMAL_SCALE, LARGE_SCALE),
        afterPaint: () -> Unit = {},
        content: @Composable () -> Unit,
    ) {
        paint(mode, content)
        val problems =
            scales.flatMap { target ->
                setScale(target)
                settle()
                afterPaint()
                settle()
                val file = capture("$name-${(target * 100).toInt()}")
                (imageProblems(file) + textProblems()).map { "$name a ${(target * 100).toInt()} %: $it" }
            }
        assertEquals("Revisa los PNG en $OUTPUT_DIR", emptyList<String>(), problems)
    }

    /** Falla si [mutate] (lo que cambia el `_uiState` o el estado) no cambia ni un píxel de la pantalla. */
    protected fun auditRefresh(
        name: String,
        mode: VaiinillaThemeMode = VaiinillaThemeMode.Light,
        mutate: () -> Unit,
        content: @Composable () -> Unit,
    ) {
        paint(mode, content)
        settle()
        val before = capture("$name-antes")
        mutate()
        settle()
        val after = capture("$name-despues")
        assertEquals(
            "$name: la pantalla no cambió tras mutar el estado",
            false,
            before.readBytes().contentEquals(after.readBytes()),
        )
    }

    protected fun paint(
        mode: VaiinillaThemeMode = VaiinillaThemeMode.Light,
        content: @Composable () -> Unit,
    ) {
        composeTestRule.setContent {
            ScreenshotTheme(mode = mode) {
                // key: las hojas y diálogos viven en otra ventana y solo leen la escala de los recursos al crearse.
                key(paintedScale) {
                    val density = LocalDensity.current
                    val configuration = Configuration(LocalConfiguration.current).apply { fontScale = paintedScale }
                    CompositionLocalProvider(
                        LocalDensity provides Density(density.density, paintedScale),
                        LocalConfiguration provides configuration,
                        content = content,
                    )
                }
            }
        }
    }

    protected fun setScale(target: Float) {
        val resources = composeTestRule.activity.resources
        val configuration = Configuration(resources.configuration).apply { fontScale = target }
        @Suppress("DEPRECATION")
        resources.updateConfiguration(configuration, resources.displayMetrics)
        paintedScale = target
    }

    protected fun settle(millis: Long = SETTLE_MS) {
        composeTestRule.mainClock.advanceTimeBy(millis)
        composeTestRule.waitForIdle()
    }

    protected fun capture(name: String): File {
        val file = File(OUTPUT_DIR, "$name.png").absoluteFile
        file.parentFile?.mkdirs()
        captureScreenRoboImage(
            file,
            RoborazziOptions(
                taskType = RoborazziTaskType.Record,
                recordOptions = RoborazziOptions.RecordOptions(resizeScale = 0.5),
            ),
        )
        return file
    }

    /** Pantalla negra o de un solo color: casi todos sus píxeles son iguales o casi negros. */
    protected fun imageProblems(file: File): List<String> {
        val image = BitmapFactory.decodeFile(file.path)
        val pixels =
            IntArray(image.width * image.height).also {
                image.getPixels(it, 0, image.width, 0, 0, image.width, image.height)
            }
        val dominant =
            pixels
                .asList()
                .groupingBy { it }
                .eachCount()
                .values
                .max()
                .toDouble() / pixels.size
        val nearBlack =
            pixels
                .count {
                    ((it shr 16) and 0xFF) < NEAR_BLACK &&
                        ((it shr 8) and 0xFF) < NEAR_BLACK &&
                        (it and 0xFF) < NEAR_BLACK
                }.toDouble() / pixels.size
        return buildList {
            if (nearBlack > BLANK_RATIO) add("pantalla negra")
            if (dominant > BLANK_RATIO && nearBlack <= BLANK_RATIO) add("pantalla de un solo color")
        }
    }

    /** Textos con elipsis, cortados, partidos a media palabra o encimados, en todas las ventanas abiertas. */
    protected fun textProblems(): List<String> {
        val nodes =
            composeTestRule
                .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .mapNotNull { node -> layoutOf(node)?.let { node to it } }
        return nodes.flatMap { (_, layout) -> cutProblems(layout) } + overlapProblems(nodes)
    }

    private fun layoutOf(node: SemanticsNode): TextLayoutResult? {
        val results = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.firstOrNull()
    }

    private fun cutProblems(layout: TextLayoutResult): List<String> {
        val text = layout.layoutInput.text.text
        return buildList {
            if ((0 until layout.lineCount).any { layout.isLineEllipsized(it) }) {
                add("«$text» sale con elipsis")
            } else if (isCut(layout)) {
                add("«$text» se corta")
            }
            for (line in 0 until layout.lineCount - 1) {
                val end = layout.getLineEnd(line)
                if (end in 1 until text.length && text[end - 1].isLetterOrDigit() && text[end].isLetterOrDigit()) {
                    add("«$text» se parte a media palabra: «${text.substring(0, end).takeLast(WORD_HINT)}/»")
                }
            }
        }
    }

    // No se usa hasVisualOverflow: mide contra el ancho máximo del texto (1500 px contra 372 en «Cartera») y marca todos.
    private fun isCut(layout: TextLayoutResult): Boolean =
        layout.multiParagraph.didExceedMaxLines ||
            layout.multiParagraph.height > layout.size.height + ROUNDING_PX ||
            (0 until layout.lineCount).any {
                layout.getLineRight(it) - layout.getLineLeft(it) >
                    layout.size.width + ROUNDING_PX
            }

    private fun overlapProblems(nodes: List<Pair<SemanticsNode, TextLayoutResult>>): List<String> {
        val visible = nodes.filter { (node, _) -> node.boundsInRoot.width > 0f && node.boundsInRoot.height > 0f }
        return visible.flatMapIndexed { index, (node, layout) ->
            visible.drop(index + 1).mapNotNull { (other, otherLayout) ->
                val sameWindow =
                    generateSequence(node) { it.parent }.last().id == generateSequence(other) { it.parent }.last().id
                val text = layout.layoutInput.text.text
                val otherText = otherLayout.layoutInput.text.text
                if (sameWindow &&
                    text != otherText &&
                    overlapRatio(node.boundsInRoot, other.boundsInRoot) > OVERLAP_RATIO
                ) {
                    "«$text» se encima con «$otherText»"
                } else {
                    null
                }
            }
        }
    }

    private fun overlapRatio(
        a: Rect,
        b: Rect,
    ): Float {
        if (!a.overlaps(b)) return 0f
        val hit = a.intersect(b)
        return hit.width * hit.height / minOf(a.width * a.height, b.width * b.height)
    }

    companion object {
        const val NORMAL_SCALE = 1f

        /** La letra grande del recorrido L01. */
        const val LARGE_SCALE = 1.3f
        const val OUTPUT_DIR = "build/outputs/roborazzi/auditoria"
        private const val SETTLE_MS = 1_500L
        private const val BLANK_RATIO = 0.995
        private const val NEAR_BLACK = 16
        private const val OVERLAP_RATIO = 0.25f
        private const val WORD_HINT = 12
        private const val ROUNDING_PX = 2f
    }
}
