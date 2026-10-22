package com.vaiinilla.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.vaiinilla.app.core.notifications.ChildDeepLink
import com.vaiinilla.app.core.notifications.ChildDeepLinkStore
import com.vaiinilla.app.core.notifications.ChildNoticeStep
import com.vaiinilla.app.domain.mode.MySchool
import kotlinx.coroutines.launch

/**
 * Tocar el aviso de un hijo: con la sesión de cliente, cambia a su escuela si hace falta (desde "Tus
 * escuelas", sin código) y va a Cartera, que abre su ficha. Si ya no es cliente de esa escuela, el aviso
 * se descarta. [action] es la misma decisión que usa un pedido abierto desde un push.
 */
@Composable
internal fun ChildNoticeEffect(
    action: OrderNotificationAction,
    currentEstablishmentId: String?,
    returnToClient: (onReturned: () -> Unit) -> Unit,
    findSchool: suspend (establishmentId: String) -> MySchool?,
    openSchool: (MySchool, onEntered: () -> Unit) -> Unit,
    openWallet: () -> Unit,
) {
    val pending by ChildDeepLinkStore.pending.collectAsState()
    val currentId by rememberUpdatedState(currentEstablishmentId)
    val scope = rememberCoroutineScope()

    suspend fun route(link: ChildDeepLink) {
        when (val step = ChildNoticeStep.of(link, currentId)) {
            ChildNoticeStep.OpenWallet -> {
                ChildDeepLinkStore.handToWallet(link)
                openWallet()
            }
            is ChildNoticeStep.SwitchSchool -> {
                ChildDeepLinkStore.clearPending()
                val school = findSchool(step.establishmentId) ?: return
                openSchool(school) {
                    ChildDeepLinkStore.handToWallet(link)
                    openWallet()
                }
            }
        }
    }

    LaunchedEffect(pending, action) {
        val link = pending ?: return@LaunchedEffect
        when (action) {
            OrderNotificationAction.OPEN_CLIENT_ORDER -> route(link)
            OrderNotificationAction.SWITCH_TO_CLIENT_ORDER -> {
                // Se suelta antes: al volver a cliente cambia el rol y este efecto correría otra vez.
                ChildDeepLinkStore.clearPending()
                returnToClient { scope.launch { route(link) } }
            }
            OrderNotificationAction.DISCARD -> ChildDeepLinkStore.clearPending()
            OrderNotificationAction.WAIT,
            OrderNotificationAction.SELECT_STAFF_ORDER,
            OrderNotificationAction.OPEN_STAFF_MODES,
            -> Unit
        }
    }
}
