package com.vaiinilla.app.data.guardianship

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.domain.guardianship.CardReload
import com.vaiinilla.app.domain.guardianship.CardReloadCheckout
import com.vaiinilla.app.domain.guardianship.ChildWallet
import com.vaiinilla.app.domain.guardianship.DependentAccountRepository
import com.vaiinilla.app.domain.guardianship.DependentContextGrant
import com.vaiinilla.app.domain.guardianship.MenuCalendar
import com.vaiinilla.app.domain.guardianship.MyGuardianControls
import com.vaiinilla.app.domain.guardianship.ScheduledDayOrder
import com.vaiinilla.app.domain.guardianship.ScheduledOrder
import java.math.BigDecimal

/**
 * El token del hijo: se pide al abrirlo y se renueva un minuto antes de que venza. Vive solo en
 * memoria y nunca reemplaza la sesión guardada del tutor, así que al volver a la lista todo sigue
 * saliendo con el token del papá.
 */
class DependentContextSession(
    private val dependentId: String,
    private val issue: (String) -> Result<DependentContextGrant>,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private var token: String? = null
    private var expiresAtMillis: Long = 0L

    @Synchronized
    fun validToken(): Result<String> {
        val current = token
        if (current != null && expiresAtMillis - nowMillis() > RENEW_BEFORE_MS) return Result.success(current)
        return refresh()
    }

    /** Dos pantallas que piden a la vez esperan la misma emisión (el método está sincronizado). */
    @Synchronized
    fun refresh(): Result<String> =
        issue(dependentId).map { grant ->
            token = grant.accessToken
            expiresAtMillis = nowMillis() + grant.expiresInSeconds * 1000
            grant.accessToken
        }

    private companion object {
        const val RENEW_BEFORE_MS = 60_000L
    }
}

/**
 * Todo lo que el tutor hace como su hijo. Cada llamada lleva el token del hijo explícito: si vence,
 * se renueva el del hijo y nunca se reintenta con el del tutor.
 */
class RemoteDependentAccountRepository(
    private val apiClient: VaiinillaApiClient,
    private val session: DependentContextSession,
    private val json: GuardianshipJson = GuardianshipJson(),
) : DependentAccountRepository {
    override fun wallet(): Result<ChildWallet> =
        asChild {
            apiClient.getWithBearer(it, "wallets/me")
        }.mapCatching(json::parseChildWallet)

    override fun startCardReload(
        amount: BigDecimal,
        idempotencyKey: String,
    ): Result<CardReloadCheckout> =
        asChild {
            apiClient.postWithBearer(
                it,
                "wallets/me/recargas-stripe",
                json.encodeReload(amount),
                key(idempotencyKey),
            )
        }.mapCatching(json::parseReloadCheckout)

    override fun cardReload(id: String): Result<CardReload> =
        asChild { apiClient.getWithBearer(it, "wallets/me/recargas-stripe/$id") }.mapCatching(json::parseReloadStatus)

    override fun menuCalendar(): Result<MenuCalendar> =
        asChild {
            apiClient.getWithBearer(it, "menu-dias")
        }.mapCatching(json::parseMenuCalendar)

    override fun controls(): Result<MyGuardianControls?> =
        asChild { apiClient.getWithBearer(it, "tutelas/mis-controles") }.mapCatching(json::parseMyControls)

    override fun reserve(
        days: List<ScheduledDayOrder>,
        idempotencyKey: String,
    ): Result<Int> =
        asChild { apiClient.postWithBearer(it, "pedidos/semana", json.encodeWeek(days), key(idempotencyKey)) }
            .mapCatching(json::parseWeekCount)

    override fun orders(): Result<List<ScheduledOrder>> =
        asChild {
            apiClient.getWithBearer(it, "pedidos")
        }.mapCatching(json::parseScheduledOrders)

    override fun cancel(
        order: ScheduledOrder,
        idempotencyKey: String,
    ): Result<Unit> =
        asChild {
            apiClient.postWithBearer(
                it,
                "pedidos/${order.id}/cancelaciones",
                json.encodeCancel(order.version, CANCEL_REASON),
                key(idempotencyKey),
            )
        }.map { }

    /** Un 401 renueva solo el token del hijo y repite una vez; los demás errores llegan tal cual. */
    private fun asChild(call: (String) -> Result<String>): Result<String> {
        val first =
            session.validToken().fold(
                onSuccess = call,
                onFailure = { return Result.failure(it) },
            )
        val error = first.exceptionOrNull()
        val result =
            if (error is ApiClientException && error.httpStatus == 401) {
                session.refresh().fold(onSuccess = call, onFailure = { Result.failure(it) })
            } else {
                first
            }
        return result.mapGuardianshipErrors()
    }

    private fun key(idempotencyKey: String) = mapOf("Idempotency-Key" to idempotencyKey)

    private companion object {
        const val CANCEL_REASON = "Cancelado por su tutor"
    }
}
