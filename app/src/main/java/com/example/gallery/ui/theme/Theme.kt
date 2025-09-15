package com.example.gallery.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6DDBFF),
    onPrimary = Color(0xFF003544),
    primaryContainer = Color(0xFF004D61),
    onPrimaryContainer = Color(0xFFBEEAFF),
    secondary = Color(0xFFB1CAD6),
    onSecondary = Color(0xFF1C3438),
    secondaryContainer = Color(0xFF334A4F),
    onSecondaryContainer = Color(0xFFCDE6F2),
    tertiary = Color(0xFFB5C6EA),
    onTertiary = Color(0xFF1F2844),
    tertiaryContainer = Color(0xFF353E5B),
    onTertiaryContainer = Color(0xFFD1E2FF),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F1419),
    onBackground = Color(0xFFDFE3E7),
    surface = Color(0xFF0F1419),
    onSurface = Color(0xFFDFE3E7),
    surfaceVariant = Color(0xFF40484C),
    onSurfaceVariant = Color(0xFFC0C8CC),
    outline = Color(0xFF8A9296),
    inverseOnSurface = Color(0xFF0F1419),
    inverseSurface = Color(0xFFDFE3E7),
    inversePrimary = Color(0xFF00677E),
    surfaceTint = Color(0xFF6DDBFF),
    outlineVariant = Color(0xFF40484C),
    scrim = Color(0xFF000000),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00677E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBEEAFF),
    onPrimaryContainer = Color(0xFF001F28),
    secondary = Color(0xFF4A6267),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDE6F2),
    onSecondaryContainer = Color(0xFF051F23),
    tertiary = Color(0xFF515573),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD8E2FF),
    onTertiaryContainer = Color(0xFF0E1B2D),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFBFCFE),
    onBackground = Color(0xFF191C1D),
    surface = Color(0xFFFBFCFE),
    onSurface = Color(0xFF191C1D),
    surfaceVariant = Color(0xFFDCE4E8),
    onSurfaceVariant = Color(0xFF40484C),
    outline = Color(0xFF70787C),
    inverseOnSurface = Color(0xFFEFF1F2),
    inverseSurface = Color(0xFF2E3132),
    inversePrimary = Color(0xFF6DDBFF),
    surfaceTint = Color(0xFF00677E),
    outlineVariant = Color(0xFFC0C8CC),
    scrim = Color(0xFF000000),
)

@Composable
fun GalleryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}