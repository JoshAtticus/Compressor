import androidx.media3.transformer.VideoEncoderSettings

fun main() {
    val b = VideoEncoderSettings.Builder()
    b.setBitrate(100)
    // b.setBitrateMode(VideoEncoderSettings.BITRATE_MODE_CQ)
}
