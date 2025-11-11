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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.badaix.snapcast.data.model.Client
import de.badaix.snapcast.data.model.ClientConfig
import de.badaix.snapcast.data.model.Host
import de.badaix.snapcast.data.model.LastSeen
import de.badaix.snapcast.data.model.Snapclient
import de.badaix.snapcast.data.model.Volume
import de.badaix.snapcast.ui.theme.SnapdroidTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private object DeviceSettingsBottomSheetDefaults {
    const val HORIZONTAL_PADDING = 16
    const val VERTICAL_PADDING = 8
    const val TITLE_BOTTOM_PADDING = 8
    const val DIVIDER_VERTICAL_PADDING = 8
    const val BOTTOM_SPACER = 16
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSettingsBottomSheet(
    client: Client,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val deviceName = client.config.name.ifEmpty { 
        client.host.name.ifEmpty { client.id }
    }
    
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = DeviceSettingsBottomSheetDefaults.HORIZONTAL_PADDING.dp,
                    vertical = DeviceSettingsBottomSheetDefaults.VERTICAL_PADDING.dp
                )
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Device Details",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = DeviceSettingsBottomSheetDefaults.TITLE_BOTTOM_PADDING.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = DeviceSettingsBottomSheetDefaults.DIVIDER_VERTICAL_PADDING.dp)
            )

            // Device Name
            DetailRow(label = "Name", value = deviceName)
            
            // Connection Status
            DetailRow(
                label = "Status", 
                value = if (client.connected) "Connected" else "Disconnected"
            )

            // Client ID
            DetailRow(label = "Client ID", value = client.id)

            HorizontalDivider(
                modifier = Modifier.padding(vertical = DeviceSettingsBottomSheetDefaults.DIVIDER_VERTICAL_PADDING.dp)
            )

            // Audio Configuration
            Text(
                text = "Audio Configuration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = DeviceSettingsBottomSheetDefaults.VERTICAL_PADDING.dp)
            )

            DetailRow(label = "Volume", value = "${client.config.volume.percent}%")
            DetailRow(label = "Muted", value = if (client.config.volume.muted) "Yes" else "No")
            DetailRow(label = "Latency", value = "${client.config.latency} ms")
            DetailRow(label = "Instance", value = client.config.instance.toString())

            HorizontalDivider(
                modifier = Modifier.padding(vertical = DeviceSettingsBottomSheetDefaults.DIVIDER_VERTICAL_PADDING.dp)
            )

            // Host Information
            Text(
                text = "Host Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = DeviceSettingsBottomSheetDefaults.VERTICAL_PADDING.dp)
            )

            DetailRow(label = "Hostname", value = client.host.name)
            DetailRow(label = "IP Address", value = client.host.ip)
            DetailRow(label = "MAC Address", value = client.host.mac)
            DetailRow(label = "Architecture", value = client.host.arch)
            DetailRow(label = "OS", value = client.host.os)

            HorizontalDivider(
                modifier = Modifier.padding(vertical = DeviceSettingsBottomSheetDefaults.DIVIDER_VERTICAL_PADDING.dp)
            )

            // Snapclient Information
            Text(
                text = "Client Software",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = DeviceSettingsBottomSheetDefaults.VERTICAL_PADDING.dp)
            )

            DetailRow(label = "Name", value = client.snapclient.name)
            DetailRow(label = "Version", value = client.snapclient.version)
            DetailRow(label = "Protocol", value = "v${client.snapclient.protocolVersion}")

            HorizontalDivider(
                modifier = Modifier.padding(vertical = DeviceSettingsBottomSheetDefaults.DIVIDER_VERTICAL_PADDING.dp)
            )

            // Last Seen
            val lastSeenDate = Date(client.lastSeen.sec * 1000L)
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
            DetailRow(label = "Last Seen", value = dateFormat.format(lastSeenDate))

            HorizontalDivider(
                modifier = Modifier.padding(vertical = DeviceSettingsBottomSheetDefaults.DIVIDER_VERTICAL_PADDING.dp)
            )

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = DeviceSettingsBottomSheetDefaults.VERTICAL_PADDING.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { showRenameDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Rename")
                }
                
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Delete")
                }
            }

            Spacer(modifier = Modifier.height(DeviceSettingsBottomSheetDefaults.BOTTOM_SPACER.dp))
        }
    }
    
    // Rename Dialog
    if (showRenameDialog) {
        RenameDialog(
            title = "Rename Device",
            currentName = client.config.name,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                onRename(newName)
                showRenameDialog = false
                onDismiss()
            }
        )
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Device?") },
            text = { Text("Are you sure you want to delete '$deviceName'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                        onDismiss()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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

@Composable
private fun RenameDialog(
    title: String,
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(name = "Device Settings Bottom Sheet", showBackground = true)
@Composable
private fun DeviceSettingsBottomSheetPreview() {
    SnapdroidTheme {
        DeviceSettingsBottomSheet(
            client = Client(
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
                id = "12345678-1234-1234-1234-123456789abc",
                lastSeen = LastSeen(sec = System.currentTimeMillis() / 1000, usec = 0),
                snapclient = Snapclient(
                    name = "Snapclient",
                    protocolVersion = 2,
                    version = "0.34.0"
                )
            ),
            onDismiss = {}
        )
    }
}

