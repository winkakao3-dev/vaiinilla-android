package com.vaiinilla.app.domain.repository

import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.domain.model.SpaceResolveResult

interface DiscoveryRepository {
    fun searchEstablishments(
        query: String,
        limit: Int = 20,
        cursor: String? = null,
    ): Result<Pair<List<PublicEstablishment>, String?>>

    fun getEstablishment(slug: String): Result<PublicEstablishment>

    fun getGuestCatalog(slug: String): Result<Catalog>

    fun resolveSpaceToken(token: String): Result<SpaceResolveResult>
}
