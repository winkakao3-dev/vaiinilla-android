package com.vaiinilla.app.ui.parentmode

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.guardianship.ChildGuardian
import com.vaiinilla.app.domain.guardianship.ChildWallet
import com.vaiinilla.app.domain.guardianship.Dependent
import com.vaiinilla.app.domain.guardianship.DependentAccountRepository
import com.vaiinilla.app.domain.guardianship.ScheduledOrder
import com.vaiinilla.app.domain.guardianship.StudentRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.math.BigDecimal

/** La ficha de un hijo: su saldo, lo apartado y sus movimientos, vistos con su contexto. */
class ChildAccountState(
    child: Dependent,
    val services: ParentModeServices,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    var child by mutableStateOf(child)
        private set
    var wallet by mutableStateOf<ChildWallet?>(null)
        private set
    var scheduled by mutableStateOf<List<ScheduledOrder>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set
    var cancelingOrderId by mutableStateOf<String?>(null)
        private set
    var changingGuardianId by mutableStateOf<String?>(null)
        private set
    var isBlockingCredential by mutableStateOf(false)
        private set
    var requests by mutableStateOf<List<StudentRequest>>(emptyList())
        private set
    var notice by mutableStateOf<String?>(null)

    /** Un token por hijo: todo lo de esta ficha sale como él. */
    val account: DependentAccountRepository = services.accounts.forDependent(child.userId)

    /** El saldo de su cartera ya cargada; mientras tanto, el de la lista de hijos. */
    val balance: BigDecimal get() = wallet?.balance ?: child.balance ?: BigDecimal.ZERO

    suspend fun load() {
        if (isLoading) return
        isLoading = true
        try {
            coroutineScope {
                val walletCall = async(io) { account.wallet() }
                val ordersCall = async(io) { account.orders() }
                val walletResult = walletCall.await()
                walletResult
                    .onSuccess {
                        wallet = it
                        loadError = null
                    }.onFailure {
                        if (wallet ==
                            null
                        ) {
                            loadError =
                                it.toUserFacingMessage("No pudimos abrir la cuenta de ${child.firstName}.")
                        }
                    }
                ordersCall.await().onSuccess { scheduled = ScheduledOrder.upcoming(it) }
            }
            loadRequests()
        } finally {
            isLoading = false
        }
    }

    /** Solo el principal pide devoluciones y bajas; si fallan, la ficha sigue sin esa sección al día. */
    private suspend fun loadRequests() {
        if (!child.isPrincipal) return
        withContext(io) { services.guardianship.studentRequests(child.userId) }.onSuccess { requests = it }
    }

    fun didSend(request: StudentRequest) {
        requests = listOf(request) + requests
    }

    suspend fun refreshWallet() {
        withContext(io) { account.wallet() }.onSuccess { wallet = it }
    }

    suspend fun refreshScheduled() {
        withContext(io) { account.orders() }.onSuccess { scheduled = ScheduledOrder.upcoming(it) }
    }

    fun updateChild(dependent: Dependent) {
        child = dependent
    }

    /** Quita a otro tutor; devuelve al hijo como quedó para que la lista de hijos también se entere. */
    suspend fun removeGuardian(guardian: ChildGuardian): Dependent? =
        changeGuardian(guardian, "${guardian.firstName} ya no cuida a ${child.firstName}.") {
            services.guardianship.removeGuardian(child.userId, guardian.id, newKey())
        }

    suspend fun makePrincipal(guardian: ChildGuardian): Dependent? =
        changeGuardian(guardian, "Ahora ${guardian.firstName} pone los controles.") {
            services.guardianship.makePrincipal(child.userId, guardian.id, newKey())
        }

    private suspend fun changeGuardian(
        guardian: ChildGuardian,
        done: String,
        call: () -> Result<Dependent>,
    ): Dependent? {
        changingGuardianId = guardian.id
        try {
            return withContext(io) { call() }.fold(
                onSuccess = {
                    child = it
                    notice = done
                    it
                },
                onFailure = {
                    notice = it.toUserFacingMessage("No se pudo cambiar.")
                    null
                },
            )
        } finally {
            changingGuardianId = null
        }
    }

    fun didSetPin(hasPin: Boolean): Dependent {
        child = child.copy(hasPin = hasPin)
        notice = if (hasPin) "Caja le pedirá su PIN a ${child.firstName}." else "${child.firstName} ya no necesita PIN."
        return child
    }

    suspend fun blockCredential(): Dependent? {
        isBlockingCredential = true
        try {
            return withContext(io) { services.guardianship.blockCredentials(child.userId, newKey()) }.fold(
                onSuccess = {
                    child = child.copy(hasCredential = false)
                    notice = "Credencial bloqueada. En Caja le dan otra."
                    child
                },
                onFailure = {
                    notice = it.toUserFacingMessage("No se pudo bloquear. Intenta otra vez.")
                    null
                },
            )
        } finally {
            isBlockingCredential = false
        }
    }

    suspend fun didReserve(days: Int) {
        notice = if (days == 1) "Apartado 1 día." else "Apartados $days días."
        refreshScheduled()
        refreshWallet()
    }

    suspend fun cancel(order: ScheduledOrder) {
        cancelingOrderId = order.id
        try {
            withContext(io) { account.cancel(order, newKey()) }
                .onSuccess {
                    scheduled = scheduled.filterNot { it.id == order.id }
                    notice = "Cancelado. El saldo regresó a ${child.firstName}."
                    refreshWallet()
                }.onFailure { notice = it.toUserFacingMessage("No se pudo cancelar.") }
        } finally {
            cancelingOrderId = null
        }
    }
}
