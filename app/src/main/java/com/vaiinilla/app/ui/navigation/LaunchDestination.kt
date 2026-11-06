package com.vaiinilla.app.ui.navigation

import com.vaiinilla.app.core.notifications.OrderNotificationTarget
import com.vaiinilla.app.domain.auth.student.StudentAuthSession
import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.model.OperationalRole

sealed class LaunchDestination {
    data object Login : LaunchDestination()

    data object Discovery : LaunchDestination()

    data object Catalog : LaunchDestination()

    data object StaffModes : LaunchDestination()
}

fun resolveLaunchDestination(
    pendingEstablishmentSlug: String?,
    session: StudentAuthSession?,
    hasStaffModes: Boolean,
    hasSavedVenue: Boolean,
    /** Falso si la consulta de modos falló: sin ella no se sabe si esta cuenta es de personal. */
    modesLoaded: Boolean = true,
    pendingSpaceToken: String? = null,
): LaunchDestination {
    // Escanear una mesa o una tienda es querer pedir ahí: no se pasa por la lista de modos.
    if (!pendingEstablishmentSlug.isNullOrBlank() || !pendingSpaceToken.isNullOrBlank()) {
        return LaunchDestination.Discovery
    }
    // Con red caída una cuenta de personal no debe terminar como cliente: Modos muestra el error y reintenta.
    if (session != null && !modesLoaded && !hasSavedVenue) return LaunchDestination.StaffModes
    if (session == null && !hasSavedVenue) return LaunchDestination.Login
    if (hasStaffModes) return LaunchDestination.StaffModes
    if (!hasSavedVenue) return LaunchDestination.Discovery
    return LaunchDestination.Catalog
}

fun LaunchDestination.toRoute(): String =
    when (this) {
        LaunchDestination.Login -> Routes.authLandingRoute(Routes.DISCOVERY)
        LaunchDestination.Discovery -> Routes.DISCOVERY
        LaunchDestination.Catalog -> Routes.CATALOG
        LaunchDestination.StaffModes -> Routes.STAFF_MODES
    }

fun hasStaffLaunchModes(roles: Collection<OperationalRole>): Boolean =
    roles.any { role ->
        when (role) {
            OperationalRole.CASHIER,
            OperationalRole.KITCHEN,
            OperationalRole.WAITER,
            OperationalRole.ADMIN,
            -> true
            OperationalRole.CLIENT -> false
        }
    }

internal enum class OrderNotificationAction {
    OPEN_CLIENT_ORDER,
    SWITCH_TO_CLIENT_ORDER,
    SELECT_STAFF_ORDER,
    OPEN_STAFF_MODES,
    WAIT,
    DISCARD,
}

internal fun resolveOrderNotificationAction(
    target: OrderNotificationTarget,
    hasStudentSession: Boolean,
    studentAuthLoading: Boolean,
    operationalRole: OperationalRole?,
    accessLoading: Boolean,
    hasStaffModes: Boolean,
): OrderNotificationAction =
    when (target) {
        OrderNotificationTarget.CLIENT ->
            when {
                !hasStudentSession ->
                    if (studentAuthLoading) {
                        OrderNotificationAction.WAIT
                    } else {
                        OrderNotificationAction.DISCARD
                    }
                operationalRole != null && operationalRole != OperationalRole.CLIENT ->
                    if (accessLoading) {
                        OrderNotificationAction.WAIT
                    } else {
                        OrderNotificationAction.SWITCH_TO_CLIENT_ORDER
                    }
                else -> OrderNotificationAction.OPEN_CLIENT_ORDER
            }
        OrderNotificationTarget.STAFF, OrderNotificationTarget.WAITER_CALL ->
            when {
                operationalRole != null && operationalRole != OperationalRole.CLIENT ->
                    OrderNotificationAction.SELECT_STAFF_ORDER
                hasStudentSession && accessLoading -> OrderNotificationAction.WAIT
                hasStudentSession && hasStaffModes -> OrderNotificationAction.OPEN_STAFF_MODES
                hasStudentSession -> OrderNotificationAction.DISCARD
                else -> OrderNotificationAction.WAIT
            }
    }

internal enum class WaiterCallAction {
    OPEN_WAITER,
    ACTIVATE_WAITER,
    OPEN_STAFF_MODES,
    WAIT,
}

/**
 * Tocar «Mesa te llama» lleva al tablero del mesero, donde la llamada se ve con su motivo (K02). Sin modo activo, se
 * activa el modo mesero que [pickWaiterCallMode] eligió; con otro modo de personal activo no se le saca de lo que
 * hace: Modos. Con la app cerrada se espera a que el arranque termine ([launchSettled]): antes no hay modos cargados
 * y el aviso se gastaba en Modos.
 */
internal fun resolveWaiterCallAction(
    operationalRole: OperationalRole?,
    accessLoading: Boolean,
    waiterModes: Int,
    launchSettled: Boolean = true,
    waiterModeChosen: Boolean = waiterModes == 1,
): WaiterCallAction =
    when {
        !launchSettled -> WaiterCallAction.WAIT
        operationalRole == OperationalRole.WAITER -> WaiterCallAction.OPEN_WAITER
        accessLoading -> WaiterCallAction.WAIT
        (operationalRole == null || operationalRole == OperationalRole.CLIENT) && waiterModeChosen ->
            WaiterCallAction.ACTIVATE_WAITER
        else -> WaiterCallAction.OPEN_STAFF_MODES
    }

/**
 * El modo mesero que abre una llamada de mesa: el del negocio que llamó si el aviso lo trae; si no, el único modo
 * mesero, o el del último negocio donde trabajó. Sin forma de saberlo, null: se elige en Modos.
 */
internal fun pickWaiterCallMode(
    modes: List<AuthorizedMode>,
    noticeEstablishmentId: String?,
    lastEstablishmentId: String?,
): AuthorizedMode? {
    val waiterModes = modes.filter { it.role == OperationalRole.WAITER && it.establishmentActive }
    noticeEstablishmentId?.let { id -> waiterModes.firstOrNull { it.establishmentId == id }?.let { return it } }
    if (waiterModes.size == 1) return waiterModes.single()
    return lastEstablishmentId?.let { id -> waiterModes.firstOrNull { it.establishmentId == id } }
}
