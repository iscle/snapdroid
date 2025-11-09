package de.badaix.snapcast.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import de.badaix.snapcast.domain.model.PlayerLogEntry
import de.badaix.snapcast.ui.theme.SnapdroidTheme
import kotlinx.coroutines.delay

private object PlayerLogsScreenDefaults {
    const val CONTENT_PADDING = 16
    const val LOG_ITEM_SPACING = 4
    const val LOG_PADDING = 8
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerLogsScreen(
    onNavigateBack: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val playerLogs by viewModel.playerLogs.collectAsState()
    val listState = rememberLazyListState()
    
    // Simple flag: has the user manually scrolled up?
    var userHasScrolledUp by remember { mutableStateOf(false) }
    
    // Check if user is at the bottom (within 2 items of the end)
    val isAtBottom = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty() || layoutInfo.totalItemsCount == 0) {
                true
            } else {
                val lastVisibleItem = visibleItems.last()
                val lastItemIndex = layoutInfo.totalItemsCount - 1
                lastVisibleItem.index >= lastItemIndex - 2
            }
        }
    }

    // Auto-scroll to bottom when new logs arrive (if user hasn't scrolled up)
    LaunchedEffect(playerLogs.size) {
        if (playerLogs.isNotEmpty() && !userHasScrolledUp) {
            delay(50) // Small delay to ensure item is added
            listState.scrollToItem(playerLogs.size - 1)
        }
    }

    // Detect when user scrolls
    LaunchedEffect(listState.isScrollInProgress) {
        // Only check when scroll ends (user finished scrolling)
        if (!listState.isScrollInProgress) {
            val atBottom = isAtBottom.value
            
            if (!atBottom && !userHasScrolledUp) {
                // User scrolled away from bottom
                userHasScrolledUp = true
            } else if (atBottom && userHasScrolledUp) {
                // User scrolled back to bottom
                userHasScrolledUp = false
                // Scroll to absolute bottom to ensure we're there
                if (playerLogs.isNotEmpty()) {
                    listState.scrollToItem(playerLogs.size - 1)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Player Logs") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(PlayerLogsScreenDefaults.CONTENT_PADDING.dp),
            verticalArrangement = Arrangement.spacedBy(PlayerLogsScreenDefaults.LOG_ITEM_SPACING.dp)
        ) {
            if (playerLogs.isEmpty()) {
                item {
                    Text(
                        text = "No logs available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(PlayerLogsScreenDefaults.LOG_PADDING.dp)
                    )
                }
            } else {
                items(playerLogs.size) { index ->
                    LogEntryItem(log = playerLogs[index])
                }
            }
        }
    }
}

@Composable
private fun LogEntryItem(log: PlayerLogEntry) {
    Text(
        text = "[${log.timestamp}] [${log.severity}] ${log.tag}: ${log.message}",
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        color = log.getSeverityColor(),
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.shapes.small
            )
            .padding(PlayerLogsScreenDefaults.LOG_PADDING.dp)
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

@Preview(name = "Player Logs Screen - Empty", showBackground = true)
@Composable
private fun PlayerLogsScreenEmptyPreview() {
    SnapdroidTheme {
        PlayerLogsScreen(onNavigateBack = {})
    }
}

@Preview(name = "Player Logs Screen - With Logs", showBackground = true)
@Composable
private fun PlayerLogsScreenWithLogsPreview() {
    SnapdroidTheme {
        // Note: This preview won't show logs since we can't inject a ViewModel with test data
        // In a real scenario, you'd use a preview parameter or test ViewModel
        PlayerLogsScreen(onNavigateBack = {})
    }
}

