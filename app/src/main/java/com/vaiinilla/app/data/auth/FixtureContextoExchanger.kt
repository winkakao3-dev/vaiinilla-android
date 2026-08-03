package com.vaiinilla.app.data.auth

import com.vaiinilla.app.core.config.DataSourceMode
import com.vaiinilla.app.core.config.EffectiveDataSourceResolver
import javax.inject.Inject
import javax.inject.Singleton

/** MOCK contexto exchange — no network. Returns a synthetic Vaiinilla JWT. */
@Singleton
class FixtureContextoExchanger
    @Inject
    constructor() : ContextoExchanger {
        override fun exchange(
            firebaseIdToken: String,
            establecimientoSlug: String,
            establecimientoId: String,
            identificadorCliente: String?,
        ): SesionesContextoDataDto =
            SesionesContextoDataDto(
                accessToken = "mock-vaiinilla-jwt-$establecimientoId",
                tokenType = "Bearer",
                expiresIn = 900,
                contexto =
                    SesionesContextoContextDto(
                        usuarioId = "mock-usuario",
                        membresiaId = "mock-membresia-$establecimientoId",
                        establecimientoId = establecimientoId,
                        rol = "cliente",
                    ),
            )
    }

@Singleton
class SwitchingContextoExchanger
    @Inject
    constructor(
        private val resolver: EffectiveDataSourceResolver,
        private val fixture: FixtureContextoExchanger,
        private val remote: SesionesContextoExchange,
    ) : ContextoExchanger {
        private fun active(): ContextoExchanger =
            when (resolver.effectiveMode()) {
                DataSourceMode.MOCK -> fixture
                DataSourceMode.REMOTE -> remote
            }

        override fun exchange(
            firebaseIdToken: String,
            establecimientoSlug: String,
            establecimientoId: String,
            identificadorCliente: String?,
        ): SesionesContextoDataDto =
            active().exchange(
                firebaseIdToken,
                establecimientoSlug,
                establecimientoId,
                identificadorCliente,
            )
    }
