package com.vaiinilla.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * First real on-device/emulator test, proving the androidTest + Hilt test runner
 * scaffolding is wired correctly end to end. Intentionally trivial: it exists to give
 * the CI instrumented-test workflow something safe to run before real UI flow suites
 * (Stripe checkout, Caja QR delivery, Cocina state transitions) are added on top of it.
 */
@RunWith(AndroidJUnit4::class)
class InstrumentedSmokeTest {
    @Test
    fun runsOnTheTargetAppPackage() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.vaiinilla.app", targetContext.packageName.removeSuffix(".dev"))
    }
}
