package com.vaiinilla.app.data.discovery

import com.vaiinilla.app.core.config.DataSourceMode
import com.vaiinilla.app.core.config.EffectiveDataSourceResolver
import com.vaiinilla.app.domain.repository.DiscoveryRepository

class SwitchingDiscoveryRepository(
    private val resolver: EffectiveDataSourceResolver,
    private val fixture: DiscoveryRepository,
    private val remote: DiscoveryRepository,
) : DiscoveryRepository {
    private fun active(): DiscoveryRepository =
        when (resolver.effectiveMode()) {
            DataSourceMode.MOCK -> fixture
            DataSourceMode.REMOTE -> remote
        }

    override fun searchEstablishments(
        query: String,
        limit: Int,
        cursor: String?,
    ) = active().searchEstablishments(query, limit, cursor)

    override fun getEstablishment(slug: String) = active().getEstablishment(slug)

    override fun getGuestCatalog(slug: String) = active().getGuestCatalog(slug)

    override fun resolveSpaceToken(token: String) = active().resolveSpaceToken(token)
}
