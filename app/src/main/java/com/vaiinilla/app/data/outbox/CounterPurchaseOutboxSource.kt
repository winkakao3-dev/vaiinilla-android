package com.vaiinilla.app.data.outbox

import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.counter.CounterShiftRegistry
import com.vaiinilla.app.domain.counter.OfflineCounterRules
import com.vaiinilla.app.domain.counter.OfflineCounterStore
import com.vaiinilla.app.domain.counter.StudentCounterRepository
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.outbox.DrainOutcome
import com.vaiinilla.app.domain.outbox.OutboxContext
import com.vaiinilla.app.domain.outbox.OutboxSource
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Las ventas de cafetería que Caja guardó sin internet. Mientras Caja las tiene abiertas en pantalla las manda su
 * propia cola; si Caja ya salió o la app está cerrada, se mandan desde aquí con el token del cajero y la misma
 * llave de idempotencia de cada venta (su `id`).
 */
@Singleton
class CounterPurchaseOutboxSource
    @Inject
    constructor(
        private val store: OfflineCounterStore,
        private val repository: StudentCounterRepository,
    ) : OutboxSource {
        var now: () -> Instant = Instant::now
        var today: () -> String = { LocalDate.now().toString() }

        override val name: String = "ventas de cafetería"

        override fun hasWork(): Boolean = store.scopesWithPurchases().isNotEmpty()

        override suspend fun drain(context: OutboxContext): DrainOutcome {
            var outcome = DrainOutcome.DONE
            for (scope in store.scopesWithPurchases()) {
                if (CounterShiftRegistry.isActive(scope)) {
                    // Su cola abierta las manda; si matan la app antes, el siguiente intento las encuentra.
                    outcome = outcome and DrainOutcome.RETRY_LATER
                    continue
                }
                val mode =
                    context
                        .modeOfMembership(scope.membershipId)
                        ?.takeIf { it.role == OperationalRole.CASHIER || it.role == OperationalRole.ADMIN }
                        ?: continue
                val token = context.tokenFor(mode)
                if (token == null) {
                    outcome = outcome and DrainOutcome.RETRY_LATER
                    continue
                }
                outcome = outcome and drainScope(scope, token)
            }
            return outcome
        }

        private fun drainScope(
            scope: com.vaiinilla.app.domain.counter.CounterScope,
            token: String,
        ): DrainOutcome {
            var snapshot = store.load(scope, today())
            for (purchase in snapshot.pending) {
                if (CounterShiftRegistry.isActive(scope)) return DrainOutcome.DONE
                val problem =
                    if (OfflineCounterRules.isTooOld(purchase, now())) {
                        OfflineCounterRules.TOO_OLD_PROBLEM
                    } else {
                        val result = repository.purchaseAs(token, purchase.studentId, purchase.items, purchase.id)
                        val error = result.exceptionOrNull()
                        when {
                            error == null -> null
                            OfflineCounterRules.isConnectionProblem(error) -> return DrainOutcome.RETRY_LATER
                            else -> error.toUserFacingMessage("No se pudo cobrar.")
                        }
                    }
                snapshot =
                    if (problem == null) {
                        snapshot.copy(purchases = snapshot.purchases.filterNot { it.id == purchase.id })
                    } else {
                        snapshot.copy(
                            purchases =
                                snapshot.purchases.map {
                                    if (it.id ==
                                        purchase.id
                                    ) {
                                        it.copy(problem = problem)
                                    } else {
                                        it
                                    }
                                },
                        )
                    }
                store.save(scope, snapshot)
            }
            return DrainOutcome.DONE
        }
    }
