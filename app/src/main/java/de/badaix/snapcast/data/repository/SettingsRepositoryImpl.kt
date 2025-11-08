package de.badaix.snapcast.data.repository

import android.content.Context
import android.media.AudioManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import de.badaix.snapcast.data.datasource.NativeProcessDataSource
import de.badaix.snapcast.data.datasource.SharedPreferencesDataSource
import de.badaix.snapcast.domain.model.AudioConfiguration
import de.badaix.snapcast.domain.model.AudioEngine
import de.badaix.snapcast.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val prefsDataSource: SharedPreferencesDataSource,
    private val nativeProcessDataSource: NativeProcessDataSource,
    @ApplicationContext private val context: Context
) : SettingsRepository {

    override suspend fun getAudioEngine(): AudioEngine = withContext(Dispatchers.IO) {
        // The SharedPreferencesDataSource handles the default, but we need to match
        // the original logic: if not explicitly set, choose based on Android version
        val storedValue = context.getSharedPreferences(
            "snapcast_preferences",
            Context.MODE_PRIVATE
        ).getString("audio_engine", null)
        
        if (storedValue != null) {
            prefsDataSource.getAudioEngine()
        } else {
            // Default selection based on Android version (matching original logic)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                AudioEngine.OPENSL
            } else {
                AudioEngine.OBOE
            }
        }
    }

    override suspend fun setAudioEngine(engine: AudioEngine) {
        prefsDataSource.setAudioEngine(engine)
    }

    override suspend fun isResamplingEnabled(): Boolean {
        return prefsDataSource.isResamplingEnabled()
    }

    override suspend fun setResamplingEnabled(enabled: Boolean) {
        prefsDataSource.setResamplingEnabled(enabled)
    }

    override suspend fun getAudioConfiguration(): AudioConfiguration = withContext(Dispatchers.IO) {
        val engine = prefsDataSource.getAudioEngine()
        val resamplingEnabled = prefsDataSource.isResamplingEnabled()

        val sampleRate = if (resamplingEnabled) {
            nativeProcessDataSource.getCurrentSampleRate()
        } else {
            null
        }

        val framesPerBuffer = if (resamplingEnabled) {
            nativeProcessDataSource.getCurrentFramesPerBuffer()
        } else {
            null
        }

        AudioConfiguration(
            engine = engine,
            enableResampling = resamplingEnabled,
            sampleRate = sampleRate,
            framesPerBuffer = framesPerBuffer
        )
    }

    override fun observeAudioConfiguration(): Flow<AudioConfiguration> {
        return prefsDataSource.audioConfigFlow.map { prefs ->
            val sampleRate = if (prefs.resamplingEnabled) {
                // Note: This is a synchronous call, but it's in a flow
                // In production, you might want to make this async
                null // Will be fetched when needed
            } else {
                null
            }

            AudioConfiguration(
                engine = prefs.engine,
                enableResampling = prefs.resamplingEnabled,
                sampleRate = sampleRate,
                framesPerBuffer = null
            )
        }
    }
}

