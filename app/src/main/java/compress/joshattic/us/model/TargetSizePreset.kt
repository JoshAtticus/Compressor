package compress.joshattic.us.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import compress.joshattic.us.R

data class TargetSizePreset(
    val id: String,
    val sizeMb: Float,
    val label: String,
    val isCustom: Boolean = false
)

private fun defaultLabelForId(id: String): String? = when (id) {
    "discord" -> "Discord"
    "github" -> "GitHub"
    "email" -> "Email"
    "stories" -> "Stories • Nitro Basic"
    "messenger" -> "Messenger • BlueSky"
    "nitro" -> "Nitro • Reels"
    "twitter" -> "Twitter/X"
    "whatsapp" -> "WhatsApp • Telegram"
    "tg_premium" -> "TG Premium • Feed"
    "x_premium" -> "X Premium"
    else -> null
}

@Composable
fun TargetSizePreset.getLocalizedLabel(): String {
    if (isCustom) return label
    // Allow renaming of built-in presets: if stored label differs from the
    // default English label, treat it as a user-customized name.
    val defaultLabel = defaultLabelForId(id)
    if (defaultLabel != null && label != defaultLabel && label.isNotBlank()) {
        return label
    }
    return when (id) {
        "discord" -> stringResource(R.string.size_discord)
        "github" -> stringResource(R.string.size_github)
        "email" -> stringResource(R.string.size_email)
        "stories" -> stringResource(R.string.size_stories)
        "messenger" -> stringResource(R.string.size_messenger)
        "nitro" -> stringResource(R.string.size_nitro)
        "twitter" -> stringResource(R.string.size_twitter)
        "whatsapp" -> stringResource(R.string.size_whatsapp)
        "tg_premium" -> stringResource(R.string.size_tg_premium)
        "x_premium" -> stringResource(R.string.size_x_premium)
        else -> label
    }
}
