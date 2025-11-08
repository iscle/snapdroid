package de.badaix.snapcast.domain.model

/**
 * Audio configuration for the native player
 */
data class AudioConfiguration(
    val engine: AudioEngine,
    val enableResampling: Boolean,
    val sampleRate: String? = null,
    val framesPerBuffer: String? = null
)

enum class AudioEngine {
    OBOE,
    OPENSL
}

