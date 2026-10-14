package com.vaiinilla.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import com.vaiinilla.app.ui.components.reducedMotion
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

/** Con productos, el carrito es un paso de cobro: su botón de pagar ocupa el lugar de la barra. */
fun shouldShowStudentNav(
    route: String?,
    catalogDetailOpen: Boolean,
    cartHasItems: Boolean = false,
    walletCodeOpen: Boolean = false,
): Boolean =
    studentTabForRoute(route) != null &&
        !(route == Routes.WALLET && walletCodeOpen) &&
        route != Routes.ASSISTANT &&
        route != Routes.ASSISTANT_CHAT &&
        !(route == Routes.CATALOG && catalogDetailOpen) &&
        !(route == Routes.CART && cartHasItems)

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
    /** El QR personal de la cartera está abierto sobre la casita: la barra se va mientras tanto. */
    walletCodeOpen: Boolean = false,
    /** Lo que va encima de todo, también de la barra (la entrada a un lugar). */
    overlay: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val activeTab = studentTabForRoute(route)
    val showNav = shouldShowStudentNav(route, catalogDetailOpen, cartHasItems = cartCount > 0, walletCodeOpen = walletCodeOpen)

    val hazeState = rememberHazeState()
    val colors = LocalVaiinillaColors.current
    Box(modifier = Modifier.fillMaxSize().background(colors.paper)) {
        Box(
            modifier = Modifier.fillMaxSize().hazeSource(hazeState),
        ) {
            content()
        }
        val reduced = reducedMotion()
        if (activeTab != null) {
            AnimatedVisibility(
                visible = showNav,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter =
                    if (reduced) {
                        EnterTransition.None
                    } else {
                        slideInVertically(spring(dampingRatio = 0.85f, stiffness = 420f)) { it } + fadeIn()
                    },
                exit =
                    if (reduced) {
                        ExitTransition.None
                    } else {
                        slideOutVertically(spring(dampingRatio = 1f, stiffness = 520f)) { it } + fadeOut()
                    },
            ) {
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
                )
            }
        }
        overlay()
    }
}
