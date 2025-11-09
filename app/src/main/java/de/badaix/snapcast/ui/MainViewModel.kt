package de.badaix.snapcast.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.badaix.snapcast.SnapcastService
import de.badaix.snapcast.data.model.Group
import de.badaix.snapcast.data.model.Volume
import de.badaix.snapcast.domain.model.DiscoveredServer
import de.badaix.snapcast.domain.model.PlayerLogEntry
import de.badaix.snapcast.domain.model.PlayerState
import de.badaix.snapcast.domain.model.ServerConfiguration
import de.badaix.snapcast.domain.repository.MdnsRepository
import de.badaix.snapcast.domain.repository.PlayerRepository
import de.badaix.snapcast.domain.repository.SettingsRepository
import de.badaix.snapcast.domain.repository.SnapcastRepository
import kotlinx.coroutines.Job
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
    private val snapcastRepository: SnapcastRepository,
    private val settingsRepository: SettingsRepository,
    private val mdnsRepository: MdnsRepository,
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

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _serverConfiguration = MutableStateFlow<ServerConfiguration?>(null)
    val serverConfiguration: StateFlow<ServerConfiguration?> = _serverConfiguration.asStateFlow()

    private val _discoveredServers = MutableStateFlow<List<DiscoveredServer>>(emptyList())
    val discoveredServers: StateFlow<List<DiscoveredServer>> = _discoveredServers.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private var discoveryJob: Job? = null

    init {
        observePlayerState()
        observePlayerLogs()
        observeServerUpdates()
        observeClientVolumeChanges()
        observeGroupMuteChanges()
        observeClientConnections()
        observeClientDisconnections()
        observeServerConfiguration()
        loadServerConfiguration()
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
                _playerLogs.value = _playerLogs.value + logEntry
            }
            .catch { e ->
                Timber.e(e, "Error observing player logs")
            }
            .launchIn(viewModelScope)
    }

    private fun observeServerUpdates() {
        snapcastRepository.observeServerUpdates()
            .onEach { server ->
                _groups.value = server.groups
                Timber.d("Server update received: ${server.groups.size} groups")
            }
            .catch { e ->
                Timber.e(e, "Error observing server updates")
                _errorMessage.value = "Error observing server updates: ${e.message}"
            }
            .launchIn(viewModelScope)
    }

    private fun observeClientVolumeChanges() {
        snapcastRepository.observeClientVolumeChanges()
            .onEach { (clientId, volume) ->
                _groups.value = _groups.value.map { group ->
                    group.copy(
                        clients = group.clients.map { client ->
                            if (client.id == clientId) {
                                client.copy(
                                    config = client.config.copy(volume = volume)
                                )
                            } else {
                                client
                            }
                        }
                    )
                }
                Timber.d("Client volume changed: $clientId -> ${volume.percent}%")
            }
            .catch { e ->
                Timber.e(e, "Error observing client volume changes")
            }
            .launchIn(viewModelScope)
    }

    private fun observeGroupMuteChanges() {
        snapcastRepository.observeGroupMuteChanges()
            .onEach { (groupId, muted) ->
                _groups.value = _groups.value.map { group ->
                    if (group.id == groupId) {
                        group.copy(muted = muted)
                    } else {
                        group
                    }
                }
                Timber.d("Group mute changed: $groupId -> $muted")
            }
            .catch { e ->
                Timber.e(e, "Error observing group mute changes")
            }
            .launchIn(viewModelScope)
    }

    private fun observeClientConnections() {
        snapcastRepository.observeClientConnections()
            .onEach { client ->
                Timber.d("Client connected: ${client.id}")
                // Server.OnUpdate will be sent by the server, so we don't need to manually update here
            }
            .catch { e ->
                Timber.e(e, "Error observing client connections")
            }
            .launchIn(viewModelScope)
    }

    private fun observeClientDisconnections() {
        snapcastRepository.observeClientDisconnections()
            .onEach { client ->
                Timber.d("Client disconnected: ${client.id}")
                // Server.OnUpdate will be sent by the server, so we don't need to manually update here
            }
            .catch { e ->
                Timber.e(e, "Error observing client disconnections")
            }
            .launchIn(viewModelScope)
    }

    private fun observeServerConfiguration() {
        settingsRepository.observeServerConfiguration()
            .onEach { config ->
                _serverConfiguration.value = config
                Timber.d("Server configuration updated: $config")
            }
            .catch { e ->
                Timber.e(e, "Error observing server configuration")
            }
            .launchIn(viewModelScope)
    }

    private fun loadServerConfiguration() {
        viewModelScope.launch {
            try {
                val config = settingsRepository.getServerConfiguration()
                _serverConfiguration.value = config
                
                // If no server is configured, start auto-discovery
                if (config == null) {
                    startMdnsDiscovery()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load server configuration")
            }
        }
    }

    fun saveServerConfiguration(config: ServerConfiguration) {
        viewModelScope.launch {
            try {
                settingsRepository.setServerConfiguration(config)
                _serverConfiguration.value = config
                stopMdnsDiscovery()
                
                // Auto-connect to the new server
                startPlayer(config.host, config.streamPort)
            } catch (e: Exception) {
                Timber.e(e, "Failed to save server configuration")
                _errorMessage.value = "Failed to save server configuration: ${e.message}"
            }
        }
    }

    fun startMdnsDiscovery() {
        if (_isDiscovering.value) return
        
        discoveryJob?.cancel()
        discoveryJob = mdnsRepository.startDiscovery()
            .onEach { servers ->
                _discoveredServers.value = servers
                _isDiscovering.value = mdnsRepository.isDiscovering()
                Timber.d("Discovered ${servers.size} servers")
            }
            .catch { e ->
                Timber.e(e, "mDNS discovery error")
                _isDiscovering.value = false
            }
            .launchIn(viewModelScope)
    }

    fun stopMdnsDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
        mdnsRepository.stopDiscovery()
        _isDiscovering.value = false
        _discoveredServers.value = emptyList()
    }

    fun startPlayer(host: String, port: Int = 1704) {
        viewModelScope.launch {
            try {
                _connectionInfo.value = ConnectionInfo(host, port)
                
                // Start the player service (port 1704 for audio streaming)
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
                
                // Connect to the control API (port 1705 for JSON-RPC)
                val controlPort = 1705
                snapcastRepository.connect(host, controlPort)
                
                // Get initial server status
                snapcastRepository.getServerStatus()
                    .onSuccess { server ->
                        _groups.value = server.groups
                        Timber.d("Initial server status: ${server.groups.size} groups")
                    }
                    .onFailure { e ->
                        Timber.e(e, "Failed to get initial server status")
                        _errorMessage.value = "Failed to get server status: ${e.message}"
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
                
                // Disconnect from control API
                snapcastRepository.disconnect()
                
                _connectionInfo.value = null
                _groups.value = emptyList()
            } catch (e: Exception) {
                Timber.e(e, "Failed to stop player service")
                _errorMessage.value = "Failed to stop player: ${e.message}"
            }
        }
    }

    fun setGroupMute(groupId: String, mute: Boolean) {
        viewModelScope.launch {
            // Optimistic update - update UI immediately
            _groups.value = _groups.value.map { group ->
                if (group.id == groupId) {
                    group.copy(muted = mute)
                } else {
                    group
                }
            }
            
            // Send to server - the Group.OnMute notification will update with actual value
            snapcastRepository.setGroupMute(groupId, mute)
                .onFailure { e ->
                    Timber.e(e, "Failed to set group mute")
                    _errorMessage.value = "Failed to set group mute: ${e.message}"
                    // Note: We don't revert the optimistic update here because
                    // the server will send the actual state via notifications
                }
        }
    }

    fun setClientVolume(clientId: String, volume: Volume) {
        viewModelScope.launch {
            // Optimistic update - update UI immediately
            _groups.value = _groups.value.map { group ->
                group.copy(
                    clients = group.clients.map { client ->
                        if (client.id == clientId) {
                            client.copy(
                                config = client.config.copy(volume = volume)
                            )
                        } else {
                            client
                        }
                    }
                )
            }
            
            // Send to server - the Client.OnVolumeChanged notification will update with actual value
            snapcastRepository.setClientVolume(clientId, volume)
                .onFailure { e ->
                    Timber.e(e, "Failed to set client volume")
                    _errorMessage.value = "Failed to set client volume: ${e.message}"
                    // Note: We don't revert the optimistic update here because
                    // the server will send the actual state via notifications
                }
        }
    }

    fun setClientMute(clientId: String, muted: Boolean) {
        viewModelScope.launch {
            // Find current volume percent from local state
            val currentVolume = _groups.value
                .flatMap { it.clients }
                .find { it.id == clientId }
                ?.config?.volume
            
            if (currentVolume == null) {
                Timber.w("Client $clientId not found in current state")
                return@launch
            }
            
            val newVolume = Volume(
                muted = muted,
                percent = currentVolume.percent
            )
            
            // Optimistic update - update UI immediately
            _groups.value = _groups.value.map { group ->
                group.copy(
                    clients = group.clients.map { client ->
                        if (client.id == clientId) {
                            client.copy(
                                config = client.config.copy(volume = newVolume)
                            )
                        } else {
                            client
                        }
                    }
                )
            }
            
            // Send to server - the Client.OnVolumeChanged notification will update with actual value
            snapcastRepository.setClientVolume(clientId, newVolume)
                .onFailure { e ->
                    Timber.e(e, "Failed to set client mute")
                    _errorMessage.value = "Failed to set client mute: ${e.message}"
                    // Note: We don't revert the optimistic update here because
                    // the server will send the actual state via notifications
                }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopMdnsDiscovery()
    }

    data class ConnectionInfo(
        val host: String,
        val port: Int
    )
}
