package de.badaix.snapcast.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.badaix.snapcast.domain.model.DiscoveredServer
import de.badaix.snapcast.domain.model.ServerConfiguration
import de.badaix.snapcast.ui.theme.SnapdroidTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerConfigurationBottomSheet(
    onDismiss: () -> Unit,
    onSaveConfiguration: (ServerConfiguration) -> Unit,
    discoveredServers: List<DiscoveredServer>,
    isDiscovering: Boolean,
    onStartDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit,
    initialConfiguration: ServerConfiguration? = null,
    initiallyShowManualEntry: Boolean = false,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var host by remember { mutableStateOf(initialConfiguration?.host ?: "") }
    var streamPort by remember { mutableStateOf(initialConfiguration?.streamPort?.toString() ?: ServerConfiguration.DEFAULT_STREAM_PORT.toString()) }
    var controlPort by remember { mutableStateOf(initialConfiguration?.controlPort?.toString() ?: ServerConfiguration.DEFAULT_CONTROL_PORT.toString()) }
    var showManualEntry by remember { mutableStateOf(initiallyShowManualEntry || initialConfiguration != null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Configure Server",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (!showManualEntry) {
                // mDNS Discovery Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Discovered Servers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    if (isDiscovering) {
                        OutlinedButton(onClick = onStopDiscovery) {
                            Text("Stop")
                        }
                    } else {
                        Button(onClick = onStartDiscovery) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text("Search")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isDiscovering) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Searching for Snapcast servers via mDNS...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (discoveredServers.isEmpty() && !isDiscovering) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "No servers found. Tap 'Search' to scan the network.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(discoveredServers) { server ->
                            DiscoveredServerCard(
                                server = server,
                                onClick = {
                                    onSaveConfiguration(
                                        ServerConfiguration(
                                            host = server.host,
                                            streamPort = server.port,
                                            controlPort = server.port + 1
                                        )
                                    )
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                HorizontalDivider()
                
                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = { showManualEntry = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enter Server Manually")
                }
            } else {
                // Manual Entry Section
                Text(
                    text = "Manual Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Server Host") },
                    placeholder = { Text("e.g., 192.168.1.100") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = streamPort,
                        onValueChange = { streamPort = it },
                        label = { Text("Stream Port") },
                        placeholder = { Text("1704") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = controlPort,
                        onValueChange = { controlPort = it },
                        label = { Text("Control Port") },
                        placeholder = { Text("1705") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (initialConfiguration == null) {
                    TextButton(
                        onClick = { showManualEntry = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Back to Discovery")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val streamPortInt = streamPort.toIntOrNull() ?: ServerConfiguration.DEFAULT_STREAM_PORT
                            val controlPortInt = controlPort.toIntOrNull() ?: ServerConfiguration.DEFAULT_CONTROL_PORT
                            
                            if (host.isNotBlank()) {
                                onSaveConfiguration(
                                    ServerConfiguration(
                                        host = host.trim(),
                                        streamPort = streamPortInt,
                                        controlPort = controlPortInt
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = host.isNotBlank()
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("Save")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DiscoveredServerCard(
    server: DiscoveredServer,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = server.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${server.host}:${server.port}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun ServerConfigurationBottomSheetPreview() {
    SnapdroidTheme {
        ServerConfigurationBottomSheet(
            onDismiss = {},
            onSaveConfiguration = {},
            discoveredServers = listOf(
                DiscoveredServer("Snapcast Server", "192.168.1.100", 1704),
                DiscoveredServer("Snapcast #2", "192.168.1.101", 1704)
            ),
            isDiscovering = false,
            onStartDiscovery = {},
            onStopDiscovery = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun ServerConfigurationBottomSheetDiscoveringPreview() {
    SnapdroidTheme {
        ServerConfigurationBottomSheet(
            onDismiss = {},
            onSaveConfiguration = {},
            discoveredServers = emptyList(),
            isDiscovering = true,
            onStartDiscovery = {},
            onStopDiscovery = {}
        )
    }
}

