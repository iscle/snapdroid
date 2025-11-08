package de.badaix.snapcast.domain.usecase

import de.badaix.snapcast.domain.repository.PlayerRepository
import javax.inject.Inject

/**
 * Use case for stopping the player
 */
class StopPlayerUseCase @Inject constructor(
    private val playerRepository: PlayerRepository
) {
    suspend operator fun invoke() {
        playerRepository.stopPlayer()
    }
}

