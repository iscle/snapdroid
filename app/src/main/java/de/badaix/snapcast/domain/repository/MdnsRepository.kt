package de.badaix.snapcast.domain.repository

import de.badaix.snapcast.domain.model.DiscoveredServer
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for mDNS server discovery
 */
interface MdnsRepository {
    /**
     * Start discovering Snapcast servers on the local network
     */
    fun startDiscovery(): Flow<List<DiscoveredServer>>

    /**
     * Stop server discovery
     */
    fun stopDiscovery()

    /**
     * Check if discovery is currently active
     */
    fun isDiscovering(): Boolean
}

