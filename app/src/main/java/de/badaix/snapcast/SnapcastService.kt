package de.badaix.snapcast

import android.app.Notification
import android.app.NotificationManager as AndroidNotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Process
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import de.badaix.snapcast.domain.model.PlayerLogEntry
import de.badaix.snapcast.domain.model.PlayerState
import de.badaix.snapcast.domain.repository.PlayerRepository
import de.badaix.snapcast.manager.PlayerServiceManager
import de.badaix.snapcast.util.WakeLockManager
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Foreground service for managing the native Snapcast player process
 */
@AndroidEntryPoint
class SnapcastService : LifecycleService() {

    @Inject
    lateinit var playerRepository: PlayerRepository

    @Inject
    lateinit var playerServiceManager: PlayerServiceManager

    @Inject
    lateinit var wakeLockManager: WakeLockManager

    private val binder = LocalBinder()

    companion object {
        const val ACTION_START = "de.badaix.snapcast.ACTION_START"
        const val ACTION_STOP = "de.badaix.snapcast.ACTION_STOP"
        const val EXTRA_HOST = "de.badaix.snapcast.EXTRA_HOST"
        const val EXTRA_PORT = "de.badaix.snapcast.EXTRA_PORT"
        const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "snapcast_service_channel"
    }

    override fun onCreate() {
        super.onCreate()
        observePlayerState()
        observePlayerLogs()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent == null) {
            return START_NOT_STICKY
        }

        when (intent.action) {
            ACTION_STOP -> {
                handleStop()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val host = intent.getStringExtra(EXTRA_HOST)
                val port = intent.getIntExtra(EXTRA_PORT, 1704)

                if (host == null) {
                    Timber.e("Cannot start: host is null")
                    return START_NOT_STICKY
                }

                handleStart(host, port)
                return START_STICKY
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("Service onDestroy")
        handleStop()
    }

    private fun handleStart(host: String, port: Int) {
        lifecycleScope.launch {
            try {
                val notification = createForegroundNotification(host)

                startForeground(NOTIFICATION_ID, notification)
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)

                wakeLockManager.acquireWakeLocks()
                playerServiceManager.startPlayer(host, port)
            } catch (e: Exception) {
                Timber.e(e, "Failed to start service")
                stopSelf()
            }
        }
    }

    private fun handleStop() {
        lifecycleScope.launch {
            try {
                playerServiceManager.stopPlayer()
                wakeLockManager.releaseWakeLocks()
                cancelNotification()
                stopForeground(STOP_FOREGROUND_REMOVE)
                Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT)
            } catch (e: Exception) {
                Timber.e(e, "Error stopping service")
            } finally {
                stopSelf()
            }
        }
    }

    private fun observePlayerState() {
        playerRepository.playerStateFlow()
            .onEach { state ->
                when (state) {
                    is PlayerState.Error -> {
                        Timber.e("Player error: ${state.message}")
                    }
                    is PlayerState.Running -> {
                        Timber.d("Player is running")
                    }
                    is PlayerState.Starting -> {
                        Timber.d("Player is starting")
                    }
                    is PlayerState.Idle -> {
                        Timber.d("Player is idle")
                    }
                }
            }
            .launchIn(lifecycleScope)
    }

    private fun observePlayerLogs() {
        playerRepository.playerLogsFlow()
            .onEach { logEntry ->
                handleSpecialLogMessages(logEntry)
            }
            .launchIn(lifecycleScope)
    }

    private fun handleSpecialLogMessages(logEntry: PlayerLogEntry) {
        when {
            logEntry.message == "Init start" -> {
                // Could schedule restart logic here if needed
            }
            logEntry.message.contains("Init failed") -> {
                // Handle initialization failure
                Timber.e("Player initialization failed")
            }
            logEntry.message == "Init done" -> {
                // Clear any restart logic
                Timber.d("Player initialization complete")
            }
        }
    }

    /**
     * Local binder for clients to interact with the service
     */
    inner class LocalBinder : Binder() {
        fun getService(): SnapcastService = this@SnapcastService
    }

    /**
     * Checks if the player is currently running
     */
    suspend fun isPlayerRunning(): Boolean {
        return playerServiceManager.isPlayerRunning()
    }

    private fun createForegroundNotification(serverHost: String): Notification {
        val contentIntent = Intent(this, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, SnapcastService::class.java)
            .setAction(ACTION_STOP)
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setContentInfo(serverHost)
            .addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.stop),
                stopPendingIntent
            )
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun cancelNotification() {
        val notificationManager = ContextCompat.getSystemService(
            this,
            AndroidNotificationManager::class.java
        )
        notificationManager?.cancel(NOTIFICATION_ID)
    }
}
