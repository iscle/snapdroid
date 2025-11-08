package de.badaix.snapcast.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.badaix.snapcast.SnapcastService
import de.badaix.snapcast.domain.model.PlayerLogEntry
import de.badaix.snapcast.domain.model.PlayerState
import de.badaix.snapcast.domain.repository.PlayerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _playerLogs = MutableStateFlow<List<PlayerLogEntry>>(emptyList())
    val playerLogs: StateFlow<List<PlayerLogEntry>> = _playerLogs.asStateFlow()

    private val _connectionInfo = MutableStateFlow<ConnectionInfo?>(null)
    val connectionInfo: StateFlow<ConnectionInfo?> = _connectionInfo.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        observePlayerState()
        observePlayerLogs()
    }

    private fun observePlayerState() {
        playerRepository.playerStateFlow()
            .onEach { state ->
                _playerState.value = state
                if (state is PlayerState.Error) {
                    _errorMessage.value = state.message
                } else {
                    _errorMessage.value = null
                }
            }
            .catch { e ->
                Timber.e(e, "Error observing player state")
                _errorMessage.value = "Error observing player state: ${e.message}"
            }
            .launchIn(viewModelScope)
    }

    private fun observePlayerLogs() {
        playerRepository.playerLogsFlow()
            .onEach { logEntry ->
                _playerLogs.value = (_playerLogs.value + logEntry).takeLast(100) // Keep last 100 logs
            }
            .catch { e ->
                Timber.e(e, "Error observing player logs")
            }
            .launchIn(viewModelScope)
    }

    fun startPlayer(host: String, port: Int = 1704) {
        viewModelScope.launch {
            try {
                _connectionInfo.value = ConnectionInfo(host, port)
                val intent = Intent(context, SnapcastService::class.java).apply {
                    action = SnapcastService.ACTION_START
                    putExtra(SnapcastService.EXTRA_HOST, host)
                    putExtra(SnapcastService.EXTRA_PORT, port)
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to start player service")
                _errorMessage.value = "Failed to start player: ${e.message}"
            }
        }
    }

    fun stopPlayer() {
        viewModelScope.launch {
            try {
                val intent = Intent(context, SnapcastService::class.java).apply {
                    action = SnapcastService.ACTION_STOP
                }
                context.startService(intent)
                _connectionInfo.value = null
            } catch (e: Exception) {
                Timber.e(e, "Failed to stop player service")
                _errorMessage.value = "Failed to stop player: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    data class ConnectionInfo(
        val host: String,
        val port: Int
    )
}
