package com.vaiinilla.app.core.notifications

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Un aviso de un hijo (su pedido, saldo bajo o recarga): a qué escuela cambiar y qué ficha abrir. */
data class ChildDeepLink(
    val dependentUserId: String,
    val establishmentId: String?,
) {
    companion object {
        const val EXTRA_DEPENDENT_ID = "child_dependent_id"
        const val EXTRA_ESTABLISHMENT_ID = "child_establishment_id"

        private val ID = Regex("^[A-Za-z0-9_-]{1,100}$")

        /** Los datos del push: `order_state` de tutor, `saldo_bajo` y `recarga_acreditada`. */
        fun fromPush(data: Map<String, String>): ChildDeepLink? {
            val dependentId = data["dependienteUsuarioId"]?.trim()?.takeIf(ID::matches) ?: return null
            return ChildDeepLink(dependentId, data["establecimientoId"]?.trim()?.takeIf(ID::matches))
        }

        fun fromIntent(intent: Intent?): ChildDeepLink? {
            val dependentId = intent?.getStringExtra(EXTRA_DEPENDENT_ID)?.trim()?.takeIf(ID::matches) ?: return null
            return ChildDeepLink(
                dependentId,
                intent.getStringExtra(EXTRA_ESTABLISHMENT_ID)?.trim()?.takeIf(ID::matches),
            )
        }
    }
}

/** Qué hace la app con el aviso de un hijo, según la escuela en la que está. */
sealed interface ChildNoticeStep {
    /** Ya está en su escuela (o el aviso no dice cuál): Cartera abre su ficha. */
    data object OpenWallet : ChildNoticeStep

    /** Primero se cambia a esa escuela desde "Tus escuelas"; si ya no es cliente ahí, se descarta. */
    data class SwitchSchool(
        val establishmentId: String,
    ) : ChildNoticeStep

    companion object {
        fun of(
            link: ChildDeepLink,
            currentEstablishmentId: String?,
        ): ChildNoticeStep {
            val target = link.establishmentId
            return if (target == null || target == currentEstablishmentId) OpenWallet else SwitchSchool(target)
        }
    }
}

/**
 * El aviso de un hijo viaja en dos tiempos: [pending] lo deja MainActivity y lo atiende la navegación
 * (cambiar de escuela, ir a Cartera); [requestedChild] lo consume Cartera ya en su escuela.
 */
object ChildDeepLinkStore {
    private val pendingFlow = MutableStateFlow<ChildDeepLink?>(null)
    private val requestedFlow = MutableStateFlow<ChildDeepLink?>(null)

    val pending: StateFlow<ChildDeepLink?> = pendingFlow.asStateFlow()
    val requestedChild: StateFlow<ChildDeepLink?> = requestedFlow.asStateFlow()

    /** Lee el aviso del intent y lo quita, para que girar la pantalla no lo vuelva a abrir. */
    fun capture(intent: Intent?) {
        val link = ChildDeepLink.fromIntent(intent) ?: return
        intent?.removeExtra(ChildDeepLink.EXTRA_DEPENDENT_ID)
        intent?.removeExtra(ChildDeepLink.EXTRA_ESTABLISHMENT_ID)
        pendingFlow.value = link
    }

    fun request(link: ChildDeepLink) {
        pendingFlow.value = link
    }

    fun clearPending() {
        pendingFlow.value = null
    }

    fun handToWallet(link: ChildDeepLink) {
        pendingFlow.value = null
        requestedFlow.value = link
    }

    fun consumeChild() {
        requestedFlow.value = null
    }

    /** Al cerrar sesión: un aviso sin atender no debe abrir la ficha de un hijo para el siguiente usuario. */
    fun reset() {
        pendingFlow.value = null
        requestedFlow.value = null
    }
}
