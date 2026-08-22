package compress.joshattic.us.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme

// ── Android 16 dark surface tokens ───────────────────────────────────────────
val Android16DarkBackground           = Color(0xFF111318)
val Android16DarkSurface              = Color(0xFF111318)
val Android16DarkSurfaceContainer     = Color(0xFF1E2027)
val Android16DarkSurfaceContainerHigh = Color(0xFF282A33)
val Android16DarkSurfaceContainerLow  = Color(0xFF181A20)
val Android16DarkOnSurface            = Color(0xFFE2E2E9)
val Android16DarkOnSurfaceVariant     = Color(0xFF9DA3B4)
val Android16DarkOutline              = Color(0xFF444754)
val Android16DarkOutlineVariant       = Color(0xFF2E303B)

// ── Android 16 light surface tokens ──────────────────────────────────────────
val Android16LightBackground           = Color(0xFFEBF0F8)
val Android16LightSurface              = Color(0xFFEBF0F8)
val Android16LightSurfaceContainer     = Color(0xFFDFE5F2)
val Android16LightSurfaceContainerHigh = Color(0xFFD4DEED)
val Android16LightSurfaceContainerHighest = Color(0xFFC7D4E8)
val Android16LightSurfaceContainerLow  = Color(0xFFF1F5FC)
val Android16LightOnSurface            = Color(0xFF191C20)
val Android16LightOnSurfaceVariant     = Color(0xFF43474E)
val Android16LightOutline              = Color(0xFF73777F)
val Android16LightOutlineVariant       = Color(0xFFC3C7D0)

// ── App Icon & Brand Colors ──────────────────────────────────────────────────
val A16BadgeAppGreen        = Color(0xFF6FA450)   // Exact #6FA450 requested
val A16BadgeAppGreenOnBadge = Color(0xFFFFFFFF)   // Clean white icon on green

// ── Harmonized Green Theme Color Tokens (Fallback for non-dynamic / Android <= 11) ──
val CompressorLightGreenBackground           = Color(0xFFEFF4ED)
val CompressorLightGreenSurface              = Color(0xFFEFF4ED)
val CompressorLightGreenSurfaceContainerLow  = Color(0xFFF5F9F3)
val CompressorLightGreenSurfaceContainer     = Color(0xFFE4EDE1)
val CompressorLightGreenSurfaceContainerHigh = Color(0xFFDAE5D6)
val CompressorLightGreenSurfaceContainerHighest = Color(0xFFCFDCCB)
val CompressorLightGreenOnSurface            = Color(0xFF171D15)
val CompressorLightGreenOnSurfaceVariant     = Color(0xFF41493E)
val CompressorLightGreenOutline              = Color(0xFF71796E)
val CompressorLightGreenOutlineVariant       = Color(0xFFC1C9BD)

val CompressorDarkGreenBackground            = Color(0xFF111411)
val CompressorDarkGreenSurface               = Color(0xFF111411)
val CompressorDarkGreenSurfaceContainerLow   = Color(0xFF191C18)
val CompressorDarkGreenSurfaceContainer      = Color(0xFF1D211C)
val CompressorDarkGreenSurfaceContainerHigh  = Color(0xFF282B26)
val CompressorDarkGreenSurfaceContainerHighest = Color(0xFF333631)
val CompressorDarkGreenOnSurface             = Color(0xFFE1E4DC)
val CompressorDarkGreenOnSurfaceVariant      = Color(0xFFC1C9BD)
val CompressorDarkGreenOutline               = Color(0xFF8B9388)
val CompressorDarkGreenOutlineVariant        = Color(0xFF41493E)

val CompressorLightPrimaryContainer      = Color(0xFFDCECCF)
val CompressorLightOnPrimaryContainer    = Color(0xFF193112)
val CompressorLightSecondary             = Color(0xFF55634E)
val CompressorLightOnSecondary           = Color(0xFFFFFFFF)
val CompressorLightSecondaryContainer    = Color(0xFFDCE8D5)
val CompressorLightOnSecondaryContainer  = Color(0xFF14290F)
val CompressorLightTertiary              = Color(0xFF3B6554)
val CompressorLightOnTertiary            = Color(0xFFFFFFFF)
val CompressorLightTertiaryContainer     = Color(0xFFBDEDE0)
val CompressorLightOnTertiaryContainer    = Color(0xFF002117)

val CompressorDarkPrimaryContainer       = Color(0xFF2E4C20)
val CompressorDarkOnPrimaryContainer     = Color(0xFFDCECCF)
val CompressorDarkSecondary              = Color(0xFFB9CCA9)
val CompressorDarkOnSecondary            = Color(0xFF26341D)
val CompressorDarkSecondaryContainer     = Color(0xFF2A3D24)
val CompressorDarkOnSecondaryContainer   = Color(0xFFD4E6C9)
val CompressorDarkTertiary               = Color(0xFFA1D0C3)
val CompressorDarkOnTertiary             = Color(0xFF073728)
val CompressorDarkTertiaryContainer      = Color(0xFF234E40)
val CompressorDarkOnTertiaryContainer     = Color(0xFFBDEDE0)


// ── Android 16 Expressive Category Badges (Theme Aware) ──────────────────────
// Display (Amber / Orange)
val A16DisplayBgDark    = Color(0xFF3E2A00)
val A16DisplayIconDark  = Color(0xFFFFB951)
val A16DisplayBgLight   = Color(0xFFFFDDB3)
val A16DisplayIconLight = Color(0xFF2A1700)

// Presets (Blue / Purple)
val A16PresetsBgDark    = Color(0xFF1E2347)
val A16PresetsIconDark  = Color(0xFFB0C4FF)
val A16PresetsBgLight   = Color(0xFFD9E2FF)
val A16PresetsIconLight = Color(0xFF001945)

// Video (Cyan / Teal - Soft Pastel)
val A16VideoBgDark    = Color(0xFF1B3B3B)
val A16VideoIconDark  = Color(0xFF90E0E0)
val A16VideoBgLight   = Color(0xFFC5EDED)
val A16VideoIconLight = Color(0xFF0A3B3B)

// Audio (Pink / Rose)
val A16AudioBgDark    = Color(0xFF3E0A1E)
val A16AudioIconDark  = Color(0xFFFFADD4)
val A16AudioBgLight   = Color(0xFFFFD8EC)
val A16AudioIconLight = Color(0xFF3B0019)

@Composable
fun getCategoryBadgeColors(category: String, isDark: Boolean = isSystemInDarkTheme()): Pair<Color, Color> {
    return when (category) {
        "display" -> if (isDark) Pair(A16DisplayBgDark, A16DisplayIconDark) else Pair(A16DisplayBgLight, A16DisplayIconLight)
        "presets" -> if (isDark) Pair(A16PresetsBgDark, A16PresetsIconDark) else Pair(A16PresetsBgLight, A16PresetsIconLight)
        "video"   -> if (isDark) Pair(A16VideoBgDark, A16VideoIconDark) else Pair(A16VideoBgLight, A16VideoIconLight)
        "audio"   -> if (isDark) Pair(A16AudioBgDark, A16AudioIconDark) else Pair(A16AudioBgLight, A16AudioIconLight)
        else      -> Pair(A16BadgeAppGreen, A16BadgeAppGreenOnBadge)
    }
}