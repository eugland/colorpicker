package com.primortex.color.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.primortex.color.service.CrosshairShape
import com.primortex.color.service.CrosshairSize
import com.primortex.color.service.SettingsService
import com.primortex.color.service.ThemeMode
import com.primortex.color.ui.components.CrosshairIndicator
import com.primortex.color.ui.components.ScreenScaffold

@Composable
fun ExploreScreen(innerPadding: PaddingValues) {
    val crosshairSize by SettingsService.crosshairSize.collectAsState()
    val crosshairShape by SettingsService.crosshairShape.collectAsState()
    val themeMode by SettingsService.themeMode.collectAsState()

    ScreenScaffold("Explore", innerPadding) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Settings", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Tune how the app looks and how the picker crosshair behaves.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // --- App preferences card (Theme, Language, etc.) ---
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "App preferences",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )

                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.DarkMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Theme",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                        Text(
                            "Choose how the app looks.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemeMode.values().forEach { mode ->
                                FilterChip(
                                    selected = themeMode == mode,
                                    onClick = { SettingsService.setThemeMode(mode) },
                                    label = { Text(mode.label) }
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = { Text("Language") },
                        supportingContent = { Text("Choose the language used in the app") },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Outlined.Language,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // navigate to language screen
                            }
                    )
                }
            }

            // --- Crosshair settings card ---
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CrosshairIndicator(
                            argb = MaterialTheme.colorScheme.primary.toArgb(),
                            size = crosshairSize,
                            shape = crosshairShape
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Picker crosshair", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Adjust the crosshair used when sampling colors.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    SectionHeader("Size")


                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CrosshairSize.values().forEach { size ->
                            FilterChip(
                                selected = crosshairSize == size,
                                onClick = { SettingsService.setCrosshairSize(size) },
                                label = { Text(size.label) }
                            )
                        }
                    }


                    SectionHeader("Shape")


                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CrosshairShape.values().forEach { shape ->
                            FilterChip(
                                selected = crosshairShape == shape,
                                onClick = { SettingsService.setCrosshairShape(shape) },
                                label = { Text(shape.label) }
                            )
                        }
                    }

                }
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Information",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    InfoSection(
                        title = "Copyright notice",
                        description = "© 2024 Color Picker by Primortex. All rights reserved.",
                        details = listOf(
                            "Third-party components: Jetpack Compose Material 3, CameraX, Coil, Ktor, Navigation Compose, and Accompanist Navigation Animation.",
                            "Open-source licenses are respected and remain the property of their respective owners."
                        )
                    )

                    HorizontalDivider()

                    InfoSection(
                        title = "Privacy statement",
                        description = "Color sampling happens on your device. Camera previews and picked photos are used only to extract colors and are not stored or sent to remote servers.",
                        details = listOf(
                            "Network activity is limited to fetching supporting data (like palette names) when needed.",
                            "You can revoke camera and photo permissions at any time in your system settings."
                        )
                    )

                    HorizontalDivider()

                    InfoSection(
                        title = "Usage guide",
                        description = "Follow these steps to get the most out of Color Picker.",
                        details = listOf(
                            "Explore settings: choose your theme, language, and adjust the picker crosshair size and shape.",
                            "Live capture: open Live Camera to aim the crosshair at any object and tap to lock in a swatch.",
                            "Pick from photos: use Photo Pick to select an image from your gallery and tap anywhere to sample colors.",
                            "View details: open any saved swatch to see its HEX, RGB, and HSL values and copy them for reuse.",
                            "Build palettes: combine multiple saved swatches into palettes from the Palette tab for quick access."
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider()
    }
}

@Composable
private fun SettingBlock(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun InfoSection(title: String, description: String, details: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        details.forEach { detail ->
            Text(
                "• $detail",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
