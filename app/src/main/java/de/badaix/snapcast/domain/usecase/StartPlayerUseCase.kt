package de.badaix.snapcast.domain.usecase

import de.badaix.snapcast.domain.model.PlayerConnectionParams
import de.badaix.snapcast.domain.repository.PlayerRepository
import de.badaix.snapcast.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use case for starting the player
 */
class StartPlayerUseCase @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(params: PlayerConnectionParams) {
        val audioConfig = settingsRepository.getAudioConfiguration()
        playerRepository.startPlayer(params, audioConfig)
    }
}

