package com.primortex.color.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color


val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE5E5EA),       // iOS Label (dark)
    onPrimary = Color(0xFF000000),

    secondary = Color(0xFFAEAEB2),     // Secondary label
    onSecondary = Color(0xFF000000),

    tertiary = Color(0xFF8E8E93),

    background = Color(0xFF000000),    // iOS system background dark
    onBackground = Color(0xFFE5E5EA),

    surface = Color(0xFF1C1C1E),       // iOS secondary system background
    onSurface = Color(0xFFE5E5EA),

    outline = Color(0xFF2C2C2E),       // iOS separator dark
)


val LightColorScheme = lightColorScheme(
    // Primary text & key UI
    primary = Color(0xFF1C1C1E),       // iOS "Label"
    onPrimary = Color(0xFFFFFFFF),

    secondary = Color(0xFF3A3A3C),     // iOS "Secondary Label"
    onSecondary = Color(0xFFFFFFFF),

    tertiary = Color(0xFF8E8E93),      // iOS "Tertiary Label / Separator"

    // Backgrounds
    background = Color(0xFFF2F2F7),    // iOS System Background (light)
    onBackground = Color(0xFF1C1C1E),

    surface = Color(0xFFFFFFFF),      // Card / sheet surface
    onSurface = Color(0xFF1C1C1E),

    outline = Color(0xFFD1D1D6),       // iOS Separator
)

val IOSLightColorScheme = lightColorScheme(
    primary = Color(0xFF1C1C1E),            // iOS Label
    onPrimary = Color(0xFFFFFFFF),

    primaryContainer = Color(0xFFE5E5EA),   // light fill
    onPrimaryContainer = Color(0xFF1C1C1E),

    inversePrimary = Color(0xFFFFFFFF),

    secondary = Color(0xFF3A3A3C),          // Secondary label
    onSecondary = Color(0xFFFFFFFF),

    secondaryContainer = Color(0xFFE9E9EE), // subtle container
    onSecondaryContainer = Color(0xFF1C1C1E),

    tertiary = Color(0xFF8E8E93),           // Tertiary label / separator-ish
    onTertiary = Color(0xFFFFFFFF),

    tertiaryContainer = Color(0xFFF0F0F5),
    onTertiaryContainer = Color(0xFF1C1C1E),

    background = Color(0xFFF2F2F7),         // iOS System Background (grouped)
    onBackground = Color(0xFF1C1C1E),

    surface = Color(0xFFFFFFFF),            // cards/sheets
    onSurface = Color(0xFF1C1C1E),

    surfaceVariant = Color(0xFFF2F2F7),     // blend with background
    onSurfaceVariant = Color(0xFF3A3A3C),

    surfaceTint = Color(0xFF0A84FF),        // iOS system blue accent

    inverseSurface = Color(0xFF1C1C1E),
    inverseOnSurface = Color(0xFFF2F2F7),

    error = Color(0xFFFF3B30),              // iOS System Red (approx)
    onError = Color(0xFFFFFFFF),

    errorContainer = Color(0xFFFFE5E3),
    onErrorContainer = Color(0xFF1C1C1E),

    outline = Color(0xFFD1D1D6),            // iOS Separator
    outlineVariant = Color(0xFFE5E5EA),     // subtle separator / outline

    scrim = Color(0x66000000),

    // Material3 surface tonal steps (approx, iOS-like)
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFEFEFF4),

    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFFAFAFC),
    surfaceContainerHighest = Color(0xFFF6F6FA),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFDFDFF),
)

val IOSDarkColorScheme = darkColorScheme(
    primary = Color(0xFFE5E5EA),            // iOS Label (dark)
    onPrimary = Color(0xFF000000),

    primaryContainer = Color(0xFF2C2C2E),   // iOS secondary surface
    onPrimaryContainer = Color(0xFFE5E5EA),

    inversePrimary = Color(0xFF1C1C1E),

    secondary = Color(0xFFAEAEB2),          // secondary label
    onSecondary = Color(0xFF000000),

    secondaryContainer = Color(0xFF1C1C1E), // primary surface layer
    onSecondaryContainer = Color(0xFFE5E5EA),

    tertiary = Color(0xFF8E8E93),
    onTertiary = Color(0xFF000000),

    tertiaryContainer = Color(0xFF2C2C2E),
    onTertiaryContainer = Color(0xFFE5E5EA),

    background = Color(0xFF000000),         // iOS system background dark
    onBackground = Color(0xFFE5E5EA),

    surface = Color(0xFF1C1C1E),            // cards/sheets in dark
    onSurface = Color(0xFFE5E5EA),

    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFFAEAEB2),

    surfaceTint = Color(0xFF0A84FF),        // iOS system blue accent

    inverseSurface = Color(0xFFF2F2F7),
    inverseOnSurface = Color(0xFF1C1C1E),

    error = Color(0xFFFF453A),              // iOS red (dark mode variant-ish)
    onError = Color(0xFF000000),

    errorContainer = Color(0xFF3A0D0B),
    onErrorContainer = Color(0xFFFFB4AE),

    outline = Color(0xFF3A3A3C),            // separator dark
    outlineVariant = Color(0xFF2C2C2E),

    scrim = Color(0x99000000),

    // tonal surface steps (layering)
    surfaceBright = Color(0xFF2C2C2E),
    surfaceDim = Color(0xFF000000),

    surfaceContainer = Color(0xFF1C1C1E),
    surfaceContainerHigh = Color(0xFF2C2C2E),
    surfaceContainerHighest = Color(0xFF3A3A3C),
    surfaceContainerLow = Color(0xFF161618),
    surfaceContainerLowest = Color(0xFF0B0B0C),
)
