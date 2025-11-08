package de.badaix.snapcast.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.badaix.snapcast.ui.theme.SnapdroidTheme

private object GroupCardDefaults {
    const val CARD_PADDING = 16
    const val VERTICAL_SPACING = 16
    const val VOLUME_SLIDER_STEPS = 99 // 100 values: 0, 1, 2, ..., 100 → 99 intervals
}

@Composable
fun GroupCard(
    name: String,
    isMuted: Boolean,
    onIsMutedChange: (Boolean) -> Unit,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    sinks: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(GroupCardDefaults.CARD_PADDING.dp),
            verticalArrangement = Arrangement.spacedBy(GroupCardDefaults.VERTICAL_SPACING.dp)
        ) {
            GroupCardHeader(
                name = name,
                isMuted = isMuted,
                onIsMutedChange = onIsMutedChange,
                volume = volume,
                onVolumeChange = onVolumeChange,
                onSettingsClick = onSettingsClick
            )

            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            sinks()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupCardHeader(
    name: String,
    isMuted: Boolean,
    onIsMutedChange: (Boolean) -> Unit,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    onSettingsClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            Row {
                IconButton(
                    onClick = { onIsMutedChange(!isMuted) }
                ) {
                    if (isMuted) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Mute"
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = "Unmute"
                        )
                    }
                }

                IconButton(
                    onClick = onSettingsClick
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More"
                    )
                }
            }
        }

        CustomSlider(
            value = volume,
            onValueChange = onVolumeChange,
            steps = GroupCardDefaults.VOLUME_SLIDER_STEPS,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupCardSink(
    name: String,
    isMuted: Boolean,
    onIsMutedChange: (Boolean) -> Unit,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    onSettingsClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Speaker,
                contentDescription = null,
            )

            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            Row {
                IconButton(
                    onClick = { onIsMutedChange(!isMuted) }
                ) {
                    if (isMuted) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Mute"
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = "Unmute"
                        )
                    }
                }

                IconButton(
                    onClick = onSettingsClick
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More"
                    )
                }
            }
        }

        CustomSlider(
            value = volume,
            onValueChange = onVolumeChange,
            steps = GroupCardDefaults.VOLUME_SLIDER_STEPS,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(name = "Group Card - Unmuted", showBackground = true)
@Composable
private fun GroupCardPreview() {
    SnapdroidTheme {
        GroupCard(
            name = "Living Room",
            isMuted = false,
            onIsMutedChange = {},
            volume = 0.75f,
            onVolumeChange = {},
            onSettingsClick = {},
            sinks = {
                GroupCardSink(
                    name = "Speaker 1",
                    isMuted = false,
                    onIsMutedChange = {},
                    volume = 0.8f,
                    onVolumeChange = {},
                    onSettingsClick = {}
                )
                GroupCardSink(
                    name = "Speaker 2",
                    isMuted = false,
                    onIsMutedChange = {},
                    volume = 0.7f,
                    onVolumeChange = {},
                    onSettingsClick = {}
                )
            }
        )
    }
}

@Preview(name = "Group Card - Muted", showBackground = true)
@Composable
private fun GroupCardMutedPreview() {
    SnapdroidTheme {
        GroupCard(
            name = "Bedroom",
            isMuted = true,
            onIsMutedChange = {},
            volume = 0.5f,
            onVolumeChange = {},
            onSettingsClick = {},
            sinks = {
                GroupCardSink(
                    name = "Bedroom Speaker",
                    isMuted = true,
                    onIsMutedChange = {},
                    volume = 0.5f,
                    onVolumeChange = {},
                    onSettingsClick = {}
                )
            }
        )
    }
}

@Preview(name = "Group Card Sink", showBackground = true)
@Composable
private fun GroupCardSinkPreview() {
    SnapdroidTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GroupCardSink(
                name = "Kitchen Speaker",
                isMuted = false,
                onIsMutedChange = {},
                volume = 0.6f,
                onVolumeChange = {},
                onSettingsClick = {}
            )
        }
    }
}
