package compress.joshattic.us.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compress.joshattic.us.R
import compress.joshattic.us.model.CompressorUiState
import compress.joshattic.us.model.QualityPreset
import compress.joshattic.us.model.QualityPresetConfig
import compress.joshattic.us.model.TargetSizePreset
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetsSettingsScreen(
    state: CompressorUiState,
    onBack: () -> Unit,
    onUpdateHighPreset: (QualityPresetConfig) -> Unit,
    onUpdateMediumPreset: (QualityPresetConfig) -> Unit,
    onUpdateLowPreset: (QualityPresetConfig) -> Unit,
    onResetQualityPresets: () -> Unit,
    onAddTargetSizePreset: (label: String, sizeMb: Float) -> Unit,
    onUpdateTargetSizePreset: (id: String, label: String, sizeMb: Float) -> Unit,
    onDeleteTargetSizePreset: (id: String) -> Unit,
    onResetTargetSizePresets: () -> Unit
) {
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var editingQualityPreset by remember { mutableStateOf<QualityPreset?>(null) }
    var editingTargetSizePreset by remember { mutableStateOf<TargetSizePreset?>(null) }
    var showAddTargetSizeDialog by remember { mutableStateOf(false) }
    var showResetTargetSizeConfirmDialog by remember { mutableStateOf(false) }
    var deletingPresetIds by remember { mutableStateOf(setOf<String>()) }

    // Dialog: Edit Quality Preset (High, Medium, Low)
    editingQualityPreset?.let { preset ->
        val currentConfig = when (preset) {
            QualityPreset.HIGH -> state.highPresetConfig
            QualityPreset.MEDIUM -> state.mediumPresetConfig
            QualityPreset.LOW -> state.lowPresetConfig
            else -> state.highPresetConfig
        }

        EditQualityPresetDialog(
            presetName = when (preset) {
                QualityPreset.HIGH -> stringResource(R.string.preset_high)
                QualityPreset.MEDIUM -> stringResource(R.string.preset_medium)
                QualityPreset.LOW -> stringResource(R.string.preset_low)
                else -> ""
            },
            config = currentConfig,
            onDismiss = {
                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                editingQualityPreset = null
            },
            onSave = { updated ->
                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                when (preset) {
                    QualityPreset.HIGH -> onUpdateHighPreset(updated)
                    QualityPreset.MEDIUM -> onUpdateMediumPreset(updated)
                    QualityPreset.LOW -> onUpdateLowPreset(updated)
                    else -> {}
                }
                editingQualityPreset = null
            }
        )
    }

    // Dialog: Add Target Size Preset
    if (showAddTargetSizeDialog) {
        AddEditTargetSizeDialog(
            preset = null,
            onDismiss = {
                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                showAddTargetSizeDialog = false
            },
            onSave = { label, mb ->
                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onAddTargetSizePreset(label, mb)
                showAddTargetSizeDialog = false
            }
        )
    }

    // Dialog: Edit Target Size Preset
    editingTargetSizePreset?.let { preset ->
        AddEditTargetSizeDialog(
            preset = preset,
            onDismiss = {
                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                editingTargetSizePreset = null
            },
            onSave = { label, mb ->
                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onUpdateTargetSizePreset(preset.id, label, mb)
                editingTargetSizePreset = null
            }
        )
    }

    // Dialog: Reset Target Size Presets Confirm
    if (showResetTargetSizeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetTargetSizeConfirmDialog = false },
            title = { Text(stringResource(R.string.reset_presets_confirm_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.reset_presets_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onResetTargetSizePresets()
                        showResetTargetSizeConfirmDialog = false
                    }
                ) {
                    Text(stringResource(R.string.reset_to_default), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        showResetTargetSizeConfirmDialog = false
                    }
                ) {
                    Text(stringResource(R.string.cancel), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.tab_presets),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                },
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 12.dp, end = 12.dp)) {
                        IconButton(
                            onClick = {
                                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                onBack()
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Quality Presets Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.header_quality_presets),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onResetQualityPresets()
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.reset_to_default), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column {
                    QualityPresetRow(
                        name = stringResource(R.string.preset_high),
                        config = state.highPresetConfig,
                        onClick = {
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            editingQualityPreset = QualityPreset.HIGH
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    QualityPresetRow(
                        name = stringResource(R.string.preset_medium),
                        config = state.mediumPresetConfig,
                        onClick = {
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            editingQualityPreset = QualityPreset.MEDIUM
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    QualityPresetRow(
                        name = stringResource(R.string.preset_low),
                        config = state.lowPresetConfig,
                        onClick = {
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            editingQualityPreset = QualityPreset.LOW
                        }
                    )
                }
            }

            // Target Size Presets Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.target_size_limits),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        showAddTargetSizeDialog = true
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.add_preset), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column {
                    state.targetSizePresets.forEachIndexed { index, preset ->
                        androidx.compose.runtime.key(preset.id) {
                            AnimatedVisibility(
                                visible = !deletingPresetIds.contains(preset.id),
                                enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                                exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                editingTargetSizePreset = preset
                                            }
                                            .padding(horizontal = 20.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            val unitGb = stringResource(R.string.unit_gb)
                                            val unitMb = stringResource(R.string.unit_mb)
                                            val formattedSize = if (preset.sizeMb >= 1024) {
                                                val valGb = preset.sizeMb / 1024f
                                                val str = if (valGb % 1f == 0f) "${valGb.toInt()}" else String.format(Locale.US, "%.1f", valGb)
                                                "$str $unitGb"
                                            } else {
                                                val str = if (preset.sizeMb % 1f == 0f) "${preset.sizeMb.toInt()}" else String.format(Locale.US, "%.1f", preset.sizeMb)
                                                "$str $unitMb"
                                            }
                                            Text(
                                                text = formattedSize,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Text(
                                            text = preset.label,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )

                                        IconButton(
                                            onClick = {
                                                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                editingTargetSizePreset = preset
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                val idToDelete = preset.id
                                                deletingPresetIds = deletingPresetIds + idToDelete
                                                coroutineScope.launch {
                                                    kotlinx.coroutines.delay(250)
                                                    onDeleteTargetSizePreset(idToDelete)
                                                    deletingPresetIds = deletingPresetIds - idToDelete
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    if (index < state.targetSizePresets.size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 20.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                OutlinedButton(
                    onClick = {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        showResetTargetSizeConfirmDialog = true
                    },
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.reset_presets_to_default), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun QualityPresetRow(
    name: String,
    config: QualityPresetConfig,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Speed,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))

            val resStr = if (config.resolutionShortSide == 0) stringResource(R.string.original) else "${config.resolutionShortSide}p"
            val fpsStr = if (config.targetFps == 0) stringResource(R.string.original) else "${config.targetFps}fps"
            val ratioStr = "${(config.sizeRatio * 100).toInt()}%"
            val audioStr = "${config.audioBitrate / 1000}k"

            Text(
                text = "$resStr • $fpsStr • $ratioStr size • $audioStr audio",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun <T> SelectionChipRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (value, label) ->
            compress.joshattic.us.ui.components.SelectionChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = label
            )
        }
    }
}

@Composable
private fun EditQualityPresetDialog(
    presetName: String,
    config: QualityPresetConfig,
    onDismiss: () -> Unit,
    onSave: (QualityPresetConfig) -> Unit
) {
    var resHeight by remember { mutableIntStateOf(config.resolutionShortSide) }
    var fps by remember { mutableIntStateOf(config.targetFps) }
    var ratio by remember { mutableFloatStateOf(config.sizeRatio) }
    var audioBitrate by remember { mutableIntStateOf(config.audioBitrate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$presetName Preset", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Resolution Selection Chips
                Text(stringResource(R.string.resolution), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                SelectionChipRow(
                    options = listOf(
                        0 to stringResource(R.string.original),
                        1080 to "1080p",
                        720 to "720p",
                        480 to "480p"
                    ),
                    selected = resHeight,
                    onSelect = { resHeight = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // FPS Selection Chips
                Text(stringResource(R.string.framerate), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                SelectionChipRow(
                    options = listOf(
                        0 to stringResource(R.string.original),
                        60 to "60fps",
                        30 to "30fps"
                    ),
                    selected = fps,
                    onSelect = { fps = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Size Ratio Slider
                Text("Target Size Ratio: ${(ratio * 100).toInt()}%", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Slider(
                    value = ratio,
                    onValueChange = { ratio = it },
                    valueRange = 0.1f..0.9f,
                    steps = 15
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Audio Bitrate Selection Chips
                Text(stringResource(R.string.audio_bitrate), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                SelectionChipRow(
                    options = listOf(
                        320000 to "320k",
                        192000 to "192k",
                        128000 to "128k",
                        96000 to "96k"
                    ),
                    selected = audioBitrate,
                    onSelect = { audioBitrate = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        QualityPresetConfig(
                            resolutionShortSide = resHeight,
                            targetFps = fps,
                            sizeRatio = ratio,
                            audioBitrate = audioBitrate
                        )
                    )
                }
            ) {
                Text(stringResource(R.string.save_action), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun AddEditTargetSizeDialog(
    preset: TargetSizePreset?,
    onDismiss: () -> Unit,
    onSave: (label: String, sizeMb: Float) -> Unit
) {
    var label by remember { mutableStateOf(preset?.label ?: "") }
    var sizeText by remember {
        mutableStateOf(
            preset?.sizeMb?.let {
                if (it % 1f == 0f) it.toInt().toString() else it.toString()
            } ?: ""
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (preset == null) R.string.add_size_preset_title else R.string.edit_size_preset_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.preset_label_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = sizeText,
                    onValueChange = { sizeText = it },
                    label = { Text(stringResource(R.string.target_size_mb_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = label.isNotBlank() && (sizeText.toFloatOrNull() ?: 0f) > 0f,
                onClick = {
                    val mb = sizeText.toFloatOrNull() ?: 10f
                    onSave(label, mb)
                }
            ) {
                Text(stringResource(R.string.save_action), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), fontWeight = FontWeight.Bold)
            }
        }
    )
}
