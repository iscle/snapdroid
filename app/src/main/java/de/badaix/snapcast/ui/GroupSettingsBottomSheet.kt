package de.badaix.snapcast.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
    groupName: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
        ) {
            Text(
                text = "Group Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = GroupSettingsBottomSheetDefaults.TITLE_BOTTOM_PADDING.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = GroupSettingsBottomSheetDefaults.DIVIDER_VERTICAL_PADDING.dp)
            )

            Text(
                text = "Group: $groupName",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = GroupSettingsBottomSheetDefaults.VERTICAL_PADDING.dp)
            )

            // TODO: Add group-specific settings here
            // - Stream selection
            // - Group name editing
            // - Other group settings

            Spacer(modifier = Modifier.height(GroupSettingsBottomSheetDefaults.BOTTOM_SPACER.dp))
        }
    }
}

@Preview(name = "Group Settings Bottom Sheet", showBackground = true)
@Composable
private fun GroupSettingsBottomSheetPreview() {
    SnapdroidTheme {
        GroupSettingsBottomSheet(
            groupName = "Living Room",
            onDismiss = {}
        )
    }
}

