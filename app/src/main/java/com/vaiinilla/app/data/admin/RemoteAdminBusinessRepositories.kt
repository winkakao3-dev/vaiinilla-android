package com.vaiinilla.app.data.admin

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.ApiJson
import com.vaiinilla.app.core.network.MutationIdempotency
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.core.network.idempotencyHeaders
import com.vaiinilla.app.core.network.mapApiFailure
import com.vaiinilla.app.core.venue.VenueContextStore
import com.vaiinilla.app.data.contract.ContractResponseParser
import com.vaiinilla.app.data.inventory.InventoryMovementType
import com.vaiinilla.app.data.inventory.InventorySupplyInput
import com.vaiinilla.app.data.inventory.InventoryUnit
import com.vaiinilla.app.data.inventory.RecipeLine
import com.vaiinilla.app.data.inventory.RemoteInventoryRepository
import com.vaiinilla.app.domain.admin.AdminLiveCounts
import com.vaiinilla.app.domain.admin.AdminOptionGroupInput
import com.vaiinilla.app.domain.admin.AdminOptionInput
import com.vaiinilla.app.domain.admin.AdminPeriod
import com.vaiinilla.app.domain.admin.AdminProductInput
import com.vaiinilla.app.domain.admin.AdminWalletSummary
import com.vaiinilla.app.domain.admin.AssistantMessage
import com.vaiinilla.app.domain.admin.AssistantPhoto
import com.vaiinilla.app.domain.admin.AssistantRecipeLine
import com.vaiinilla.app.domain.admin.AssistantJob
import com.vaiinilla.app.domain.admin.AssistantReply
import com.vaiinilla.app.domain.admin.AssistantSheet
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
import com.vaiinilla.app.domain.model.Product
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
    private val venueContext: VenueContextStore,
    private val json: AdminBusinessJson = AdminBusinessJson(),
) : AdminStoreRepository {
    private val plain = ApiJson.plain
    private val assistantJson = StoreAssistantJson()
    private val inventory = RemoteInventoryRepository(apiClient)
    private val adminJson = AdminContractJson()
    private val catalogParser = ContractResponseParser()

    // El admin puede llegar a Mesas y espacios sin estado operativo: el tipo también sale de la configuración.
    private fun syncVenueType(result: Result<StoreConfig>) {
        result.onSuccess { venueContext.update(it.typeWire) }
    }

    override fun config(): Result<StoreConfig> =
        apiClient
            .get(
                "establecimiento/configuracion",
            ).mapCatching(json::parseStoreConfig)
            .mapApiErrors()
            .also(::syncVenueType)

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
        sheets: List<AssistantSheet>,
    ): Result<AssistantReply> =
        apiClient
            .post("establecimiento/asistente", assistantJson.encodeMessages(messages, photoIds, sheets))
            .mapCatching(assistantJson::parseReply)
            .mapApiErrors()

    override fun startAssistantJob(
        messages: List<AssistantMessage>,
        photoIds: List<String>,
        sheets: List<AssistantSheet>,
    ): Result<String> =
        apiClient
            .post("establecimiento/asistente/trabajos", assistantJson.encodeMessages(messages, photoIds, sheets))
            .mapCatching(assistantJson::parseJobId)
            .mapApiErrors()

    override fun assistantJob(id: String): Result<AssistantJob> =
        apiClient
            .get("establecimiento/asistente/trabajos/$id")
            .mapCatching(assistantJson::parseJob)
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
            is AssistantStep.CourtHours -> applyCourtHours(step)
            is AssistantStep.Supply -> applyAssistantSupply(step)
            is AssistantStep.Recipe -> applyAssistantRecipe(step)
            is AssistantStep.Movement -> applyMovement(step, idempotencyKey)
            is AssistantStep.ProductEdit -> applyProductEdit(step, idempotencyKey)
            is AssistantStep.SupplyEdit -> applySupplyEdit(step)
            is AssistantStep.SpaceEdit -> applySpaceEdit(step, idempotencyKey)
            is AssistantStep.RoleChange ->
                apiClient
                    .patch(
                        "personal/invitaciones/membresias/${step.change.membershipId}",
                        json.encodeRole(step.change.role),
                        key(idempotencyKey),
                    ).map { }
                    .mapApiErrors()
            is AssistantStep.Booking -> applyBooking(step, idempotencyKey)
            is AssistantStep.OrderCancel ->
                apiClient
                    .post(
                        "pedidos/${step.cancel.orderId}/cancelaciones",
                        assistantJson.encodeCancel(step.cancel),
                        key(idempotencyKey),
                    ).map { }
                    .mapApiErrors()
        }

    /** El `id` del movimiento sale de la llave del paso: si se reintenta, el servidor no lo cuenta dos veces. */
    private fun applyMovement(
        step: AssistantStep.Movement,
        idempotencyKey: String,
    ): Result<Unit> {
        val movement = step.movement
        val type =
            when (movement.type) {
                "entrada" -> InventoryMovementType.ENTRY
                "merma" -> InventoryMovementType.WASTE
                else -> InventoryMovementType.COUNT
            }
        val id =
            java.util.UUID
                .nameUUIDFromBytes("asistente-movimiento:$idempotencyKey".toByteArray())
                .toString()
        return inventory
            .registerMovement(
                id,
                movement.supplyId,
                type,
                movement.quantity,
                movement.unitCost?.toDouble(),
                movement.note,
            ).map { }
    }

    /**
     * Archivar o agotar tienen su propia ruta. Precio y descripción van en el PUT, que pide el producto completo:
     * se lee como está y solo cambia lo que dice el asistente.
     */
    private fun applyProductEdit(
        step: AssistantStep.ProductEdit,
        idempotencyKey: String,
    ): Result<Unit> =
        runCatching {
            val edit = step.edit
            if (edit.archive) {
                apiClient
                    .post(
                        "catalogo/productos/${edit.productId}/archivar",
                        EMPTY_BODY,
                        key(MutationIdempotency.assistant(idempotencyKey, "archivar")),
                    ).mapApiErrors()
                    .getOrThrow()
                return@runCatching
            }
            if (edit.price != null || edit.setsDescription) {
                val product = findProduct(edit.productId) ?: throw IllegalStateException("${edit.name} ya no está en tu menú.")
                val input =
                    AdminProductInput(
                        categoryId = product.categoryId,
                        preparationStation = product.preparationStation,
                        name = product.name,
                        description = if (edit.setsDescription) edit.description.orEmpty() else product.description,
                        ingredients = product.ingredients,
                        allergens = product.allergens.takeUnless { it.trim() in setOf("{}", "[]") }.orEmpty(),
                        estimatedTimeMinutes = product.estimatedTimeMinutes,
                        counterPrice = edit.price ?: product.counterPrice,
                        available = edit.available ?: product.available,
                        optionGroups =
                            product.optionGroups.map { group ->
                                AdminOptionGroupInput(
                                    id = group.id,
                                    name = group.name,
                                    minimumSelections = group.minimumSelections,
                                    maximumSelections = group.maximumSelections,
                                    options = group.options.map { AdminOptionInput(it.id, it.name, it.extraPrice) },
                                )
                            },
                    )
                apiClient
                    .put(
                        "catalogo/productos/${edit.productId}",
                        adminJson.encodeProduct(input),
                        key(MutationIdempotency.assistant(idempotencyKey, "producto")),
                    ).mapApiErrors()
                    .getOrThrow()
                return@runCatching
            }
            edit.available?.let { available ->
                apiClient
                    .post(
                        "catalogo/productos/${edit.productId}/disponibilidad",
                        catalogParser.encodeAvailability(available),
                        key(MutationIdempotency.assistant(idempotencyKey, "disponible")),
                    ).mapApiErrors()
                    .getOrThrow()
            }
            Unit
        }

    /** El producto como lo tiene el menú, página por página. */
    private fun findProduct(id: Int): Product? {
        var cursor: String? = null
        repeat(MAX_CATALOG_PAGES) {
            val raw =
                apiClient
                    .get("catalogo", cursor?.let { mapOf("cursor" to it) } ?: emptyMap())
                    .mapApiErrors()
                    .getOrThrow()
            val catalog = catalogParser.parseCatalog(raw)
            catalog.products.firstOrNull { it.id == id }?.let { return it }
            cursor = catalog.cursor ?: return null
        }
        return null
    }

    /** El PATCH del insumo pide nombre y mínimo: se leen los de hoy y solo cambia lo que dice el asistente. */
    private fun applySupplyEdit(step: AssistantStep.SupplyEdit): Result<Unit> =
        runCatching {
            val edit = step.edit
            val current =
                inventory
                    .supplies()
                    .getOrThrow()
                    .supplies
                    .firstOrNull { it.id == edit.supplyId }
                    ?: throw IllegalStateException("${edit.name} ya no está en tu inventario.")
            inventory
                .updateSupply(
                    current.id,
                    InventorySupplyInput(
                        name = current.name,
                        category = null,
                        unit = null,
                        minimum = edit.minimum ?: current.minimum,
                        unitCost = edit.unitCost?.toDouble(),
                        supplierName = edit.supplierName,
                        supplierPhone = edit.supplierPhone,
                    ),
                ).getOrThrow()
            Unit
        }

    private fun applySpaceEdit(
        step: AssistantStep.SpaceEdit,
        idempotencyKey: String,
    ): Result<Unit> =
        apiClient
            .patch(
                "espacios/${step.edit.spaceId}",
                json.encodeSpacePatch(
                    SpacePatch(
                        active = step.edit.active,
                        hourlyPrice = step.edit.hourlyPrice,
                        setsHourlyPrice = step.edit.setsHourlyPrice,
                    ),
                ),
                key(idempotencyKey),
            ).map { }
            .mapApiErrors()

    /** Aparta la pista y la deja por cobrar en efectivo en Caja, como una renta de mostrador. */
    private fun applyBooking(
        step: AssistantStep.Booking,
        idempotencyKey: String,
    ): Result<Unit> =
        apiClient
            .post(
                "reservas",
                assistantJson.encodeBooking(step.booking),
                key(MutationIdempotency.assistant(idempotencyKey, "reserva")),
            ).mapCatching(assistantJson::parseTextId)
            .mapApiErrors()
            .mapCatching { reservationId ->
                apiClient
                    .post(
                        "reservas/$reservationId/pago",
                        assistantJson.encodeCashLater(),
                        key(MutationIdempotency.assistant(idempotencyKey, "por-cobrar")),
                    ).mapApiErrors()
                    .getOrThrow()
                Unit
            }

    /** El horario de las pistas: se lee el de hoy y solo cambia lo que dice el asistente (el PUT lo pide completo). */    private fun applyCourtHours(step: AssistantStep.CourtHours): Result<Unit> =
        apiClient
            .get("reservas/configuracion")
            .mapCatching(json::parseSchedule)
            .mapApiErrors()
            .mapCatching { current ->
                val hours = step.hours
                val next =
                    current.copy(
                        opening = hours.opening,
                        closing = hours.closing,
                        weekDays = hours.weekDays ?: current.weekDays,
                        durations = hours.durations ?: current.durations,
                    )
                apiClient
                    .put("reservas/configuracion", json.encodeSchedule(next))
                    .mapApiErrors()
                    .getOrThrow()
                Unit
            }

    /** Da de alta el insumo. Si ya hay uno con ese nombre (por ejemplo al reintentar), ya quedó. */
    private fun applyAssistantSupply(step: AssistantStep.Supply): Result<Unit> {
        val supply = step.supply
        return inventory
            .createSupply(
                InventorySupplyInput(
                    name = supply.name,
                    category = null,
                    unit = InventoryUnit.of(supply.unit),
                    minimum = supply.minimum ?: 0.0,
                    unitCost = supply.unitCost?.toDouble(),
                    initialStock = supply.initialStock,
                ),
            ).map { }
            .recoverCatching { if ((it as? CatalogRepositoryException)?.code == SUPPLY_NAME_TAKEN) Unit else throw it }
    }

    /**
     * Lo nuevo de la misma propuesta (producto o insumo sin id) se busca por nombre: se creó en un paso anterior.
     * La receta se lee, se le agregan o cambian estas líneas y se guarda completa, porque el PUT la reemplaza.
     */
    private fun applyAssistantRecipe(step: AssistantStep.Recipe): Result<Unit> =
        runCatching {
            val recipe = step.recipe
            val supplies by lazy {
                inventory
                    .supplies()
                    .getOrThrow()
                    .supplies
                    .associateBy { assistantNameKey(it.name) }
            }
            val productId =
                recipe.productId
                    ?: inventory
                        .products()
                        .getOrThrow()
                        .firstOrNull { assistantNameKey(it.name) == assistantNameKey(recipe.productName) }
                        ?.id
                    ?: throw IllegalStateException("${recipe.productName} todavía no está en tu menú.")
            val lines =
                recipe.lines.map { line ->
                    val supplyId =
                        line.supplyId
                            ?: supplies[assistantNameKey(line.supplyName)]?.id
                            ?: throw IllegalStateException("Falta dar de alta ${line.supplyName}.")
                    line.copy(supplyId = supplyId)
                }
            val current = inventory.recipe(productId).getOrThrow()
            inventory.saveRecipe(productId, mergeAssistantRecipe(current, lines)).getOrThrow()
            Unit
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
        apiClient
            .patch(
                "establecimiento/configuracion",
                body,
            ).mapCatching(json::parseStoreConfig)
            .mapApiErrors()
            .also(::syncVenueType)

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
    private val venueContext: VenueContextStore,
    private val json: AdminBusinessJson = AdminBusinessJson(),
) : AdminSpacesRepository {
    override fun spaces(): Result<List<ManagedSpace>> =
        apiClient.get("espacios/administracion").mapCatching(json::parseSpaces).mapApiErrors()

    override fun create(
        name: String,
        type: SpaceType,
        idempotencyKey: String,
        capacity: Int?,
        floor: Int?,
        category: String?,
    ): Result<ManagedSpace> =
        apiClient
            .post("espacios", json.encodeNewSpace(name, type.wire, capacity, floor, category), key(idempotencyKey))
            .mapCatching(json::parseSpace)
            .mapApiErrors()

    override fun createBatch(
        type: SpaceType,
        prefix: String,
        from: Int,
        to: Int,
        idempotencyKey: String,
        capacity: Int?,
        floor: Int?,
        category: String?,
    ): Result<SpaceBatchResult> =
        apiClient
            .post(
                "espacios/lote",
                json.encodeBatch(type.wire, prefix, from, to, capacity, floor, category),
                key(idempotencyKey),
            ).mapCatching(json::parseBatch)
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

    private fun syncVenueType(raw: Result<String>) {
        raw
            .getOrNull()
            ?.let { body ->
                runCatching { json.scheduleVenueType(body) }.getOrNull()
            }?.let(venueContext::update)
    }

    override fun reservationSchedule(): Result<ReservationSchedule> =
        apiClient
            .get("reservas/configuracion")
            .also(::syncVenueType)
            .mapCatching(json::parseSchedule)
            .mapApiErrors()

    override fun saveReservationSchedule(
        schedule: ReservationSchedule,
        idempotencyKey: String,
    ): Result<ReservationSchedule> =
        apiClient
            .put("reservas/configuracion", json.encodeSchedule(schedule), key(idempotencyKey))
            .also(::syncVenueType)
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

private const val SUPPLY_NAME_TAKEN = "INSUMO_NAME_CONFLICT"

// El menú llega por páginas; con esto alcanza para cualquier tienda real.
private const val MAX_CATALOG_PAGES = 20

/** Igual que el servidor compara nombres: sin acentos, sin mayúsculas y sin espacios en las orillas. */
internal fun assistantNameKey(name: String): String =
    java.text.Normalizer
        .normalize(name, java.text.Normalizer.Form.NFKD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase()
        .trim()

/**
 * La receta que se guarda: la de hoy con las líneas base de [lines] agregadas o cambiadas. Las líneas por opción y
 * los insumos que el asistente no menciona se quedan como estaban. Cada línea de [lines] ya trae su id.
 */
internal fun mergeAssistantRecipe(
    current: List<RecipeLine>,
    lines: List<AssistantRecipeLine>,
): List<RecipeLine> {
    val incoming = lines.mapNotNull { it.supplyId }.toSet()
    val kept = current.filterNot { it.optionId == null && it.supplyId in incoming }
    return kept +
        lines.map {
            RecipeLine(
                supplyId = requireNotNull(it.supplyId),
                supplyName = it.supplyName,
                unit = InventoryUnit.of(it.unit),
                quantity = it.quantity,
                optionId = null,
                optionName = null,
                cost = 0.0,
            )
        }
}
