package com.vaiinilla.app.ui.navigation

import com.vaiinilla.app.domain.auth.student.StudentAuthSession
import com.vaiinilla.app.domain.mode.AuthorizedModeContext
import com.vaiinilla.app.domain.model.OperationalRole

/** Hasta dónde llegó la cuenta de quien usa la app. */
internal enum class AccountStage {
    /** Sin sesión: invitado o recién instalada. */
    ANONYMOUS,

    /** Con sesión pero sin verificar el correo. */
    UNVERIFIED,

    /** Verificada, pero todavía sin el contexto de cliente de este negocio. */
    VERIFIED,

    /** Lista para pedir y pagar como cliente. */
    READY,
}

/**
 * Quién usa la app ahora, calculado en un solo lugar. Antes cada efecto de [AppNavHost] repetía su propia mezcla
 * de sesión, correo verificado, contexto de cliente y rol de personal, con pequeñas diferencias entre ellas.
 */
internal data class AppIdentity(
    val stage: AccountStage,
    /** Rol de personal activo (Caja, Cocina, Mesero, Administración); `null` si entra como cliente. */
    val staffRole: OperationalRole?,
) {
    /** Puede usar su cuenta de cliente (cartera, pedidos), aunque además tenga un modo de personal. */
    val canUseClientAccount: Boolean get() = stage == AccountStage.READY

    /** Está en la app como cliente: cuenta lista y sin un modo de personal activo. */
    val isClient: Boolean get() = canUseClientAccount && staffRole == null
}

/** [readyForCheckout] se consulta solo con el correo ya verificado, como antes. */
internal fun appIdentity(
    session: StudentAuthSession?,
    readyForCheckout: () -> Boolean,
    activeContext: AuthorizedModeContext?,
): AppIdentity {
    val stage =
        when {
            session == null -> AccountStage.ANONYMOUS
            !session.emailVerified -> AccountStage.UNVERIFIED
            readyForCheckout() -> AccountStage.READY
            else -> AccountStage.VERIFIED
        }
    return AppIdentity(stage = stage, staffRole = activeContext?.role)
}
