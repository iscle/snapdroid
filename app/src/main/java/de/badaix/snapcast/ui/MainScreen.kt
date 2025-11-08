package de.badaix.snapcast.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
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
import androidx.lifecycle.viewmodel.compose.viewModel
import de.badaix.snapcast.domain.model.PlayerLogEntry
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
    const val PREVIEW_GROUP_COUNT = 3
    const val PREVIEW_SINK_COUNT = 3
}

private object MainScreenStrings {
    const val APP_TITLE = "Snapcast"
    const val SETTINGS_CONTENT_DESCRIPTION = "Settings"
    const val START_PLAYER_CONTENT_DESCRIPTION = "Start Player"
    const val STOP_PLAYER_CONTENT_DESCRIPTION = "Stop Player"
}

@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    val playerState by viewModel.playerState.collectAsState()
    val playerLogs by viewModel.playerLogs.collectAsState()
    val connectionInfo by viewModel.connectionInfo.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var groupSettingsOpen by remember { mutableStateOf(false) }
    var deviceSettingsOpen by remember { mutableStateOf(false) }
    var selectedGroupName by remember { mutableStateOf<String?>(null) }
    var selectedDeviceName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message)
                viewModel.clearError()
            }
        }
    }

    val isPlayerActive = playerState is PlayerState.Running || playerState is PlayerState.Starting

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MainTopBar(
                subtitle = connectionInfo?.let { "${it.host}:${it.port}" },
                onSettingsClick = { /* TODO */ }
            )
        },
        floatingActionButton = {
            MainFloatingActionButton(
                isActive = isPlayerActive,
                onStart = {
                    viewModel.startPlayer(
                        host = MainScreenDefaults.DEFAULT_SERVER_HOST,
                        port = MainScreenDefaults.DEFAULT_SERVER_PORT
                    )
                },
                onStop = {
                    viewModel.stopPlayer()
                }
            )
        }
    ) { innerPadding ->
        MainContent(
            modifier = Modifier.padding(innerPadding),
            playerState = playerState,
            connectionInfo = connectionInfo,
            playerLogs = playerLogs,
            onGroupSettingsClick = { groupName ->
                selectedGroupName = groupName
                groupSettingsOpen = true
            },
            onDeviceSettingsClick = { deviceName ->
                selectedDeviceName = deviceName
                deviceSettingsOpen = true
            }
        )

        if (groupSettingsOpen && selectedGroupName != null) {
            GroupSettingsBottomSheet(
                groupName = selectedGroupName!!,
                onDismiss = {
                    groupSettingsOpen = false
                    selectedGroupName = null
                }
            )
        }

        if (deviceSettingsOpen && selectedDeviceName != null) {
            DeviceSettingsBottomSheet(
                deviceName = selectedDeviceName!!,
                onDismiss = {
                    deviceSettingsOpen = false
                    selectedDeviceName = null
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
    playerState: PlayerState,
    connectionInfo: MainViewModel.ConnectionInfo?,
    playerLogs: List<de.badaix.snapcast.domain.model.PlayerLogEntry>,
    onGroupSettingsClick: (String) -> Unit,
    onDeviceSettingsClick: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(MainScreenDefaults.CONTENT_PADDING.dp),
        verticalArrangement = Arrangement.spacedBy(MainScreenDefaults.CONTENT_SPACING.dp)
    ) {
        item {
            PlayerStatusCard(
                playerState = playerState,
                connectionInfo = connectionInfo
            )
        }

        item {
            PlayerLogsCard(logs = playerLogs)
        }

        // TODO: Replace with actual groups from SnapcastRepository
        items(MainScreenDefaults.PREVIEW_GROUP_COUNT) { index ->
            val groupName = "Group $index"
            GroupCard(
                name = groupName,
                isMuted = false,
                onIsMutedChange = {},
                volume = 0.5f,
                onVolumeChange = {},
                onSettingsClick = { onGroupSettingsClick(groupName) },
                sinks = {
                    repeat(MainScreenDefaults.PREVIEW_SINK_COUNT) { sinkIndex ->
                        val deviceName = "Sink ${sinkIndex + 1}"
                        GroupCardSink(
                            name = deviceName,
                            isMuted = false,
                            onIsMutedChange = {},
                            volume = 0.5f,
                            onVolumeChange = {},
                            onSettingsClick = { onDeviceSettingsClick(deviceName) }
                        )
                    }
                }
            )
        }
    }
}

@Preview(name = "Main Screen - Idle", showBackground = true, showSystemUi = true)
@Composable
private fun MainScreenIdlePreview() {
    SnapdroidTheme {
        MainContent(
            playerState = PlayerState.Idle,
            connectionInfo = null,
            playerLogs = emptyList(),
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
            playerState = PlayerState.Running,
            connectionInfo = MainViewModel.ConnectionInfo("192.168.1.100", 1704),
            playerLogs = listOf(
                PlayerLogEntry("10:30:15", "INFO", "SnapcastClient", "Connected to server"),
                PlayerLogEntry("10:30:16", "INFO", "SnapcastClient", "Stream started")
            ),
            onGroupSettingsClick = {},
            onDeviceSettingsClick = {}
        )
    }
}

@Preview(name = "Main Top Bar", showBackground = true)
@Composable
private fun MainTopBarPreview() {
    SnapdroidTheme {
        MainTopBar(
            subtitle = "192.168.1.100:1704",
            onSettingsClick = {}
        )
    }
}

@Preview(name = "Main Top Bar - No Subtitle", showBackground = true)
@Composable
private fun MainTopBarNoSubtitlePreview() {
    SnapdroidTheme {
        MainTopBar(
            subtitle = null,
            onSettingsClick = {}
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
