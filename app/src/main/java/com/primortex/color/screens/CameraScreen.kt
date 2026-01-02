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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.primortex.color.app.PickedColor
import com.primortex.color.service.RecentPicksService
import com.primortex.color.ui.util.argbToHex


@Preview
@Composable
fun PreviewCameraScreen() {
    CameraScreen(
        onOpenLiveCameraPicker = {},
        onPickFromAlbum = { _ -> }
    )
}
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
        if (uri != null) {
            onPickFromAlbum(uri.toString())
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Camera",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        item {
            ChooseSourceCard(
                onOpenLiveCameraPicker = onOpenLiveCameraPicker,
                onPickFromAlbum =  { pickPhotoLauncher.launch("image/*") }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent picks", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { RecentPicksService.clear() }) { Text("Clear") }
            }
        }


        if (history.isEmpty()) {
            item {
                AssistChip(
                    onClick = { /* no-op */ },
                    enabled = false,
                    label = { Text("No picks yet. Choose Camera or Album to start.") }
                )
            }
        } else {
            items(history) { picked ->
                ColorHistoryRow(
                    picked = picked
                )
            }
        }
    }
}

@Composable
private fun ChooseSourceCard(
    onOpenLiveCameraPicker: () -> Unit,
    onPickFromAlbum: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Choose a source",
                style = MaterialTheme.typography.titleMedium
            )

            CardButton(
                icon = { Icon(Icons.Filled.PhotoCamera, contentDescription = "Use Camera") },
                title = "Use Camera",
                subtitle = "Real-time picker with crosshair",
                onClick = onOpenLiveCameraPicker
            )

            CardButton(
                icon = { Icon(Icons.Filled.Collections, contentDescription = "Pick from Album") },
                title = "Pick from Album",
                subtitle = "Choose a photo to pick colors from",
                onClick = onPickFromAlbum
            )

            Text(
                text = "Tip: zoom + tap for precision.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CardButton(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) { icon() }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ColorHistoryRow(
    picked: PickedColor
) {
    val hex = argbToHex(picked.argb)
    val name = picked.name

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(picked.argb))
            )

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = hex,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
