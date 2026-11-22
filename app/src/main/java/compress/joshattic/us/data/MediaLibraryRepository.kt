package compress.joshattic.us.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import compress.joshattic.us.model.MediaKind
import compress.joshattic.us.model.MediaLibraryItem
import compress.joshattic.us.model.OptimizerQualityMode
import compress.joshattic.us.model.QualityPresetConfig
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaLibraryRepository(
    private val context: Context
) {
    suspend fun scanMedia(
        videoSizeRatio: Float,
        photoQualityPercent: Int
    ): List<MediaLibraryItem> = withContext(Dispatchers.IO) {
        val videos = queryVideos(videoSizeRatio)
        val images = queryImages(photoQualityPercent)
        (videos + images).sortedByDescending { it.sizeBytes }
    }

    /**
     * Returns (estimatedOutputBytes, estimatedSavingsBytes).
     *
     * Video estimates mirror [compress.joshattic.us.compression.VideoCompressionEngine]
     * `computeTargetBitrate`: sizeRatio → 4% cushion → 2% + 50KB muxer overhead.
     * Calibrated on device (OnePlus CPH2747): 927.5 MB @ High 0.88 → ~767.9 MB actual.
     */
    fun estimateSavings(
        sizeBytes: Long,
        kind: MediaKind,
        videoSizeRatio: Float,
        photoQualityPercent: Int
    ): Pair<Long, Long> {
        if (sizeBytes <= 0L) return 0L to 0L
        val estimatedOutput = when (kind) {
            MediaKind.VIDEO -> estimateVideoOutputBytes(sizeBytes, videoSizeRatio)
            MediaKind.IMAGE -> {
                // JPEG quality is not linear with bytes; use a soft curve toward the quality %.
                val q = (photoQualityPercent / 100f).coerceIn(0.2f, 0.95f)
                val ratio = (0.35f + q * 0.60f).coerceIn(0.25f, 0.95f)
                (sizeBytes * ratio).toLong().coerceAtLeast(1L)
            }
        }.coerceAtMost(sizeBytes)
        val savings = (sizeBytes - estimatedOutput).coerceAtLeast(0L)
        return estimatedOutput to savings
    }

    /** Shared with unit tests — keeps list estimates aligned with the encoder budget. */
    fun estimateVideoOutputBytes(sizeBytes: Long, videoSizeRatio: Float): Long {
        if (sizeBytes <= 0L) return 0L
        val ratio = videoSizeRatio.coerceIn(0.05f, 0.95f)
        val cushioned = sizeBytes.toDouble() * ratio * VIDEO_ENCODER_SIZE_CUSHION
        val overhead = cushioned * VIDEO_ENCODER_OVERHEAD_FRACTION + VIDEO_ENCODER_OVERHEAD_BYTES
        return (cushioned - overhead)
            .coerceAtLeast(cushioned * 0.1)
            .toLong() // truncates toward zero; within ~1 byte of exact for large files
            .coerceAtLeast(1L)
            .coerceAtMost(sizeBytes)
    }

    fun videoConfigForMode(mode: OptimizerQualityMode): QualityPresetConfig {
        // Size/quality tradeoff. Resolution/FPS may drop for Medium/Low, but
        // never below FREE_SPACE_MIN_* (720p / 24fps) — that is the most we reduce.
        // High keeps source resolution/FPS and only targets a gentler size ratio.
        // Engine also clamps — see VideoCompressionEngine.
        return when (mode) {
            OptimizerQualityMode.HIGH -> QualityPresetConfig(
                resolutionShortSide = 0, // keep original
                targetFps = 0, // keep original
                sizeRatio = 0.88f,
                audioBitrate = 256_000
            )
            OptimizerQualityMode.MEDIUM -> QualityPresetConfig(
                resolutionShortSide = FREE_SPACE_MEDIUM_SHORT_SIDE,
                targetFps = FREE_SPACE_MEDIUM_FPS,
                sizeRatio = 0.72f,
                audioBitrate = 192_000
            )
            OptimizerQualityMode.LOW -> QualityPresetConfig(
                resolutionShortSide = FREE_SPACE_MIN_SHORT_SIDE,
                targetFps = FREE_SPACE_MIN_FPS,
                sizeRatio = 0.55f,
                audioBitrate = 160_000
            )
        }
    }

    fun photoQualityForMode(mode: OptimizerQualityMode): Int {
        return when (mode) {
            OptimizerQualityMode.HIGH -> 92
            OptimizerQualityMode.MEDIUM -> 85
            OptimizerQualityMode.LOW -> 75
        }
    }

    private fun queryVideos(videoSizeRatio: Float): List<MediaLibraryItem> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val projection = buildList {
            add(MediaStore.Video.Media._ID)
            add(MediaStore.Video.Media.DISPLAY_NAME)
            add(MediaStore.Video.Media.SIZE)
            add(MediaStore.Video.Media.WIDTH)
            add(MediaStore.Video.Media.HEIGHT)
            add(MediaStore.Video.Media.DURATION)
            add(MediaStore.Video.Media.DATE_ADDED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Video.Media.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                add(MediaStore.Video.Media.DATA)
            }
        }.toTypedArray()
        val items = mutableListOf<MediaLibraryItem>()
        context.contentResolver.query(
            collection,
            projection,
            "${MediaStore.Video.Media.SIZE} > 0",
            null,
            "${MediaStore.Video.Media.SIZE} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val pathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                cursor.getColumnIndex(MediaStore.Video.Media.DATA)
            }
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                val size = resolveAccurateSize(uri, cursor.getLong(sizeCol))
                val (estimated, savings) = estimateSavings(size, MediaKind.VIDEO, videoSizeRatio, 70)
                val rawPath = if (pathCol >= 0 && !cursor.isNull(pathCol)) cursor.getString(pathCol) else null
                val (folderKey, folderLabel) = resolveFolder(rawPath, isRelative = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                items.add(
                    MediaLibraryItem(
                        id = id,
                        uri = uri,
                        kind = MediaKind.VIDEO,
                        displayName = cursor.getString(nameCol) ?: "video_$id",
                        sizeBytes = size,
                        width = cursor.getInt(widthCol),
                        height = cursor.getInt(heightCol),
                        durationMs = cursor.getLong(durationCol),
                        dateAddedSeconds = cursor.getLong(dateCol),
                        folderKey = folderKey,
                        folderLabel = folderLabel,
                        estimatedOutputBytes = estimated,
                        estimatedSavingsBytes = savings
                    )
                )
            }
        }
        return items
    }

    private fun queryImages(photoQualityPercent: Int): List<MediaLibraryItem> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val projection = buildList {
            add(MediaStore.Images.Media._ID)
            add(MediaStore.Images.Media.DISPLAY_NAME)
            add(MediaStore.Images.Media.SIZE)
            add(MediaStore.Images.Media.WIDTH)
            add(MediaStore.Images.Media.HEIGHT)
            add(MediaStore.Images.Media.DATE_ADDED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Images.Media.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                add(MediaStore.Images.Media.DATA)
            }
        }.toTypedArray()
        val items = mutableListOf<MediaLibraryItem>()
        // Offset image IDs so they don't collide with video MediaStore IDs in selection sets.
        val imageIdOffset = 1_000_000_000_000L
        context.contentResolver.query(
            collection,
            projection,
            "${MediaStore.Images.Media.SIZE} > 0",
            null,
            "${MediaStore.Images.Media.SIZE} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val pathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                cursor.getColumnIndex(MediaStore.Images.Media.DATA)
            }
            while (cursor.moveToNext()) {
                val mediaStoreId = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, mediaStoreId)
                val size = resolveAccurateSize(uri, cursor.getLong(sizeCol))
                val (estimated, savings) = estimateSavings(size, MediaKind.IMAGE, 0.4f, photoQualityPercent)
                val rawPath = if (pathCol >= 0 && !cursor.isNull(pathCol)) cursor.getString(pathCol) else null
                val (folderKey, folderLabel) = resolveFolder(rawPath, isRelative = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                items.add(
                    MediaLibraryItem(
                        id = imageIdOffset + mediaStoreId,
                        uri = uri,
                        kind = MediaKind.IMAGE,
                        displayName = cursor.getString(nameCol) ?: "image_$mediaStoreId",
                        sizeBytes = size,
                        width = cursor.getInt(widthCol),
                        height = cursor.getInt(heightCol),
                        durationMs = 0L,
                        dateAddedSeconds = cursor.getLong(dateCol),
                        folderKey = folderKey,
                        folderLabel = folderLabel,
                        estimatedOutputBytes = estimated,
                        estimatedSavingsBytes = savings
                    )
                )
            }
        }
        return items
    }

    /**
     * Prefer MediaStore SIZE when present; fall back to the real descriptor length
     * when the provider reports 0/missing (common on some OEM builds).
     */
    private fun resolveAccurateSize(uri: Uri, reportedSize: Long): Long {
        if (reportedSize > 0L) return reportedSize
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    companion object {
        /** Same cushion factor as VideoCompressionEngine target bitrate. */
        const val VIDEO_ENCODER_SIZE_CUSHION = 0.96f
        /** Matches computeTargetBitrate 2% container overhead fraction. */
        const val VIDEO_ENCODER_OVERHEAD_FRACTION = 0.02
        /** Matches computeTargetBitrate fixed 50KB muxer overhead. */
        const val VIDEO_ENCODER_OVERHEAD_BYTES = 50L * 1024L

        /**
         * Lowest Free-space output short side. Compress may reduce toward this,
         * but never below it (the most reduction allowed).
         */
        const val FREE_SPACE_MIN_SHORT_SIDE = 720
        /**
         * Lowest Free-space output fps. Compress may reduce toward this,
         * but never below it (the most reduction allowed).
         */
        const val FREE_SPACE_MIN_FPS = 24

        const val FREE_SPACE_MEDIUM_SHORT_SIDE = 1080
        const val FREE_SPACE_MEDIUM_FPS = 30

        fun resolveFolder(rawPath: String?, isRelative: Boolean): Pair<String, String> {
            if (rawPath.isNullOrBlank()) return "" to ""
            val key = if (isRelative) {
                rawPath.trim().trimEnd('/')
            } else {
                File(rawPath).parent?.trimEnd('/', '\\')?.replace('\\', '/') ?: ""
            }
            if (key.isBlank()) return "" to ""
            val label = key.substringAfterLast('/').ifBlank { key }
            return key to label
        }
    }
}
