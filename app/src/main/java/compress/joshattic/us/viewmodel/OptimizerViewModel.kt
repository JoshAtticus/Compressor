package compress.joshattic.us.viewmodel

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MimeTypes
import compress.joshattic.us.compression.VideoCompressRequest
import compress.joshattic.us.compression.VideoCompressionEngine
import compress.joshattic.us.data.CompressedHistoryStore
import compress.joshattic.us.data.ExclusionFilterStore
import compress.joshattic.us.data.MediaLibraryRepository
import compress.joshattic.us.data.OptimizerQueueStore
import compress.joshattic.us.model.MediaKind
import compress.joshattic.us.model.OptimizerPermissionStatus
import compress.joshattic.us.model.OptimizerQualityMode
import compress.joshattic.us.model.OptimizerQueueItem
import compress.joshattic.us.model.OptimizerUiState
import compress.joshattic.us.model.QueueItemStatus
import compress.joshattic.us.utils.DeleteMediaResult
import compress.joshattic.us.utils.ImageCompressor
import compress.joshattic.us.utils.canScanMediaLibrary
import compress.joshattic.us.utils.deleteMediaUri
import compress.joshattic.us.utils.resolveOptimizerPermissionStatus
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OptimizerViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences by lazy {
        application.getSharedPreferences("compressor_prefs", Context.MODE_PRIVATE)
    }

    private val repository = MediaLibraryRepository(application)
    private val videoEngine = VideoCompressionEngine()

    private val _uiState = MutableStateFlow(OptimizerUiState())
    val uiState = _uiState.asStateFlow()

    private var queueJob: Job? = null
    private var cancelRequested = false

    /** Pending MediaStore publish while the user confirms deleting the original. */
    private var pendingReplaceQueueId: String? = null
    private var pendingPublishedUri: Uri? = null
    private var pendingCacheUri: Uri? = null

    init {
        applyQualityMode(OptimizerQualityMode.HIGH)
        restorePersistedOptimizerState()
        refreshPermissionStatus()
    }

    private fun restorePersistedOptimizerState() {
        viewModelScope.launch(Dispatchers.IO) {
            val restored = OptimizerQueueStore.load(prefs)
            val history = CompressedHistoryStore.load(prefs) +
                restored.filter { it.status == QueueItemStatus.DONE }.map { it.media.id }
            CompressedHistoryStore.save(prefs, history)
            val excludedIds = ExclusionFilterStore.loadIds(prefs)
            val excludedFolders = ExclusionFilterStore.loadFolders(prefs)
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        queue = restored,
                        compressedHistoryIds = history,
                        excludedIds = excludedIds,
                        excludedFolders = excludedFolders
                    )
                }
            }
            pruneOrphanOptimizerCache(restored)
            OptimizerQueueStore.save(prefs, restored)
        }
    }

    private fun persistExclusions() {
        val state = _uiState.value
        ExclusionFilterStore.save(prefs, state.excludedIds, state.excludedFolders)
    }

    private fun rememberCompressed(vararg mediaIds: Long) {
        val valid = mediaIds.filter { it > 0L }
        if (valid.isEmpty()) return
        val next = CompressedHistoryStore.add(prefs, *valid.toLongArray())
        _uiState.update { it.copy(compressedHistoryIds = next) }
    }

    private fun mediaStoreIdFromUri(uri: Uri): Long? {
        return uri.lastPathSegment?.toLongOrNull()
    }

    fun refreshPermissionStatus() {
        val status = resolveOptimizerPermissionStatus(getApplication())
        _uiState.update { it.copy(permissionStatus = status) }
        if (canScanMediaLibrary(status) && _uiState.value.allItems.isEmpty() && !_uiState.value.isScanning) {
            scanLibrary()
        }
    }

    fun onPermissionResult() {
        refreshPermissionStatus()
    }

    fun applyQualityMode(mode: OptimizerQualityMode) {
        val config = repository.videoConfigForMode(mode)
        val videoRatio = config.sizeRatio
        val photoQuality = repository.photoQualityForMode(mode)
        _uiState.update { state ->
            val updatedItems = state.allItems.map { item ->
                val (estimated, savings) = repository.estimateSavings(
                    item.sizeBytes,
                    item.kind,
                    videoRatio,
                    photoQuality
                )
                item.copy(estimatedOutputBytes = estimated, estimatedSavingsBytes = savings)
            }
            state.copy(
                qualityMode = mode,
                videoSizeRatio = videoRatio,
                photoQualityPercent = photoQuality,
                allItems = updatedItems
            )
        }
    }

    fun scanLibrary() {
        val status = _uiState.value.permissionStatus
        if (!canScanMediaLibrary(status)) return
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, scanError = null) }
            try {
                val state = _uiState.value
                val items = repository.scanMedia(state.videoSizeRatio, state.photoQualityPercent)
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        allItems = items,
                        selectedIds = it.selectedIds.intersect(items.map { item -> item.id }.toSet())
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        scanError = e.localizedMessage ?: "Scan failed"
                    )
                }
            }
        }
    }

    fun toggleSelection(id: Long) {
        _uiState.update { state ->
            val next = state.selectedIds.toMutableSet()
            if (!next.add(id)) next.remove(id)
            state.copy(selectedIds = next)
        }
    }

    fun selectAllVisible() {
        _uiState.update { it.copy(selectedIds = it.items.map { item -> item.id }.toSet()) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    fun excludeFile(id: Long) {
        _uiState.update { state ->
            state.copy(
                excludedIds = state.excludedIds + id,
                selectedIds = state.selectedIds - id
            )
        }
        persistExclusions()
    }

    fun excludeFolder(folderKey: String) {
        if (folderKey.isBlank()) return
        _uiState.update { state ->
            val hiddenIds = state.allItems
                .filter { it.folderKey == folderKey }
                .map { it.id }
                .toSet()
            state.copy(
                excludedFolders = state.excludedFolders + folderKey,
                selectedIds = state.selectedIds - hiddenIds
            )
        }
        persistExclusions()
    }

    fun clearExclusions() {
        _uiState.update {
            it.copy(excludedIds = emptySet(), excludedFolders = emptySet())
        }
        ExclusionFilterStore.clear(prefs)
    }

    fun enqueueSelected() {
        val selected = _uiState.value.selectedItems
        if (selected.isEmpty()) return
        val previous = _uiState.value.queue
        val queueItems = selected.map { media ->
            OptimizerQueueItem(
                id = UUID.randomUUID().toString(),
                media = media
            )
        }
        // Keep finished reviews; append new work. Drop unfinished leftovers.
        val kept = previous.filter {
            it.status == QueueItemStatus.DONE ||
                it.status == QueueItemStatus.SKIPPED ||
                it.status == QueueItemStatus.FAILED
        }
        _uiState.update {
            it.copy(
                queue = kept + queueItems,
                selectedIds = emptySet(),
                reviewingItemId = null
            )
        }
        persistQueue()
        startQueue()
    }

    fun clearQueue() {
        cancelQueue()
        val toDelete = _uiState.value.queue
        viewModelScope.launch(Dispatchers.IO) {
            deleteQueueOutputFiles(toDelete)
            clearAllOptimizerCache()
            OptimizerQueueStore.clear(prefs)
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(queue = emptyList(), reviewingItemId = null, isProcessingQueue = false)
                }
            }
        }
    }

    fun cancelQueue() {
        cancelRequested = true
        videoEngine.cancel()
        queueJob?.cancel()
        _uiState.update { state ->
            state.copy(
                isProcessingQueue = false,
                queue = state.queue.map { item ->
                    when (item.status) {
                        QueueItemStatus.QUEUED, QueueItemStatus.RUNNING ->
                            item.copy(status = QueueItemStatus.CANCELLED, progress = 0f)
                        else -> item
                    }
                }
            )
        }
        persistQueue()
    }

    fun openReview(itemId: String) {
        val item = _uiState.value.queue.firstOrNull { it.id == itemId } ?: return
        if (item.status != QueueItemStatus.DONE || item.reviewed) return
        _uiState.update { it.copy(reviewingItemId = itemId) }
    }

    fun closeReview() {
        _uiState.update { it.copy(reviewingItemId = null) }
    }

    /** Keep both originals + compressed; mark review finished so Review is hidden. */
    fun keepBothForReview() {
        val itemId = _uiState.value.reviewingItemId ?: return
        _uiState.update { state ->
            state.copy(
                reviewingItemId = null,
                queue = state.queue.map {
                    if (it.id == itemId) it.copy(reviewed = true) else it
                }
            )
        }
        persistQueue()
    }

    /** Re-queue a failed item and resume processing if idle. */
    fun retryFailed(itemId: String) {
        val item = _uiState.value.queue.firstOrNull { it.id == itemId } ?: return
        if (item.status != QueueItemStatus.FAILED) return
        deleteOutputFile(item.outputUri)
        updateQueueItem(itemId) {
            it.copy(
                status = QueueItemStatus.QUEUED,
                progress = 0f,
                outputUri = null,
                outputSizeBytes = 0L,
                outputWidth = 0,
                outputHeight = 0,
                outputFps = 0f,
                errorMessage = null,
                originalDeleted = false,
                reviewed = false
            )
        }
        if (!_uiState.value.isProcessingQueue) {
            startQueue()
        }
    }

    /**
     * Publish the compressed file into the original's album, then delete the original
     * so the library keeps the smaller file in its place.
     */
    fun deleteOriginalForReview(
        context: Context,
        onRequiresIntentSender: (IntentSender) -> Unit
    ) {
        val item = _uiState.value.reviewingItem ?: return
        if (item.outputUri == null || item.outputSizeBytes <= 0L) return
        viewModelScope.launch(Dispatchers.IO) {
            val published = publishCompressedBesideOriginal(context, item)
            if (published == null) {
                _uiState.update { state ->
                    state.copy(
                        queue = state.queue.map {
                            if (it.id == item.id) {
                                it.copy(errorMessage = "Could not save compressed file to gallery")
                            } else {
                                it
                            }
                        }
                    )
                }
                return@launch
            }
            when (val result = deleteMediaUri(context, item.media.uri)) {
                is DeleteMediaResult.Success -> {
                    finalizeOriginalReplace(
                        queueId = item.id,
                        publishedUri = published,
                        cacheUri = item.outputUri,
                        originalMediaId = item.media.id
                    )
                }
                is DeleteMediaResult.RequiresUserConsent -> {
                    pendingReplaceQueueId = item.id
                    pendingPublishedUri = published
                    pendingCacheUri = item.outputUri
                    withContext(Dispatchers.Main) {
                        onRequiresIntentSender(result.intentSender)
                    }
                }
                is DeleteMediaResult.Failed -> {
                    // Roll back the unpublished gallery copy.
                    runCatching { context.contentResolver.delete(published, null, null) }
                    _uiState.update { state ->
                        state.copy(
                            queue = state.queue.map {
                                if (it.id == item.id) {
                                    it.copy(errorMessage = result.error ?: "Delete failed")
                                } else {
                                    it
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    /** User cancelled the system delete prompt — drop the pending gallery copy. */
    fun cancelPendingOriginalReplace() {
        val published = pendingPublishedUri
        pendingReplaceQueueId = null
        pendingPublishedUri = null
        pendingCacheUri = null
        if (published != null) {
            viewModelScope.launch(Dispatchers.IO) {
                runCatching {
                    getApplication<Application>().contentResolver.delete(published, null, null)
                }
            }
        }
    }

    fun onOriginalDeletedForReview() {
        val queueId = pendingReplaceQueueId ?: _uiState.value.reviewingItemId ?: return
        val published = pendingPublishedUri
        val cacheUri = pendingCacheUri
            ?: _uiState.value.queue.firstOrNull { it.id == queueId }?.outputUri
        val originalMediaId = _uiState.value.queue.firstOrNull { it.id == queueId }?.media?.id
        if (published == null) {
            // Consent succeeded but publish never happened — mark deleted only.
            _uiState.update { state ->
                state.copy(
                    queue = state.queue.map {
                        if (it.id == queueId) it.copy(originalDeleted = true) else it
                    }
                )
            }
            persistQueue()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            finalizeOriginalReplace(
                queueId = queueId,
                publishedUri = published,
                cacheUri = cacheUri,
                originalMediaId = originalMediaId
            )
        }
    }

    private fun finalizeOriginalReplace(
        queueId: String,
        publishedUri: Uri,
        cacheUri: Uri?,
        originalMediaId: Long?
    ) {
        val context = getApplication<Application>()
        // Finish pending MediaStore row if we inserted with IS_PENDING.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                context.contentResolver.update(publishedUri, values, null, null)
            }
        }
        // Drop cache copy now that gallery holds the replacement.
        if (cacheUri != null && cacheUri != publishedUri) {
            deleteOutputFile(cacheUri)
        }
        pendingReplaceQueueId = null
        pendingPublishedUri = null
        pendingCacheUri = null

        _uiState.update { state ->
            state.copy(
                reviewingItemId = if (state.reviewingItemId == queueId) null else state.reviewingItemId,
                queue = state.queue.map {
                    if (it.id == queueId) {
                        it.copy(
                            originalDeleted = true,
                            reviewed = true,
                            outputUri = publishedUri,
                            errorMessage = null
                        )
                    } else {
                        it
                    }
                },
                // Hide the deleted original from the Free space list immediately.
                allItems = if (originalMediaId != null) {
                    state.allItems.filterNot { it.id == originalMediaId }
                } else {
                    state.allItems
                },
                selectedIds = if (originalMediaId != null) {
                    state.selectedIds - originalMediaId
                } else {
                    state.selectedIds
                }
            )
        }
        persistQueue()
        // Mark both the (deleted) original and the published replacement as compressed.
        val publishedId = mediaStoreIdFromUri(publishedUri)
        rememberCompressed(
            *listOfNotNull(originalMediaId, publishedId).toLongArray()
        )
        // Pick up the newly published compressed file in the library.
        scanLibrary()
    }

    /**
     * Writes the compressed cache file into the same album/folder as the original
     * (falls back to Movies/Compressor or Pictures/Compressor).
     */
    private fun publishCompressedBesideOriginal(
        context: Context,
        item: OptimizerQueueItem
    ): Uri? {
        val cacheUri = item.outputUri ?: return null
        val sourceFile = when (cacheUri.scheme) {
            "file" -> cacheUri.path?.let { File(it) }
            else -> null
        }
        if (sourceFile == null || !sourceFile.exists()) return null

        val relativePath = resolvePublishRelativePath(context, item)
        val displayName = item.media.displayName.ifBlank {
            if (item.media.kind == MediaKind.VIDEO) {
                "video_${item.media.id}.mp4"
            } else {
                "image_${item.media.id}.jpg"
            }
        }
        val mime = when (item.media.kind) {
            MediaKind.VIDEO -> "video/mp4"
            MediaKind.IMAGE -> "image/jpeg"
        }
        val collection = when (item.media.kind) {
            MediaKind.VIDEO -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
            MediaKind.IMAGE -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.MediaColumns.DATE_MODIFIED, System.currentTimeMillis() / 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.IS_PENDING, 1)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            }
        }
        val published = context.contentResolver.insert(collection, values) ?: return null
        return try {
            context.contentResolver.openOutputStream(published)?.use { out ->
                sourceFile.inputStream().use { input -> input.copyTo(out) }
            } ?: run {
                context.contentResolver.delete(published, null, null)
                return null
            }
            published
        } catch (_: Exception) {
            runCatching { context.contentResolver.delete(published, null, null) }
            null
        }
    }

    private fun resolvePublishRelativePath(context: Context, item: OptimizerQueueItem): String {
        val fromItem = item.media.folderKey.trim().trim('/')
        if (fromItem.isNotBlank()) return "$fromItem/"

        val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(MediaStore.MediaColumns.RELATIVE_PATH)
        } else {
            emptyArray()
        }
        if (projection.isNotEmpty()) {
            runCatching {
                context.contentResolver.query(item.media.uri, projection, null, null, null)
                    ?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val idx = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                            if (idx >= 0 && !cursor.isNull(idx)) {
                                val path = cursor.getString(idx)?.trim()?.trim('/')
                                if (!path.isNullOrBlank()) return "$path/"
                            }
                        }
                    }
            }
        }
        return when (item.media.kind) {
            MediaKind.VIDEO -> "${Environment.DIRECTORY_MOVIES}/Compressor/"
            MediaKind.IMAGE -> "${Environment.DIRECTORY_PICTURES}/Compressor/"
        }
    }

    /** Discard the compressed output and undo its contribution to space-saved totals. */
    fun deleteCompressedForReview() {
        val item = _uiState.value.reviewingItem ?: return
        viewModelScope.launch(Dispatchers.IO) {
            deleteOutputFile(item.outputUri)
            val saved = item.savedBytes
            if (saved > 0) {
                val total = (prefs.getLong("total_saved_bytes", 0L) - saved).coerceAtLeast(0L)
                prefs.edit { putLong("total_saved_bytes", total) }
            }
            _uiState.update { state ->
                val nextQueue = state.queue.map {
                    if (it.id == item.id) {
                        it.copy(
                            status = QueueItemStatus.CANCELLED,
                            outputUri = null,
                            outputSizeBytes = 0L,
                            outputWidth = 0,
                            outputHeight = 0,
                            outputFps = 0f,
                            progress = 0f,
                            errorMessage = null
                        )
                    } else {
                        it
                    }
                }
                state.copy(
                    reviewingItemId = null,
                    totalSavedThisSession = (state.totalSavedThisSession - saved).coerceAtLeast(0L),
                    queue = nextQueue
                )
            }
            persistQueue()
        }
    }

    private fun startQueue() {
        cancelRequested = false
        queueJob?.cancel()
        queueJob = viewModelScope.launch {
            _uiState.update { it.copy(isProcessingQueue = true) }
            // Re-check for QUEUED each round so mid-run retries are picked up.
            while (!cancelRequested) {
                val next = _uiState.value.queue.firstOrNull {
                    it.status == QueueItemStatus.QUEUED
                } ?: break
                updateQueueItem(next.id) {
                    it.copy(status = QueueItemStatus.RUNNING, progress = 0f)
                }
                processItem(next)
            }
            _uiState.update { it.copy(isProcessingQueue = false) }
            persistQueue()
            pruneOrphanOptimizerCache(_uiState.value.queue)
        }
    }

    private suspend fun processItem(item: OptimizerQueueItem) {
        val context = getApplication<Application>()
        val state = _uiState.value
        try {
            when (item.media.kind) {
                MediaKind.IMAGE -> {
                    val dir = File(context.cacheDir, "optimizer_images")
                    val name = item.media.displayName.substringBeforeLast('.') + "_Compressed.jpg"
                    val result = ImageCompressor.compress(
                        context = context,
                        sourceUri = item.media.uri,
                        qualityPercent = state.photoQualityPercent,
                        outputDir = dir,
                        outputName = name
                    )
                    if (result.skippedBecauseLarger) {
                        updateQueueItem(item.id) {
                            it.copy(
                                status = QueueItemStatus.SKIPPED,
                                progress = 1f,
                                outputSizeBytes = item.media.sizeBytes
                            )
                        }
                    } else {
                        addSavedBytes(item.media.sizeBytes - result.outputSizeBytes)
                        updateQueueItem(item.id) {
                            it.copy(
                                status = QueueItemStatus.DONE,
                                progress = 1f,
                                outputUri = Uri.fromFile(result.outputFile),
                                outputSizeBytes = result.outputSizeBytes
                            )
                        }
                        rememberCompressed(item.media.id)
                    }
                }
                MediaKind.VIDEO -> {
                    val config = repository.videoConfigForMode(state.qualityMode)
                    val preferredCodec = if (prefs.getBoolean("all_codecs_enabled", false)) {
                        MimeTypes.VIDEO_H265
                    } else if (hasHevc()) {
                        MimeTypes.VIDEO_H265
                    } else {
                        MimeTypes.VIDEO_H264
                    }
                    val result = videoEngine.compress(
                        context = context,
                        request = VideoCompressRequest(
                            inputUri = item.media.uri,
                            originalSizeBytes = item.media.sizeBytes,
                            originalWidth = item.media.width,
                            originalHeight = item.media.height,
                            durationMs = item.media.durationMs,
                            originalFps = item.media.fps,
                            displayName = item.media.displayName,
                            presetConfig = config,
                            preferredVideoCodec = preferredCodec,
                            // Prefer hitting target gently; avoid extra cushion that pushes quality down.
                            extraSizeCushion = false
                        ),
                        onProgress = { progress, size ->
                            updateQueueItem(item.id) {
                                it.copy(progress = progress, outputSizeBytes = size)
                            }
                        }
                    )
                    when {
                        result.errorMessage != null -> {
                            updateQueueItem(item.id) {
                                it.copy(
                                    status = QueueItemStatus.FAILED,
                                    errorMessage = result.errorMessage,
                                    progress = 0f
                                )
                            }
                        }
                        result.skippedBecauseLarger -> {
                            updateQueueItem(item.id) {
                                it.copy(
                                    status = QueueItemStatus.SKIPPED,
                                    progress = 1f,
                                    outputSizeBytes = item.media.sizeBytes
                                )
                            }
                        }
                        else -> {
                            addSavedBytes(item.media.sizeBytes - result.outputSizeBytes)
                            updateQueueItem(item.id) {
                                it.copy(
                                    status = QueueItemStatus.DONE,
                                    progress = 1f,
                                    outputUri = Uri.fromFile(result.outputFile),
                                    outputSizeBytes = result.outputSizeBytes,
                                    outputWidth = result.outputWidth,
                                    outputHeight = result.outputHeight,
                                    outputFps = result.outputFps
                                )
                            }
                            rememberCompressed(item.media.id)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            updateQueueItem(item.id) {
                it.copy(
                    status = QueueItemStatus.FAILED,
                    errorMessage = e.localizedMessage ?: "Failed",
                    progress = 0f
                )
            }
        }
    }

    private fun addSavedBytes(saved: Long) {
        if (saved <= 0) return
        val total = prefs.getLong("total_saved_bytes", 0L) + saved
        prefs.edit { putLong("total_saved_bytes", total) }
        _uiState.update { it.copy(totalSavedThisSession = it.totalSavedThisSession + saved) }
    }

    private fun updateQueueItem(id: String, transform: (OptimizerQueueItem) -> OptimizerQueueItem) {
        _uiState.update { state ->
            state.copy(queue = state.queue.map { if (it.id == id) transform(it) else it })
        }
        val updated = _uiState.value.queue.firstOrNull { it.id == id }
        if (updated != null && updated.status != QueueItemStatus.RUNNING &&
            updated.status != QueueItemStatus.QUEUED
        ) {
            persistQueue()
        }
    }

    private fun persistQueue() {
        OptimizerQueueStore.save(prefs, _uiState.value.queue)
    }

    private fun deleteOutputFile(uri: Uri?) {
        if (uri == null) return
        when (uri.scheme) {
            "file" -> uri.path?.let { path -> runCatching { File(path).delete() } }
            else -> runCatching { deleteMediaUri(getApplication(), uri) }
        }
    }

    private fun deleteQueueOutputFiles(queue: List<OptimizerQueueItem>) {
        queue.forEach { deleteOutputFile(it.outputUri) }
    }

    private fun optimizerCacheDirs(): List<File> {
        val cache = getApplication<Application>().cacheDir
        return listOf(File(cache, "optimizer_videos"), File(cache, "optimizer_images"))
    }

    private fun clearAllOptimizerCache() {
        optimizerCacheDirs().forEach { dir ->
            if (dir.exists()) {
                dir.listFiles()?.forEach { runCatching { it.delete() } }
            }
        }
    }

    /** Delete cache files that are not referenced by the current/persisted queue. */
    private fun pruneOrphanOptimizerCache(queue: List<OptimizerQueueItem>) {
        val keep = queue.mapNotNull { item ->
            item.outputUri?.takeIf { it.scheme == "file" }?.path
        }.toSet()
        optimizerCacheDirs().forEach { dir ->
            if (!dir.exists()) return@forEach
            dir.listFiles()?.forEach { file ->
                if (file.absolutePath !in keep) {
                    runCatching { file.delete() }
                }
            }
        }
    }

    private fun hasHevc(): Boolean {
        return try {
            android.media.MediaCodecList(android.media.MediaCodecList.ALL_CODECS).codecInfos.any { info ->
                info.isEncoder && info.supportedTypes.any {
                    it.equals(MimeTypes.VIDEO_H265, ignoreCase = true)
                }
            }
        } catch (_: Exception) {
            false
        }
    }
}
