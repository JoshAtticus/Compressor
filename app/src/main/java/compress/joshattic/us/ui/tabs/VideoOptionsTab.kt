package compress.joshattic.us.ui.tabs

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import compress.joshattic.us.R
import compress.joshattic.us.model.CompressorUiState
import compress.joshattic.us.utils.expressiveScale
import compress.joshattic.us.viewmodel.CompressorViewModel
import java.util.Locale

@SuppressLint("DefaultLocale")
@Composable
fun VideoOptionsTab(state: CompressorUiState, viewModel: CompressorViewModel) {
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
            Text(stringResource(R.string.advanced_options), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))

            var sliderValue by remember { mutableFloatStateOf(state.targetSizeMb) }
            var isUserInteracting by remember { mutableStateOf(false) }
            
            LaunchedEffect(state.targetSizeMb) {
                if (!isUserInteracting) {
                    sliderValue = state.targetSizeMb
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.target_size), style = MaterialTheme.typography.labelLarge)
                Text(
                    String.format(Locale.US, "%.1f MB", sliderValue), 
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            val minSize = 0.1f
            val maxSize = maxOf(10f, state.targetSizeMb, (state.originalSize / (1024f*1024f)))
            
            val sliderFraction = if (maxSize > minSize) {
                (kotlin.math.ln(sliderValue / minSize) / kotlin.math.ln(maxSize / minSize)).toFloat().coerceIn(0f, 1f)
            } else {
                0f
            }

            Slider(
                value = sliderFraction,
                onValueChange = { fraction ->
                    isUserInteracting = true
                    val calculatedSize = minSize * kotlin.math.exp(fraction * kotlin.math.ln(maxSize / minSize)).toFloat()
                    
                    val roundedSize = when {
                        calculatedSize < 2.5f -> kotlin.math.round(calculatedSize * 10f) / 10f
                        calculatedSize < 10f -> kotlin.math.round(calculatedSize * 2f) / 2f
                        calculatedSize < 50f -> kotlin.math.round(calculatedSize)
                        else -> kotlin.math.round(calculatedSize / 5f) * 5f
                    }.coerceIn(minSize, maxSize)

                    if (sliderValue != roundedSize) {
                        sliderValue = roundedSize
                        viewModel.setTargetSize(roundedSize)
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                },
                onValueChangeFinished = {
                    isUserInteracting = false
                },
                valueRange = 0f..1f,
                steps = 0
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.slider_less_space), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Text(stringResource(R.string.slider_balanced), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Text(stringResource(R.string.slider_high_quality), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(stringResource(R.string.encoding), style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val supported = state.supportedCodecs
                supported.forEach { codec ->
                    val interactionSource = remember { MutableInteractionSource() }
                    val labelText = when (codec) {
                        androidx.media3.common.MimeTypes.VIDEO_AV1 -> stringResource(R.string.av1_high_efficiency)
                        androidx.media3.common.MimeTypes.VIDEO_H265 -> stringResource(R.string.h265_efficient)
                        androidx.media3.common.MimeTypes.VIDEO_H264 -> stringResource(R.string.h264_compat)
                        else -> codec.substringAfter("/").uppercase()
                    }
                    FilterChip(
                        selected = state.videoCodec == codec,
                        onClick = { 
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setVideoCodec(codec) 
                        },
                        interactionSource = interactionSource,
                        label = { Text(labelText) },
                        modifier = Modifier.expressiveScale(interactionSource)
                    )
                }
            }
             
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(stringResource(R.string.resolution), style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                
                val res4320 = stringResource(R.string.res_8k)
                val res2160 = stringResource(R.string.res_4k)
                val res1440 = stringResource(R.string.res_2k)
                val res1080 = stringResource(R.string.res_1080p)
                val res720 = stringResource(R.string.res_720p)
                val res540 = stringResource(R.string.res_540p)
                val res480 = stringResource(R.string.res_480p)

                val resThreeQuarters = stringResource(R.string.res_three_quarters)
                val resHalf = stringResource(R.string.res_half)
                val resQuarter = stringResource(R.string.res_quarter)

                val allRes = listOf(4320 to res4320, 2160 to res2160, 1440 to res1440, 1080 to res1080, 720 to res720, 540 to res540, 480 to res480)
                val isVertical = state.originalHeight > state.originalWidth
                val originalShortSide = minOf(state.originalWidth, state.originalHeight)
                val currentShortSide = if (
                    isVertical &&
                    state.originalWidth > 0 &&
                    state.originalHeight > 0 &&
                    state.targetResolutionHeight > 0
                ) {
                    (state.targetResolutionHeight.toLong() * state.originalWidth / state.originalHeight).toInt()
                } else if (state.targetResolutionHeight > 0) {
                    state.targetResolutionHeight
                } else {
                    originalShortSide
                }
                
                val options = remember(state.originalWidth, state.originalHeight) {
                    val shortSide = minOf(state.originalWidth, state.originalHeight)
                    val standard = allRes.filter { it.first <= shortSide }
                    val fractions = listOf(
                        (shortSide * 0.75).toInt() to resThreeQuarters,
                        (shortSide * 0.5).toInt() to resHalf,
                        (shortSide * 0.25).toInt() to resQuarter
                    )
                    (standard + fractions)
                        .filter { it.first > 0 }
                        .sortedByDescending { it.first }
                        .distinctBy { it.first }
                }

                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                     val interactionSource = remember { MutableInteractionSource() }
                     FilterChip(
                        selected = state.targetResolutionHeight == state.originalHeight || state.targetResolutionHeight == 0,
                        onClick = { 
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setResolution(originalShortSide)
                        }, 
                        interactionSource = interactionSource,
                        label = { Text(stringResource(R.string.original) + " • ${originalShortSide}p") },
                        modifier = Modifier.padding(end = 8.dp).expressiveScale(interactionSource)
                    )
                    options.forEach { (res, label) ->
                         val itemInteractionSource = remember { MutableInteractionSource() }
                         FilterChip(
                            selected = currentShortSide == res,
                            onClick = { 
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.setResolution(res) 
                            }, 
                            interactionSource = itemInteractionSource,
                            label = { Text(label) },
                            modifier = Modifier.padding(end = 8.dp).expressiveScale(itemInteractionSource)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(stringResource(R.string.framerate), style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                 val iSource1 = remember { MutableInteractionSource() }
                 FilterChip(
                    selected = state.targetFps == 0,
                    onClick = { 
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setFps(0) 
                    },
                    interactionSource = iSource1,
                    label = { Text(stringResource(R.string.original) + " • ${state.originalFps.toInt()}") },
                    modifier = Modifier.expressiveScale(iSource1)
                )
                val iSource2 = remember { MutableInteractionSource() }
                FilterChip(
                    selected = state.targetFps == 60,
                    onClick = { 
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setFps(60) 
                    },
                    interactionSource = iSource2,
                    label = { Text(stringResource(R.string.fps_60)) },
                    enabled = state.originalFps >= 50f,
                    modifier = Modifier.expressiveScale(iSource2)
                )
                val iSource3 = remember { MutableInteractionSource() }
                FilterChip(
                    selected = state.targetFps == 30,
                    onClick = { 
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setFps(30) 
                    },
                    interactionSource = iSource3,
                    label = { Text(stringResource(R.string.fps_30)) },
                    modifier = Modifier.expressiveScale(iSource3)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
    }
}
