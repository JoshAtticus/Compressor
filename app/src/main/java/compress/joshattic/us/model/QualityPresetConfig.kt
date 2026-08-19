package compress.joshattic.us.model

data class QualityPresetConfig(
    val resolutionShortSide: Int = 0, // 0 for original, or 1080, 720, 480
    val targetFps: Int = 0,           // 0 for original, or 60, 30
    val sizeRatio: Float = 0.7f,      // Target size ratio vs original (e.g. 0.7 = 70%)
    val audioBitrate: Int = 320000,   // 320000, 192000, 128000
    val label: String? = null         // Custom display name, null = use default localized name
)
