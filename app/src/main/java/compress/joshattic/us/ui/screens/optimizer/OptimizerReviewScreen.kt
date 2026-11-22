package compress.joshattic.us.ui.screens.optimizer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import compress.joshattic.us.R
import compress.joshattic.us.model.MediaKind
import compress.joshattic.us.model.OptimizerQueueItem
import compress.joshattic.us.utils.MediaMetaProbe
import compress.joshattic.us.utils.ProbedMediaMeta
import compress.joshattic.us.utils.formatFileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun OptimizerReviewScreen(
    item: OptimizerQueueItem,
    onBack: () -> Unit,
    onKeepBoth: () -> Unit,
    onDeleteOriginal: () -> Unit,
    onDeleteCompressed: () -> Unit
) {
    val context = LocalContext.current
    val outputUri = item.outputUri
    var showingCompressed by remember(item.id) { mutableStateOf(true) }

    val originalMeta by produceState(
        initialValue = ProbedMediaMeta(
            width = item.media.width,
            height = item.media.height,
            fps = item.media.fps,
            durationMs = item.media.durationMs
        ),
        item.media.uri,
        item.media.kind
    ) {
        if (item.media.kind != MediaKind.VIDEO) return@produceState
        value = withContext(Dispatchers.IO) {
            val probed = MediaMetaProbe.probe(context, item.media.uri)
            ProbedMediaMeta(
                width = probed.width.takeIf { it > 0 } ?: item.media.width,
                height = probed.height.takeIf { it > 0 } ?: item.media.height,
                fps = probed.fps.takeIf { it > 0f } ?: item.media.fps,
                durationMs = probed.durationMs.takeIf { it > 0L } ?: item.media.durationMs
            )
        }
    }

    val compressedMeta by produceState(
        initialValue = ProbedMediaMeta(
            width = item.outputWidth,
            height = item.outputHeight,
            fps = item.outputFps,
            durationMs = item.media.durationMs
        ),
        outputUri,
        item.outputWidth,
        item.outputHeight,
        item.outputFps
    ) {
        if (item.media.kind != MediaKind.VIDEO || outputUri == null) return@produceState
        // Always re-probe: stored capture-tag fps can disagree with real encoded track fps.
        value = withContext(Dispatchers.IO) {
            val probed = MediaMetaProbe.probe(context, outputUri)
            ProbedMediaMeta(
                width = probed.width.takeIf { it > 0 } ?: item.outputWidth,
                height = probed.height.takeIf { it > 0 } ?: item.outputHeight,
                fps = probed.fps.takeIf { it > 0f } ?: item.outputFps,
                durationMs = probed.durationMs.takeIf { it > 0L } ?: item.media.durationMs
            )
        }
    }

    val activeMeta = if (showingCompressed) compressedMeta else originalMeta
    val activeSize = if (showingCompressed) item.outputSizeBytes else item.media.sizeBytes

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
            Text(
                text = stringResource(R.string.free_space_review_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = item.media.displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                ReviewSideCard(
                    title = stringResource(
                        if (showingCompressed) {
                            R.string.free_space_compressed
                        } else {
                            R.string.original
                        }
                    ),
                    sizeLabel = formatFileSize(activeSize),
                    width = if (item.media.kind == MediaKind.VIDEO) {
                        activeMeta.width
                    } else {
                        item.media.width
                    },
                    height = if (item.media.kind == MediaKind.VIDEO) {
                        activeMeta.height
                    } else {
                        item.media.height
                    },
                    fps = if (item.media.kind == MediaKind.VIDEO) activeMeta.fps else 0f,
                    showFps = item.media.kind == MediaKind.VIDEO,
                    emphasize = showingCompressed,
                    modifier = Modifier.weight(1f)
                )
                if (outputUri != null) {
                    FilledTonalButton(
                        onClick = { showingCompressed = !showingCompressed }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SwapHoriz,
                            contentDescription = stringResource(
                                R.string.free_space_switch_version
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                if (showingCompressed) {
                                    R.string.free_space_show_original
                                } else {
                                    R.string.free_space_show_compressed
                                }
                            )
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            if (outputUri != null) {
                when (item.media.kind) {
                    MediaKind.VIDEO -> ReviewComparePlayer(
                        originalUri = item.media.uri,
                        compressedUri = outputUri,
                        showingCompressed = showingCompressed,
                        durationHintMs = maxOf(
                            originalMeta.durationMs,
                            compressedMeta.durationMs,
                            item.media.durationMs
                        )
                    )
                    MediaKind.IMAGE -> ReviewImagePreview(
                        uri = if (showingCompressed) outputUri else item.media.uri
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }

        if (item.originalDeleted) {
            Text(
                text = stringResource(R.string.original_deleted),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Button(onClick = onKeepBoth, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.free_space_done))
            }
            Spacer(Modifier.height(8.dp))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onKeepBoth,
                        modifier = Modifier.weight(1f),
                        contentPadding = ButtonDefaults.ContentPadding
                    ) {
                        Text(
                            text = stringResource(R.string.free_space_keep_both),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                    OutlinedButton(
                        onClick = onDeleteOriginal,
                        modifier = Modifier.weight(1f),
                        contentPadding = ButtonDefaults.ContentPadding
                    ) {
                        Text(
                            text = stringResource(R.string.free_space_delete_original),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                TextButton(
                    onClick = onDeleteCompressed,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = ButtonDefaults.TextButtonContentPadding
                ) {
                    Text(
                        text = stringResource(R.string.free_space_delete_compressed),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewSideCard(
    title: String,
    sizeLabel: String,
    width: Int,
    height: Int,
    fps: Float,
    showFps: Boolean,
    emphasize: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = sizeLabel,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (emphasize) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
        val tech = buildTechLabel(width, height, fps, showFps)
        if (tech.isNotBlank()) {
            Text(
                text = tech,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReviewImagePreview(uri: Uri) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) {
                null
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        val frame = bitmap
        if (frame != null && !frame.isRecycled) {
            Image(
                bitmap = frame.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun buildTechLabel(width: Int, height: Int, fps: Float, showFps: Boolean): String {
    val parts = mutableListOf<String>()
    if (width > 0 && height > 0) {
        parts.add("${width}×${height}")
    }
    if (showFps && fps > 0f) {
        val fpsLabel = if (fps % 1f == 0f) {
            "${fps.toInt()} fps"
        } else {
            String.format("%.2f fps", fps)
        }
        parts.add(fpsLabel)
    }
    return parts.joinToString(" · ")
}
