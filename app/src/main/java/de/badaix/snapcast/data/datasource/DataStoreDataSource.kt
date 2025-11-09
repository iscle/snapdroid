package de.badaix.snapcast.data.datasource

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import de.badaix.snapcast.domain.model.AudioEngine
import de.badaix.snapcast.domain.model.ServerConfiguration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "snapcast_preferences")

@Singleton
class DataStoreDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    // Preference keys
    private object PreferencesKeys {
        val DEVICE_ID = stringPreferencesKey("device_id")
        val AUDIO_ENGINE = stringPreferencesKey("audio_engine")
        val RESAMPLING_ENABLED = booleanPreferencesKey("resampling_enabled")
        val SERVER_HOST = stringPreferencesKey("server_host")
        val SERVER_STREAM_PORT = intPreferencesKey("server_stream_port")
        val SERVER_CONTROL_PORT = intPreferencesKey("server_control_port")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    // Audio configuration flow
    val audioConfigFlow: Flow<AudioConfigPrefs> = dataStore.data.map { preferences ->
        AudioConfigPrefs(
            engine = getAudioEngineFromPreferences(preferences),
            resamplingEnabled = preferences[PreferencesKeys.RESAMPLING_ENABLED] ?: false
        )
    }

    // Server configuration flow
    val serverConfigFlow: Flow<ServerConfiguration?> = dataStore.data.map { preferences ->
        val host = preferences[PreferencesKeys.SERVER_HOST]
        if (host != null) {
            val streamPort = preferences[PreferencesKeys.SERVER_STREAM_PORT] ?: ServerConfiguration.DEFAULT_STREAM_PORT
            val controlPort = preferences[PreferencesKeys.SERVER_CONTROL_PORT] ?: ServerConfiguration.DEFAULT_CONTROL_PORT
            ServerConfiguration(host, streamPort, controlPort)
        } else {
            null
        }
    }

    suspend fun getDeviceId(): String {
        val preferences = dataStore.data.first()
        val existingId = preferences[PreferencesKeys.DEVICE_ID]
        
        return if (existingId != null) {
            existingId
        } else {
            val newId = UUID.randomUUID().toString()
            dataStore.edit { prefs ->
                prefs[PreferencesKeys.DEVICE_ID] = newId
            }
            newId
        }
    }

    suspend fun getAudioEngine(): AudioEngine {
        val preferences = dataStore.data.first()
        return getAudioEngineFromPreferences(preferences)
    }

    private fun getAudioEngineFromPreferences(preferences: Preferences): AudioEngine {
        val engineName = preferences[PreferencesKeys.AUDIO_ENGINE]
        return when (engineName) {
            "Oboe" -> AudioEngine.OBOE
            "OpenSL" -> AudioEngine.OPENSL
            else -> AudioEngine.OBOE // Default
        }
    }

    suspend fun setAudioEngine(engine: AudioEngine) {
        val engineName = when (engine) {
            AudioEngine.OBOE -> "Oboe"
            AudioEngine.OPENSL -> "OpenSL"
        }
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUDIO_ENGINE] = engineName
        }
    }

    suspend fun isResamplingEnabled(): Boolean {
        val preferences = dataStore.data.first()
        return preferences[PreferencesKeys.RESAMPLING_ENABLED] ?: false
    }

    suspend fun setResamplingEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.RESAMPLING_ENABLED] = enabled
        }
    }

    suspend fun getServerConfiguration(): ServerConfiguration? {
        val preferences = dataStore.data.first()
        val host = preferences[PreferencesKeys.SERVER_HOST] ?: return null
        val streamPort = preferences[PreferencesKeys.SERVER_STREAM_PORT] ?: ServerConfiguration.DEFAULT_STREAM_PORT
        val controlPort = preferences[PreferencesKeys.SERVER_CONTROL_PORT] ?: ServerConfiguration.DEFAULT_CONTROL_PORT
        return ServerConfiguration(host, streamPort, controlPort)
    }

    suspend fun setServerConfiguration(config: ServerConfiguration?) {
        dataStore.edit { preferences ->
            if (config == null) {
                preferences.remove(PreferencesKeys.SERVER_HOST)
                preferences.remove(PreferencesKeys.SERVER_STREAM_PORT)
                preferences.remove(PreferencesKeys.SERVER_CONTROL_PORT)
            } else {
                preferences[PreferencesKeys.SERVER_HOST] = config.host
                preferences[PreferencesKeys.SERVER_STREAM_PORT] = config.streamPort
                preferences[PreferencesKeys.SERVER_CONTROL_PORT] = config.controlPort
            }
        }
    }

    suspend fun hasServerConfiguration(): Boolean {
        val preferences = dataStore.data.first()
        return preferences.contains(PreferencesKeys.SERVER_HOST)
    }

    suspend fun hasCompletedOnboarding(): Boolean {
        val preferences = dataStore.data.first()
        return preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    data class AudioConfigPrefs(
        val engine: AudioEngine,
        val resamplingEnabled: Boolean
    )
}

