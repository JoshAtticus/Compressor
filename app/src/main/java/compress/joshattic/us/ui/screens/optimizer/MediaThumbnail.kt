package compress.joshattic.us.ui.screens.optimizer

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Size
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeometrySize
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import compress.joshattic.us.R
import compress.joshattic.us.model.MediaKind
import compress.joshattic.us.model.MediaLibraryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MediaThumbnail(
    item: MediaLibraryItem,
    size: Dp = 56.dp,
    thumbnailPx: Int = 168,
    previouslyCompressed: Boolean = false,
    onPreviouslyCompressedClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(8.dp)
    val primary = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current

    val transition = rememberInfiniteTransition(label = "compressedGlow")
    val animatedGlow by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    val glowAlpha = if (previouslyCompressed) animatedGlow else 0f

    val bitmap by produceState<Bitmap?>(initialValue = null, item.uri, item.kind, thumbnailPx) {
        value = withContext(Dispatchers.IO) {
            loadMediaThumbnail(context, item, px = thumbnailPx)
        }
    }

    val glowPadPx = with(density) { 5.dp.toPx() }
    val cornerPx = with(density) { 8.dp.toPx() }

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (previouslyCompressed) {
                    Modifier.drawBehind {
                        val glowColor = primary.copy(alpha = 0.22f + 0.38f * glowAlpha)
                        // Soft outer glow rings
                        for (i in 3 downTo 1) {
                            val expand = glowPadPx * i / 3f * glowAlpha
                            drawRoundRect(
                                color = glowColor.copy(alpha = glowColor.alpha * (i / 3f)),
                                topLeft = Offset(-expand, -expand),
                                size = GeometrySize(
                                    this.size.width + expand * 2f,
                                    this.size.height + expand * 2f
                                ),
                                cornerRadius = CornerRadius(cornerPx + expand / 2f),
                                style = Stroke(width = 2.5f.dp.toPx())
                            )
                        }
                    }
                } else {
                    Modifier
                }
            )
            .then(
                if (previouslyCompressed) {
                    Modifier.border(
                        width = 2.dp,
                        color = primary.copy(alpha = 0.45f + 0.55f * glowAlpha),
                        shape = shape
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (previouslyCompressed && onPreviouslyCompressedClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.free_space_previously_compressed),
                        onClick = onPreviouslyCompressedClick
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        val frame = bitmap
        if (frame != null && !frame.isRecycled) {
            Image(
                bitmap = frame.asImageBitmap(),
                contentDescription = if (previouslyCompressed) {
                    stringResource(R.string.free_space_previously_compressed)
                } else {
                    null
                },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = if (item.kind == MediaKind.VIDEO) {
                    Icons.Outlined.Videocam
                } else {
                    Icons.Outlined.Image
                },
                contentDescription = if (previouslyCompressed) {
                    stringResource(R.string.free_space_previously_compressed)
                } else {
                    null
                },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun loadMediaThumbnail(
    context: android.content.Context,
    item: MediaLibraryItem,
    px: Int
): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.loadThumbnail(item.uri, Size(px, px), null)
        } else {
            when (item.kind) {
                MediaKind.IMAGE -> {
                    val opts = android.graphics.BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    context.contentResolver.openInputStream(item.uri)?.use {
                        android.graphics.BitmapFactory.decodeStream(it, null, opts)
                    }
                    var sample = 1
                    val maxDim = maxOf(opts.outWidth, opts.outHeight).coerceAtLeast(1)
                    while (maxDim / sample > px * 2) sample *= 2
                    val decode = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
                    context.contentResolver.openInputStream(item.uri)?.use {
                        android.graphics.BitmapFactory.decodeStream(it, null, decode)
                    }
                }
                MediaKind.VIDEO -> {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, item.uri)
                        retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    } finally {
                        retriever.release()
                    }
                }
            }
        }
    } catch (_: Exception) {
        null
    }
}
