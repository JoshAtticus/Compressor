package compress.joshattic.us.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary                = A16BadgeAppGreen,
    onPrimary              = CompressorDarkGreenBackground,
    primaryContainer       = CompressorDarkPrimaryContainer,
    onPrimaryContainer     = CompressorDarkOnPrimaryContainer,
    secondary              = CompressorDarkSecondary,
    onSecondary            = CompressorDarkOnSecondary,
    secondaryContainer     = CompressorDarkSecondaryContainer,
    onSecondaryContainer   = CompressorDarkOnSecondaryContainer,
    tertiary               = CompressorDarkTertiary,
    onTertiary             = CompressorDarkOnTertiary,
    tertiaryContainer      = CompressorDarkTertiaryContainer,
    onTertiaryContainer    = CompressorDarkOnTertiaryContainer,
    background             = CompressorDarkGreenBackground,
    surface                = CompressorDarkGreenSurface,
    surfaceContainer       = CompressorDarkGreenSurfaceContainer,
    surfaceContainerHigh   = CompressorDarkGreenSurfaceContainerHigh,
    surfaceContainerHighest= CompressorDarkGreenSurfaceContainerHighest,
    surfaceContainerLow    = CompressorDarkGreenSurfaceContainerLow,
    onBackground           = CompressorDarkGreenOnSurface,
    onSurface              = CompressorDarkGreenOnSurface,
    onSurfaceVariant       = CompressorDarkGreenOnSurfaceVariant,
    outline                = CompressorDarkGreenOutline,
    outlineVariant         = CompressorDarkGreenOutlineVariant,
    surfaceTint            = A16BadgeAppGreen,
)

private val LightColorScheme = lightColorScheme(
    primary                = A16BadgeAppGreen,
    onPrimary              = A16BadgeAppGreenOnBadge,
    primaryContainer       = CompressorLightPrimaryContainer,
    onPrimaryContainer     = CompressorLightOnPrimaryContainer,
    secondary              = CompressorLightSecondary,
    onSecondary            = CompressorLightOnSecondary,
    secondaryContainer     = CompressorLightSecondaryContainer,
    onSecondaryContainer   = CompressorLightOnSecondaryContainer,
    tertiary               = CompressorLightTertiary,
    onTertiary             = CompressorLightOnTertiary,
    tertiaryContainer      = CompressorLightTertiaryContainer,
    onTertiaryContainer    = CompressorLightOnTertiaryContainer,
    background             = CompressorLightGreenBackground,
    surface                = CompressorLightGreenSurface,
    surfaceContainer       = CompressorLightGreenSurfaceContainer,
    surfaceContainerHigh   = CompressorLightGreenSurfaceContainerHigh,
    surfaceContainerHighest= CompressorLightGreenSurfaceContainerHighest,
    surfaceContainerLow    = CompressorLightGreenSurfaceContainerLow,
    onBackground           = CompressorLightGreenOnSurface,
    onSurface              = CompressorLightGreenOnSurface,
    onSurfaceVariant       = CompressorLightGreenOnSurfaceVariant,
    outline                = CompressorLightGreenOutline,
    outlineVariant         = CompressorLightGreenOutlineVariant,
    surfaceTint            = A16BadgeAppGreen,
)

@Composable
fun CompressorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val dynamicScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (!darkTheme) {
                val lightAccentBg = dynamicScheme.surfaceTint.copy(alpha = 0.08f).compositeOver(dynamicScheme.surfaceContainerLow)
                dynamicScheme.copy(
                    background = lightAccentBg,
                    surface = lightAccentBg
                )
            } else dynamicScheme
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}