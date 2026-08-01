package com.vaiinilla.app.ui.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vaiinilla.app.ui.components.StudentTab
import com.vaiinilla.app.ui.components.VaiinillaBottomNav

/** Overlay persistent-style student nav for Roborazzi screens that compose a single screen. */
@Composable
fun ScreenshotWithStudentNav(
    activeTab: StudentTab,
    cartCount: Int = 0,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()
        VaiinillaBottomNav(
            activeTab = activeTab,
            cartCount = cartCount,
            onTabSelected = {},
            enableDrag = false,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
