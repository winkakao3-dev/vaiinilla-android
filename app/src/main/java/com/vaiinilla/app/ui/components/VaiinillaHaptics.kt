package com.vaiinilla.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

class VaiinillaHaptics(private val haptic: HapticFeedback) {
    fun click() {
        try {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        } catch (_: Throwable) {}
    }

    fun selection() {
        try {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        } catch (_: Throwable) {}
    }

    fun impact() {
        try {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Throwable) {}
    }

    fun success() {
        try {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Throwable) {}
    }
}

@Composable
fun rememberVaiinillaHaptics(): VaiinillaHaptics {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) { VaiinillaHaptics(haptic) }
}
