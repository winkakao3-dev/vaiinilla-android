package com.vaiinilla.app.data.admin

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.domain.admin.AdminLiveCounts
import com.vaiinilla.app.domain.admin.AdminPeriod
import com.vaiinilla.app.domain.admin.AdminWalletSummary
import com.vaiinilla.app.domain.admin.CashbackRule
import com.vaiinilla.app.domain.admin.InvitationState
import com.vaiinilla.app.domain.admin.InvitationsPage
import com.vaiinilla.app.domain.admin.ManagedSpace
import com.vaiinilla.app.domain.admin.ReservationSchedule
import com.vaiinilla.app.domain.admin.SpaceBatchResult
import com.vaiinilla.app.domain.admin.SpacePatch
import com.vaiinilla.app.domain.admin.SpaceType
import com.vaiinilla.app.domain.admin.StaffInvitation
import com.vaiinilla.app.domain.admin.StaffMembership
import com.vaiinilla.app.domain.admin.StaffRole
import com.vaiinilla.app.domain.admin.StoreConfig
import com.vaiinilla.app.domain.admin.StoreFlow
import com.vaiinilla.app.domain.admin.StoreVideo
import com.vaiinilla.app.domain.admin.StripeSetup
import com.vaiinilla.app.domain.repository.AdminSpacesRepository
import com.vaiinilla.app.domain.repository.AdminStaffRepository
import com.vaiinilla.app.domain.repository.AdminStoreRepository
import com.vaiinilla.app.domain.repository.CatalogRepositoryException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject

private const val EMPTY_BODY = "{}"

private fun key(idempotencyKey: String) = mapOf("Idempotency-Key" to idempotencyKey)

/** Los errores del backend llegan con su código para que la pantalla diga qué pasó. */
private fun <T> Result<T>.mapApiErrors(): Result<T> =
    fold(
        onSuccess = { Result.success(it) },
        onFailure = { error ->
            Result.failure(
                if (error is ApiClientException) {
                    CatalogRepositoryException(
                        error.code,
                        error.message ?: error.code,
                        error,
                    )
                } else {
                    error
                },
            )
        },
    )

class RemoteAdminStaffRepository(
    private val apiClient: VaiinillaApiClient,
    private val json: AdminBusinessJson = AdminBusinessJson(),
) : AdminStaffRepository {
    override fun invitations(
        state: InvitationState?,
        cursor: String?,
    ): Result<InvitationsPage> {
        val query =
            buildMap {
                state?.let { put("estado", it.wire) }
                cursor?.let { put("cursor", it) }
                put("limit", PAGE_SIZE.toString())
            }
        return apiClient.get("personal/invitaciones", query).mapCatching(json::parseInvitationsPage).mapApiErrors()
    }

    override fun invite(
        email: String,
        role: StaffRole,
        idempotencyKey: String,
    ): Result<StaffInvitation> =
        apiClient
            .post("personal/invitaciones", json.encodeInvitation(email, role.wire), key(idempotencyKey))
            .mapCatching(json::parseInvitation)
            .mapApiErrors()

    override fun resend(
        invitationId: String,
        idempotencyKey: String,
    ): Result<StaffInvitation> =
        apiClient
            .post("personal/invitaciones/$invitationId/reenviar", EMPTY_BODY, key(idempotencyKey))
            .mapCatching(json::parseInvitation)
            .mapApiErrors()

    override fun revoke(
        invitationId: String,
        idempotencyKey: String,
    ): Result<StaffInvitation> =
        apiClient
            .post("personal/invitaciones/$invitationId/revocar", EMPTY_BODY, key(idempotencyKey))
            .mapCatching(json::parseInvitation)
            .mapApiErrors()

    override fun memberships(): Result<List<StaffMembership>> =
        apiClient.get("personal/invitaciones/membresias").mapCatching(json::parseMemberships).mapApiErrors()

    override fun changeRole(
        membershipId: String,
        role: StaffRole,
        idempotencyKey: String,
    ): Result<StaffMembership> =
        apiClient
            .patch("personal/invitaciones/membresias/$membershipId", json.encodeRole(role.wire), key(idempotencyKey))
            .mapCatching(json::parseMembership)
            .mapApiErrors()

    override fun deactivate(
        membershipId: String,
        idempotencyKey: String,
    ): Result<Unit> =
        apiClient
            .post("personal/invitaciones/membresias/$membershipId/desactivar", EMPTY_BODY, key(idempotencyKey))
            .map { }
            .mapApiErrors()

    private companion object {
        const val PAGE_SIZE = 20
    }
}

