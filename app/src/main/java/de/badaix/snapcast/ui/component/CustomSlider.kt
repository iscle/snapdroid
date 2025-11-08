package de.badaix.snapcast.ui.component

import androidx.annotation.IntRange
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.badaix.snapcast.ui.theme.SnapdroidTheme

private object CustomSliderDefaults {
    const val THUMB_SIZE = 20
    const val TRACK_HEIGHT = 4
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    @IntRange(from = 0) steps: Int = 0,
    thumb: @Composable (SliderState) -> Unit = {
        val colors = SliderDefaults.colors()
        Canvas(modifier = Modifier.size(CustomSliderDefaults.THUMB_SIZE.dp)) {
            drawCircle(color = colors.thumbColor)
        }
    },
    track: @Composable (SliderState) -> Unit = { sliderState ->
        val colors = SliderDefaults.colors()
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(CustomSliderDefaults.TRACK_HEIGHT.dp)
        ) {
            val fraction = sliderState.coercedValueAsFraction

            // Position where active track ends (thumb center)
            val thumbCenterX = fraction * size.width

            val centerY = size.height / 2
            val strokeWidth = size.height

            // Active track: from left edge (adjusted) to thumb center
            drawLine(
                color = colors.activeTrackColor,
                start = Offset(0f, centerY),
                end = Offset(thumbCenterX, centerY),
                strokeWidth = strokeWidth
            )

            // Inactive track: from thumb center to right edge (adjusted)
            drawLine(
                color = colors.disabledInactiveTrackColor,
                start = Offset(thumbCenterX, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = strokeWidth
            )
        }
    },
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        interactionSource = interactionSource,
        steps = steps, // 100 values: 0, 1, 2, ..., 100 → 99 intervals between them
        thumb = thumb,
        track = track,
        valueRange = valueRange
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Custom Slider - Default", showBackground = true)
@Composable
private fun CustomSliderPreview() {
    SnapdroidTheme {
        var value by remember { mutableFloatStateOf(0.5f) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Volume: ${(value * 100).toInt()}%")
            CustomSlider(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Custom Slider - With Steps", showBackground = true)
@Composable
private fun CustomSliderWithStepsPreview() {
    SnapdroidTheme {
        var value by remember { mutableFloatStateOf(0.75f) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Volume: ${(value * 100).toInt()}%")
            CustomSlider(
                value = value,
                onValueChange = { value = it },
                steps = 99,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Custom Slider - Disabled", showBackground = true)
@Composable
private fun CustomSliderDisabledPreview() {
    SnapdroidTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Volume: 50%")
            CustomSlider(
                value = 0.5f,
                onValueChange = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}