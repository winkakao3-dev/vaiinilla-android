package com.vaiinilla.app.data.localhub

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.vaiinilla.app.domain.localhub.LocalHubRules

/** Dónde está la Caja del local en el Wi‑Fi. */
data class LocalHubEndpoint(
    val host: String,
    val port: Int,
)

/**
 * Anuncio y búsqueda de la Caja en la red del local con DNS-SD (NsdManager): nadie tiene que escribir una IP. El
 * anuncio lleva el negocio, así Cocina de una tienda no se conecta a la Caja de otra en la misma red; aun así, la
 * firma es lo que de verdad protege.
 */
class LocalHubDiscovery(
    context: Context,
) {
    private val nsd = context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var registration: NsdManager.RegistrationListener? = null
    private var discovery: NsdManager.DiscoveryListener? = null

    @Synchronized
    fun advertise(
        establishmentId: String,
        port: Int,
    ) {
        if (registration != null) return
        val info =
            NsdServiceInfo().apply {
                serviceName = "Vaiinilla Caja ${establishmentId.take(SHORT_ID)}"
                serviceType = LocalHubRules.SERVICE_TYPE
                setPort(port)
                setAttribute(LocalHubRules.ESTABLISHMENT_ATTRIBUTE, establishmentId.lowercase())
            }
        val listener =
            object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(serviceInfo: NsdServiceInfo) = Unit

                override fun onRegistrationFailed(
                    serviceInfo: NsdServiceInfo,
                    errorCode: Int,
                ) {
                    synchronized(this@LocalHubDiscovery) { registration = null }
                }

                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) = Unit

                override fun onUnregistrationFailed(
                    serviceInfo: NsdServiceInfo,
                    errorCode: Int,
                ) = Unit
            }
        registration = listener
        runCatching { nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener) }
            .onFailure { registration = null }
    }

    @Synchronized
    fun stopAdvertising() {
        registration?.let { runCatching { nsd.unregisterService(it) } }
        registration = null
    }

    /** Busca la Caja de [establishmentId]; [onFound] puede llamarse más de una vez si la Caja cambia de IP. */
    @Synchronized
    fun find(
        establishmentId: String,
        onFound: (LocalHubEndpoint) -> Unit,
    ) {
        if (discovery != null) return
        val wanted = establishmentId.lowercase()
        val listener =
            object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(serviceType: String) = Unit

                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    if (!serviceInfo.serviceType.contains(SERVICE_TYPE_NAME)) return
                    resolve(serviceInfo) { resolved ->
                        val est =
                            resolved.attributes[LocalHubRules.ESTABLISHMENT_ATTRIBUTE]
                                ?.toString(Charsets.UTF_8)
                                ?.lowercase()
                        val host = resolved.host?.hostAddress
                        if (est == wanted && host != null) onFound(LocalHubEndpoint(host, resolved.port))
                    }
                }

                override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit

                override fun onDiscoveryStopped(serviceType: String) = Unit

                override fun onStartDiscoveryFailed(
                    serviceType: String,
                    errorCode: Int,
                ) {
                    synchronized(this@LocalHubDiscovery) { discovery = null }
                }

                override fun onStopDiscoveryFailed(
                    serviceType: String,
                    errorCode: Int,
                ) = Unit
            }
        discovery = listener
        runCatching { nsd.discoverServices(LocalHubRules.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener) }
            .onFailure { discovery = null }
    }

    @Synchronized
    fun stopFinding() {
        discovery?.let { runCatching { nsd.stopServiceDiscovery(it) } }
        discovery = null
    }

    // resolveService quedó obsoleto en Android 14, pero es el único que existe desde Android 8 (minSdk 26) y sigue
    // funcionando; una resolución a la vez basta para una sola Caja por local.
    @Suppress("DEPRECATION")
    private fun resolve(
        serviceInfo: NsdServiceInfo,
        onResolved: (NsdServiceInfo) -> Unit,
    ) {
        runCatching {
            nsd.resolveService(
                serviceInfo,
                object : NsdManager.ResolveListener {
                    override fun onResolveFailed(
                        serviceInfo: NsdServiceInfo,
                        errorCode: Int,
                    ) = Unit

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) = onResolved(serviceInfo)
                },
            )
        }
    }

    private companion object {
        const val SHORT_ID = 8
        const val SERVICE_TYPE_NAME = "_vaiinilla._tcp"
    }
}
