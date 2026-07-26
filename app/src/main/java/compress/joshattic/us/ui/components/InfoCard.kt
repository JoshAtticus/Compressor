package compress.joshattic.us.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import compress.joshattic.us.R
import compress.joshattic.us.model.CompressorUiState

@Composable
fun InfoCard(state: CompressorUiState) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
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
            
            Box(modifier = Modifier.height(40.dp).width(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
            
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
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
}
