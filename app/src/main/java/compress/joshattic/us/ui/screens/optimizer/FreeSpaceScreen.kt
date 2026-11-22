package compress.joshattic.us.ui.screens.optimizer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import compress.joshattic.us.R
import compress.joshattic.us.model.MediaKind
import compress.joshattic.us.model.MediaLibraryItem
import compress.joshattic.us.model.OptimizerPermissionStatus
import compress.joshattic.us.model.OptimizerQualityMode
import compress.joshattic.us.model.OptimizerUiState
import compress.joshattic.us.utils.formatFileSize
import java.util.concurrent.TimeUnit

@Composable
fun FreeSpaceScreen(
    state: OptimizerUiState,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    onToggle: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onQualityModeSelected: (OptimizerQualityMode) -> Unit,
    onCompressSelected: () -> Unit,
    onOpenQueue: () -> Unit,
    onExcludeFile: (Long) -> Unit,
    onExcludeFolder: (String) -> Unit,
    onClearExclusions: () -> Unit
) {
    when {
        state.permissionStatus == OptimizerPermissionStatus.UNKNOWN ||
            state.permissionStatus == OptimizerPermissionStatus.DENIED -> {
            PermissionGate(
                denied = state.permissionStatus == OptimizerPermissionStatus.DENIED,
                onRequestPermission = onRequestPermission
            )
        }
        else -> {
            LibraryContent(
                state = state,
                onRefresh = onRefresh,
                onToggle = onToggle,
                onSelectAll = onSelectAll,
                onClearSelection = onClearSelection,
                onQualityModeSelected = onQualityModeSelected,
                onCompressSelected = onCompressSelected,
                onOpenQueue = onOpenQueue,
                onExcludeFile = onExcludeFile,
                onExcludeFolder = onExcludeFolder,
                onClearExclusions = onClearExclusions
            )
        }
    }
}

@Composable
private fun PermissionGate(
    denied: Boolean,
    onRequestPermission: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Outlined.Storage,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.free_space_permission_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.free_space_permission_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRequestPermission) {
                Text(
                    if (denied) stringResource(R.string.free_space_grant_again)
                    else stringResource(R.string.free_space_allow_access)
                )
            }
        }
    }
}

@Composable
private fun LibraryContent(
    state: OptimizerUiState,
    onRefresh: () -> Unit,
    onToggle: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onQualityModeSelected: (OptimizerQualityMode) -> Unit,
    onCompressSelected: () -> Unit,
    onOpenQueue: () -> Unit,
    onExcludeFile: (Long) -> Unit,
    onExcludeFolder: (String) -> Unit,
    onClearExclusions: () -> Unit
) {
    var excludeTarget by remember { mutableStateOf<MediaLibraryItem?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.free_space_large_media),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (state.hasActiveQueue) {
                TextButton(onClick = onOpenQueue) {
                    Text(stringResource(R.string.free_space_view_queue))
                }
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.free_space_refresh))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QualityModeChip(
                mode = OptimizerQualityMode.HIGH,
                selected = state.qualityMode,
                onSelected = onQualityModeSelected,
                label = stringResource(R.string.preset_high)
            )
            QualityModeChip(
                mode = OptimizerQualityMode.MEDIUM,
                selected = state.qualityMode,
                onSelected = onQualityModeSelected,
                label = stringResource(R.string.preset_medium)
            )
            QualityModeChip(
                mode = OptimizerQualityMode.LOW,
                selected = state.qualityMode,
                onSelected = onQualityModeSelected,
                label = stringResource(R.string.preset_low)
            )
        }

        Text(
            text = stringResource(R.string.free_space_quality_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onSelectAll) {
                Text(stringResource(R.string.free_space_select_all))
            }
            TextButton(onClick = onClearSelection) {
                Text(stringResource(R.string.free_space_clear))
            }
            if (state.hasExclusions) {
                TextButton(onClick = onClearExclusions) {
                    Text(stringResource(R.string.free_space_clear_filters))
                }
            }
            Spacer(Modifier.weight(1f))
            if (state.hasExclusions) {
                Text(
                    text = stringResource(R.string.free_space_filters_active, state.exclusionCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            if (state.permissionStatus == OptimizerPermissionStatus.PARTIAL) {
                Text(
                    text = stringResource(R.string.free_space_partial_access),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isScanning -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.scanError != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.scanError, color = MaterialTheme.colorScheme.error)
                    }
                }
                state.items.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(
                                if (state.hasExclusions && state.allItems.isNotEmpty()) {
                                    R.string.free_space_empty_filtered
                                } else {
                                    R.string.free_space_empty
                                }
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    var expandedId by remember { mutableStateOf<Long?>(null) }
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 96.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.items, key = { it.id }) { item ->
                            MediaRow(
                                item = item,
                                selected = item.id in state.selectedIds,
                                previouslyCompressed = item.id in state.compressedHistoryIds,
                                expanded = expandedId == item.id,
                                onToggle = { onToggle(item.id) },
                                onExpandToggle = {
                                    expandedId = if (expandedId == item.id) null else item.id
                                },
                                onLongPress = { excludeTarget = item }
                            )
                        }
                    }
                }
            }
        }

        excludeTarget?.let { target ->
            ExcludeMediaDialog(
                item = target,
                onDismiss = { excludeTarget = null },
                onExcludeFile = {
                    onExcludeFile(target.id)
                    excludeTarget = null
                },
                onExcludeFolder = {
                    onExcludeFolder(target.folderKey)
                    excludeTarget = null
                }
            )
        }

        if (state.selectedIds.isNotEmpty()) {
            Surface(tonalElevation = 3.dp, shadowElevation = 6.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(
                                R.string.free_space_selected_count,
                                state.selectedIds.size
                            ),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(
                                R.string.free_space_estimated_reclaim,
                                state.formattedEstimatedReclaim
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(onClick = onCompressSelected) {
                        Text(stringResource(R.string.free_space_compress_selected))
                    }
                }
            }
        }
    }
}

