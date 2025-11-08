package de.badaix.snapcast.domain.repository

/**
 * Repository interface for managing device unique identifier
 */
interface DeviceIdRepository {
    /**
     * Gets or generates a unique device identifier
     */
    suspend fun getDeviceId(): String
}

