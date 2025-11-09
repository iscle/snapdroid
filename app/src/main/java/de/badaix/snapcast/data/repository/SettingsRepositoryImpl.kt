package de.badaix.snapcast.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import de.badaix.snapcast.data.datasource.DataStoreDataSource
import de.badaix.snapcast.data.datasource.NativeProcessDataSource
import de.badaix.snapcast.domain.model.AudioConfiguration
import de.badaix.snapcast.domain.model.AudioEngine
import de.badaix.snapcast.domain.model.ServerConfiguration
import de.badaix.snapcast.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStoreDataSource: DataStoreDataSource,
    private val nativeProcessDataSource: NativeProcessDataSource,
    @ApplicationContext private val context: Context
) : SettingsRepository {

    override suspend fun getAudioEngine(): AudioEngine = withContext(Dispatchers.IO) {
        dataStoreDataSource.getAudioEngine()
    }

    override suspend fun setAudioEngine(engine: AudioEngine) {
        dataStoreDataSource.setAudioEngine(engine)
    }

    override suspend fun isResamplingEnabled(): Boolean {
        return dataStoreDataSource.isResamplingEnabled()
    }

    override suspend fun setResamplingEnabled(enabled: Boolean) {
        dataStoreDataSource.setResamplingEnabled(enabled)
    }

    override suspend fun getAudioConfiguration(): AudioConfiguration = withContext(Dispatchers.IO) {
        val engine = dataStoreDataSource.getAudioEngine()
        val resamplingEnabled = dataStoreDataSource.isResamplingEnabled()

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
        return dataStoreDataSource.audioConfigFlow.map { prefs ->
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

    override suspend fun getServerConfiguration(): ServerConfiguration? {
        return dataStoreDataSource.getServerConfiguration()
    }

    override suspend fun setServerConfiguration(config: ServerConfiguration?) {
        dataStoreDataSource.setServerConfiguration(config)
    }

    override suspend fun hasServerConfiguration(): Boolean {
        return dataStoreDataSource.hasServerConfiguration()
    }

    override fun observeServerConfiguration(): Flow<ServerConfiguration?> {
        return dataStoreDataSource.serverConfigFlow
    }
}

