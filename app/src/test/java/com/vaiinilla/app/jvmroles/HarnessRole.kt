package com.vaiinilla.app.jvmroles

import com.vaiinilla.app.domain.model.OperationalRole

/** Un rol del arnés: su nombre en la CLI, su persona en el sandbox de dev y su modo dentro de la app. */
enum class HarnessRole(
    val cliName: String,
    private val aliases: Set<String>,
    val sandboxKey: String,
    val operationalRole: OperationalRole,
) {
    ALUMNO("alumno", setOf("cliente"), "cliente", OperationalRole.CLIENT),
    // El papá del sandbox: un cliente con hijos (solo existe en un backend con Padres e hijos).
    TUTOR("tutor", setOf("papa", "papá"), "tutor", OperationalRole.CLIENT),
    CAJA("caja", setOf("cajero"), "cajero", OperationalRole.CASHIER),
    COCINA("cocina", emptySet(), "cocina", OperationalRole.KITCHEN),
    MESERO("mesero", emptySet(), "mesero", OperationalRole.WAITER),
    ADMIN("admin", setOf("administracion", "administración"), "admin", OperationalRole.ADMIN),
    ;

    companion object {
        fun parse(name: String): HarnessRole? {
            val wanted = name.trim().lowercase()
            return entries.firstOrNull { wanted == it.cliName || wanted in it.aliases }
        }
    }
}

/** El rol al frente. Los stores del arnés guardan una sesión por rol y leen de aquí cuál toca. */
object ActiveRole {
    @Volatile
    var current: HarnessRole? = null
}
