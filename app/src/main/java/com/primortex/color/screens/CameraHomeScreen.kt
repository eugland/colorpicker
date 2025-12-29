package com.primortex.color.screens



import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.primortex.color.app.PickedColor
import com.primortex.color.service.RecentPicksService

@Composable
fun CameraHomeScreen(
    onOpenCamera: () -> Unit,
    onOpenAlbum: () -> Unit,
) {
    val history by RecentPicksService.history.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Camera", style = MaterialTheme.typography.headlineSmall) }

        item {
            Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Choose a source", style = MaterialTheme.typography.titleMedium)

                    SourceButton(
                        icon = { Icon(Icons.Filled.PhotoCamera, null) },
                        title = "Use Camera",
                        subtitle = "Real-time picker with crosshair",
                        onClick = onOpenCamera
                    )
                    SourceButton(
                        icon = { Icon(Icons.Filled.Collections, null) },
                        title = "Pick from Album",
                        subtitle = "Choose a photo to pick colors from",
                        onClick = onOpenAlbum
                    )
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Recent picks", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { RecentPicksService.clear() }) { Text("Clear") }
            }
        }

        if (history.isEmpty()) {
            item {
                AssistChip(onClick = {}, label = { Text("No picks yet. Choose Camera or Album to start.") })
            }
        } else {
            items(history) { picked ->
                HistoryRow(picked)
            }
        }
    }
}

@Composable
private fun SourceButton(icon: @Composable () -> Unit, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).clickable(onClick = onClick),
        tonalElevation = 2.dp
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(42.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                Box(contentAlignment = Alignment.Center) { icon() }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HistoryRow(picked: PickedColor) {
    val hex = argbToHex(picked.argb)
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(54.dp).clip(CircleShape).background(Color(picked.argb)))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(hex, style = MaterialTheme.typography.titleMedium)
                Text(picked.source, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

fun argbToHex(argb: Int): String = "#%06X".format(argb and 0x00FFFFFF)
