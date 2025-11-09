package de.badaix.snapcast.domain.repository

import de.badaix.snapcast.domain.model.AudioConfiguration
import de.badaix.snapcast.domain.model.AudioEngine
import de.badaix.snapcast.domain.model.ServerConfiguration
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing application settings
 */
interface SettingsRepository {
    /**
     * Gets the configured audio engine
     */
    suspend fun getAudioEngine(): AudioEngine

    /**
     * Sets the audio engine
     */
    suspend fun setAudioEngine(engine: AudioEngine)

    /**
     * Checks if audio resampling is enabled
     */
    suspend fun isResamplingEnabled(): Boolean

    /**
     * Sets whether audio resampling is enabled
     */
    suspend fun setResamplingEnabled(enabled: Boolean)

    /**
     * Gets the current audio configuration
     */
    suspend fun getAudioConfiguration(): AudioConfiguration

    /**
     * Observes audio configuration changes
     */
    fun observeAudioConfiguration(): Flow<AudioConfiguration>

    /**
     * Gets the configured server
     */
    suspend fun getServerConfiguration(): ServerConfiguration?

    /**
     * Sets the server configuration
     */
    suspend fun setServerConfiguration(config: ServerConfiguration?)

    /**
     * Checks if a server is configured
     */
    suspend fun hasServerConfiguration(): Boolean

    /**
     * Observes server configuration changes
     */
    fun observeServerConfiguration(): Flow<ServerConfiguration?>
}

