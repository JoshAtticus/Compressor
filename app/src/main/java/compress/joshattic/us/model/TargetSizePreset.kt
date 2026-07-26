package compress.joshattic.us.model

data class TargetSizePreset(
    val id: String,
    val sizeMb: Float,
    val label: String,
    val isCustom: Boolean = false
)
