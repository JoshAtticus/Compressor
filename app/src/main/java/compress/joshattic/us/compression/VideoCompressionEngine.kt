package compress.joshattic.us.compression

import android.content.Context
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.AudioEncoderSettings
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultDecoderFactory
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import compress.joshattic.us.data.MediaLibraryRepository
import compress.joshattic.us.model.QualityPresetConfig
import compress.joshattic.us.utils.VolumeAudioProcessor
import java.io.File
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

data class VideoCompressRequest(
    val inputUri: Uri,
    val originalSizeBytes: Long,
    val originalWidth: Int,
    val originalHeight: Int,
    val durationMs: Long,
    val originalFps: Float,
    val displayName: String?,
    val presetConfig: QualityPresetConfig,
    val preferredVideoCodec: String = MimeTypes.VIDEO_H265,
    val extraSizeCushion: Boolean = false
)

data class VideoCompressResult(
    val outputFile: File,
    val outputSizeBytes: Long,
    val skippedBecauseLarger: Boolean,
    val outputWidth: Int = 0,
    val outputHeight: Int = 0,
    val outputFps: Float = 0f,
    val errorMessage: String? = null
)

/**
 * Shared Media3 Transformer path for Free space batch jobs.
 * Compress tab continues to use [compress.joshattic.us.viewmodel.CompressorViewModel] for full UX.
 */
@OptIn(UnstableApi::class)
class VideoCompressionEngine {

    @Volatile
    private var activeTransformer: Transformer? = null

    fun cancel() {
        activeTransformer?.cancel()
        activeTransformer = null
    }

