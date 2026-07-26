package compress.joshattic.us.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary                = A16BadgeAppGreen,
    onPrimary              = Android16DarkBackground,
    background             = Android16DarkBackground,
    surface                = Android16DarkSurface,
    surfaceContainer       = Android16DarkSurfaceContainer,
    surfaceContainerHigh   = Android16DarkSurfaceContainerHigh,
    surfaceContainerLow    = Android16DarkSurfaceContainerLow,
    onBackground           = Android16DarkOnSurface,
    onSurface              = Android16DarkOnSurface,
    onSurfaceVariant       = Android16DarkOnSurfaceVariant,
    outline                = Android16DarkOutline,
    outlineVariant         = Android16DarkOutlineVariant,
)

private val LightColorScheme = lightColorScheme(
    primary                = A16BadgeAppGreen,
    onPrimary              = Android16LightBackground,
    background             = Android16LightBackground,
    surface                = Android16LightSurface,
    surfaceContainer       = Android16LightSurfaceContainer,
    surfaceContainerHigh   = Android16LightSurfaceContainerHigh,
    surfaceContainerLow    = Android16LightSurfaceContainerLow,
    onBackground           = Android16LightOnSurface,
    onSurface              = Android16LightOnSurface,
    onSurfaceVariant       = Android16LightOnSurfaceVariant,
    outline                = Android16LightOutline,
    outlineVariant         = Android16LightOutlineVariant,
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
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
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