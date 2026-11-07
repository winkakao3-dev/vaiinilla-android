package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.domain.model.OperationalRole

/**
 * Quién tiene el rol operativo ahora. Cada rol de personal es su propio ViewModel con su propio estado; el cliente
 * y el resto (Cocina, Mesero, Administración) siguen en [OperationalViewModel]. Sólo uno está activo a la vez:
 * entrar a un rol suelta el anterior sin tirar lo que recuerda, igual que cuando todo vivía en un solo ViewModel.
 */
class OperationalRoles(
    private val client: OperationalViewModel,
    private val cashier: CashierViewModel,
    private val kitchen: KitchenViewModel,
    private val waiter: WaiterOrdersViewModel,
) {
    /** El rol activo, o null si nadie lo tiene (en Modos o sin entrar). Se lee en composición. */
    val activeRole: OperationalRole?
        get() =
            client.uiState.value.role
                ?: cashier.uiState.value.role
                ?: kitchen.uiState.value.role
                ?: waiter.uiState.value.role

    fun enter(role: OperationalRole) {
        when (role) {
            OperationalRole.CASHIER -> {
                releaseAllBut(OperationalRole.CASHIER)
                cashier.start()
            }
            OperationalRole.KITCHEN -> {
                releaseAllBut(OperationalRole.KITCHEN)
                kitchen.start()
            }
            OperationalRole.WAITER -> {
                releaseAllBut(OperationalRole.WAITER)
                waiter.start()
            }
            else -> {
                releaseAllBut(role)
                client.setRole(role)
            }
        }
    }

    /** Vuelve a Modos: ningún rol queda activo. */
    fun clearAll() {
        client.clearRole()
        cashier.clearRole()
        kitchen.clearRole()
        waiter.clearRole()
    }

    /** A quién le avisa la pantalla de [role] que está a la vista. */
    fun presence(role: OperationalRole): OperationalPresence =
        when (role) {
            OperationalRole.CASHIER -> cashier
            OperationalRole.KITCHEN -> kitchen
            OperationalRole.WAITER -> waiter
            else -> error("Solo el personal de Caja, Cocina y Mesero marca presencia: $role")
        }

    /** Suelta a todos menos al rol que entra; sólo uno tiene el rol a la vez. */
    private fun releaseAllBut(entering: OperationalRole) {
        if (entering != OperationalRole.CASHIER) cashier.release()
        if (entering != OperationalRole.KITCHEN) kitchen.release()
        if (entering != OperationalRole.WAITER) waiter.release()
        if (entering in STAFF_ROLES) client.releaseRole()
    }
}

private val STAFF_ROLES = setOf(OperationalRole.CASHIER, OperationalRole.KITCHEN, OperationalRole.WAITER)
