package com.vaiinilla.app.data.fixture

import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.Category
import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.domain.model.OptionGroup
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.model.ProductOption
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContractFixtureParser
    @Inject
    constructor() {
        private val json =
            Json {
                ignoreUnknownKeys = false
                explicitNulls = true
                isLenient = false
            }

        fun parseCatalog(raw: String): Catalog {
            val envelope = json.decodeFromString<CatalogEnvelopeDto>(raw)
            require(envelope.error == null) { "El fixture de catálogo contiene error." }

            return Catalog(
                categories =
                    envelope.data.categories.map { dto ->
                        Category(id = dto.id, name = dto.name, order = dto.order)
                    },
                products =
                    envelope.data.products.map { dto ->
                        Product(
                            id = dto.id,
                            categoryId = dto.categoryId,
                            preparationStation = PreparationStation.fromWireValue(dto.preparationStation),
                            name = dto.name,
                            description = dto.description,
                            ingredients = dto.ingredients,
                            allergens = dto.allergens,
                            estimatedTimeMinutes = dto.estimatedTimeMinutes,
                            counterPrice = dto.counterPrice,
                            digitalPrice = dto.digitalPrice,
                            available = dto.available,
                            imageUrl = dto.imageUrl,
                            optionGroups =
                                dto.optionGroups.map { group ->
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
                    },
                cursor = envelope.meta.cursor,
            )
        }

        fun parseOperationalStatus(raw: String): OperationalStatus {
            val envelope = json.decodeFromString<OperationalStatusEnvelopeDto>(raw)
            require(envelope.error == null) { "El fixture de estado operativo contiene error." }
            return OperationalStatus(
                acceptingOrders = envelope.data.acceptingOrders,
                cashSessionOpen = envelope.data.cashSessionOpen,
                cashierOnline = envelope.data.cashierOnline,
                kitchenOnline = envelope.data.kitchenOnline,
                estimatedTimeMinutes = envelope.data.estimatedTimeMinutes,
                consultedAt = envelope.data.consultedAt,
            )
        }
    }
