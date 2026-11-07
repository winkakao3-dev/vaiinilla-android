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
) {
    /** El rol activo, o null si nadie lo tiene (en Modos o sin entrar). Se lee en composición. */
    val activeRole: OperationalRole?
        get() = client.uiState.value.role ?: cashier.uiState.value.role

    fun enter(role: OperationalRole) {
        when (role) {
            OperationalRole.CASHIER -> {
                client.releaseRole()
                cashier.start()
            }
            else -> {
                cashier.release()
                client.setRole(role)
            }
        }
    }

    /** Vuelve a Modos: ningún rol queda activo. */
    fun clearAll() {
        client.clearRole()
        cashier.clearRole()
    }

    /** A quién le avisa la pantalla de [role] que está a la vista. */
    fun presence(role: OperationalRole): OperationalPresence = if (role == OperationalRole.CASHIER) cashier else client
}
