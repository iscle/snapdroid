package de.badaix.snapcast.domain.model

/**
 * Represents the state of mDNS discovery
 */
sealed class MdnsDiscoveryState {
    data object Idle : MdnsDiscoveryState()
    data object Discovering : MdnsDiscoveryState()
    data class Discovered(val servers: List<DiscoveredServer>) : MdnsDiscoveryState()
    data class Error(val message: String) : MdnsDiscoveryState()
}

