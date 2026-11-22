package compress.joshattic.us.utils

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri

data class ProbedMediaMeta(
    val width: Int = 0,
    val height: Int = 0,
    val fps: Float = 0f,
    val durationMs: Long = 0L
)

object MediaMetaProbe {
    fun probe(context: Context, uri: Uri): ProbedMediaMeta {
        var width = 0
        var height = 0
        var captureFps = 0f
        var trackFps = 0f
        var durationMs = 0L

        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull()
                ?: 0
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull()
                ?: 0
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull()
                ?: 0
            if (rotation == 90 || rotation == 270) {
                val tmp = width
                width = height
                height = tmp
            }
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: 0L
            // Camera capture tag often survives re-encode and can disagree with real track fps.
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
                if (!mime.startsWith("video/")) continue
                if (width <= 0 && format.containsKey(MediaFormat.KEY_WIDTH)) {
                    width = format.getInteger(MediaFormat.KEY_WIDTH)
                }
                if (height <= 0 && format.containsKey(MediaFormat.KEY_HEIGHT)) {
                    height = format.getInteger(MediaFormat.KEY_HEIGHT)
                }
                if (trackFps <= 0f) {
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
        return ProbedMediaMeta(width = width, height = height, fps = fps, durationMs = durationMs)
    }

    private fun readFrameRate(format: MediaFormat): Float {
        return try {
            if (!format.containsKey(MediaFormat.KEY_FRAME_RATE)) return 0f
            try {
                format.getInteger(MediaFormat.KEY_FRAME_RATE).toFloat()
            } catch (_: ClassCastException) {
                format.getFloat(MediaFormat.KEY_FRAME_RATE)
            }
        } catch (_: Exception) {
            0f
        }
    }
}
