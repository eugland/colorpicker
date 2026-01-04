package com.primortex.color.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.Gradient
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.primortex.color.app.PickedColor
import com.primortex.color.service.RecentPicksService
import com.primortex.color.ui.util.argbToHex

@Composable
fun CameraScreen(
    innerPadding: PaddingValues = PaddingValues(),
    onOpenLiveCameraPicker: () -> Unit,
    onOpenColorSlider: () -> Unit,
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
            .padding(16.dp),
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

        ColorSliderCard(onOpenColorSlider = onOpenColorSlider)

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
    }
}

@Composable
private fun ColorSliderCard(onOpenColorSlider: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        onClick = onOpenColorSlider
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Outlined.Gradient, contentDescription = null)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Color slider", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Adjust RGB or HSL and see live names and hex. Save or build palettes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(onClick = onOpenColorSlider) { Text("Open") }
        }
    }
}

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

