package com.primortex.color.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.primortex.color.app.PickedColor
import com.primortex.color.service.RecentPicksService
import com.primortex.color.ui.util.argbToHex

@Composable
fun CameraScreen(
    innerPadding: PaddingValues = PaddingValues(),
    onOpenLiveCameraPicker: () -> Unit,
    onPickFromAlbum: (String) -> Unit
) {
    val history by RecentPicksService.history.collectAsState()

    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) onPickFromAlbum(uri.toString())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Title + subtitle
        Text("Camera", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Pick colors live or from a photo. Tap any swatch to copy.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Hero: Live picker
        LivePickerHeroCard(
            onOpenLiveCameraPicker = onOpenLiveCameraPicker
        )

        // Two tiles: Camera / Album
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SourceTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.PhotoCamera,
                title = "Live Camera",
                subtitle = "Crosshair + zoom",
                onClick = onOpenLiveCameraPicker
            )
            SourceTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Collections,
                title = "From Album",
                subtitle = "Pick from a photo",
                onClick = { pickPhotoLauncher.launch("image/*") }
            )
        }

        // Recents header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent picks", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { RecentPicksService.clear() }) { Text("Clear") }
        }

        ToolsSection()
    }
}

@Composable
private fun ToolsSection() {
    val tools = listOf(
        CameraTool(
            title = "Color Builder",
            summary = "Build precise colors with familiar sliders and quick save actions.",
            sections = listOf(
                ToolSection(
                    heading = "Top",
                    bulletPoints = listOf(
                        "Preview swatch paired with HEX readout and a Copy shortcut.",
                        "Mode toggle for RGB | HSL | HSV (start with RGB and HSL)."
                    )
                ),
                ToolSection(
                    heading = "Middle",
                    bulletPoints = listOf(
                        "Slider rows for the active mode: RGB (R, G, B, optional A) or HSL (Hue 0–360, Sat 0–100, Light 0–100).",
                        "Each slider row keeps a label, the slider itself, a numeric input, and an optional lock toggle."
                    )
                ),
                ToolSection(
                    heading = "Bottom",
                    bulletPoints = listOf(
                        "Actions: Add to Recents, Save color, Add to palette (choose a palette), and Create palette (start a new palette with this color)."
                    )
                )
            )
        )
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Tools", style = MaterialTheme.typography.titleMedium)

        tools.forEach { tool ->
            ToolCard(tool)
        }
    }
}

@Composable
private fun ToolCard(tool: CameraTool) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Build, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text(tool.title, style = MaterialTheme.typography.titleMedium)
            }

            Text(
                tool.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            tool.sections.forEach { section ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(section.heading, style = MaterialTheme.typography.labelLarge)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        section.bulletPoints.forEach { point ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text("•", color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    point,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class CameraTool(
    val title: String,
    val summary: String,
    val sections: List<ToolSection>
)

private data class ToolSection(
    val heading: String,
    val bulletPoints: List<String>
)

@Composable
private fun LivePickerHeroCard(onOpenLiveCameraPicker: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Colorize, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Live color picker", style = MaterialTheme.typography.titleLarge)
            }

            Text(
                "Point at anything. Pinch to zoom. Add to palette instantly.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onOpenLiveCameraPicker,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Live Picking")
            }
        }
    }
}

@Composable
private fun SourceTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        onClick = onClick
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null)
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

