package de.badaix.snapcast.data.datasource

import android.content.Context
import android.media.AudioManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import de.badaix.snapcast.domain.model.AudioConfiguration
import de.badaix.snapcast.domain.model.AudioEngine
import de.badaix.snapcast.domain.model.PlayerConnectionParams
import de.badaix.snapcast.domain.model.PlayerLogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for managing the native player process
 */
@Singleton
class NativeProcessDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var nativeProcess: Process? = null
    private var logReaderThread: Thread? = null
    private val _logEntries = MutableSharedFlow<PlayerLogEntry>(extraBufferCapacity = 64)
    val logEntries: Flow<PlayerLogEntry> = _logEntries.asSharedFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var isInitialized = false

    suspend fun startProcess(
        params: PlayerConnectionParams,
        audioConfig: AudioConfiguration,
        deviceId: String
    ) = withContext(Dispatchers.IO) {
        if (nativeProcess != null) {
            Timber.w("Process already running")
            return@withContext
        }

        try {
            val playerName = when (audioConfig.engine) {
                AudioEngine.OBOE -> "oboe"
                AudioEngine.OPENSL -> "opensl"
            }

            val sampleFormat = buildSampleFormat(audioConfig)

            val command = buildList {
                add("${context.applicationInfo.nativeLibraryDir}/libsnapclient.so")
                add("-h")
                add(params.serverHost)
                add("-p")
                add(params.serverPort.toString())
                add("--hostID")
                add(deviceId)
                add("--player")
                add(playerName)
                add("--sampleformat")
                add(sampleFormat)
                add("--logfilter")
                add("*:info,Stats:debug")
            }

            val processBuilder = ProcessBuilder(command)
                .redirectErrorStream(true)

            val environment = processBuilder.environment()
            audioConfig.sampleRate?.let { environment["SAMPLE_RATE"] = it }
            audioConfig.framesPerBuffer?.let { environment["FRAMES_PER_BUFFER"] = it }

            Timber.d("Starting native process with command: ${command.joinToString(" ")}")
            nativeProcess = processBuilder.start()

            startLogReader()
            _isRunning.value = true
        } catch (e: Exception) {
            Timber.e(e, "Failed to start native process")
            throw e
        }
    }

    private fun buildSampleFormat(audioConfig: AudioConfiguration): String {
        return if (audioConfig.enableResampling && audioConfig.sampleRate != null) {
            "${audioConfig.sampleRate}:*:*"
        } else {
            "*:16:*"
        }
    }

    private fun startLogReader() {
        val process = nativeProcess ?: return

        logReaderThread = Thread {
            try {
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    reader.lineSequence().forEach { line ->
                        parseAndEmitLog(line)
                    }
                }
            } catch (e: Exception) {
                if (!Thread.currentThread().isInterrupted) {
                    Timber.e(e, "Error reading process output")
                }
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    private fun parseAndEmitLog(line: String) {
        if (!isInitialized) {
            isInitialized = true
            Timber.d("Player process initialized")
        }

        val severityOpen = line.indexOf('[')
        val severityClose = line.indexOf(']', severityOpen)
        val tagOpen = line.indexOf('(', severityClose)
        val tagClose = line.indexOf(')', tagOpen)

        if (severityOpen > 0 && severityClose > 0) {
            val timestamp = line.substring(0, severityOpen - 1)
            val severity = line.substring(severityOpen + 1, severityClose)
            val tag = if (tagOpen > 0 && tagClose > 0) {
                line.substring(tagOpen + 1, tagClose)
            } else {
                ""
            }
            val message = line.substring(
                maxOf(severityClose, if (tagClose > 0) tagClose else severityClose) + 2
            )

            val entry = PlayerLogEntry(timestamp, severity, tag, message)
            _logEntries.tryEmit(entry)

            handleSpecialLogMessages(message)
        }
    }

    private fun handleSpecialLogMessages(message: String) {
        when {
            message == "Init start" && !isInitialized -> {
                // Schedule restart logic if needed
            }
            message.contains("Init failed") -> {
                // Handle init failure
            }
            message == "Init done" -> {
                // Clear restart logic
            }
        }
    }

    suspend fun stopProcess() = withContext(Dispatchers.IO) {
        try {
            logReaderThread?.interrupt()
            logReaderThread = null

            nativeProcess?.destroy()
            nativeProcess = null

            _isRunning.value = false
            isInitialized = false
        } catch (e: Exception) {
            Timber.e(e, "Error stopping process")
        }
    }

    suspend fun getCurrentSampleRate(): String? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        } else {
            null
        }
    }

    suspend fun getCurrentFramesPerBuffer(): String? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
        } else {
            null
        }
    }

}

