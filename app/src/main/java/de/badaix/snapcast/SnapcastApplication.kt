package de.badaix.snapcast

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class SnapcastApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // In release builds, you might want to use a crash reporting tree
            // Timber.plant(CrashReportingTree())
        }

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = ContextCompat.getSystemService(this, NotificationManager::class.java)
                ?: throw IllegalStateException("NotificationManager not available")
            val channel = NotificationChannel(
                "snapcast_service_channel",
                "Snapcast Service",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Notifications for Snapcast's background service"
            notificationManager.createNotificationChannel(channel)
        }
    }
}
