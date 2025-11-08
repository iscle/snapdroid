package de.badaix.snapcast.domain.model

/**
 * Represents the current state of the player
 */
sealed interface PlayerState {
    data object Idle : PlayerState
    data object Starting : PlayerState
    data object Running : PlayerState
    data class Error(val message: String, val exception: Throwable? = null) : PlayerState
}

