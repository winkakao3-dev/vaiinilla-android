package com.vaiinilla.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.vaiinilla.app.ui.components.StudentTab
import com.vaiinilla.app.ui.components.VaiinillaBottomNav
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/** Routes that participate in the persistent student shell (content swaps; nav stays). */
fun studentTabForRoute(route: String?): StudentTab? =
    when {
        route == null -> null
        route == Routes.CATALOG -> StudentTab.MENU
        route == Routes.ASSISTANT ||
            route == Routes.ASSISTANT_CHAT -> StudentTab.ASSISTANT
        route == Routes.STUDENT_TRACKING -> StudentTab.ORDERS
        route == Routes.WALLET -> StudentTab.WALLET
        route == Routes.CART -> StudentTab.CART
        else -> null
    }

fun routeForStudentTab(tab: StudentTab): String =
    when (tab) {
        StudentTab.MENU -> Routes.CATALOG
        StudentTab.ASSISTANT -> Routes.ASSISTANT
        StudentTab.ORDERS -> Routes.STUDENT_TRACKING
        StudentTab.WALLET -> Routes.WALLET
        StudentTab.CART -> Routes.CART
    }

fun studentTabOrder(tab: StudentTab?): Int =
    when (tab) {
        StudentTab.MENU -> 0
        StudentTab.ORDERS -> 1
        StudentTab.WALLET -> 2
        StudentTab.CART -> 3
        StudentTab.ASSISTANT, null -> -1
    }

fun shouldShowStudentNav(
    route: String?,
    catalogDetailOpen: Boolean,
): Boolean =
    studentTabForRoute(route) != null &&
        route != Routes.ASSISTANT &&
        route != Routes.ASSISTANT_CHAT &&
        !(route == Routes.CATALOG && catalogDetailOpen)

/**
 * Persistent student chrome — Compose equivalent of Flutter
 * `Scaffold(extendBody: true, bottomNavigationBar: SpringFloatingNavBar)`.
 *
 * [content] (NavHost) fills the screen; the solid floating dock overlays and never remounts when
 * switching student tabs. Detail surfaces can hide the dock while the route remains `catalog`.
 */
@Composable
fun StudentShellHost(
    navController: NavHostController,
    cartCount: Int,
    onNavigateStudent: (String) -> Unit,
    onPrepareStudent: (StudentTab) -> Unit = {},
    catalogDetailOpen: Boolean = false,
    content: @Composable () -> Unit,
) {
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val activeTab = studentTabForRoute(route)
    val showNav = shouldShowStudentNav(route, catalogDetailOpen)

    val hazeState = rememberHazeState()
    val colors = LocalVaiinillaColors.current
    Box(modifier = Modifier.fillMaxSize().background(colors.paper)) {
        Box(
            modifier = Modifier.fillMaxSize().hazeSource(hazeState),
        ) {
            content()
        }
        if (activeTab != null && showNav) {
            VaiinillaBottomNav(
                activeTab = activeTab,
                cartCount = cartCount,
                hazeState = hazeState,
                enableDrag = true,
                onTabPreparing = onPrepareStudent,
                onTabSelected = { tab ->
                    val target = routeForStudentTab(tab)
                    onNavigateStudent(target)
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
