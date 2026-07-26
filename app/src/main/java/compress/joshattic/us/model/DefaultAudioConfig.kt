package compress.joshattic.us.model

data class DefaultAudioConfig(
    val defaultAudioBitrate: Int = 128_000, // 128 kbps
    val defaultRemoveAudio: Boolean = false,
    val defaultAudioVolume: Float = 1.0f    // 100%
)
