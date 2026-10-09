package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val GeminiDarkColorScheme = darkColorScheme(
    primary = GeminiBlue,
    onPrimary = GeminiBackground,
    primaryContainer = GeminiSurfaceVariant,
    onPrimaryContainer = GeminiTextPrimary,
    secondary = GeminiPurple,
    onSecondary = GeminiBackground,
    secondaryContainer = GeminiSurfaceHighlight,
    onSecondaryContainer = GeminiTextPrimary,
    tertiary = GeminiPink,
    onTertiary = GeminiBackground,
    background = GeminiBackground,
    onBackground = GeminiTextPrimary,
    surface = GeminiSurface,
    onSurface = GeminiTextPrimary,
    surfaceVariant = GeminiSurfaceVariant,
    onSurfaceVariant = GeminiTextSecondary,
    outline = GeminiOutline,
    outlineVariant = GeminiSurfaceHighlight
)

private val GeminiLightColorScheme = lightColorScheme(
    primary = GeminiBlue,
    onPrimary = GeminiTextPrimary,
    primaryContainer = Color(0xFFD2E3FC),
    onPrimaryContainer = Color(0xFF041E49),
    secondary = GeminiPurple,
    onSecondary = GeminiTextPrimary,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF21005D),
    tertiary = GeminiPink,
    onTertiary = GeminiTextPrimary,
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF1F1F1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F1F1F),
    surfaceVariant = Color(0xFFF1F3F4),
    onSurfaceVariant = Color(0xFF444746),
    outline = Color(0xFF747775),
    outlineVariant = Color(0xFFC4C7C5)
)

@Composable
fun GeminiTheme(
    darkTheme: Boolean = true, // Default to Gemini's iconic dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) GeminiDarkColorScheme else GeminiLightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
