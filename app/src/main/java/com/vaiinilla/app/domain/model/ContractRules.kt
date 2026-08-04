package com.vaiinilla.app.domain.model

object ContractRules {
    private val moneyPattern = Regex("^(0|[1-9]\\d*)\\.\\d{2}$")
    private val utcTimestampPattern =
        Regex(
            "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{3})?Z$",
        )

    fun isValidMoney(value: String): Boolean = moneyPattern.matches(value)

    fun validateCatalog(catalog: Catalog) {
        require(catalog.categories.isNotEmpty()) { "El catálogo debe incluir categorías." }
        require(
            catalog.categories
                .map { it.id }
                .distinct()
                .size == catalog.categories.size,
        ) {
            "Los IDs de categoría deben ser únicos."
        }
        require(
            catalog.products
                .map { it.id }
                .distinct()
                .size == catalog.products.size,
        ) {
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
            require(
                product.optionGroups
                    .map { it.id }
                    .distinct()
                    .size == product.optionGroups.size,
            ) {
                "Los IDs de grupos de opción deben ser únicos dentro del producto ${product.id}."
            }
            product.optionGroups.forEach { group ->
                require(group.minimumSelections >= 0)
                require(group.maximumSelections >= group.minimumSelections)
                require(group.maximumSelections <= group.options.size)
                require(
                    group.options
                        .map { it.id }
                        .distinct()
                        .size == group.options.size,
                ) {
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

    fun validateSelections(
        product: Product,
        selectedOptionIds: Set<Int>,
    ) {
        val allOptionIds = product.optionGroups.flatMap(OptionGroup::options).mapTo(mutableSetOf(), ProductOption::id)
        require(selectedOptionIds.all { it in allOptionIds }) {
            "Una opción seleccionada no pertenece al producto ${product.id}."
        }

        product.optionGroups.forEach { group ->
            val count = group.options.count { it.id in selectedOptionIds }
            require(count in group.minimumSelections..group.maximumSelections) {
                "El grupo ${group.id} requiere entre ${group.minimumSelections} y ${group.maximumSelections} selecciones."
            }
        }
    }

    fun validateCreateOrderRequest(request: CreateOrderRequest) {
        require(request.paymentMethod == PaymentMethod.CASH) { "VAI-10 solo acepta efectivo." }
        require(request.destination == OrderDestination.TAKE_AWAY) {
            "VAI-10 solo acepta destino para_llevar."
        }
        require(request.spaceId == null) { "para_llevar exige espacio_id null." }
        validateOrderItems(request)
    }

    /**
     * Entrega 01 REMOTE: el backend acepta efectivo para recoger o para un espacio
     * resuelto por QR. El tenant valida que el espacio exista y esté activo; el
     * cliente no puede limitar ese identificador a las mesas de una fixture local.
     */
    fun validateRemoteOrderRequest(request: CreateOrderRequest) {
        require(request.paymentMethod == PaymentMethod.CASH) {
            "La Entrega 01 REMOTE solo acepta efectivo."
        }
        when (request.destination) {
            OrderDestination.TAKE_AWAY ->
                require(request.spaceId == null) {
                    "para_llevar exige espacio_id null."
                }
            OrderDestination.IN_SPACE ->
                require(request.spaceId != null) {
                    "en_espacio requiere un espacio resuelto por QR."
                }
        }
        validateOrderItems(request)
    }

    fun validateStudentCheckoutRequest(request: CreateOrderRequest) {
        require(request.paymentMethod in PaymentMethod.entries) {
            "metodo_pago no soportado en checkout alumno."
        }
        when (request.destination) {
            OrderDestination.TAKE_AWAY ->
                require(request.spaceId == null) {
                    "para_llevar exige espacio_id null."
                }
            OrderDestination.IN_SPACE ->
                require(request.spaceId in DemoCheckoutFixtures.DEMO_SPACE_IDS) {
                    "en_espacio requiere un espacio demo válido."
                }
        }
        validateOrderItems(request)
    }

    private fun validateOrderItems(request: CreateOrderRequest) {
        require(request.items.size in 1..50) { "El pedido debe contener entre 1 y 50 líneas." }
        request.items.forEach { item ->
            require(item.quantity in 1..20) { "cantidad debe estar entre 1 y 20." }
            require(item.optionIds.distinct().size == item.optionIds.size) {
                "opcion_ids no puede contener duplicados."
            }
        }
    }
}
