package compress.joshattic.us.model

import androidx.media3.common.MimeTypes

data class DefaultVideoConfig(
    val defaultVideoCodec: String = MimeTypes.VIDEO_H265,
    val defaultTargetResolutionHeight: Int = 0, // 0 for original, or 1080, 720, 480
    val defaultTargetFps: Int = 0,               // 0 for original, or 60, 30
    val defaultSizeRatio: Float = 0.7f          // Target size ratio vs original (e.g. 0.7 = 70%)
)
