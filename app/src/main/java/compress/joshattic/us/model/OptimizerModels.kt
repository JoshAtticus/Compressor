package compress.joshattic.us.model

import android.net.Uri
import compress.joshattic.us.utils.formatFileSize

enum class MainDestination {
    COMPRESS,
    FREE_SPACE
}

enum class MediaKind {
    VIDEO,
    IMAGE
}

enum class OptimizerPermissionStatus {
    UNKNOWN,
    GRANTED,
    PARTIAL,
    DENIED
}

enum class QueueItemStatus {
    QUEUED,
    RUNNING,
    DONE,
    SKIPPED,
    FAILED,
    CANCELLED
}

/**
 * Free space High/Medium/Low size tradeoffs. Medium/Low may lower resolution/FPS
 * for savings; 720p / 24fps is the most reduction allowed (never go lower).
 */
enum class OptimizerQualityMode {
    HIGH,
    MEDIUM,
    LOW
}

data class MediaLibraryItem(
    val id: Long,
    val uri: Uri,
    val kind: MediaKind,
    val displayName: String,
    val sizeBytes: Long,
    val width: Int = 0,
    val height: Int = 0,
    /** Capture/encode fps when known; 0 if unavailable from MediaStore. */
    val fps: Float = 0f,
    val durationMs: Long = 0L,
    val dateAddedSeconds: Long = 0L,
    /** Stable folder key used for temporary exclusions (relative path or parent path). */
    val folderKey: String = "",
    val folderLabel: String = "",
    val estimatedOutputBytes: Long = sizeBytes,
    val estimatedSavingsBytes: Long = 0L
) {
    val formattedSize: String get() = formatFileSize(sizeBytes)
    val formattedSavings: String get() = formatFileSize(estimatedSavingsBytes)
}

data class OptimizerQueueItem(
    val id: String,
    val media: MediaLibraryItem,
    val status: QueueItemStatus = QueueItemStatus.QUEUED,
    val progress: Float = 0f,
    val outputUri: Uri? = null,
    val outputSizeBytes: Long = 0L,
    val outputWidth: Int = 0,
    val outputHeight: Int = 0,
    val outputFps: Float = 0f,
    val errorMessage: String? = null,
    val originalDeleted: Boolean = false,
    /** True after the user finishes review (keep both / replace). Hides the Review button. */
    val reviewed: Boolean = false
) {
    val savedBytes: Long
        get() = if (outputSizeBytes > 0 && media.sizeBytes > outputSizeBytes) {
            media.sizeBytes - outputSizeBytes
        } else {
            0L
        }
}

data class OptimizerUiState(
    val permissionStatus: OptimizerPermissionStatus = OptimizerPermissionStatus.UNKNOWN,
    val isScanning: Boolean = false,
    val scanError: String? = null,
    val allItems: List<MediaLibraryItem> = emptyList(),
    val excludedIds: Set<Long> = emptySet(),
    val excludedFolders: Set<String> = emptySet(),
    val selectedIds: Set<Long> = emptySet(),
    val qualityMode: OptimizerQualityMode = OptimizerQualityMode.HIGH,
    val videoSizeRatio: Float = 0.90f,
    val photoQualityPercent: Int = 92,
    val queue: List<OptimizerQueueItem> = emptyList(),
    val isProcessingQueue: Boolean = false,
    val reviewingItemId: String? = null,
    val totalSavedThisSession: Long = 0L,
    /** MediaStore IDs successfully compressed before (survives queue clear). */
    val compressedHistoryIds: Set<Long> = emptySet()
) {
    val items: List<MediaLibraryItem>
        get() = allItems.filter { item ->
            item.id !in excludedIds &&
                (item.folderKey.isBlank() || item.folderKey !in excludedFolders)
        }

    val selectedItems: List<MediaLibraryItem>
        get() = items.filter { it.id in selectedIds }

    val estimatedReclaimBytes: Long
        get() = selectedItems.sumOf { it.estimatedSavingsBytes }

    val formattedEstimatedReclaim: String
        get() = formatFileSize(estimatedReclaimBytes)

    val reviewingItem: OptimizerQueueItem?
        get() = queue.firstOrNull { it.id == reviewingItemId }

    val hasActiveQueue: Boolean
        get() = queue.isNotEmpty()

    val hasExclusions: Boolean
        get() = excludedIds.isNotEmpty() || excludedFolders.isNotEmpty()

    val exclusionCount: Int
        get() = excludedIds.size + excludedFolders.size
}