@Composable
private fun QualityModeChip(
    mode: OptimizerQualityMode,
    selected: OptimizerQualityMode,
    onSelected: (OptimizerQualityMode) -> Unit,
    label: String
) {
    FilterChip(
        selected = selected == mode,
        onClick = { onSelected(mode) },
        label = { Text(label) }
    )
}

@Composable
private fun ExcludeMediaDialog(
    item: MediaLibraryItem,
    onDismiss: () -> Unit,
    onExcludeFile: () -> Unit,
    onExcludeFolder: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.free_space_exclude_title)) },
        text = {
            Column {
                TextButton(
                    onClick = onExcludeFile,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.free_space_exclude_file))
                }
                if (item.folderKey.isNotBlank()) {
                    TextButton(
                        onClick = onExcludeFolder,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            stringResource(
                                R.string.free_space_exclude_folder,
                                item.folderLabel.ifBlank { item.folderKey }
                            )
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.free_space_exclude_folder_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaRow(
    item: MediaLibraryItem,
    selected: Boolean,
    previouslyCompressed: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onExpandToggle: () -> Unit,
    onLongPress: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var showCompressedInfo by remember(item.id) { mutableStateOf(false) }
    val onHistoryClick: (() -> Unit)? = if (previouslyCompressed) {
        { showCompressedInfo = true }
    } else {
        null
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onExpandToggle,
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress()
                    }
                )
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = selected, onCheckedChange = { onToggle() })
            MediaThumbnail(
                item = item,
                previouslyCompressed = previouslyCompressed,
                onPreviouslyCompressedClick = onHistoryClick
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.displayName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = buildMeta(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(item.formattedSize, fontWeight = FontWeight.Bold)
                if (item.estimatedSavingsBytes > 0) {
                    Text(
                        text = stringResource(R.string.free_space_save_up_to, item.formattedSavings),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onExpandToggle) {
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = stringResource(
                        if (expanded) R.string.free_space_collapse else R.string.free_space_expand
                    )
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                when (item.kind) {
                    MediaKind.VIDEO -> MediaPreviewPlayer(item = item)
                    MediaKind.IMAGE -> {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            MediaThumbnail(
                                item = item,
                                size = 220.dp,
                                thumbnailPx = 512,
                                previouslyCompressed = previouslyCompressed,
                                onPreviouslyCompressedClick = onHistoryClick
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCompressedInfo) {
        AlertDialog(
            onDismissRequest = { showCompressedInfo = false },
            title = { Text(stringResource(R.string.free_space_previously_compressed)) },
            text = { Text(stringResource(R.string.free_space_previously_compressed_why)) },
            confirmButton = {
                TextButton(onClick = { showCompressedInfo = false }) {
                    Text(stringResource(R.string.free_space_previously_compressed_ok))
                }
            }
        )
    }
}

private fun buildMeta(item: MediaLibraryItem): String {
    val parts = mutableListOf<String>()
    if (item.width > 0 && item.height > 0) {
        parts.add("${item.width}×${item.height}")
    }
    if (item.kind == MediaKind.VIDEO && item.fps > 0f) {
        parts.add(
            if (item.fps % 1f == 0f) "${item.fps.toInt()}fps" else String.format("%.1ffps", item.fps)
        )
    }
    if (item.kind == MediaKind.VIDEO && item.durationMs > 0) {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(item.durationMs)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(item.durationMs) % 60
        parts.add("%d:%02d".format(minutes, seconds))
    }
    parts.add(formatFileSize(item.sizeBytes))
    return parts.joinToString(" · ")
}
