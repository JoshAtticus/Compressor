package compress.joshattic.us.ui.screens.optimizer

import android.net.Uri
import android.view.TextureView
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import compress.joshattic.us.R
import compress.joshattic.us.model.MediaLibraryItem
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun MediaPreviewPlayer(
    item: MediaLibraryItem,
    modifier: Modifier = Modifier
) {
    MediaPreviewPlayer(
        uri = item.uri,
        durationHintMs = item.durationMs,
        modifier = modifier
    )
}

@Composable
fun MediaPreviewPlayer(
    uri: Uri,
    modifier: Modifier = Modifier,
    durationHintMs: Long = 0L
) {
    val context = LocalContext.current
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = false
        }
    }

    var isPlaying by remember(uri) { mutableStateOf(false) }
    var positionMs by remember(uri) { mutableLongStateOf(0L) }
    var durationMs by remember(uri) {
        mutableLongStateOf(durationHintMs.coerceAtLeast(0L))
    }
    var scrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableFloatStateOf(0f) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val d = player.duration
                    if (d > 0) durationMs = d
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player, scrubbing) {
        while (isActive) {
            if (!scrubbing) {
                positionMs = player.currentPosition.coerceAtLeast(0L)
                val d = player.duration
                if (d > 0) durationMs = d
            }
            delay(200)
        }
    }

    val effectiveDuration = durationMs.coerceAtLeast(1L)
    val sliderValue = if (scrubbing) {
        scrubValue
    } else {
        (positionMs.toFloat() / effectiveDuration).coerceIn(0f, 1f)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
        ) {
            AndroidView(
                factory = { ctx ->
                    TextureView(ctx).also { texture ->
                        player.setVideoTextureView(texture)
                    }
                },
                update = { texture ->
                    player.setVideoTextureView(texture)
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(Modifier.height(8.dp))

        Slider(
            value = sliderValue,
            onValueChange = { value ->
                scrubbing = true
                scrubValue = value
                positionMs = (value * effectiveDuration).toLong()
            },
            onValueChangeFinished = {
                scrubbing = false
                player.seekTo((scrubValue * effectiveDuration).toLong())
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalIconButton(
                onClick = {
                    if (player.isPlaying) player.pause() else player.play()
                }
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(
                        if (isPlaying) R.string.free_space_pause else R.string.free_space_play
                    )
                )
            }

            Text(
                text = "${formatTimestamp(positionMs)} / ${formatTimestamp(durationMs)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTimestamp(ms: Long): String {
    val totalSec = TimeUnit.MILLISECONDS.toSeconds(ms.coerceAtLeast(0L))
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return "%d:%02d".format(minutes, seconds)
}
