package com.primortex.color.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Copyright
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.HelpCenter
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
    var showCrosshairSettings by rememberSaveable { mutableStateOf(false) }

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

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Settings", style = MaterialTheme.typography.titleMedium)

                SettingsActionButton(
                    icon = Icons.Outlined.Language,
                    title = "Language",
                    subtitle = "Choose the language used in the app"
                )
                SettingsActionButton(
                    icon = Icons.Outlined.DarkMode,
                    title = "Theme",
                    subtitle = "Light, dark, or system default"
                )
                SettingsActionButton(
                    icon = Icons.Outlined.CenterFocusStrong,
                    title = "Picker crosshair",
                    subtitle = "Change how the focus crosshair looks",
                    onClick = { showCrosshairSettings = !showCrosshairSettings }
                )
                SettingsActionButton(
                    icon = Icons.Outlined.PrivacyTip,
                    title = "Privacy statement",
                    subtitle = "Learn how we handle your data"
                )
                SettingsActionButton(
                    icon = Icons.Outlined.HelpCenter,
                    title = "Guide",
                    subtitle = "Tips for getting the best color picks"
                )
                SettingsActionButton(
                    icon = Icons.Outlined.Copyright,
                    title = "Copyright",
                    subtitle = "Copyright and licensing details"
                )

                if (showCrosshairSettings) {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
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

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Size", style = MaterialTheme.typography.labelLarge)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CrosshairSize.values().forEach { size ->
                                        FilterChip(
                                            selected = crosshairSize == size,
                                            onClick = { SettingsService.setCrosshairSize(size) },
                                            label = { Text(size.label) }
                                        )
                                    }
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Shape", style = MaterialTheme.typography.labelLarge)
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
        }
    }
}

@Composable
private fun SettingsActionButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    OutlinedButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
