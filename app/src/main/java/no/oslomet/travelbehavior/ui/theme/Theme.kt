package no.oslomet.travelbehavior.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    // HVA: Endret fra PrimaryGreen til SecondaryGreen
    // HVORFOR: MMI - Reduserer "visual glare" i mørkt tema, gjør knappen mer behagelig.
    primary = SecondaryGreen,
    onPrimary = TextDark,
    secondary = SecondaryGreen,
    onSecondary = TextDark,
    secondaryContainer = DarkGreen,
    onSecondaryContainer = TextLight,
    tertiary = AccentBlue,
    background = Color(0xFF121413),
    onBackground = TextLight,
    surface = Color(0xFF1E211F),
    onSurface = TextLight,
    surfaceContainer = Color(0xFF1E211F),
    surfaceVariant = Color(0xFF2C312E),
    onSurfaceVariant = Color(0xFFBFC9C2),
    error = ErrorRed,
    onError = TextDark
)

private val LightColorScheme = lightColorScheme(
    primary = DarkGreen,
    onPrimary = TextLight,
    secondary = SecondaryGreen,
    onSecondary = TextDark,
    secondaryContainer = PrimaryGreen,
    onSecondaryContainer = TextDark,
    tertiary = AccentBlue,
    background = BackgroundLight,
    onBackground = TextDark,
    surface = CardSecondaryBackground,
    onSurface = TextDark,
    surfaceContainer = CardSecondaryBackground,
    surfaceVariant = Color(0xFFE1E9E3),
    onSurfaceVariant = TextDark,
    error = ErrorRed,
    onError = TextLight
)

@Composable
fun BachelorAppH2025Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
