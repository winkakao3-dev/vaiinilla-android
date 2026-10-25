package com.vaiinilla.app.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** FLOW-15: el sondeo de una pantalla se detiene con la app en segundo plano y vuelve al regresar. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WhileStartedTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private class FakeOwner : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this)
        override val lifecycle: Lifecycle get() = registry
    }

    @Test
    fun `se detiene en segundo plano y vuelve al regresar`() {
        val owner = FakeOwner().apply { registry.currentState = Lifecycle.State.RESUMED }
        val calls = mutableListOf<String>()
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                WhileStarted(onStart = { calls += "start" }, onStop = { calls += "stop" })
            }
        }
        composeTestRule.waitForIdle()
        assertEquals(listOf("start"), calls)

        composeTestRule.runOnIdle { owner.registry.currentState = Lifecycle.State.CREATED }
        assertEquals(listOf("start", "stop"), calls)

        composeTestRule.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        assertEquals(listOf("start", "stop", "start"), calls)
    }
}
