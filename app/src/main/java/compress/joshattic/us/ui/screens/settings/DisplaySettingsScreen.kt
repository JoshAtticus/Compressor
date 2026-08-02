package compress.joshattic.us.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import compress.joshattic.us.R
import compress.joshattic.us.model.CompressorUiState
import compress.joshattic.us.model.FilenameSegment

private data class TokenChipItem(val key: String, val labelRes: Int)

private val availableTokenChips = listOf(
    TokenChipItem("original_name", R.string.token_original_name),
    TokenChipItem("compressed", R.string.token_compressed),
    TokenChipItem("date", R.string.token_date),
    TokenChipItem("time", R.string.token_time),
    TokenChipItem("random", R.string.token_random),
    TokenChipItem("resolution", R.string.token_resolution),
    TokenChipItem("framerate", R.string.token_framerate),
    TokenChipItem("bitrate", R.string.token_bitrate),
    TokenChipItem("audio_bitrate", R.string.token_audio_bitrate),
    TokenChipItem("encoding", R.string.token_encoding),
    TokenChipItem("audio_status", R.string.token_audio_status),
    TokenChipItem("preset", R.string.token_preset)
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DisplaySettingsScreen(
    state: CompressorUiState,
    onBack: () -> Unit,
    onToggleAutoSaveToPhotos: () -> Unit,
    onChangeOutputLocation: () -> Unit,
    onResetOutputLocation: () -> Unit,
    onToggleShowBitrate: () -> Unit,
    onToggleBitrateUnit: () -> Unit,
    onToggleShowStorageSaved: () -> Unit,
    onToggleShowTargetSizePreset: () -> Unit,
    onUpdateFilenameSegments: (List<FilenameSegment>) -> Unit = {},
    onInsertFilenameTokenAt: (Int, String) -> Unit = { _, _ -> },
    onRemoveFilenameSegmentAt: (Int) -> Unit = {},
    onMoveFilenameSegment: (Int, Int) -> Unit = { _, _ -> },
    onResetFilenamePattern: () -> Unit = {},
    previewFileName: String = ""
) {
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var draggingSourceIndex by remember { mutableStateOf<Int?>(null) }
    var draggingTokenKey by remember { mutableStateOf<String?>(null) }
    var draggingOffset by remember { mutableStateOf(Offset.Zero) }
    var dropTargetRect by remember { mutableStateOf<Rect?>(null) }
    val segmentRects = remember { mutableMapOf<Int, Rect>() }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                LargeTopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.general_settings_title),
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
                // General Options Section Header
                Text(
                    text = stringResource(R.string.header_general_options),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column {
                        val autoSaveSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                        if (autoSaveSupported) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        onToggleAutoSaveToPhotos()
                                    }
                                    .padding(horizontal = 20.dp, vertical = 18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.auto_save_photos_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(R.string.auto_save_photos_subtitle),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Switch(
                                    checked = state.autoSaveToPhotos,
                                    onCheckedChange = {
                                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        onToggleAutoSaveToPhotos()
                                    }
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        }

                        // Save location
                        val hasCustomLocation = !state.customOutputTreeUri.isNullOrBlank()
                        val locationLabel = if (hasCustomLocation) {
                            state.customOutputFolderName
                                ?.takeIf { it.isNotBlank() }
                                ?: stringResource(R.string.output_location_default)
                        } else {
                            stringResource(R.string.output_location_default)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    onChangeOutputLocation()
                                }
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.output_location_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = locationLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.output_location_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }

                        AnimatedVisibility(visible = hasCustomLocation) {
                            Column {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            onResetOutputLocation()
                                        }
                                        .padding(horizontal = 20.dp, vertical = 18.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.output_location_reset_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = stringResource(R.string.output_location_reset_subtitle),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )

                        // ============================================================
                        // Filename Builder Section — COMBINED text + chip input area
                        // ============================================================
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 18.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.filename_builder_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.filename_builder_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            val isOverDropTarget = draggingTokenKey != null && dropTargetRect?.contains(draggingOffset) == true

                            // ---- Combined Filename Input Area ----
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isOverDropTarget) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { coordinates ->
                                        dropTargetRect = coordinates.boundsInWindow()
                                    }
                                    .then(
                                        if (draggingTokenKey != null) {
                                            Modifier.border(
                                                width = 2.dp,
                                                color = if (isOverDropTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                        } else {
                                            Modifier.border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                        }
                                    )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(1.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        state.filenameSegments.forEachIndexed { index, segment ->
                                            when (segment) {
                                                is FilenameSegment.Text -> {
                                                    val placeholderText = when {
                                                        index == 0 -> stringResource(R.string.filename_placeholder_prefix)
                                                        index == state.filenameSegments.lastIndex -> stringResource(R.string.filename_placeholder_suffix)
                                                        else -> ""
                                                    }

                                                    BasicTextField(
                                                        value = segment.value,
                                                        onValueChange = { newText ->
                                                            val updated = state.filenameSegments.toMutableList()
                                                            updated[index] = FilenameSegment.Text(newText)
                                                            onUpdateFilenameSegments(updated)
                                                        },
                                                        textStyle = TextStyle(
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                                            fontWeight = FontWeight.SemiBold
                                                        ),
                                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                                        singleLine = true,
                                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                                        modifier = Modifier
                                                            .widthIn(
                                                                min = if (segment.value.isEmpty()) {
                                                                    if (placeholderText.isNotEmpty()) 45.dp else 0.dp
                                                                } else 12.dp,
                                                                max = 200.dp
                                                            )
                                                            .width(IntrinsicSize.Min)
                                                            .padding(vertical = 4.dp),
                                                        decorationBox = { innerTextField ->
                                                            if (segment.value.isEmpty() && placeholderText.isNotEmpty()) {
                                                                Text(
                                                                    text = placeholderText,
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                                                )
                                                            }
                                                            innerTextField()
                                                        }
                                                    )
                                                }
                                                is FilenameSegment.Token -> {
                                                    val chipItem = availableTokenChips.firstOrNull { it.key == segment.key }
                                                    val chipLabel = chipItem?.let { stringResource(it.labelRes) } ?: segment.key
                                                    var chipRect by remember { mutableStateOf<Rect?>(null) }

                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Surface(
                                                            shape = RoundedCornerShape(12.dp),
                                                            color = MaterialTheme.colorScheme.primaryContainer,
                                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            modifier = Modifier
                                                                .onGloballyPositioned { coordinates ->
                                                                    val bounds = coordinates.boundsInWindow()
                                                                    chipRect = bounds
                                                                    segmentRects[index] = bounds
                                                                }
                                                                .pointerInput(segment.key, index) {
                                                                    detectDragGestures(
                                                                        onDragStart = { startOffset ->
                                                                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                                            draggingSourceIndex = index
                                                                            draggingTokenKey = segment.key
                                                                            val startPos = chipRect?.topLeft ?: Offset.Zero
                                                                            draggingOffset = startPos + startOffset
                                                                        },
                                                                        onDrag = { change, dragAmount ->
                                                                            change.consume()
                                                                            draggingOffset += dragAmount
                                                                        },
                                                                        onDragEnd = {
                                                                            val srcIdx = draggingSourceIndex
                                                                            if (srcIdx != null) {
                                                                                if (dropTargetRect?.contains(draggingOffset) == true) {
                                                                                    val targetIdx = findClosestSegmentIndex(draggingOffset, segmentRects)
                                                                                    if (targetIdx != srcIdx) {
                                                                                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                                                        onMoveFilenameSegment(srcIdx, targetIdx)
                                                                                    }
                                                                                } else {
                                                                                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                                                    onRemoveFilenameSegmentAt(srcIdx)
                                                                                }
                                                                            }
                                                                            draggingSourceIndex = null
                                                                            draggingTokenKey = null
                                                                        },
                                                                        onDragCancel = {
                                                                            draggingSourceIndex = null
                                                                            draggingTokenKey = null
                                                                        }
                                                                    )
                                                                }
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                            ) {
                                                                Text(
                                                                    text = "⋮⋮ $chipLabel",
                                                                    style = MaterialTheme.typography.labelMedium,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }

                                                        if (index < state.filenameSegments.lastIndex) {
                                                            Text(
                                                                text = "_",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Dynamic Drag Feedback Banner
                                    AnimatedVisibility(visible = draggingTokenKey != null) {
                                        val isExisting = draggingSourceIndex != null
                                        val isInside = isOverDropTarget

                                        val (statusText, bgColor, textColor) = when {
                                            isExisting && !isInside -> Triple(
                                                stringResource(R.string.filename_drag_release_remove),
                                                MaterialTheme.colorScheme.errorContainer,
                                                MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            isExisting && isInside -> Triple(
                                                stringResource(R.string.filename_drag_over_reorder),
                                                MaterialTheme.colorScheme.primaryContainer,
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            !isExisting && isInside -> Triple(
                                                stringResource(R.string.filename_drag_release_add),
                                                MaterialTheme.colorScheme.primaryContainer,
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            else -> Triple(
                                                stringResource(R.string.filename_drag_into_area),
                                                MaterialTheme.colorScheme.surfaceVariant,
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            color = bgColor,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = statusText,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Available Chips Palette (only chips not yet added)
                            val usedTokenKeys = state.filenameSegments
                                .filterIsInstance<FilenameSegment.Token>()
                                .map { it.key }
                                .toSet()
                            val remainingChips = availableTokenChips.filterNot { usedTokenKeys.contains(it.key) }

                            Text(
                                text = stringResource(R.string.filename_available_chips_title),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (remainingChips.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.filename_all_chips_added),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            } else {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    remainingChips.forEach { chip ->
                                        var chipRect by remember { mutableStateOf<Rect?>(null) }

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier
                                                .onGloballyPositioned { coordinates ->
                                                    chipRect = coordinates.boundsInWindow()
                                                }
                                                .pointerInput(chip.key) {
                                                    detectDragGestures(
                                                        onDragStart = { startOffset ->
                                                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                            draggingTokenKey = chip.key
                                                            val startPos = chipRect?.topLeft ?: Offset.Zero
                                                            draggingOffset = startPos + startOffset
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            draggingOffset += dragAmount
                                                        },
                                                        onDragEnd = {
                                                            if (dropTargetRect?.contains(draggingOffset) == true) {
                                                                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                                // Insert at the end of the segments list
                                                                onInsertFilenameTokenAt(state.filenameSegments.size, chip.key)
                                                            }
                                                            draggingTokenKey = null
                                                        },
                                                        onDragCancel = {
                                                            draggingTokenKey = null
                                                        }
                                                    )
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "⋮⋮ ${stringResource(chip.labelRes)}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Live Preview
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = stringResource(R.string.filename_preview_label),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = previewFileName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Reset to Default
                            OutlinedButton(
                                onClick = {
                                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    onResetFilenamePattern()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.filename_reset_button))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Display Options Section Header
                Text(
                    text = stringResource(R.string.header_display_options),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column {
                        // Show bitrate
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    onToggleShowBitrate()
                                }
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.show_bitrate),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.show_bitrate_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Switch(
                                checked = state.showBitrate,
                                onCheckedChange = {
                                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    onToggleShowBitrate()
                                }
                            )
                        }

                        // Bitrate unit (Mbps vs kbps)
                        AnimatedVisibility(visible = state.showBitrate) {
                            Column {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            onToggleBitrateUnit()
                                        }
                                        .padding(horizontal = 20.dp, vertical = 18.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.bitrate_unit_mbps),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (state.useMbps) stringResource(R.string.bitrate_unit_mbps_subtitle) else stringResource(R.string.bitrate_unit_kbps_subtitle),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Switch(
                                        checked = state.useMbps,
                                        onCheckedChange = {
                                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            onToggleBitrateUnit()
                                        }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Show storage saved on main page
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    onToggleShowStorageSaved()
                                }
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.show_storage_saved_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.show_storage_saved_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Switch(
                                checked = state.showStorageSaved,
                                onCheckedChange = {
                                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    onToggleShowStorageSaved()
                                }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Show target size preset
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    onToggleShowTargetSizePreset()
                                }
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.show_target_size_preset_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.show_target_size_preset_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Switch(
                                checked = state.showTargetSizePreset,
                                onCheckedChange = {
                                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    onToggleShowTargetSizePreset()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Floating Dragged Chip Overlay following touch gesture across the screen
        draggingTokenKey?.let { key ->
            val chipLabel = availableTokenChips.firstOrNull { it.key == key }
                ?.let { stringResource(it.labelRes) } ?: key

            Box(modifier = Modifier.fillMaxSize()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shadowElevation = 12.dp,
                    tonalElevation = 12.dp,
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = draggingOffset.x - 40.dp.toPx()
                            translationY = draggingOffset.y - 40.dp.toPx()
                            scaleX = 1.15f
                            scaleY = 1.15f
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⋮⋮ $chipLabel",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun findClosestSegmentIndex(offset: Offset, rects: Map<Int, Rect>): Int {
    if (rects.isEmpty()) return 0
    var minDistance = Float.MAX_VALUE
    var closest = 0
    rects.forEach { (index, rect) ->
        val center = rect.center
        val distSq = (offset.x - center.x) * (offset.x - center.x) + (offset.y - center.y) * (offset.y - center.y)
        if (distSq < minDistance) {
            minDistance = distSq
            closest = index
        }
    }
    return closest
}
