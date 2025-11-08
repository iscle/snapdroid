package de.badaix.snapcast.manager

import de.badaix.snapcast.domain.model.PlayerConnectionParams
import de.badaix.snapcast.domain.model.PlayerState
import de.badaix.snapcast.domain.repository.PlayerRepository
import de.badaix.snapcast.domain.usecase.StartPlayerUseCase
import de.badaix.snapcast.domain.usecase.StopPlayerUseCase
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class for coordinating player service operations
 */
@Singleton
class PlayerServiceManager @Inject constructor(
    private val startPlayerUseCase: StartPlayerUseCase,
    private val stopPlayerUseCase: StopPlayerUseCase,
    private val playerRepository: PlayerRepository
) {
    /**
     * Starts the player with the given connection parameters
     */
    suspend fun startPlayer(host: String, port: Int) {
        try {
            val params = PlayerConnectionParams(host, port)
            startPlayerUseCase(params)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start player")
            throw e
        }
    }

    /**
     * Stops the player
     */
    suspend fun stopPlayer() {
        try {
            stopPlayerUseCase()
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop player")
            throw e
        }
    }

    /**
     * Checks if the player is currently running
     */
    suspend fun isPlayerRunning(): Boolean {
        val state = playerRepository.playerStateFlow().first()
        return state is PlayerState.Running
    }
}

