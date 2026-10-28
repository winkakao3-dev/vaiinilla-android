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
    /** Códigos del catálogo de alérgenos (`gluten`, `lacteos`…); valen solo si [allergensReviewed]. */
    val allergenCodes: List<String> = emptyList(),
    /** En una escuela, sin revisar no se le vende a un alumno con alergias. */
    val allergensReviewed: Boolean = true,
    /**
     * Catálogo visto por un alumno: por qué no lo puede pedir (`alergeno`, `producto`, `categoria` o
     * `sin_revisar`), o null si puede. Se muestra bloqueado, no se oculta (Kao, 27 oct).
     */
    val blockReason: String? = null,
    /** Archivado por el admin: fuera del menú, pero se puede desarchivar (`archivado`). */
    val archived: Boolean = false,
)

data class CatalogProductDraft(
    val categoryId: Int,
    val preparationStation: PreparationStation,
    val name: String,
    val description: String = "",
    val ingredients: String = "",
    val allergens: String = "",
    val estimatedTimeMinutes: Int,
    val counterPrice: String,
    val available: Boolean = true,
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

enum class PreparationStation(
    val wireValue: String,
) {
    KITCHEN("cocina"),
    CASHIER("caja"),
    ;

    companion object {
        /** Una estación nueva no rompe el menú entero: el producto se trata como de cocina. */
        fun fromWireValue(value: String): PreparationStation = entries.firstOrNull { it.wireValue == value } ?: KITCHEN
    }
}
