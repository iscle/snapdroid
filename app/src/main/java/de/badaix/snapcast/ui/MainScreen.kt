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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.badaix.snapcast.ui.component.GroupCard
import de.badaix.snapcast.ui.component.GroupCardSink

@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    var started by remember { mutableStateOf(false) }
    var groupSettingsOpen by remember { mutableStateOf(false) }
    var deviceSettingsOpen by remember { mutableStateOf(false) }
    var selectedGroupName by remember { mutableStateOf<String?>(null) }
    var selectedDeviceName by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            MainTopBar(
                subtitle = "rk3318-box",
                onSettingsClick = { /* TODO */ }
            )
        },
        floatingActionButton = {
            MainFloatingActionButton(
                started = started,
                onStart = {
                    started = true
                },
                onStop = {
                    started = false
                }
            )
        }
    ) { innerPadding ->
        MainContent(
            modifier = Modifier.padding(innerPadding),
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
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text("Snapcast", maxLines = 1, overflow = TextOverflow.Ellipsis)
                subtitle?.let {
                    Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        actions = {
            IconButton(
                onClick = onSettingsClick
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings"
                )
            }
        }
    )
}

@Composable
private fun MainFloatingActionButton(
    started: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    FloatingActionButton(
        onClick = {
            if (started) {
                onStop()
            } else {
                onStart()
            }
        },
    ) {
        if (started) {
            Icon(Icons.Filled.Stop, "Stop")
        } else {
            Icon(Icons.Filled.PlayArrow, "Start")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    modifier: Modifier = Modifier,
    onGroupSettingsClick: (String) -> Unit,
    onDeviceSettingsClick: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(3) { index ->
            val groupName = "Group $index"
            GroupCard(
                name = groupName,
                isMuted = false,
                onIsMutedChange = {  },
                volume = 0.5f,
                onVolumeChange = {  },
                onSettingsClick = { onGroupSettingsClick(groupName) },
                sinks = {
                    for (i in 1..3) {
                        val deviceName = "Sink $i"
                        GroupCardSink(
                            name = deviceName,
                            isMuted = false,
                            onIsMutedChange = {  },
                            volume = 0.5f,
                            onVolumeChange = {  },
                            onSettingsClick = { onDeviceSettingsClick(deviceName) }
                        )
                    }
                }
            )
        }
    }
}
