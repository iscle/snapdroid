package de.badaix.snapcast.util

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.net.wifi.WifiManager
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages wake locks for keeping the device awake during playback
 */
@Singleton
class WakeLockManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var powerWakeLock: PowerManager.WakeLock? = null
    private var wifiWakeLock: WifiManager.WifiLock? = null

    fun acquireWakeLocks() {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager

            val wakeLockType = if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
                Timber.d("Running on TV device, using FULL_WAKE_LOCK")
                PowerManager.FULL_WAKE_LOCK
            } else {
                Timber.d("Running on non-TV device, using PARTIAL_WAKE_LOCK")
                PowerManager.PARTIAL_WAKE_LOCK
            }

            powerWakeLock = powerManager.newWakeLock(
                wakeLockType,
                "snapcast:WakeLock"
            ).apply {
                acquire()
            }

            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiWakeLock = wifiManager?.createWifiLock(
                WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                "snapcast:WifiWakeLock"
            )?.apply {
                acquire()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to acquire wake locks")
        }
    }

    fun releaseWakeLocks() {
        try {
            powerWakeLock?.takeIf { it.isHeld }?.release()
            powerWakeLock = null

            wifiWakeLock?.takeIf { it.isHeld }?.release()
            wifiWakeLock = null
        } catch (e: Exception) {
            Timber.e(e, "Failed to release wake locks")
        }
    }
}

