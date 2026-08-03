package compress.joshattic.us.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import compress.joshattic.us.R
import compress.joshattic.us.model.CompressorUiState
import compress.joshattic.us.viewmodel.CompressorViewModel

@Composable
fun TargetSizeWarning(state: CompressorUiState, viewModel: CompressorViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    val warningColor = Color(0xFFF2B233)

    OutlinedButton(
        onClick = { showDialog = true },
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .padding(vertical = 4.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = warningColor.copy(alpha = 0.14f),
            contentColor = warningColor
        ),
        border = BorderStroke(1.dp, warningColor),
        shape = RoundedCornerShape(24.dp)
    ) {
        Icon(
            Icons.Outlined.WarningAmber,
            contentDescription = null,
            tint = warningColor
        )
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(R.string.target_size_warning),
            color = warningColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }

    if (showDialog) {
        val suggestion = state.suggestedForTarget()
        val hasResolutionSuggestion = suggestion.targetResolutionHeight != state.targetResolutionHeight
        val hasFpsSuggestion = suggestion.targetFps != state.targetFps
        val hasAudioSuggestion = !state.removeAudio && suggestion.audioBitrate != state.audioBitrate

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.target_size_warning_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.target_size_warning_details))
                    if (hasResolutionSuggestion) {
                        SuggestionRow(
                            current = resolutionLabel(state),
                            suggested = resolutionLabel(suggestion),
                            onAccept = viewModel::acceptSuggestedResolution
                        )
                    }
                    if (hasFpsSuggestion) {
                        SuggestionRow(
                            current = "${effectiveFps(state)}fps",
                            suggested = "${effectiveFps(suggestion)}fps",
                            onAccept = viewModel::acceptSuggestedFps
                        )
                    }
                    if (hasAudioSuggestion) {
                        SuggestionRow(
                            current = audioLabel(state.audioBitrate, state.originalAudioBitrate),
                            suggested = audioLabel(suggestion.audioBitrate, state.originalAudioBitrate),
                            onAccept = viewModel::acceptSuggestedAudioBitrate
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.acceptAllSuggestions()
                    showDialog = false
                }) {
                    Text(stringResource(R.string.accept_all))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

@Composable
private fun SuggestionRow(current: String, suggested: String, onAccept: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            ) {
                Text(current, modifier = Modifier.padding(10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(suggested, modifier = Modifier.padding(10.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        OutlinedButton(onClick = onAccept) {
            Text(stringResource(R.string.accept_suggestion))
        }
    }
}

private fun resolutionLabel(state: CompressorUiState): String {
    val height = if (state.targetResolutionHeight > 0) state.targetResolutionHeight else state.originalHeight
    val width = if (state.originalHeight > 0) (state.originalWidth.toFloat() / state.originalHeight * height).toInt() else 0
    return "${width}x${height}"
}

private fun effectiveFps(state: CompressorUiState): Int =
    if (state.targetFps > 0) state.targetFps else state.originalFps.toInt()

private fun audioLabel(bitrate: Int, original: Int): String =
    if (bitrate == 0) "Original${if (original > 0) " • ${original / 1000}k" else ""}" else "${bitrate / 1000}k"