    suspend fun compress(
        context: Context,
        request: VideoCompressRequest,
        onProgress: (Float, Long) -> Unit = { _, _ -> }
    ): VideoCompressResult = withContext(Dispatchers.Main) {
        val outputDir = File(context.cacheDir, "optimizer_videos")
        outputDir.mkdirs()
        val base = request.displayName?.substringBeforeLast('.') ?: "video_${System.currentTimeMillis()}"
        val outputFile = File(outputDir, "${base}_Compressed.mp4")
        if (outputFile.exists()) outputFile.delete()

        val probe = withContext(Dispatchers.IO) { probe(context, request.inputUri) }
        // Prefer rotation-corrected probe size. MediaStore width/height often ignore
        // orientation and would make portrait clips scale like landscape (wrong ratio).
        val width = when {
            probe.width > 0 -> probe.width
            request.originalWidth > 0 -> request.originalWidth
            else -> 0
        }
        val height = when {
            probe.height > 0 -> probe.height
            request.originalHeight > 0 -> request.originalHeight
            else -> 0
        }
        val fps = if (request.originalFps > 0f) request.originalFps else probe.fps
        val durationMs = if (request.durationMs > 0) request.durationMs else probe.durationMs
        val originalSize = if (request.originalSizeBytes > 0) {
            request.originalSizeBytes
        } else {
            probe.sizeBytes
        }

        val config = request.presetConfig
        // Optional short-side cap for savings. 0 = keep source. Never below 720p floor
        // (that floor is the most Free space is allowed to reduce).
        val cappedShortSide = if (config.resolutionShortSide > 0) {
            config.resolutionShortSide.coerceAtLeast(MediaLibraryRepository.FREE_SPACE_MIN_SHORT_SIDE)
        } else {
            0
        }
        // Optional fps cap. 0 = keep source. Never below 24fps floor.
        val requestedFps = if (config.targetFps > 0) {
            config.targetFps.coerceAtLeast(MediaLibraryRepository.FREE_SPACE_MIN_FPS)
        } else {
            0
        }
        val targetFps = if (requestedFps > 0 && fps > requestedFps + 0.5f) requestedFps else 0
        val targetSizeMb = (originalSize / (1024.0 * 1024.0) * config.sizeRatio)
            .toFloat()
            .coerceAtLeast(0.1f)
        val targetBitrate = computeTargetBitrate(
            targetSizeMb = targetSizeMb,
            durationMs = durationMs,
            audioBitrate = config.audioBitrate,
            extraSizeCushion = request.extraSizeCushion
        )

        val videoMime = if (hasEncoder(request.preferredVideoCodec)) {
            request.preferredVideoCodec
        } else {
            MimeTypes.VIDEO_H264
        }

        suspendCancellableCoroutine { cont ->
            val decoderFactory = DefaultDecoderFactory.Builder(context)
                .setEnableDecoderFallback(true)
                .build()

            val encoderFactory = DefaultEncoderFactory.Builder(context)
                .setEnableFallback(true)
                .setRequestedVideoEncoderSettings(
                    VideoEncoderSettings.Builder()
                        .setBitrate(targetBitrate)
                        .setBitrateMode(android.media.MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
                        .build()
                )
                .setRequestedAudioEncoderSettings(
                    AudioEncoderSettings.Builder()
                        .setBitrate(config.audioBitrate)
                        .build()
                )
                .build()

            val transformer = Transformer.Builder(context)
                .setVideoMimeType(videoMime)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .setMaxDelayBetweenMuxerSamplesMs(30_000)
                .setAssetLoaderFactory(
                    androidx.media3.transformer.DefaultAssetLoaderFactory(
                        context,
                        decoderFactory,
                        androidx.media3.common.util.Clock.DEFAULT,
                        null
                    )
                )
                .setEncoderFactory(encoderFactory)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        activeTransformer = null
                        val finalSize = outputFile.length()
                        val skipped = originalSize > 0 && finalSize >= originalSize
                        if (skipped) {
                            outputFile.delete()
                        }
                        val outMeta = if (!skipped && outputFile.exists()) {
                            probeFile(outputFile)
                        } else {
                            null
                        }
                        if (cont.isActive) {
                            cont.resume(
                                VideoCompressResult(
                                    outputFile = outputFile,
                                    outputSizeBytes = if (skipped) originalSize else finalSize,
                                    skippedBecauseLarger = skipped,
                                    outputWidth = outMeta?.width ?: 0,
                                    outputHeight = outMeta?.height ?: 0,
                                    outputFps = outMeta?.fps ?: 0f
                                )
                            )
                        }
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        activeTransformer = null
                        if (cont.isActive) {
                            cont.resume(
                                VideoCompressResult(
                                    outputFile = outputFile,
                                    outputSizeBytes = 0L,
                                    skippedBecauseLarger = false,
                                    errorMessage = exportException.localizedMessage
                                        ?: "Compression failed"
                                )
                            )
                        }
                    }
                })
                .build()

            activeTransformer = transformer
            cont.invokeOnCancellation {
                transformer.cancel()
                activeTransformer = null
            }

            val effects = mutableListOf<Effect>()
            // createForShortSide keeps the decoded frame's aspect ratio (no stretch).
            if (cappedShortSide > 0 && width > 0 && height > 0) {
                val currentShort = minOf(width, height)
                if (currentShort > cappedShortSide) {
                    effects.add(Presentation.createForShortSide(cappedShortSide))
                }
            }

            val edited = EditedMediaItem.Builder(MediaItem.fromUri(request.inputUri))
                .setEffects(Effects(listOf(VolumeAudioProcessor().apply { setVolume(1f) }), effects))
                .apply {
                    if (targetFps > 0) setFrameRate(targetFps)
                }
                .build()

            val sequence = EditedMediaItemSequence.withAudioAndVideoFrom(listOf(edited))
            val composition = Composition.Builder(listOf(sequence)).build()
            transformer.start(composition, outputFile.absolutePath)

