package de.badaix.snapcast.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import de.badaix.snapcast.data.model.Group
import de.badaix.snapcast.data.model.Volume
import de.badaix.snapcast.domain.model.PlayerState
import de.badaix.snapcast.ui.component.GroupCard
import de.badaix.snapcast.ui.component.GroupCardSink
import de.badaix.snapcast.ui.theme.SnapdroidTheme
import kotlinx.coroutines.launch

private object MainScreenDefaults {
    const val CONTENT_PADDING = 16
    const val CONTENT_SPACING = 16
    const val DEFAULT_SERVER_HOST = "localhost"
    const val DEFAULT_SERVER_PORT = 1704
}

private object MainScreenStrings {
    const val APP_TITLE = "Snapcast"
    const val SETTINGS_CONTENT_DESCRIPTION = "Settings"
    const val START_PLAYER_CONTENT_DESCRIPTION = "Start Player"
    const val STOP_PLAYER_CONTENT_DESCRIPTION = "Stop Player"
}

@Composable
fun MainScreen(
    onNavigateToLogs: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val playerState by viewModel.playerState.collectAsState()
    val connectionInfo by viewModel.connectionInfo.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val serverConfiguration by viewModel.serverConfiguration.collectAsState()
    val discoveredServers by viewModel.discoveredServers.collectAsState()
    val isDiscovering by viewModel.isDiscovering.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var groupSettingsOpen by remember { mutableStateOf(false) }
    var deviceSettingsOpen by remember { mutableStateOf(false) }
    var serverConfigOpen by remember { mutableStateOf(false) }
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var selectedClient by remember { mutableStateOf<de.badaix.snapcast.data.model.Client?>(null) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message)
                viewModel.clearError()
            }
        }
    }

    val isPlayerActive = playerState is PlayerState.Running || playerState is PlayerState.Starting
    val hasServerConfiguration = serverConfiguration != null

    @OptIn(ExperimentalMaterial3Api::class)
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MainTopBar(
                subtitle = connectionInfo?.let { 
                    if (it.port == MainScreenDefaults.DEFAULT_SERVER_PORT) {
                        it.host
                    } else {
                        "${it.host}:${it.port}"
                    }
                },
                onSettingsClick = { serverConfigOpen = true },
                onLogsClick = onNavigateToLogs
            )
        },
        floatingActionButton = {
            if (hasServerConfiguration) {
                MainFloatingActionButton(
                    isActive = isPlayerActive,
                    onStart = {
                        serverConfiguration?.let { config ->
                            viewModel.startPlayer(
                                host = config.host,
                                port = config.streamPort
                            )
                        }
                    },
                    onStop = {
                        viewModel.stopPlayer()
                    }
                )
            }
        }
    ) { innerPadding ->
        if (!hasServerConfiguration) {
            // Show empty state when no server is configured
            EmptyServerConfigurationScreen(
                onConfigureServer = { 
                    serverConfigOpen = true
                },
                isSearchingForServers = isDiscovering,
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            // Show normal content when server is configured
            MainContent(
                modifier = Modifier.padding(innerPadding),
                groups = groups,
                onGroupMuteChange = { groupId, muted ->
                    viewModel.setGroupMute(groupId, muted)
                },
                onGroupVolumeChange = { groupId, volume ->
                    // For group volume, we need to update all clients in the group
                    // This is a simplified approach - in reality, you might want to
                    // calculate the average and adjust each client proportionally
                    val currentGroups = groups
                    currentGroups.find { it.id == groupId }?.clients?.forEach { client ->
                        viewModel.setClientVolume(
                            clientId = client.id,
                            volume = Volume(muted = client.config.volume.muted, percent = volume)
                        )
                    }
                },
                onClientMuteChange = { clientId, muted ->
                    viewModel.setClientMute(clientId, muted)
                },
                onClientVolumeChange = { clientId, volume ->
                    val currentGroups = groups
                    currentGroups.forEach { group ->
                        group.clients.find { it.id == clientId }?.let { client ->
                            viewModel.setClientVolume(
                                clientId = clientId,
                                volume = Volume(muted = client.config.volume.muted, percent = volume)
                            )
                        }
                    }
                },
                onGroupSettingsClick = { group ->
                    selectedGroup = group
                    groupSettingsOpen = true
                },
                onDeviceSettingsClick = { client ->
                    selectedClient = client
                    deviceSettingsOpen = true
                }
            )
        }

        if (serverConfigOpen) {
            ServerConfigurationBottomSheet(
                onDismiss = { serverConfigOpen = false },
                onSaveConfiguration = { config ->
                    viewModel.saveServerConfiguration(config)
                    serverConfigOpen = false
                },
                discoveredServers = discoveredServers,
                isDiscovering = isDiscovering,
                onStartDiscovery = { viewModel.startMdnsDiscovery() },
                onStopDiscovery = { viewModel.stopMdnsDiscovery() },
                initialConfiguration = serverConfiguration,
                initiallyShowManualEntry = !hasServerConfiguration // Start with manual entry when no server configured
            )
        }

        if (groupSettingsOpen && selectedGroup != null) {
            GroupSettingsBottomSheet(
                group = selectedGroup!!,
                onDismiss = {
                    groupSettingsOpen = false
                    selectedGroup = null
                }
            )
        }

        if (deviceSettingsOpen && selectedClient != null) {
            DeviceSettingsBottomSheet(
                client = selectedClient!!,
                onDismiss = {
                    deviceSettingsOpen = false
                    selectedClient = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    subtitle: String?,
    onSettingsClick: () -> Unit,
    onLogsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Column {
                Text(
                    text = MainScreenStrings.APP_TITLE,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                subtitle?.let {
                    Text(
                        text = it,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onLogsClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Article,
                    contentDescription = "View Player Logs"
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = MainScreenStrings.SETTINGS_CONTENT_DESCRIPTION
                )
            }
        }
    )
}

@Composable
private fun MainFloatingActionButton(
    isActive: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = if (isActive) onStop else onStart,
        modifier = modifier
    ) {
        if (isActive) {
            Icon(
                imageVector = Icons.Filled.Stop,
                contentDescription = MainScreenStrings.STOP_PLAYER_CONTENT_DESCRIPTION
            )
        } else {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = MainScreenStrings.START_PLAYER_CONTENT_DESCRIPTION
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    modifier: Modifier = Modifier,
    groups: List<Group>,
    onGroupMuteChange: (String, Boolean) -> Unit,
    onGroupVolumeChange: (String, Int) -> Unit,
    onClientMuteChange: (String, Boolean) -> Unit,
    onClientVolumeChange: (String, Int) -> Unit,
    onGroupSettingsClick: (Group) -> Unit,
    onDeviceSettingsClick: (de.badaix.snapcast.data.model.Client) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(MainScreenDefaults.CONTENT_PADDING.dp),
        verticalArrangement = Arrangement.spacedBy(MainScreenDefaults.CONTENT_SPACING.dp)
    ) {
        items(groups.size) { index ->
            val group = groups[index]
            val groupVolume = calculateGroupVolume(group)
            val groupName = group.name.ifEmpty { 
                group.streamId.ifEmpty { group.id }
            }
            
            GroupCard(
                name = groupName,
                isMuted = group.muted,
                onIsMutedChange = { muted -> onGroupMuteChange(group.id, muted) },
                volume = groupVolume / 100f,
                onVolumeChange = { volume -> onGroupVolumeChange(group.id, (volume * 100).toInt()) },
                onSettingsClick = { onGroupSettingsClick(group) },
                sinks = {
                    group.clients.forEach { client ->
                        val clientName = client.config.name.ifEmpty { 
                            client.host.name.ifEmpty { client.id }
                        }
                        GroupCardSink(
                            name = clientName,
                            isMuted = client.config.volume.muted,
                            onIsMutedChange = { muted -> onClientMuteChange(client.id, muted) },
                            volume = client.config.volume.percent / 100f,
                            onVolumeChange = { volume -> onClientVolumeChange(client.id, (volume * 100).toInt()) },
                            onSettingsClick = { onDeviceSettingsClick(client) }
                        )
                    }
                }
            )
        }
    }
}

/**
 * Calculate the average volume of all clients in a group
 */
private fun calculateGroupVolume(group: Group): Int {
    if (group.clients.isEmpty()) return 0
    val totalVolume = group.clients.sumOf { it.config.volume.percent }
    return totalVolume / group.clients.size
}

@Preview(name = "Main Screen - Idle", showBackground = true, showSystemUi = true)
@Composable
private fun MainScreenIdlePreview() {
    SnapdroidTheme {
        MainContent(
            groups = emptyList(),
            onGroupMuteChange = { _, _ -> },
            onGroupVolumeChange = { _, _ -> },
            onClientMuteChange = { _, _ -> },
            onClientVolumeChange = { _, _ -> },
            onGroupSettingsClick = {},
            onDeviceSettingsClick = {}
        )
    }
}

@Preview(name = "Main Screen - Running", showBackground = true, showSystemUi = true)
@Composable
private fun MainScreenRunningPreview() {
    SnapdroidTheme {
        MainContent(
            groups = emptyList(),
            onGroupMuteChange = { _, _ -> },
            onGroupVolumeChange = { _, _ -> },
            onClientMuteChange = { _, _ -> },
            onClientVolumeChange = { _, _ -> },
            onGroupSettingsClick = {},
            onDeviceSettingsClick = {}
        )
    }
}

@Preview(name = "Main Top Bar - Default Port", showBackground = true)
@Composable
private fun MainTopBarPreview() {
    SnapdroidTheme {
        MainTopBar(
            subtitle = "192.168.1.100",
            onSettingsClick = {},
            onLogsClick = {}
        )
    }
}

@Preview(name = "Main Top Bar - Custom Port", showBackground = true)
@Composable
private fun MainTopBarCustomPortPreview() {
    SnapdroidTheme {
        MainTopBar(
            subtitle = "192.168.1.100:8080",
            onSettingsClick = {},
            onLogsClick = {}
        )
    }
}

@Preview(name = "Main Top Bar - No Subtitle", showBackground = true)
@Composable
private fun MainTopBarNoSubtitlePreview() {
    SnapdroidTheme {
        MainTopBar(
            subtitle = null,
            onSettingsClick = {},
            onLogsClick = {}
        )
    }
}

@Preview(name = "Main FAB - Inactive", showBackground = true)
@Composable
private fun MainFloatingActionButtonInactivePreview() {
    SnapdroidTheme {
        MainFloatingActionButton(
            isActive = false,
            onStart = {},
            onStop = {}
        )
    }
}

@Preview(name = "Main FAB - Active", showBackground = true)
@Composable
private fun MainFloatingActionButtonActivePreview() {
    SnapdroidTheme {
        MainFloatingActionButton(
            isActive = true,
            onStart = {},
            onStop = {}
        )
    }
}
