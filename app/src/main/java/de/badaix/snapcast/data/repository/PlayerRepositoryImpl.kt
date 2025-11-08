package de.badaix.snapcast.data.repository

import de.badaix.snapcast.data.datasource.NativeProcessDataSource
import de.badaix.snapcast.domain.model.AudioConfiguration
import de.badaix.snapcast.domain.model.PlayerConnectionParams
import de.badaix.snapcast.domain.model.PlayerLogEntry
import de.badaix.snapcast.domain.model.PlayerState
import de.badaix.snapcast.domain.repository.DeviceIdRepository
import de.badaix.snapcast.domain.repository.PlayerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerRepositoryImpl @Inject constructor(
    private val nativeProcessDataSource: NativeProcessDataSource,
    private val deviceIdRepository: DeviceIdRepository
) : PlayerRepository {

    private val lastLogMessage = MutableStateFlow<String?>(null)

    override fun playerStateFlow(): Flow<PlayerState> {
        return combine(
            nativeProcessDataSource.isRunning,
            lastLogMessage
        ) { isRunning, lastLog ->
            when {
                !isRunning -> PlayerState.Idle
                lastLog == null -> PlayerState.Starting
                else -> {
                    when {
                        lastLog.contains("Init failed") -> {
                            PlayerState.Error("Initialization failed: $lastLog")
                        }
                        lastLog == "Init done" -> PlayerState.Running
                        else -> PlayerState.Starting
                    }
                }
            }
        }
    }

    override fun playerLogsFlow(): Flow<PlayerLogEntry> {
        return nativeProcessDataSource.logEntries.onEach { log ->
            lastLogMessage.value = log.message
        }
    }

    override suspend fun startPlayer(
        params: PlayerConnectionParams,
        audioConfig: AudioConfiguration
    ) {
        try {
            val deviceId = deviceIdRepository.getDeviceId()
            
            // Enhance audio config with system properties if resampling is enabled
            val enhancedConfig = if (audioConfig.enableResampling) {
                val sampleRate = nativeProcessDataSource.getCurrentSampleRate()
                val framesPerBuffer = nativeProcessDataSource.getCurrentFramesPerBuffer()
                audioConfig.copy(
                    sampleRate = sampleRate,
                    framesPerBuffer = framesPerBuffer
                )
            } else {
                audioConfig
            }

            nativeProcessDataSource.startProcess(params, enhancedConfig, deviceId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start player")
            throw e
        }
    }

    override suspend fun stopPlayer() {
        nativeProcessDataSource.stopProcess()
    }

    override suspend fun isPlayerRunning(): Boolean {
        return nativeProcessDataSource.isRunning.value
    }
}

