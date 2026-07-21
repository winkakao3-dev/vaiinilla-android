package com.vaiinilla.app.domain.model

object ContractRules {
    private val moneyPattern = Regex("^(0|[1-9]\\d*)\\.\\d{2}$")
    private val utcTimestampPattern = Regex(
        "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{3})?Z$",
    )

    fun isValidMoney(value: String): Boolean = moneyPattern.matches(value)

    fun validateCatalog(catalog: Catalog) {
        require(catalog.categories.isNotEmpty()) { "El catálogo debe incluir categorías." }
        require(catalog.categories.map { it.id }.distinct().size == catalog.categories.size) {
            "Los IDs de categoría deben ser únicos."
        }
        require(catalog.products.map { it.id }.distinct().size == catalog.products.size) {
            "Los IDs de producto deben ser únicos."
        }

        val categoryIds = catalog.categories.mapTo(mutableSetOf(), Category::id)
        catalog.products.forEach { product ->
            require(product.categoryId in categoryIds) {
                "El producto ${product.id} referencia una categoría inexistente."
            }
            require(product.estimatedTimeMinutes >= 0) {
                "tiempo_estimado_min inválido para ${product.id}"
            }
            require(isValidMoney(product.counterPrice)) { "precio_mostrador inválido para ${product.id}" }
            require(isValidMoney(product.digitalPrice)) { "precio_digital inválido para ${product.id}" }
            require(product.imageUrl.isNotBlank()) { "imagen_url vacía para ${product.id}" }
            require(product.optionGroups.map { it.id }.distinct().size == product.optionGroups.size) {
                "Los IDs de grupos de opción deben ser únicos dentro del producto ${product.id}."
            }
            product.optionGroups.forEach { group ->
                require(group.minimumSelections >= 0)
                require(group.maximumSelections >= group.minimumSelections)
                require(group.maximumSelections <= group.options.size)
                require(group.options.map { it.id }.distinct().size == group.options.size) {
                    "Los IDs de opción deben ser únicos dentro del grupo ${group.id}."
                }
                group.options.forEach { option ->
                    require(isValidMoney(option.extraPrice)) { "precio_extra inválido para ${option.id}" }
                }
            }
        }
    }

    fun validateOperationalStatus(status: OperationalStatus) {
        require(status.estimatedTimeMinutes >= 0) { "tiempo_estimado_min no puede ser negativo." }
        require(utcTimestampPattern.matches(status.consultedAt)) {
            "consultado_en debe usar ISO 8601 UTC."
        }
    }
}
