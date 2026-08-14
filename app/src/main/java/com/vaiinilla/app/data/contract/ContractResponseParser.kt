package com.vaiinilla.app.data.contract

import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.CatalogProductDraft
import com.vaiinilla.app.domain.model.Category
import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.domain.model.OptionGroup
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.model.ProductOption
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

        fun encodeAvailability(available: Boolean): String =
            json.encodeToString(ProductAvailabilityWriteDto(available))
    }

private fun ProductDto.toDomain(): Product =
    Product(
        id = id,
        categoryId = categoryId,
        preparationStation = PreparationStation.fromWireValue(preparationStation),
        name = name,
        description = description.orEmpty(),
        ingredients = ingredients.orEmpty(),
        allergens = allergens.orEmpty(),
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
    )
