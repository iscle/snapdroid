package de.badaix.snapcast.util

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages wake locks for keeping the device awake during playback and mDNS operations.
 * 
 * Uses PARTIAL_WAKE_LOCK to keep the CPU awake for:
 * - Audio playback processing
 * - mDNS discovery and advertising
 * 
 * Uses WiFi lock to ensure WiFi stays active for:
 * - mDNS multicast operations
 * - Network communication with Snapcast servers
 * 
 * Uses Multicast lock to ensure multicast packets are received for:
 * - mDNS service discovery
 * - mDNS service advertising
 */
@Singleton
class WakeLockManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var powerWakeLock: PowerManager.WakeLock? = null
    private var wifiWakeLock: WifiManager.WifiLock? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    /**
     * Acquires wake locks needed for background audio playback and mDNS operations.
     * 
     * - PARTIAL_WAKE_LOCK: Keeps CPU awake for audio processing and network operations
     * - WiFi Lock: Ensures WiFi stays active for mDNS multicast and network communication
     */
    fun acquireWakeLocks() {
        try {
            // Acquire CPU wake lock for audio playback and mDNS operations
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager != null) {
                powerWakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "snapcast:WakeLock"
                ).apply {
                    // Use non-reference counted to ensure it's released when service stops
                    // For non-reference counted wakelocks, use acquire() without timeout
                    // The service lifecycle will manage the wakelock duration
                    setReferenceCounted(false)
                    acquire()
                    Timber.d("Acquired PARTIAL_WAKE_LOCK for audio playback and mDNS")
                }
            } else {
                Timber.w("PowerManager not available, cannot acquire wake lock")
            }

            // Acquire WiFi lock for mDNS multicast operations
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager != null) {
                // Use WIFI_MODE_FULL for compatibility (WIFI_MODE_FULL_HIGH_PERF is deprecated in API 29+)
                val wifiMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    WifiManager.WIFI_MODE_FULL_HIGH_PERF
                } else {
                    WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                }
                
                wifiWakeLock = wifiManager.createWifiLock(
                    wifiMode,
                    "snapcast:WifiWakeLock"
                ).apply {
                    setReferenceCounted(false)
                    acquire()
                    Timber.d("Acquired WiFi lock for mDNS operations")
                }

                // Acquire multicast lock for mDNS multicast packet reception
                multicastLock = wifiManager.createMulticastLock("snapcast:MulticastLock").apply {
                    setReferenceCounted(false)
                    acquire()
                    Timber.d("Acquired multicast lock for mDNS discovery")
                }
            } else {
                Timber.w("WifiManager not available, cannot acquire WiFi lock")
            }
        } catch (e: SecurityException) {
            Timber.e(e, "Permission denied when acquiring wake locks")
        } catch (e: Exception) {
            Timber.e(e, "Failed to acquire wake locks")
        }
    }

    /**
     * Releases all acquired wake locks.
     * Safe to call multiple times.
     */
    fun releaseWakeLocks() {
        try {
            powerWakeLock?.let { lock ->
                if (lock.isHeld) {
                    lock.release()
                    Timber.d("Released PARTIAL_WAKE_LOCK")
                }
                powerWakeLock = null
            }

            multicastLock?.let { lock ->
                if (lock.isHeld) {
                    lock.release()
                    Timber.d("Released multicast lock")
                }
                multicastLock = null
            }

            wifiWakeLock?.let { lock ->
                if (lock.isHeld) {
                    lock.release()
                    Timber.d("Released WiFi lock")
                }
                wifiWakeLock = null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to release wake locks")
        }
    }

    /**
     * Checks if wake locks are currently held.
     */
    fun areWakeLocksHeld(): Boolean {
        return (powerWakeLock?.isHeld == true) || 
               (wifiWakeLock?.isHeld == true) || 
               (multicastLock?.isHeld == true)
    }
}

