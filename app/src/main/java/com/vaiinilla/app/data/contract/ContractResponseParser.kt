package com.vaiinilla.app.data.contract

import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.CatalogProductDraft
import com.vaiinilla.app.domain.model.Category
import com.vaiinilla.app.domain.model.HomeDeliveryOffer
import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.domain.model.OptionGroup
import com.vaiinilla.app.domain.model.OrderWindow
import com.vaiinilla.app.domain.model.PreorderOffer
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.model.ProductOption
import com.vaiinilla.app.domain.model.SpaceDelivery
import com.vaiinilla.app.domain.repository.StoreImages
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContractResponseParser
    @Inject
    constructor() {
        private val json =
            Json {
                ignoreUnknownKeys = true
                explicitNulls = true
                isLenient = false
            }

        fun parseCatalog(raw: String): Catalog {
            val envelope = json.decodeFromString<CatalogEnvelopeDto>(raw)
            require(envelope.error == null) { "La respuesta de catálogo contiene error." }

            return Catalog(
                categories =
                    envelope.data.categories.map { dto ->
                        Category(id = dto.id, name = dto.name, order = dto.order)
                    },
                products =
                    envelope.data.products.map { dto -> dto.toDomain() },
                cursor = envelope.meta.cursor,
            )
        }

        fun parseOperationalStatus(raw: String): OperationalStatus {
            val envelope = json.decodeFromString<OperationalStatusEnvelopeDto>(raw)
            require(envelope.error == null) { "La respuesta de estado operativo contiene error." }
            return OperationalStatus(
                acceptingOrders = envelope.data.acceptingOrders,
                cashSessionOpen = envelope.data.cashSessionOpen,
                cashierOnline = envelope.data.cashierOnline,
                kitchenOnline = envelope.data.kitchenOnline,
                estimatedTimeMinutes = envelope.data.estimatedTimeMinutes,
                consultedAt = envelope.data.consultedAt,
                deliveryRequiresQr = envelope.data.deliveryRequiresQr,
                allowsPayAtEnd = envelope.data.allowsPayAtEnd,
                spaceDelivery = SpaceDelivery.fromWire(envelope.data.spaceDelivery),
                acceptsCard = envelope.data.acceptsCard,
                allowsBalance = envelope.data.allowsBalance,
                businessType = envelope.data.businessType,
                orderWindows = envelope.data.orderWindows.map { OrderWindow(it.from, it.to) },
                withinOrderWindow = envelope.data.withinOrderWindow,
                isSchool = envelope.data.isSchool,
                isParentUnlocked = envelope.data.isParentUnlocked,
                cashierConnected = envelope.data.cashierConnected,
                kitchenConnected = envelope.data.kitchenConnected,
                homeDelivery =
                    envelope.data.delivery
                        ?.takeIf { it.activo }
                        ?.let { delivery ->
                            val fee = delivery.costo?.textOrNull() ?: return@let null
                            HomeDeliveryOffer(
                                fee = fee,
                                minimum = delivery.minimo?.textOrNull(),
                                radiusKm = delivery.radioKm?.textOrNull().orEmpty(),
                                payOnArrival = envelope.data.payOnArrival ?: false,
                                transferDetails =
                                    envelope.data.transferDetails
                                        ?.trim()
                                        ?.ifEmpty { null },
                            )
                        },
                preorders =
                    envelope.data.requiresAcceptance?.let { requires ->
                        PreorderOffer(
                            daysAhead = envelope.data.daysAhead ?: 0,
                            cutoff = envelope.data.cutoff?.take(5),
                            requiresAcceptance = requires,
                            depositPercent = envelope.data.depositPercent,
                            answerMinutes = envelope.data.answerMinutes,
                        )
                    },
            )
        }

        private fun kotlinx.serialization.json.JsonPrimitive.textOrNull(): String? =
            if (this is kotlinx.serialization.json.JsonNull) null else content.trim().ifEmpty { null }

        /** Logo y foto del negocio: a Caja le llegan solas y a la administración dentro de su configuración. */
        fun parseStoreImages(raw: String): StoreImages {
            val envelope = json.decodeFromString<StoreImagesEnvelopeDto>(raw)
            require(envelope.error == null) { "La respuesta de imágenes del negocio contiene error." }
            return StoreImages(
                photoUrl = envelope.data.photoUrl?.takeIf { it.isNotBlank() },
                logoUrl = envelope.data.logoUrl?.takeIf { it.isNotBlank() },
            )
        }

        fun parseProduct(raw: String): Product {
            val envelope = json.decodeFromString<ProductEnvelopeDto>(raw)
            require(envelope.error == null) { "La respuesta de producto contiene error." }
            return envelope.data.toDomain()
        }

        fun encodeProductDraft(draft: CatalogProductDraft): String =
            json.encodeToString(
                CatalogProductWriteDto(
                    categoryId = draft.categoryId,
                    preparationStation = draft.preparationStation.wireValue,
                    name = draft.name,
                    description = draft.description.ifBlank { null },
                    ingredients = draft.ingredients.ifBlank { null },
                    allergens = draft.allergens.ifBlank { null },
                    estimatedTimeMinutes = draft.estimatedTimeMinutes,
                    counterPrice = draft.counterPrice,
                    available = draft.available,
                    optionGroups = emptyList(),
                ),
            )

        fun encodeAvailability(available: Boolean): String = json.encodeToString(ProductAvailabilityWriteDto(available))
    }

/** El servidor a veces manda el arreglo de Postgres como texto: "{}" o "{huevo,lactosa}". */
internal fun cleanAllergenText(raw: String?): String {
    val text = raw.orEmpty().trim()
    if (!(text.startsWith("{") && text.endsWith("}"))) return text
    return text
        .removeSurrounding("{", "}")
        .split(',')
        .map {
            it.trim().trim('"')
        }.filter { it.isNotEmpty() }
        .joinToString(", ")
}

private fun ProductDto.toDomain(): Product =
    Product(
        id = id,
        categoryId = categoryId,
        preparationStation = PreparationStation.fromWireValue(preparationStation),
        name = name,
        description = description.orEmpty(),
        ingredients = ingredients.orEmpty(),
        allergens = cleanAllergenText(allergens),
        estimatedTimeMinutes = estimatedTimeMinutes,
        counterPrice = counterPrice,
        digitalPrice = digitalPrice,
        available = available,
        imageUrl = imageUrl.orEmpty(),
        optionGroups =
            optionGroups.map { group ->
                OptionGroup(
                    id = group.id,
                    name = group.name,
                    minimumSelections = group.minimumSelections,
                    maximumSelections = group.maximumSelections,
                    options =
                        group.options.map { option ->
                            ProductOption(
                                id = option.id,
                                name = option.name,
                                extraPrice = option.extraPrice,
                            )
                        },
                )
            },
        allergenCodes = allergenCodes,
        allergensReviewed = allergensReviewed,
        archived = archived,
        preparationDays = preparationDays,
        maximumPerDay = maximumPerDay,
        blockReason = if (blocked) blockReason?.trim()?.ifEmpty { null } ?: "producto" else null,
    )
