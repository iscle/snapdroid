package de.badaix.snapcast.domain.model

/**
 * Represents a log entry from the native player process
 */
data class PlayerLogEntry(
    val timestamp: String,
    val severity: String,
    val tag: String,
    val message: String
)

