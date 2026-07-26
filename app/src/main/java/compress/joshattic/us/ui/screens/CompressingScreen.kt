package compress.joshattic.us.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compress.joshattic.us.R
import compress.joshattic.us.model.CompressorUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CompressingScreen(
    state: CompressorUiState,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var thumbnail by remember { mutableStateOf<ImageBitmap?>(null) }
    
    // Load Thumbnail
    LaunchedEffect(state.selectedUri) {
        if (state.selectedUri == null) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val retriever = android.media.MediaMetadataRetriever()
                retriever.setDataSource(context, state.selectedUri)
                val bitmap = retriever.getFrameAtTime(0)
                thumbnail = bitmap?.asImageBitmap()
                try { retriever.release() } catch(e: Exception){}
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    val formattedSize = state.formattedCurrentOutputSize

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f/9f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.DarkGray)
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail!!,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        formattedSize.forEach { char ->
                            AnimatedContent(
                                targetState = char,
                                transitionSpec = {
                                    if (char.isDigit()) {
                                        slideInVertically { height -> height } + fadeIn() togetherWith
                                        slideOutVertically { height -> -height } + fadeOut()
                                    } else {
                                        fadeIn() togetherWith fadeOut()
                                    }
                                },
                                label = "CharAnimation"
                            ) { targetChar ->
                                Text(
                                    text = targetChar.toString(),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        stringResource(R.string.compressing_video_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    val animatedProgress by animateFloatAsState(
                        targetValue = state.progress,
                        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                        label = "ProgressAnimation"
                    )
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        strokeCap = StrokeCap.Round
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text(
                    stringResource(R.string.cancel),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        }
    }
}
