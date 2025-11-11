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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Alignment
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
    allGroups: List<Group>,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit = {},
    onUpdateClients: (List<String>) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val groupName = group.name.ifEmpty { 
        group.streamId.ifEmpty { group.id }
    }
    
    var showRenameDialog by remember { mutableStateOf(false) }
    var showManageClientsDialog by remember { mutableStateOf(false) }

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

            HorizontalDivider(
                modifier = Modifier.padding(vertical = GroupSettingsBottomSheetDefaults.DIVIDER_VERTICAL_PADDING.dp)
            )

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = GroupSettingsBottomSheetDefaults.VERTICAL_PADDING.dp),
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
                
                FilledTonalButton(
                    onClick = { showManageClientsDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = "Manage Clients",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Manage")
                }
            }

            Spacer(modifier = Modifier.height(GroupSettingsBottomSheetDefaults.BOTTOM_SPACER.dp))
        }
    }
    
    // Rename Dialog
    if (showRenameDialog) {
        RenameDialog(
            title = "Rename Group",
            currentName = group.name,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                onRename(newName)
                showRenameDialog = false
                onDismiss()
            }
        )
    }
    
    // Manage Clients Dialog
    if (showManageClientsDialog) {
        ManageClientsDialog(
            group = group,
            allGroups = allGroups,
            onDismiss = { showManageClientsDialog = false },
            onConfirm = { clientIds ->
                onUpdateClients(clientIds)
                showManageClientsDialog = false
                onDismiss()
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

@Composable
private fun ManageClientsDialog(
    group: Group,
    allGroups: List<Group>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    // Get all clients from all groups
    val allClients = allGroups.flatMap { it.clients }.distinctBy { it.id }
    
    // Start with current group's client IDs
    var selectedClientIds by remember { 
        mutableStateOf(group.clients.map { it.id }.toSet())
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Group Clients") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Select which clients belong to this group:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                allClients.forEach { client ->
                    val clientName = client.config.name.ifEmpty { 
                        client.host.name.ifEmpty { client.id }
                    }
                    val isSelected = selectedClientIds.contains(client.id)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                selectedClientIds = if (checked) {
                                    selectedClientIds + client.id
                                } else {
                                    selectedClientIds - client.id
                                }
                            }
                        )
                        Text(
                            text = clientName,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedClientIds.toList()) }
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
            allGroups = listOf(), // Empty for preview simplicity
            onDismiss = {}
        )
    }
}

