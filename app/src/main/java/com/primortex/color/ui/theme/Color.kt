package com.primortex.color.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color


val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE0E0E0),       // light gray
    onPrimary = Color(0xFF121212),

    secondary = Color(0xFF9E9E9E),
    onSecondary = Color.Black,

    tertiary = Color(0xFF616161),

    background = Color(0xFF121212),    // true neutral black
    onBackground = Color(0xFFEDEDED),

    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFEDEDED),

    outline = Color(0xFF2A2A2A),
)

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2E2E2E),       // graphite
    onPrimary = Color.White,

    secondary = Color(0xFF6B6B6B),     // neutral gray
    onSecondary = Color.White,

    tertiary = Color(0xFF9E9E9E),      // divider gray

    background = Color(0xFFF7F7F7),    // soft neutral
    onBackground = Color(0xFF121212),

    surface = Color.White,
    onSurface = Color(0xFF121212),

    outline = Color(0xFFE0E0E0),
)