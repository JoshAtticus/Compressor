package compress.joshattic.us.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import compress.joshattic.us.R
import compress.joshattic.us.model.CompressorUiState

private const val LARGE_FONT_SCALE = 1.3f

@Composable
fun InfoCard(state: CompressorUiState, compact: Boolean = false) {
    val stackVertically = !compact && LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    var expanded by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (compact) Modifier.clickable { expanded = !expanded } else Modifier),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        if (compact) {
            CompactInfo(state, expanded)
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    OriginalInfo(state, Modifier.fillMaxWidth())
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    EstimatedInfo(state, Modifier.fillMaxWidth(), Alignment.Start, percentOnOwnLine = true)
                }
            }
        } else if (stackVertically) {
            Column(modifier = Modifier.padding(20.dp)) {
                OriginalInfo(state, Modifier.fillMaxWidth())
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                EstimatedInfo(state, Modifier.fillMaxWidth(), Alignment.Start, percentOnOwnLine = true)
            }
        } else {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OriginalInfo(state, Modifier.weight(1f))
                Box(modifier = Modifier.height(40.dp).width(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                EstimatedInfo(state, Modifier.weight(1f), Alignment.End, percentOnOwnLine = false)
            }
        }
    }
}

@Composable
private fun CompactInfo(state: CompressorUiState, expanded: Boolean) {
    val originalMb = state.originalSize / (1024f * 1024f)
    val actualEst = maxOf(state.targetSizeMb, state.minimumSizeMb)
    val pct = if (originalMb > 0) (1f - (actualEst / originalMb)) * 100f else 0f
    val pctInt = pct.toInt()
    val pctText = when {
        originalMb <= 0f -> null
        pctInt > 0 -> "-$pctInt%"
        else -> "+${-pctInt}%"
    }
    val pctColor = if (pctInt > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    state.formattedOriginalSize,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "→",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    state.estimatedSize,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (pctText != null) {
                Text(
                    pctText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = pctColor,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 8.dp)
                .size(24.dp)
        )
    }
}

@Composable
private fun OriginalInfo(state: CompressorUiState, modifier: Modifier) {
    Column(modifier) {
        Text(
            stringResource(R.string.original),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            state.formattedOriginalSize,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "${state.originalWidth}x${state.originalHeight} • ${state.originalFps.toInt()}fps",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        if (state.showBitrate) {
            Text(
                state.formattedOriginalBitrate,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun EstimatedInfo(
    state: CompressorUiState,
    modifier: Modifier,
    horizontalAlignment: Alignment.Horizontal,
    percentOnOwnLine: Boolean
) {
    Column(modifier = modifier, horizontalAlignment = horizontalAlignment) {
        Text(
            stringResource(R.string.estimated),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        AnimatedContent(
            targetState = state.estimatedSize,
            transitionSpec = {
                 slideInVertically { it / 2 } + fadeIn() togetherWith slideOutVertically { -it / 2 } + fadeOut()
            },
            label = "EstimateAnimation"
        ) { text ->
            Text(
                text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        val originalMb = state.originalSize / (1024f * 1024f)
        val actualEst = maxOf(state.targetSizeMb, state.minimumSizeMb)
        val pct = if (originalMb > 0) (1f - (actualEst / originalMb)) * 100f else 0f
        val pctInt = pct.toInt()

        val targetRes = if (state.targetResolutionHeight > 0) state.targetResolutionHeight else state.originalHeight
        val targetW = if (state.originalHeight > 0) (state.originalWidth.toFloat() / state.originalHeight * targetRes).toInt() else 0
        val targetFps = if (state.targetFps > 0) state.targetFps else state.originalFps.toInt()

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "${targetW}x${targetRes} • ${targetFps}fps",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        )

        if (percentOnOwnLine) {
            if (state.showBitrate) {
                Text(
                    state.formattedBitrate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            }
            if (originalMb > 0) {
                Text(
                    if (pctInt > 0) "-$pctInt%" else "+${-pctInt}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (pctInt > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.showBitrate) {
                    Text(
                        state.formattedBitrate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }

                if (originalMb > 0) {
                     if (state.showBitrate) {
                         Text(
                             " • ",
                             style = MaterialTheme.typography.labelSmall,
                             color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                         )
                     }

                     val color = if (pctInt > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                     val text = if (pctInt > 0) "-$pctInt%" else "+${-pctInt}%"

                     Text(
                        text,
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Bold
                     )
                }
            }
        }
    }
}
