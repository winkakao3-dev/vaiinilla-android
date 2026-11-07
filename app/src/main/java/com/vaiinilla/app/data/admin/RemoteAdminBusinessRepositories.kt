package com.vaiinilla.app.data.admin

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.ApiJson
import com.vaiinilla.app.core.network.MutationIdempotency
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.core.network.idempotencyHeaders
import com.vaiinilla.app.core.network.mapApiFailure
import com.vaiinilla.app.domain.admin.AdminLiveCounts
import com.vaiinilla.app.domain.admin.AdminPeriod
import com.vaiinilla.app.domain.admin.AdminWalletSummary
import com.vaiinilla.app.domain.admin.AssistantMessage
import com.vaiinilla.app.domain.admin.AssistantPhoto
import com.vaiinilla.app.domain.admin.AssistantReply
import com.vaiinilla.app.domain.admin.AssistantStep
import com.vaiinilla.app.domain.admin.CashbackRule
import com.vaiinilla.app.domain.admin.ForcedCloseResult
import com.vaiinilla.app.domain.admin.HomeDeliverySettings
import com.vaiinilla.app.domain.admin.InvitationState
import com.vaiinilla.app.domain.admin.InvitationsPage
import com.vaiinilla.app.domain.admin.ManagedSpace
import com.vaiinilla.app.domain.admin.PreorderSettings
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

private const val EMPTY_BODY = "{}"

private fun key(idempotencyKey: String) = idempotencyHeaders(idempotencyKey)

