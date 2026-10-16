package com.vaiinilla.app.domain.model

enum class OperationalRole(
    val label: String,
    val wireValue: String,
) {
    CLIENT("Alumno", "cliente"),
    CASHIER("Caja", "cajero"),
    KITCHEN("Cocina", "cocina"),
    WAITER("Mesero", "mesero"),

    /** El dueño o encargado: su propio modo con todo lo que hace en la web, separado de Caja. */
    ADMIN("Administración", "admin"),
}
