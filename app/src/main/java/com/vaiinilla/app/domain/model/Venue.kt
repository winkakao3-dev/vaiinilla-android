package com.vaiinilla.app.domain.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Cómo se llama lo que se renta en el negocio abierto. En un boliche (`tipo: boliche`) las
 * canchas del servidor son pistas: mismo contrato (`tipo: cancha`), otro nombre y otro dibujo.
 * En un hotel (`tipo: hotel`) las habitaciones son espacios `mesa` que la app llama «Habitación».
 * Se actualiza con cada estado operativo; es estado de Compose para que las pantallas cambien solas.
 */
object Venue {
    const val BOWLING = "boliche"
    const val HOTEL = "hotel"

    var businessType: String? by mutableStateOf(null)
        private set

    /** Una escuela es una cafetería con `modo_escuela`: no tiene un `tipo` propio. */
    var isSchool: Boolean by mutableStateOf(false)
        private set

    val isBowling: Boolean get() = businessType == BOWLING

    val isHotel: Boolean get() = businessType == HOTEL

    /**
     * Lo llama quien guarda el último negocio visto, para que un aviso que llega con la app cerrada hable como el
     * negocio y no como «cliente» a secas.
     */
    var onChange: ((type: String?, school: Boolean) -> Unit)? = null

    /** `school` en null deja el valor que había: no todos los estados lo traen. */
    fun update(
        type: String?,
        school: Boolean? = null,
    ) {
        val newSchool = school ?: isSchool
        if (type == businessType && newSchool == isSchool) return
        businessType = type
        isSchool = newSchool
        onChange?.invoke(type, newSchool)
    }

    /** Vuelve a poner el último negocio guardado sin avisar a [onChange]. */
    fun restore(
        type: String?,
        school: Boolean,
    ) {
        businessType = type
        isSchool = school
    }

    /** Cómo se llama aquí a quien pide: «alumno», «huésped» o «cliente». */
    val customer: String get() = Customer.of(businessType, isSchool).lower

    val customerTitle: String get() = Customer.of(businessType, isSchool).title

    /**
     * La etiqueta de un modo para un aviso: el modo cliente se llama Alumno en una escuela y Huésped en un hotel. Un
     * aviso puede ser de otro negocio que el abierto, por eso la escuela se puede pasar aparte.
     */
    fun roleLabel(
        role: OperationalRole,
        school: Boolean = isSchool,
    ): String = if (role == OperationalRole.CLIENT) Customer.of(businessType, school).title else role.label

    /** «cancha» o «pista». */
    val court: String get() = if (isBowling) "pista" else "cancha"

    /** «Cancha» o «Pista». */
    val courtTitle: String get() = if (isBowling) "Pista" else "Cancha"

    val courts: String get() = if (isBowling) "pistas" else "canchas"

    val courtsTitle: String get() = if (isBowling) "Pistas" else "Canchas"

    /** «habitación»: lo que se renta en un hotel. */
    val room: String get() = "habitación"

    val roomTitle: String get() = "Habitación"

    val rooms: String get() = "habitaciones"

    val roomsTitle: String get() = "Habitaciones"
}

/** Quien pide en el negocio, con el nombre que le toca según el tipo. */
enum class Customer(
    val lower: String,
    val title: String,
) {
    STUDENT("alumno", "Alumno"),
    GUEST("huésped", "Huésped"),
    CUSTOMER("cliente", "Cliente"),
    ;

    companion object {
        fun of(
            type: String?,
            school: Boolean,
        ): Customer =
            when {
                school -> STUDENT
                type == Venue.HOTEL -> GUEST
                else -> CUSTOMER
            }
    }
}