class RemoteAdminStoreRepository(
    private val apiClient: VaiinillaApiClient,
    private val json: AdminBusinessJson = AdminBusinessJson(),
) : AdminStoreRepository {
    private val plain = Json { ignoreUnknownKeys = true }

    override fun config(): Result<StoreConfig> =
        apiClient.get("establecimiento/configuracion").mapCatching(json::parseStoreConfig).mapApiErrors()

    // El PATCH de la configuración no pide llave de idempotencia: guarda lo que se manda tal cual.
    override fun saveFlow(flow: StoreFlow): Result<StoreConfig> = patchConfig(json.encodeFlow(flow))

    override fun saveVideos(videos: List<StoreVideo>): Result<StoreConfig> = patchConfig(json.encodeVideos(videos))

    override fun setVisibleInDirectory(visible: Boolean): Result<StoreConfig> =
        patchConfig(json.encodeDirectory(visible))

    private fun patchConfig(body: String): Result<StoreConfig> =
        apiClient.patch("establecimiento/configuracion", body).mapCatching(json::parseStoreConfig).mapApiErrors()

    override fun stripe(): Result<StripeSetup?> =
        apiClient.get("stripe/configuracion").mapCatching(json::parseStripeSetup).mapApiErrors()

    override fun stripeOnboardingLink(idempotencyKey: String): Result<String> =
        apiClient
            .post("stripe/onboarding", EMPTY_BODY, key(idempotencyKey))
            .mapCatching(json::parseOnboardingLink)
            .mapApiErrors()

    override fun setCardPayments(
        enabled: Boolean,
        idempotencyKey: String,
    ): Result<StripeSetup> =
        apiClient
            .patch("stripe/configuracion", json.encodeCardPayments(enabled), key(idempotencyKey))
            .mapCatching { requireNotNull(json.parseStripeSetup(it)) }
            .mapApiErrors()

    override fun setFeePaidByCustomer(
        paidByCustomer: Boolean,
        idempotencyKey: String,
    ): Result<StripeSetup> =
        apiClient
            .patch("stripe/comision-al-cliente", json.encodeFeePaidByCustomer(paidByCustomer), key(idempotencyKey))
            .mapCatching { requireNotNull(json.parseStripeSetup(it)) }
            .mapApiErrors()

    override fun cashbackRule(): Result<CashbackRule?> =
        apiClient.get("wallets/reglas-cashback").mapCatching(json::parseCashbackRule).mapApiErrors()

    override fun saveCashbackRule(
        rule: CashbackRule,
        idempotencyKey: String,
    ): Result<CashbackRule> =
        apiClient
            .post("wallets/reglas-cashback", json.encodeCashbackRule(rule), key(idempotencyKey))
            .mapCatching { requireNotNull(json.parseCashbackRule(it)) }
            .mapApiErrors()

    override fun walletSummary(period: AdminPeriod): Result<AdminWalletSummary> =
        apiClient
            .get("reportes/resumen", mapOf("desde" to period.from.toString(), "hasta" to period.to.toString()))
            .mapCatching(json::parseWalletSummary)
            .mapApiErrors()

    override fun liveCounts(): Result<AdminLiveCounts> {
        val invitations =
            apiClient
                .get("personal/invitaciones", mapOf("estado" to "pendiente", "limit" to "50"))
                .mapCatching { json.parseCursorCount(it).first }
                .getOrNull()
        val cashOpen =
            apiClient
                .get("sesiones-caja/activa")
                .mapCatching { raw ->
                    plain.parseToJsonElement(raw).jsonObject["data"].let {
                        it != null &&
                            it != JsonNull
                    }
                }.getOrNull()
        val active =
            apiClient
                .get("pedidos", mapOf("estado" to "por_cobrar,cobrado,preparando,listo", "limit" to "100"))
                .mapCatching(json::parseCursorCount)
                .getOrNull()
        if (invitations == null && cashOpen == null && active == null) {
            return Result.failure(IllegalStateException("No se pudo leer el estado del negocio."))
        }
        return Result.success(AdminLiveCounts(invitations, cashOpen, active?.first, active?.second == true))
    }
}

