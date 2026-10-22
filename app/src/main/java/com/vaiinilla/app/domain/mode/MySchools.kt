package com.vaiinilla.app.domain.mode

import com.vaiinilla.app.domain.model.OperationalRole

/** Una escuela donde ya es cliente: desde Dónde pedir vuelve a ella de un toque, sin código. */
data class MySchool(
    val id: String,
    val name: String,
    val slug: String,
)

object MySchools {
    /** Solo escuelas (no todas las cafeterías) donde tiene membresía de cliente y que siguen abiertas. */
    fun from(modes: List<AuthorizedMode>): List<MySchool> =
        modes
            .filter {
                it.role == OperationalRole.CLIENT &&
                    it.isSchool &&
                    it.establishmentActive &&
                    it.establishmentSlug.isNotBlank()
            }.distinctBy { it.establishmentId }
            .map { MySchool(id = it.establishmentId, name = it.establishmentName, slug = it.establishmentSlug) }

    /** La escuela donde ya está no se repite. */
    fun others(
        schools: List<MySchool>,
        currentEstablishmentId: String?,
    ): List<MySchool> = schools.filter { it.id != currentEstablishmentId }
}
