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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Gavel
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.primortex.color.service.CrosshairShape
import com.primortex.color.service.CrosshairSize
import com.primortex.color.service.PickerSensitivity
import com.primortex.color.service.SettingsService
import com.primortex.color.service.ThemeMode
import com.primortex.color.ui.components.CrosshairIndicator
import com.primortex.color.ui.components.ScreenScaffold

@Composable
fun ExploreScreen(
    innerPadding: PaddingValues,
    onOpenLanguage: () -> Unit,
    onOpenCopyright: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenUsageGuide: () -> Unit
) {
    val crosshairSize by SettingsService.crosshairSize.collectAsState()
    val crosshairShape by SettingsService.crosshairShape.collectAsState()
    val themeMode by SettingsService.themeMode.collectAsState()
    val pickerSensitivity by SettingsService.pickerSensitivity.collectAsState()
    val appLanguage by SettingsService.appLanguage.collectAsState()

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
                                    label = { Text(stringResource(mode.labelRes)) }
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = { Text("Language") },
                        supportingContent = { Text(appLanguage.name) },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Outlined.Language,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOpenLanguage()
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

                    SectionHeader("Sensitivity")

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Control how reactive the picker is to color changes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PickerSensitivity.values().forEach { sensitivity ->
                                FilterChip(
                                    selected = pickerSensitivity == sensitivity,
                                    onClick = { SettingsService.setPickerSensitivity(sensitivity) },
                                    label = { Text(sensitivity.label) }
                                )
                            }
                        }
                    }

                }
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Information",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )

                    InfoLink(
                        title = "Copyright notice",
                        subtitle = "Ownership, third-party components, and licensing details",
                        icon = Icons.Outlined.Gavel,
                        onClick = onOpenCopyright
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    InfoLink(
                        title = "Privacy statement",
                        subtitle = "How camera and photo data stay on-device",
                        icon = Icons.Outlined.Description,
                        onClick = onOpenPrivacy
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    InfoLink(
                        title = "Usage guide",
                        subtitle = "Step-by-step guide and index for key tasks",
                        icon = Icons.Outlined.Description,
                        onClick = onOpenUsageGuide
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
private fun InfoLink(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}
