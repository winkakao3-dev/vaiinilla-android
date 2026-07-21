package com.vaiinilla.app.domain.model

data class Catalog(
    val categories: List<Category>,
    val products: List<Product>,
    val cursor: String?,
)

data class Category(
    val id: Int,
    val name: String,
    val order: Int,
)

data class Product(
    val id: Int,
    val categoryId: Int,
    val preparationStation: PreparationStation,
    val name: String,
    val description: String,
    val ingredients: String,
    val allergens: String,
    val estimatedTimeMinutes: Int,
    val counterPrice: String,
    val digitalPrice: String,
    val available: Boolean,
    val imageUrl: String,
    val optionGroups: List<OptionGroup>,
)

data class OptionGroup(
    val id: Int,
    val name: String,
    val minimumSelections: Int,
    val maximumSelections: Int,
    val options: List<ProductOption>,
)

data class ProductOption(
    val id: Int,
    val name: String,
    val extraPrice: String,
)

enum class PreparationStation(val wireValue: String) {
    KITCHEN("cocina"),
    CASHIER("caja");

    companion object {
        fun fromWireValue(value: String): PreparationStation = entries.firstOrNull {
            it.wireValue == value
        } ?: throw IllegalArgumentException("estacion_preparacion no soportada: $value")
    }
}
