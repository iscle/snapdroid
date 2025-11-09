package de.badaix.snapcast.data.repository

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import de.badaix.snapcast.domain.model.DiscoveredServer
import de.badaix.snapcast.domain.repository.MdnsRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MdnsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : MdnsRepository {

    private val nsdManager: NsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    private var multicastLock: WifiManager.MulticastLock? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var isCurrentlyDiscovering = false

    override fun startDiscovery(): Flow<List<DiscoveredServer>> = callbackFlow {
        val discoveredServers = mutableMapOf<String, DiscoveredServer>()

        // Acquire multicast lock for Android 9+
        // https://stackoverflow.com/questions/53615125/nsdmanager-discovery-does-not-work-on-android-9
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        multicastLock = wifiManager?.createMulticastLock("snapcast_mdns")?.apply {
            setReferenceCounted(true)
            acquire()
        }

        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Timber.w("Resolve failed for ${serviceInfo.serviceName}, error code: $errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                val host = serviceInfo.host?.hostAddress ?: serviceInfo.host?.canonicalHostName
                if (host != null) {
                    val server = DiscoveredServer(
                        name = serviceInfo.serviceName,
                        host = host,
                        port = serviceInfo.port
                    )
                    discoveredServers[serviceInfo.serviceName] = server
                    trySend(discoveredServers.values.toList())
                    Timber.d("Service resolved: ${server.name} at ${server.host}:${server.port}")
                }
            }
        }

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.e("Discovery start failed, service type: $serviceType, error code: $errorCode")
                isCurrentlyDiscovering = false
                close()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.e("Discovery stop failed, service type: $serviceType, error code: $errorCode")
                isCurrentlyDiscovering = false
            }

            override fun onDiscoveryStarted(serviceType: String) {
                Timber.i("Discovery started, service type: $serviceType")
                isCurrentlyDiscovering = true
                // Send initial empty list
                trySend(emptyList())
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Timber.i("Discovery stopped, service type: $serviceType")
                isCurrentlyDiscovering = false
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Timber.d("Service found: ${serviceInfo.serviceName}")
                // Only resolve Snapcast services
                if (serviceInfo.serviceName.startsWith(SERVICE_NAME_PREFIX, ignoreCase = true)) {
                    try {
                        nsdManager.resolveService(serviceInfo, resolveListener)
                    } catch (e: IllegalArgumentException) {
                        Timber.e(e, "Failed to resolve service: ${serviceInfo.serviceName}")
                    }
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Timber.d("Service lost: ${serviceInfo.serviceName}")
                discoveredServers.remove(serviceInfo.serviceName)
                trySend(discoveredServers.values.toList())
            }
        }

        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start service discovery")
            close(e)
        }

        awaitClose {
            stopDiscovery()
        }
    }

    override fun stopDiscovery() {
        try {
            discoveryListener?.let {
                nsdManager.stopServiceDiscovery(it)
                discoveryListener = null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop service discovery")
        }

        multicastLock?.let {
            if (it.isHeld) {
                it.release()
            }
            multicastLock = null
        }

        isCurrentlyDiscovering = false
    }

    override fun isDiscovering(): Boolean = isCurrentlyDiscovering

    companion object {
        private const val SERVICE_TYPE = "_snapcast._tcp."
        private const val SERVICE_NAME_PREFIX = "Snapcast"
    }
}