            // Progress polling on IO-friendly coroutine tied to this continuation's job.
            cont.context[kotlinx.coroutines.Job]?.let { parentJob ->
                kotlinx.coroutines.CoroutineScope(parentJob + Dispatchers.Main.immediate).launch {
                    val holder = ProgressHolder()
                    while (isActive && activeTransformer === transformer) {
                        val state = transformer.getProgress(holder)
                        if (state != Transformer.PROGRESS_STATE_NOT_STARTED) {
                            onProgress(holder.progress / 100f, outputFile.length())
                        }
                        delay(200)
                    }
                }
            }
        }
    }

    private data class Probe(
        val width: Int,
        val height: Int,
        val fps: Float,
        val durationMs: Long,
        val sizeBytes: Long,
        val hasAudio: Boolean
    )

    private fun probe(context: Context, uri: Uri): Probe {
        var width = 0
        var height = 0
        var captureFps = 0f
        var trackFps = 0f
        var durationMs = 0L
        var sizeBytes = 0L
        var hasAudio = false

        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use {
                sizeBytes = it.statSize
            }
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            if (rotation == 90 || rotation == 270) {
                val tmp = width
                width = height
                height = tmp
            }
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            captureFps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
                ?.toFloatOrNull()
                ?: 0f
            retriever.release()
        } catch (_: Exception) {
        }

        try {
            val extractor = MediaExtractor()
            extractor.setDataSource(context, uri, null)
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    hasAudio = true
                }
                if (mime.startsWith("video/") && trackFps <= 0f) {
                    trackFps = readFrameRate(format)
                }
            }
            extractor.release()
        } catch (_: Exception) {
        }

        // Prefer encoded track fps; capture tag can lag behind after re-encode.
        val fps = when {
            trackFps > 0f -> trackFps
            captureFps > 0f -> captureFps
            else -> 30f
        }
        return Probe(width, height, fps, durationMs, sizeBytes, hasAudio)
    }

    private fun probeFile(file: File): Probe {
        var width = 0
        var height = 0
        var captureFps = 0f
        var trackFps = 0f
        var durationMs = 0L
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            if (rotation == 90 || rotation == 270) {
                val tmp = width
                width = height
                height = tmp
            }
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            captureFps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
                ?.toFloatOrNull()
                ?: 0f
            retriever.release()
        } catch (_: Exception) {
        }
        try {
            val extractor = MediaExtractor()
            extractor.setDataSource(file.absolutePath)
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") && trackFps <= 0f) {
                    trackFps = readFrameRate(format)
                }
            }
            extractor.release()
        } catch (_: Exception) {
        }
        val fps = when {
            trackFps > 0f -> trackFps
            captureFps > 0f -> captureFps
            else -> 0f
        }
        return Probe(
            width = width,
            height = height,
            fps = fps,
            durationMs = durationMs,
            sizeBytes = file.length(),
            hasAudio = false
        )
    }

    private fun readFrameRate(format: MediaFormat): Float {
        return try {
            if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                try {
                    format.getInteger(MediaFormat.KEY_FRAME_RATE).toFloat()
                } catch (_: ClassCastException) {
                    format.getFloat(MediaFormat.KEY_FRAME_RATE)
                }
            } else {
                0f
            }
        } catch (_: Exception) {
            0f
        }
    }

    private fun computeTargetBitrate(
        targetSizeMb: Float,
        durationMs: Long,
        audioBitrate: Int,
        extraSizeCushion: Boolean
    ): Int {
        val durationSec = if (durationMs > 0) durationMs / 1000.0 else 1.0
        val cushion = if (extraSizeCushion) 0.88 else 0.96
        val targetBits = targetSizeMb * 8 * 1024 * 1024 * cushion
        val audioBits = audioBitrate * durationSec
        val overhead = (targetBits * 0.02) + (50 * 1024 * 8)
        val available = (targetBits - audioBits - overhead).coerceAtLeast(targetBits * 0.1)
        return (available / durationSec).toLong().coerceAtLeast(100_000L).toInt()
    }

    private fun hasEncoder(mimeType: String): Boolean {
        return try {
            MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos.any { info ->
                info.isEncoder && info.supportedTypes.any { it.equals(mimeType, ignoreCase = true) }
            }
        } catch (_: Exception) {
            mimeType == MimeTypes.VIDEO_H264
        }
    }
}
