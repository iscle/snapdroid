package de.badaix.snapcast.data.datasource

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import de.badaix.snapcast.domain.model.AudioEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPreferencesDataSource @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _audioConfigFlow = MutableStateFlow(
        AudioConfigPrefs(
            engine = getAudioEngineInternal(),
            resamplingEnabled = getResamplingEnabledInternal()
        )
    )

    val audioConfigFlow: Flow<AudioConfigPrefs> = _audioConfigFlow.asStateFlow()

    suspend fun getDeviceId(): String = withContext(Dispatchers.IO) {
        val existingId = prefs.getString(KEY_DEVICE_ID, null)
        if (existingId != null) {
            existingId
        } else {
            val newId = java.util.UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
            newId
        }
    }

    suspend fun getAudioEngine(): AudioEngine = withContext(Dispatchers.IO) {
        getAudioEngineInternal()
    }

    private fun getAudioEngineInternal(): AudioEngine {
        val engineName = prefs.getString(KEY_AUDIO_ENGINE, null)
        return when (engineName) {
            "Oboe" -> AudioEngine.OBOE
            "OpenSL" -> AudioEngine.OPENSL
            else -> AudioEngine.OBOE // Default
        }
    }

    suspend fun setAudioEngine(engine: AudioEngine) = withContext(Dispatchers.IO) {
        val engineName = when (engine) {
            AudioEngine.OBOE -> "Oboe"
            AudioEngine.OPENSL -> "OpenSL"
        }
        prefs.edit().putString(KEY_AUDIO_ENGINE, engineName).apply()
        updateAudioConfigFlow()
    }

    suspend fun isResamplingEnabled(): Boolean = withContext(Dispatchers.IO) {
        getResamplingEnabledInternal()
    }

    private fun getResamplingEnabledInternal(): Boolean {
        return prefs.getBoolean(KEY_RESAMPLING_ENABLED, false)
    }

    suspend fun setResamplingEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean(KEY_RESAMPLING_ENABLED, enabled).apply()
        updateAudioConfigFlow()
    }

    private fun updateAudioConfigFlow() {
        _audioConfigFlow.value = AudioConfigPrefs(
            engine = getAudioEngineInternal(),
            resamplingEnabled = getResamplingEnabledInternal()
        )
    }

    data class AudioConfigPrefs(
        val engine: AudioEngine,
        val resamplingEnabled: Boolean
    )

    companion object {
        private const val PREFS_NAME = "snapcast_preferences"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_AUDIO_ENGINE = "audio_engine"
        private const val KEY_RESAMPLING_ENABLED = "resampling_enabled"
    }
}

