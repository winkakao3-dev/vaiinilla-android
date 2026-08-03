package com.vaiinilla.app.domain.discovery

import com.vaiinilla.app.core.network.ApiClientException

object DiscoveryFailures {
    const val ESTABLISHMENT_SUSPENDED = "ESTABLISHMENT_SUSPENDED"

    fun establishmentSuspended(message: String): ApiClientException =
        ApiClientException(
            code = ESTABLISHMENT_SUSPENDED,
            message = message,
            httpStatus = 403,
        )

    fun isEstablishmentSuspended(error: Throwable?): Boolean =
        (error as? ApiClientException)?.code == ESTABLISHMENT_SUSPENDED
}
