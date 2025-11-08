package de.badaix.snapcast.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.badaix.snapcast.domain.model.PlayerLogEntry
import de.badaix.snapcast.ui.theme.SnapdroidTheme

private object PlayerLogsCardDefaults {
    const val CARD_PADDING = 16
    const val TITLE_BOTTOM_PADDING = 8
    const val LOGS_HEIGHT = 200
    const val LOGS_PADDING = 8
    const val LOG_ITEM_SPACING = 4
    const val MAX_DISPLAYED_LOGS = 50
}

@Composable
fun PlayerLogsCard(
    logs: List<PlayerLogEntry>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PlayerLogsCardDefaults.CARD_PADDING.dp)
        ) {
            Text(
                text = "Player Logs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = PlayerLogsCardDefaults.TITLE_BOTTOM_PADDING.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PlayerLogsCardDefaults.LOGS_HEIGHT.dp)
                    .background(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.shapes.small
                    )
                    .padding(PlayerLogsCardDefaults.LOGS_PADDING.dp),
                verticalArrangement = Arrangement.spacedBy(PlayerLogsCardDefaults.LOG_ITEM_SPACING.dp)
            ) {
                if (logs.isEmpty()) {
                    item {
                        Text(
                            text = "No logs available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(logs.takeLast(PlayerLogsCardDefaults.MAX_DISPLAYED_LOGS)) { log ->
                        LogEntryItem(log = log)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogEntryItem(log: PlayerLogEntry) {
    Text(
        text = "[${log.severity}] ${log.tag}: ${log.message}",
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        color = log.getSeverityColor()
    )
}

@Composable
private fun PlayerLogEntry.getSeverityColor(): androidx.compose.ui.graphics.Color = when (severity.lowercase()) {
    "error", "fatal" -> MaterialTheme.colorScheme.error
    "warning", "warn" -> MaterialTheme.colorScheme.errorContainer
    "info" -> MaterialTheme.colorScheme.primary
    "debug" -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.onSurface
}

@Preview(name = "Empty Logs", showBackground = true)
@Composable
private fun PlayerLogsCardEmptyPreview() {
    SnapdroidTheme {
        PlayerLogsCard(logs = emptyList())
    }
}

@Preview(name = "With Logs", showBackground = true)
@Composable
private fun PlayerLogsCardWithLogsPreview() {
    SnapdroidTheme {
        PlayerLogsCard(
            logs = listOf(
                PlayerLogEntry("10:30:15", "INFO", "SnapcastClient", "Connecting to server..."),
                PlayerLogEntry("10:30:16", "DEBUG", "SnapcastClient", "Socket connection established"),
                PlayerLogEntry("10:30:17", "INFO", "SnapcastClient", "Handshake completed"),
                PlayerLogEntry("10:30:18", "WARNING", "SnapcastClient", "High latency detected: 150ms"),
                PlayerLogEntry("10:30:19", "ERROR", "SnapcastClient", "Failed to decode audio frame"),
                PlayerLogEntry("10:30:20", "INFO", "SnapcastClient", "Reconnecting..."),
                PlayerLogEntry("10:30:21", "DEBUG", "SnapcastClient", "Buffer status: 80%"),
                PlayerLogEntry("10:30:22", "INFO", "SnapcastClient", "Stream started successfully")
            )
        )
    }
}

