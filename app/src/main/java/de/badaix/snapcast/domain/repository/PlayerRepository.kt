package de.badaix.snapcast.domain.repository

import de.badaix.snapcast.domain.model.AudioConfiguration
import de.badaix.snapcast.domain.model.PlayerConnectionParams
import de.badaix.snapcast.domain.model.PlayerLogEntry
import de.badaix.snapcast.domain.model.PlayerState
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing the native player process
 */
interface PlayerRepository {
    /**
     * Returns a Flow of the current player state
     */
    fun playerStateFlow(): Flow<PlayerState>

    /**
     * Returns a Flow of log entries from the native player
     */
    fun playerLogsFlow(): Flow<PlayerLogEntry>

    /**
     * Starts the player with the given connection parameters
     */
    suspend fun startPlayer(params: PlayerConnectionParams, audioConfig: AudioConfiguration)

    /**
     * Stops the player
     */
    suspend fun stopPlayer()

    /**
     * Checks if the player is currently running
     */
    suspend fun isPlayerRunning(): Boolean
}