class RemoteAdminSpacesRepository(
    private val apiClient: VaiinillaApiClient,
    private val json: AdminBusinessJson = AdminBusinessJson(),
) : AdminSpacesRepository {
    override fun spaces(): Result<List<ManagedSpace>> =
        apiClient.get("espacios/administracion").mapCatching(json::parseSpaces).mapApiErrors()

    override fun create(
        name: String,
        type: SpaceType,
        idempotencyKey: String,
    ): Result<ManagedSpace> =
        apiClient
            .post("espacios", json.encodeNewSpace(name, type.wire), key(idempotencyKey))
            .mapCatching(json::parseSpace)
            .mapApiErrors()

    override fun createBatch(
        type: SpaceType,
        prefix: String,
        from: Int,
        to: Int,
        idempotencyKey: String,
    ): Result<SpaceBatchResult> =
        apiClient
            .post("espacios/lote", json.encodeBatch(type.wire, prefix, from, to), key(idempotencyKey))
            .mapCatching(json::parseBatch)
            .mapApiErrors()

    override fun update(
        spaceId: Int,
        patch: SpacePatch,
        idempotencyKey: String,
    ): Result<ManagedSpace> =
        apiClient
            .patch("espacios/$spaceId", json.encodeSpacePatch(patch), key(idempotencyKey))
            .mapCatching(json::parseSpace)
            .mapApiErrors()

    override fun delete(
        spaceId: Int,
        idempotencyKey: String,
    ): Result<Unit> = apiClient.delete("espacios/$spaceId", headers = key(idempotencyKey)).map { }.mapApiErrors()

    override fun uploadImage(
        spaceId: Int,
        bytes: ByteArray,
        filename: String,
        mimeType: String,
        idempotencyKey: String,
    ): Result<ManagedSpace> =
        apiClient
            .putMultipart("espacios/$spaceId/imagen", "imagen", filename, mimeType, bytes, key(idempotencyKey))
            .mapCatching(json::parseSpace)
            .mapApiErrors()

    override fun removeImage(
        spaceId: Int,
        idempotencyKey: String,
    ): Result<ManagedSpace> =
        apiClient
            .delete("espacios/$spaceId/imagen", headers = key(idempotencyKey))
            .mapCatching(json::parseSpace)
            .mapApiErrors()

    override fun reservationSchedule(): Result<ReservationSchedule> =
        apiClient.get("reservas/configuracion").mapCatching(json::parseSchedule).mapApiErrors()

    override fun saveReservationSchedule(
        schedule: ReservationSchedule,
        idempotencyKey: String,
    ): Result<ReservationSchedule> =
        apiClient
            .put("reservas/configuracion", json.encodeSchedule(schedule), key(idempotencyKey))
            .mapCatching(json::parseSchedule)
            .mapApiErrors()

    override fun closeWithoutCharging(
        spaceId: Int,
        note: String?,
        idempotencyKey: String,
    ): Result<Unit> =
        apiClient
            .post("espacios/$spaceId/sesion/cierres", json.encodeForcedClose(note), key(idempotencyKey))
            .map { }
            .mapApiErrors()
}
