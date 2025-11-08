package de.badaix.snapcast.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.badaix.snapcast.domain.model.PlayerState
import de.badaix.snapcast.ui.MainViewModel.ConnectionInfo
import de.badaix.snapcast.ui.theme.SnapdroidTheme

private object PlayerStatusCardDefaults {
    const val CARD_PADDING = 16
    const val VERTICAL_SPACING = 8
}

@Composable
fun PlayerStatusCard(
    playerState: PlayerState,
    connectionInfo: ConnectionInfo?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (playerState) {
                is PlayerState.Running -> MaterialTheme.colorScheme.primaryContainer
                is PlayerState.Starting -> MaterialTheme.colorScheme.secondaryContainer
                is PlayerState.Error -> MaterialTheme.colorScheme.errorContainer
                is PlayerState.Idle -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PlayerStatusCardDefaults.CARD_PADDING.dp),
            verticalArrangement = Arrangement.spacedBy(PlayerStatusCardDefaults.VERTICAL_SPACING.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Player Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = playerState.getDisplayText(),
                    style = MaterialTheme.typography.labelLarge,
                    color = playerState.getStatusColor()
                )
            }

            connectionInfo?.let { info ->
                Text(
                    text = "Server: ${info.host}:${info.port}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (playerState is PlayerState.Error) {
                Text(
                    text = playerState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun PlayerState.getDisplayText(): String = when (this) {
    is PlayerState.Running -> "Running"
    is PlayerState.Starting -> "Starting..."
    is PlayerState.Error -> "Error"
    is PlayerState.Idle -> "Idle"
}

@Composable
private fun PlayerState.getStatusColor(): androidx.compose.ui.graphics.Color = when (this) {
    is PlayerState.Running -> MaterialTheme.colorScheme.primary
    is PlayerState.Starting -> MaterialTheme.colorScheme.secondary
    is PlayerState.Error -> MaterialTheme.colorScheme.error
    is PlayerState.Idle -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Preview(name = "Idle State", showBackground = true)
@Composable
private fun PlayerStatusCardIdlePreview() {
    SnapdroidTheme {
        PlayerStatusCard(
            playerState = PlayerState.Idle,
            connectionInfo = null
        )
    }
}

@Preview(name = "Starting State", showBackground = true)
@Composable
private fun PlayerStatusCardStartingPreview() {
    SnapdroidTheme {
        PlayerStatusCard(
            playerState = PlayerState.Starting,
            connectionInfo = ConnectionInfo("localhost", 1704)
        )
    }
}

@Preview(name = "Running State", showBackground = true)
@Composable
private fun PlayerStatusCardRunningPreview() {
    SnapdroidTheme {
        PlayerStatusCard(
            playerState = PlayerState.Running,
            connectionInfo = ConnectionInfo("192.168.1.100", 1704)
        )
    }
}

@Preview(name = "Error State", showBackground = true)
@Composable
private fun PlayerStatusCardErrorPreview() {
    SnapdroidTheme {
        PlayerStatusCard(
            playerState = PlayerState.Error("Failed to connect to server"),
            connectionInfo = ConnectionInfo("192.168.1.100", 1704)
        )
    }
}

