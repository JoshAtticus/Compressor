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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import compress.joshattic.us.R
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * One ExoPlayer that swaps original ↔ compressed media while keeping the
 * same playback position. A single decoder avoids devices that can't run
 * two video players at once.
 */
@Composable
fun ReviewComparePlayer(
    originalUri: Uri,
    compressedUri: Uri,
    showingCompressed: Boolean,
    modifier: Modifier = Modifier,
    durationHintMs: Long = 0L
) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setSeekParameters(SeekParameters.EXACT)
            playWhenReady = false
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember {
        mutableLongStateOf(durationHintMs.coerceAtLeast(0L))
    }
    var scrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableFloatStateOf(0f) }
    var textureView by remember { mutableStateOf<TextureView?>(null) }

    val showingCompressedState = rememberUpdatedState(showingCompressed)
    val originalUriState = rememberUpdatedState(originalUri)
    val compressedUriState = rememberUpdatedState(compressedUri)

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

            override fun onPlayerError(error: PlaybackException) {
                isPlaying = false
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // Load / swap media when the visible version changes.
    LaunchedEffect(showingCompressed, originalUri, compressedUri) {
        val uri = if (showingCompressed) compressedUri else originalUri
        val keepPlaying = player.isPlaying || player.playWhenReady
        val resumeAt = player.currentPosition.coerceAtLeast(0L)

        player.playWhenReady = false
        player.setMediaItem(MediaItem.fromUri(uri), /* resetPosition = */ resumeAt <= 0L)
        player.prepare()
        textureView?.let { player.setVideoTextureView(it) }

        if (resumeAt > 0L) {
            player.seekTo(resumeAt)
        }
        awaitReady(player)

        positionMs = player.currentPosition.coerceAtLeast(0L)
        val d = player.duration
        if (d > 0) durationMs = d

        if (keepPlaying && showingCompressedState.value == showingCompressed) {
            val expected = if (showingCompressed) {
                compressedUriState.value
            } else {
                originalUriState.value
            }
            if (expected == uri) {
                player.playWhenReady = true
                player.play()
            }
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
                        textureView = texture
                        player.setVideoTextureView(texture)
                    }
                },
                update = { texture ->
                    if (textureView !== texture) {
                        textureView = texture
                        player.setVideoTextureView(texture)
                    }
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
                val seek = (scrubValue * effectiveDuration).toLong()
                scrubbing = false
                player.seekTo(seek)
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
                    if (player.isPlaying) {
                        player.pause()
                    } else {
                        textureView?.let { player.setVideoTextureView(it) }
                        if (player.playbackState == Player.STATE_IDLE) {
                            val uri = if (showingCompressedState.value) {
                                compressedUriState.value
                            } else {
                                originalUriState.value
                            }
                            player.setMediaItem(MediaItem.fromUri(uri))
                            player.prepare()
                        }
                        player.playWhenReady = true
                        player.play()
                    }
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

/** Poll on the main thread only — never touch the player from timeout cancel handlers. */
private suspend fun awaitReady(player: ExoPlayer) {
    repeat(40) {
        when {
            player.playerError != null -> return
            player.playbackState == Player.STATE_READY && !player.isLoading -> return
            else -> delay(50)
        }
    }
}

private fun formatTimestamp(ms: Long): String {
    val totalSec = TimeUnit.MILLISECONDS.toSeconds(ms.coerceAtLeast(0L))
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return "%d:%02d".format(minutes, seconds)
}
