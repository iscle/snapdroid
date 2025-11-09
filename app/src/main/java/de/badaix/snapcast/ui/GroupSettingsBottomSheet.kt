package de.badaix.snapcast.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.badaix.snapcast.data.model.Client
import de.badaix.snapcast.data.model.ClientConfig
import de.badaix.snapcast.data.model.Group
import de.badaix.snapcast.data.model.Host
import de.badaix.snapcast.data.model.LastSeen
import de.badaix.snapcast.data.model.Snapclient
import de.badaix.snapcast.data.model.Volume
import de.badaix.snapcast.ui.theme.SnapdroidTheme

private object GroupSettingsBottomSheetDefaults {
    const val HORIZONTAL_PADDING = 16
    const val VERTICAL_PADDING = 8
    const val TITLE_BOTTOM_PADDING = 8
    const val DIVIDER_VERTICAL_PADDING = 8
    const val BOTTOM_SPACER = 16
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSettingsBottomSheet(
    group: Group,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val groupName = group.name.ifEmpty { 
        group.streamId.ifEmpty { group.id }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = GroupSettingsBottomSheetDefaults.HORIZONTAL_PADDING.dp,
                    vertical = GroupSettingsBottomSheetDefaults.VERTICAL_PADDING.dp
                )
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Group Details",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = GroupSettingsBottomSheetDefaults.TITLE_BOTTOM_PADDING.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = GroupSettingsBottomSheetDefaults.DIVIDER_VERTICAL_PADDING.dp)
            )

            // Group Name
            DetailRow(label = "Name", value = groupName)
            
            // Stream
            DetailRow(label = "Stream", value = group.streamId)
            
            // Muted Status
            DetailRow(label = "Muted", value = if (group.muted) "Yes" else "No")
            
            // Group ID
            DetailRow(label = "Group ID", value = group.id)

            HorizontalDivider(
                modifier = Modifier.padding(vertical = GroupSettingsBottomSheetDefaults.DIVIDER_VERTICAL_PADDING.dp)
            )

            // Clients
            Text(
                text = "Clients (${group.clients.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = GroupSettingsBottomSheetDefaults.VERTICAL_PADDING.dp)
            )

            group.clients.forEach { client ->
                val clientName = client.config.name.ifEmpty { 
                    client.host.name.ifEmpty { client.id }
                }
                val status = if (client.connected) "Connected" else "Disconnected"
                DetailRow(label = clientName, value = status)
            }

            Spacer(modifier = Modifier.height(GroupSettingsBottomSheetDefaults.BOTTOM_SPACER.dp))
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(name = "Group Settings Bottom Sheet", showBackground = true)
@Composable
private fun GroupSettingsBottomSheetPreview() {
    SnapdroidTheme {
        GroupSettingsBottomSheet(
            group = Group(
                clients = listOf(
                    Client(
                        config = ClientConfig(
                            instance = 1,
                            latency = 0,
                            name = "Kitchen Speaker",
                            volume = Volume(muted = false, percent = 75)
                        ),
                        connected = true,
                        host = Host(
                            arch = "arm64-v8a",
                            ip = "192.168.1.100",
                            mac = "00:00:00:00:00:00",
                            name = "kitchen-device",
                            os = "Android 15"
                        ),
                        id = "client-1",
                        lastSeen = LastSeen(sec = System.currentTimeMillis() / 1000, usec = 0),
                        snapclient = Snapclient(
                            name = "Snapclient",
                            protocolVersion = 2,
                            version = "0.34.0"
                        )
                    ),
                    Client(
                        config = ClientConfig(
                            instance = 1,
                            latency = 0,
                            name = "",
                            volume = Volume(muted = false, percent = 100)
                        ),
                        connected = false,
                        host = Host(
                            arch = "x86_64",
                            ip = "192.168.1.101",
                            mac = "aa:bb:cc:dd:ee:ff",
                            name = "bedroom-pc",
                            os = "Linux"
                        ),
                        id = "client-2",
                        lastSeen = LastSeen(sec = System.currentTimeMillis() / 1000 - 3600, usec = 0),
                        snapclient = Snapclient(
                            name = "Snapclient",
                            protocolVersion = 2,
                            version = "0.34.0"
                        )
                    )
                ),
                id = "group-id-123",
                muted = false,
                name = "Living Room",
                streamId = "default"
            ),
            onDismiss = {}
        )
    }
}

