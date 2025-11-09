package de.badaix.snapcast.domain.model

/**
 * Represents a configured Snapcast server
 */
data class ServerConfiguration(
    val host: String,
    val streamPort: Int = DEFAULT_STREAM_PORT,
    val controlPort: Int = DEFAULT_CONTROL_PORT
) {
    companion object {
        const val DEFAULT_STREAM_PORT = 1704
        const val DEFAULT_CONTROL_PORT = 1705
    }
}

/**
 * Represents a server discovered via mDNS
 */
data class DiscoveredServer(
    val name: String,
    val host: String,
    val port: Int
)

