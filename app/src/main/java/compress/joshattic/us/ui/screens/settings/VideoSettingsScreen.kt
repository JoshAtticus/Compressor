package compress.joshattic.us.ui.screens.settings

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.MimeTypes
import compress.joshattic.us.R
import compress.joshattic.us.model.CompressorUiState
import compress.joshattic.us.model.DefaultVideoConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoSettingsScreen(
    state: CompressorUiState,
    onBack: () -> Unit,
    onUpdateVideoConfig: (DefaultVideoConfig) -> Unit,
    onResetVideoConfig: () -> Unit
) {
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val videoConfig = state.defaultVideoConfig

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.tab_video),
                        fontWeight = FontWeight.Bold
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
            Text(
                text = stringResource(R.string.header_default_video_options),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 0.dp, bottom = 8.dp)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    // Default Codec
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Text(
                            text = stringResource(R.string.default_video_codec),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.default_video_codec_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val codecOptions = mutableListOf(
                            MimeTypes.VIDEO_H265 to stringResource(R.string.h265_efficient),
                            MimeTypes.VIDEO_H264 to stringResource(R.string.h264_compat)
                        )
                        if (state.supportedCodecs.contains(MimeTypes.VIDEO_AV1)) {
                            codecOptions.add(MimeTypes.VIDEO_AV1 to stringResource(R.string.av1_high_efficiency))
                        }

                        SelectionChipRow(
                            options = codecOptions,
                            selected = videoConfig.defaultVideoCodec,
                            onSelect = { onUpdateVideoConfig(videoConfig.copy(defaultVideoCodec = it)) }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(start = 20.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Default Resolution
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Text(
                            text = stringResource(R.string.default_resolution),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SelectionChipRow(
                            options = listOf(
                                0 to stringResource(R.string.original),
                                1080 to "1080p",
                                720 to "720p",
                                480 to "480p"
                            ),
                            selected = videoConfig.defaultTargetResolutionHeight,
                            onSelect = { onUpdateVideoConfig(videoConfig.copy(defaultTargetResolutionHeight = it)) }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(start = 20.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Default Framerate
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Text(
                            text = stringResource(R.string.default_framerate),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SelectionChipRow(
                            options = listOf(
                                0 to stringResource(R.string.original),
                                60 to "60fps",
                                30 to "30fps"
                            ),
                            selected = videoConfig.defaultTargetFps,
                            onSelect = { onUpdateVideoConfig(videoConfig.copy(defaultTargetFps = it)) }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(start = 20.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Default Target Size Ratio
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Text(
                            text = "${stringResource(R.string.default_size_ratio)}: ${(videoConfig.defaultSizeRatio * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Slider(
                            value = videoConfig.defaultSizeRatio,
                            onValueChange = {
                                onUpdateVideoConfig(videoConfig.copy(defaultSizeRatio = it))
                                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            },
                            valueRange = 0.1f..0.9f,
                            steps = 15
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                OutlinedButton(
                    onClick = {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onResetVideoConfig()
                    },
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.reset_video_defaults), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
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
