package compress.joshattic.us.ui.screens.optimizer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import compress.joshattic.us.R
import compress.joshattic.us.model.OptimizerQueueItem
import compress.joshattic.us.model.OptimizerUiState
import compress.joshattic.us.model.QueueItemStatus
import compress.joshattic.us.utils.formatFileSize

@Composable
fun OptimizerQueueScreen(
    state: OptimizerUiState,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    onClear: () -> Unit,
    onReview: (String) -> Unit,
    onRetry: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
            Text(
                text = stringResource(R.string.free_space_queue_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (state.isProcessingQueue) {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel))
                }
            } else {
                TextButton(onClick = onClear) {
                    Text(stringResource(R.string.free_space_clear_queue))
                }
            }
        }

        if (state.totalSavedThisSession > 0) {
            Text(
                text = stringResource(
                    R.string.free_space_session_saved,
                    formatFileSize(state.totalSavedThisSession)
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.queue, key = { it.id }) { item ->
                val canReview = item.status == QueueItemStatus.DONE && !item.reviewed
                val canRetry = item.status == QueueItemStatus.FAILED
                QueueRow(
                    item = item,
                    canReview = canReview,
                    canRetry = canRetry,
                    onReview = { onReview(item.id) },
                    onRetry = { onRetry(item.id) }
                )
            }
        }
    }
}

@Composable
private fun QueueRow(
    item: OptimizerQueueItem,
    canReview: Boolean,
    canRetry: Boolean,
    onReview: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canReview, onClick = onReview)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.media.displayName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = statusLabel(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            when {
                canReview -> {
                    Button(onClick = onReview) {
                        Text(stringResource(R.string.free_space_review))
                    }
                }
                canRetry -> {
                    OutlinedButton(onClick = onRetry) {
                        Text(stringResource(R.string.free_space_retry))
                    }
                }
            }
        }
        if (item.status == QueueItemStatus.RUNNING) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { item.progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun statusLabel(item: OptimizerQueueItem): String {
    return when (item.status) {
        QueueItemStatus.QUEUED -> stringResource(R.string.free_space_status_queued)
        QueueItemStatus.RUNNING -> stringResource(
            R.string.free_space_status_running,
            (item.progress * 100).toInt()
        )
        QueueItemStatus.DONE -> stringResource(
            R.string.free_space_status_done,
            formatFileSize(item.media.sizeBytes),
            formatFileSize(item.outputSizeBytes)
        )
        QueueItemStatus.SKIPPED -> stringResource(R.string.free_space_status_skipped)
        QueueItemStatus.FAILED -> item.errorMessage
            ?: stringResource(R.string.free_space_status_failed)
        QueueItemStatus.CANCELLED -> stringResource(R.string.free_space_status_cancelled)
    }
}
