package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Google Gemini Dark Theme Palette
val GeminiBackground = Color(0xFF131314)
val GeminiSurface = Color(0xFF1E1F20)
val GeminiSurfaceVariant = Color(0xFF282A2C)
val GeminiSurfaceHighlight = Color(0xFF37393B)
val GeminiSurfaceLight = Color(0xFF2C2D30)

// Text and icons
val GeminiTextPrimary = Color(0xFFE3E3E3)
val GeminiTextSecondary = Color(0xFFC4C7C5)
val GeminiTextTertiary = Color(0xFF8E918F)
val GeminiOutline = Color(0xFF444746)

// Gemini signature accent colors
val GeminiBlue = Color(0xFF4285F4)
val GeminiCyan = Color(0xFF4FC3F7)
val GeminiPurple = Color(0xFF9B72CB)
val GeminiPink = Color(0xFFD96570)
val GeminiIndigo = Color(0xFF7C4DFF)
val GeminiAmber = Color(0xFFFFBA00)
val GeminiGreen = Color(0xFF34A853)

// User bubble color
val GeminiUserBubble = Color(0xFF282A2C)
val GeminiAssistantBubble = Color(0xFF1E1F20)
val GeminiCodeBlockBackground = Color(0xFF18181B)

// Gradients
val GeminiGradientBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFF4285F4),
        Color(0xFF9B72CB),
        Color(0xFFD96570)
    )
)

val GeminiSparkleBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFF68B5FB),
        Color(0xFFB388FF),
        Color(0xFFFF80AB)
    )
)

val GeminiLiveOrbGlow = Brush.radialGradient(
    colors = listOf(
        Color(0xFF9B72CB).copy(alpha = 0.8f),
        Color(0xFF4285F4).copy(alpha = 0.5f),
        Color(0xFF131314).copy(alpha = 0f)
    )
)
