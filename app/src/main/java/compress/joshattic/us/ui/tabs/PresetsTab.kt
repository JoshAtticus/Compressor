package compress.joshattic.us.ui.tabs

import compress.joshattic.us.model.getLocalizedLabel
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import compress.joshattic.us.R
import compress.joshattic.us.model.CompressorUiState
import compress.joshattic.us.model.QualityPreset
import compress.joshattic.us.utils.ExpressiveSpatialSpring
import compress.joshattic.us.utils.expressiveScale
import compress.joshattic.us.viewmodel.CompressorViewModel

@Composable
fun PresetsTab(state: CompressorUiState, viewModel: CompressorViewModel) {
    val scrollState = rememberScrollState()
    val haptics = LocalHapticFeedback.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .padding(bottom = 80.dp)
    ) {
        
        Text(
            stringResource(R.string.quality_preset),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val presets = listOf(
            Triple(QualityPreset.HIGH, state.highPresetConfig.label ?: stringResource(R.string.preset_high), stringResource(R.string.preset_high_desc)),
            Triple(QualityPreset.MEDIUM, state.mediumPresetConfig.label ?: stringResource(R.string.preset_medium), stringResource(R.string.preset_medium_desc)),
            Triple(QualityPreset.LOW, state.lowPresetConfig.label ?: stringResource(R.string.preset_low), stringResource(R.string.preset_low_desc))
        )
        
        presets.forEach { (preset, title, sub) ->
            val selected = state.activePreset == preset
            val isEnabled = when(preset) {
                QualityPreset.MEDIUM -> state.originalHeight >= 1080 
                QualityPreset.LOW -> state.originalHeight >= 720
                else -> true
            }

            val selectionScale by animateFloatAsState(if (selected) 1.02f else 1f, animationSpec = ExpressiveSpatialSpring, label = "selectionScale")
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val pressScale by animateFloatAsState(if (isPressed) 0.96f else 1f, animationSpec = ExpressiveSpatialSpring, label = "pressScale")
            
            OutlinedCard(
                onClick = { 
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.applyPreset(preset) 
                },
                enabled = isEnabled,
                shape = RoundedCornerShape(24.dp),
                interactionSource = interactionSource,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .graphicsLayer { 
                        scaleX = selectionScale * pressScale
                        scaleY = selectionScale * pressScale
                    },
                colors = if (selected) {
                    CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                } else {
                    CardDefaults.outlinedCardColors(
                        containerColor = if (isEnabled) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha=0.3f),
                        contentColor = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha=0.38f)
                    )
                },
                border = if (selected) BorderStroke(0.dp, Color.Transparent) else BorderStroke(1.dp, if (isEnabled) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.38f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            title, 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Bold,
                            color = if (isEnabled) Color.Unspecified else MaterialTheme.colorScheme.onSurface.copy(alpha=0.38f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            sub, 
                            style = MaterialTheme.typography.bodyMedium, 
                            color = if (isEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.38f)
                        )
                    }
                    if (selected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        val sizePresets = state.targetSizePresets.filter { preset -> 
            preset.sizeMb < (state.originalSize.toFloat() / (1024f * 1024f))
        }

        if (state.showTargetSizePreset && sizePresets.isNotEmpty()) {
            Text(
                stringResource(R.string.target_size_limits),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sizePresets.forEach { preset ->
                    val interactionSource = remember { MutableInteractionSource() }
                    FilterChip(
                        selected = state.targetSizeMb == preset.sizeMb,
                        onClick = { 
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setTargetSize(preset.sizeMb) 
                        },
                        interactionSource = interactionSource,
                        label = { 
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val unitGb = stringResource(R.string.unit_gb)
                                val unitMb = stringResource(R.string.unit_mb)
                                val unitKb = stringResource(R.string.unit_kb)
                                val formattedSize = when {
                                    preset.sizeMb >= 1024f -> "${(preset.sizeMb / 1024f).toInt()} $unitGb"
                                    preset.sizeMb < 1f -> "${(preset.sizeMb * 1024f).toInt()} $unitKb"
                                    else -> "${preset.sizeMb.toInt()} $unitMb"
                                }
                                Text(
                                    formattedSize, 
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(preset.getLocalizedLabel(), style = MaterialTheme.typography.labelSmall)
                            }
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .expressiveScale(interactionSource)
                    )
                }
            }
        }
    }
}