/** Los errores del backend llegan con su código para que la pantalla diga qué pasó. */
private fun <T> Result<T>.mapApiErrors(): Result<T> =
    mapApiFailure {
        CatalogRepositoryException(
            it.code,
            it.message ?: it.code,
            it,
        )
    }

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
    private val plain = ApiJson.plain
    private val assistantJson = StoreAssistantJson()

    override fun config(): Result<StoreConfig> =
        apiClient.get("establecimiento/configuracion").mapCatching(json::parseStoreConfig).mapApiErrors()

    // El PATCH de la configuración no pide llave de idempotencia: guarda lo que se manda tal cual.
    override fun saveFlow(flow: StoreFlow): Result<StoreConfig> = patchConfig(json.encodeFlow(flow))

    override fun saveVideos(videos: List<StoreVideo>): Result<StoreConfig> = patchConfig(json.encodeVideos(videos))

    override fun setVisibleInDirectory(visible: Boolean): Result<StoreConfig> =
        patchConfig(json.encodeDirectory(visible))

    override fun setSchoolMode(isSchool: Boolean): Result<StoreConfig> = patchConfig(json.encodeSchoolMode(isSchool))

    override fun saveHomeDelivery(delivery: HomeDeliverySettings): Result<StoreConfig> =
        patchConfig(json.encodeHomeDelivery(delivery))

    override fun savePreorders(preorders: PreorderSettings): Result<StoreConfig> =
        patchConfig(json.encodePreorders(preorders))

    override fun saveStoreLocation(
        latitude: Double,
        longitude: Double,
    ): Result<StoreConfig> = patchConfig(json.encodeStoreLocation(latitude, longitude))

    override fun assistantReply(
        messages: List<AssistantMessage>,
        photoIds: List<String>,
    ): Result<AssistantReply> =
        apiClient
            .post("establecimiento/asistente", assistantJson.encodeMessages(messages, photoIds))
            .mapCatching(assistantJson::parseReply)
            .mapApiErrors()

    override fun applyAssistantStep(
        step: AssistantStep,
        photos: Map<String, AssistantPhoto>,
        idempotencyKey: String,
    ): Result<Unit> =
        when (step) {
            is AssistantStep.Settings -> applyAssistantSettings(step)
            is AssistantStep.Spaces ->
                apiClient
                    .post(
                        "espacios/lote",
                        json.encodeBatch(step.group.type, step.group.prefix, 1, step.group.count),
                        key(idempotencyKey),
                    ).mapApiErrors()
                    .mapCatching {
                        val price = step.group.hourlyPrice
                        if (price != null) setHourlyPrice(step, price, idempotencyKey)
                    }
            is AssistantStep.Menu -> applyAssistantMenu(step, photos, idempotencyKey)
            is AssistantStep.StorePhoto -> {
                val photo = photos[step.photoId]
                if (photo == null) {
                    Result.failure(IllegalStateException("La foto ya no está en el teléfono."))
                } else {
                    val upload = if (step.kind == "logo") photo.logo else photo.image
                    apiClient
                        .putMultipart(
                            "establecimiento/${step.kind}",
                            "imagen",
                            upload.filename,
                            upload.mimeType,
                            upload.bytes,
                            key(idempotencyKey),
                        ).map { }
                        .mapApiErrors()
                }
            }
            is AssistantStep.ProductPhoto -> {
                val photo = photos[step.photo.photoId]
                if (photo == null) {
                    Result.failure(IllegalStateException("La foto ya no está en el teléfono."))
                } else {
                    apiClient
                        .putMultipart(
                            "catalogo/productos/${step.photo.productId}/imagen",
                            "imagen",
                            photo.image.filename,
                            photo.image.mimeType,
                            photo.image.bytes,
                            key(idempotencyKey),
                        ).map { }
                        .mapApiErrors()
                }
            }
            is AssistantStep.Invite ->
                apiClient
                    .post(
                        "personal/invitaciones",
                        json.encodeInvitation(step.invite.email, step.invite.role),
                        key(idempotencyKey),
                    ).map { }
                    .mapApiErrors()
        }

    /** Pone el precio por hora a las canchas del grupo que aún no tienen (las nuevas y las que ya estaban). */
    private fun setHourlyPrice(
        step: AssistantStep.Spaces,
        price: String,
        idempotencyKey: String,
    ) {
        val names = (1..step.group.count).map { "${step.group.prefix} $it" }.toSet()
        val spaces =
            apiClient
                .get("espacios/administracion")
                .mapCatching(json::parseSpaces)
                .mapApiErrors()
                .getOrThrow()
        val failed = mutableListOf<String>()
        var cause: Throwable? = null
        spaces
            .filter { it.name in names && it.typeWire == step.group.type && it.active && it.hourlyPrice == null }
            .forEach { space ->
                apiClient
                    .patch(
                        "espacios/${space.id}",
                        json.encodeSpacePatch(SpacePatch(hourlyPrice = price, setsHourlyPrice = true)),
                        key(MutationIdempotency.assistant(idempotencyKey, "precio", "${space.id}")),
                    ).mapApiErrors()
                    .onFailure {
                        failed += "precio de ${space.name}"
                        cause = cause ?: it
                    }
            }
        if (failed.isNotEmpty()) throw AssistantPartialFailure(failed, cause)
    }

    /**
     * Todos los ajustes en un PATCH. Si el servidor rechaza uno (por ejemplo una regla que cruza dos
     * campos), se guardan uno por uno para no perder los demás.
     */
    private fun applyAssistantSettings(step: AssistantStep.Settings): Result<Unit> {
        val together = patchConfig(step.patchBody)
        if (together.isSuccess) return Result.success(Unit)
        val firstError = together.exceptionOrNull()!!
        // Solo si el servidor rechazó el contenido (4xx de validación); sin red o sin permiso no se insiste.
        val status = (firstError.cause as? ApiClientException)?.httpStatus
        if (status != HTTP_BAD_REQUEST && status != HTTP_CONFLICT && status != HTTP_UNPROCESSABLE) {
            return Result.failure(firstError)
        }
        val fields = Json.parseToJsonElement(step.patchBody).jsonObject
        val failed = mutableListOf<String>()
        var cause: Throwable? = null
        fields.forEach { (field, value) ->
            patchConfig(JsonObject(mapOf(field to value)).toString()).onFailure {
                failed += step.titles[field] ?: field
                cause = cause ?: it
            }
        }
        return if (failed.isEmpty()) Result.success(Unit) else Result.failure(AssistantPartialFailure(failed, cause))
    }

    /** La categoría (si no existe) y sus productos uno por uno; si alguno falla sigue con los demás. */
    private fun applyAssistantMenu(
        step: AssistantStep.Menu,
        photos: Map<String, AssistantPhoto>,
        idempotencyKey: String,
    ): Result<Unit> {
        val category = step.category
        val categoryId =
            category.existingId
                ?: apiClient
                    .post(
                        "catalogo/categorias",
                        assistantJson.encodeCategory(category.name, 0),
                        key(MutationIdempotency.assistant(idempotencyKey, "categoria")),
                    ).mapCatching(assistantJson::parseId)
                    .mapApiErrors()
                    .getOrElse { return Result.failure(it) }
        val failed = mutableListOf<String>()
        var firstError: Throwable? = null
        category.products.forEachIndexed { index, product ->
            val created =
                apiClient
                    .post(
                        "catalogo/productos",
                        assistantJson.encodeProduct(categoryId, product),
                        key(MutationIdempotency.assistant(idempotencyKey, "producto", "$index")),
                    ).mapCatching(assistantJson::parseId)
                    .mapApiErrors()
            val productId =
                created.getOrElse {
                    failed += product.name
                    firstError = firstError ?: it
                    return@forEachIndexed
                }
            val photo = product.photoId?.let(photos::get) ?: return@forEachIndexed
            apiClient
                .putMultipart(
                    "catalogo/productos/$productId/imagen",
                    "imagen",
                    photo.image.filename,
                    photo.image.mimeType,
                    photo.image.bytes,
                    key(MutationIdempotency.assistant(idempotencyKey, "foto", "$index")),
                ).mapApiErrors()
                .onFailure {
                    failed += "foto de ${product.name}"
                    firstError = firstError ?: it
                }
        }
        return if (failed.isEmpty()) {
            Result.success(Unit)
        } else {
            Result.failure(AssistantPartialFailure(failed, firstError))
        }
    }

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
        capacity: Int?,
    ): Result<ManagedSpace> =
        apiClient
            .post("espacios", json.encodeNewSpace(name, type.wire, capacity), key(idempotencyKey))
            .mapCatching(json::parseSpace)
            .mapApiErrors()

    override fun createBatch(
        type: SpaceType,
        prefix: String,
        from: Int,
        to: Int,
        idempotencyKey: String,
        capacity: Int?,
    ): Result<SpaceBatchResult> =
        apiClient
            .post("espacios/lote", json.encodeBatch(type.wire, prefix, from, to, capacity), key(idempotencyKey))
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
        reason: String,
        idempotencyKey: String,
    ): Result<ForcedCloseResult> =
        apiClient
            .post("espacios/$spaceId/sesion/cierres", json.encodeForcedClose(reason), key(idempotencyKey))
            .mapCatching(json::parseForcedClose)
            .mapApiErrors()
}

/** Parte de un paso del asistente no se pudo guardar; [items] dice qué (productos o fotos). */
private const val HTTP_BAD_REQUEST = 400
private const val HTTP_CONFLICT = 409
private const val HTTP_UNPROCESSABLE = 422

class AssistantPartialFailure(
    val items: List<String>,
    cause: Throwable?,
) : Exception("No se pudo guardar: ${items.joinToString(", ")}", cause)
