package com.vaiinilla.app.domain.model

enum class OperationalRole(
    val label: String,
    val wireValue: String,
) {
    CLIENT("Alumno", "cliente"),
    CASHIER("Caja", "cajero"),
    KITCHEN("Cocina", "cocina"),
    WAITER("Mesero", "mesero"),
}
