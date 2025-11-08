package de.badaix.snapcast.domain.model

/**
 * Connection parameters for the native player process
 */
data class PlayerConnectionParams(
    val serverHost: String,
    val serverPort: Int = 1704
)

