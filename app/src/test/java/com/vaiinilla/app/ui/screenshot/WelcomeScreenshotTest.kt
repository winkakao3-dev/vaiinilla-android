package com.vaiinilla.app.ui.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.ui.auth.student.StudentAuthUiState
import com.vaiinilla.app.ui.screens.StudentAuthLandingScreen
import com.vaiinilla.app.ui.screens.WelcomeBootStage
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * The welcome hero loops forever (stickers flip one after another), so every capture drives the
 * clock by hand instead of waiting for idle.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class WelcomeScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun showWelcome(mode: VaiinillaThemeMode = VaiinillaThemeMode.Light) {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            ScreenshotTheme(mode) {
                StudentAuthLandingScreen(
                    state = StudentAuthUiState(),
                    onBack = null,
                    onRegister = {},
                    onLogin = {},
                    onGoogleSignIn = {},
                    onExplore = {},
                )
            }
        }
    }

    private fun captureAt(
        millis: Long,
        name: String,
    ) {
        composeTestRule.mainClock.advanceTimeBy(millis)
        composeTestRule.onRoot().captureRoboImage(name)
    }

    @Test
    fun welcome_settled_light() {
        showWelcome()
        captureAt(2100, "welcome_settled_light.png")
    }

    @Test
    fun welcome_settled_dark() {
        showWelcome(VaiinillaThemeMode.Dark)
        captureAt(2100, "welcome_settled_dark.png")
    }

    @Test
    @Config(qualifiers = "w360dp-h740dp-normal-long-notround-any-xxhdpi")
    fun welcome_small_phone() {
        showWelcome()
        captureAt(2100, "welcome_small_phone.png")
    }

    @Test
    fun welcome_burst() {
        showWelcome()
        captureAt(480, "welcome_burst.png")
    }

    @Test
    fun welcome_flip() {
        showWelcome()
        captureAt(2520, "welcome_flip.png")
    }

    @Test
    fun welcome_boot() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            ScreenshotTheme {
                WelcomeBootStage(reduceMotion = false)
            }
        }
        captureAt(900, "welcome_boot.png")
    }

    /** Frame sequence for reviewing the motion as a video; only runs with WELCOME_FRAMES=1. */
    @Test
    fun welcome_motion_frames() {
        assumeTrue(System.getenv("WELCOME_FRAMES") == "1")
        val out = File(System.getenv("WELCOME_FRAMES_DIR") ?: "build/welcome-frames").apply { mkdirs() }
        showWelcome()
        val stepMs = 40L
        repeat(150) { frame ->
            composeTestRule.onRoot().captureRoboImage(File(out, "f_%03d.png".format(frame)).absolutePath)
            composeTestRule.mainClock.advanceTimeBy(stepMs)
        }
    }
}
