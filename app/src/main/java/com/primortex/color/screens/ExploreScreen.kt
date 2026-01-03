package com.primortex.color.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import com.primortex.color.ui.components.CrosshairIndicator
import com.primortex.color.ui.components.ScreenScaffold

@Composable
fun ExploreScreen(innerPadding: PaddingValues) {
    val crosshairSize by SettingsService.crosshairSize.collectAsState()
    val crosshairShape by SettingsService.crosshairShape.collectAsState()
    val themeMode by SettingsService.themeMode.collectAsState() // add this in SettingsService

    ScreenScaffold("Explore", innerPadding) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Text(
                        "App preferences",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )

                    ListItem(
                        headlineContent = { Text("Theme") },
                        supportingContent = { Text(themeMode.label) },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Outlined.DarkMode,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // open a bottom sheet, dialog, or navigate
                                // Example: showThemeSheet = true
                            }
                    )

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
