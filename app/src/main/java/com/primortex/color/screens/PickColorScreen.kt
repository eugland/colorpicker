package com.primortex.color.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.Gradient
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.primortex.color.R
import com.primortex.color.service.RecentPicksService

@Composable
fun CameraScreen(
    innerPadding: PaddingValues = PaddingValues(),
    onOpenLiveCameraPicker: () -> Unit,
    onOpenColorSlider: () -> Unit,
    onOpenColorBlindEnhancer: () -> Unit,
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
        Text(stringResource(R.string.camera_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.camera_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LivePickerWaterCard(
            icon = Icons.Filled.PhotoCamera,
            title = stringResource(R.string.pick_color_here),
            description = stringResource(R.string.pick_color_here_description),
            badgeText = stringResource(R.string.live_picking),
            gradientColors = listOf(
                Color(0xFF1B6EF3),
                Color(0xFF2FD1C6),
                Color(0xFF3FAFE8),
                Color(0xFF3561E8)
            ),
            onClick = onOpenLiveCameraPicker
        )

        LivePickerWaterCard(
            icon = Icons.Filled.Collections,
            title = stringResource(R.string.from_gallery),
            description = stringResource(R.string.from_album_subtitle),
            badgeText = stringResource(R.string.from_album),
            gradientColors = listOf(
                Color(0xFF5B3FE3), // deeper indigo
                Color(0xFF9A55E6), // darker violet
                Color(0xFFE18A3F), // burnt orange
                Color(0xFFD95B54)  // muted coral
            ),
            onClick = { pickPhotoLauncher.launch("image/*") }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.tools), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))

        }

        ColorSliderCard(onOpenColorSlider = onOpenColorSlider)
        ColorBlindEnhancerCard(onOpenColorBlindEnhancer = onOpenColorBlindEnhancer)
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
                Text(
                    stringResource(R.string.color_slider),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    stringResource(R.string.color_slider_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(onClick = onOpenColorSlider) { Text(stringResource(R.string.open)) }
        }
    }
}

@Composable
private fun ColorBlindEnhancerCard(onOpenColorBlindEnhancer: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        onClick = onOpenColorBlindEnhancer
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Outlined.Colorize, contentDescription = null)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.color_blind_enhancer),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    stringResource(R.string.color_blind_enhancer_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(onClick = onOpenColorBlindEnhancer) {
                Text(stringResource(R.string.open))
            }
        }
    }
}

@Composable
private fun LivePickerWaterCard(
    icon: ImageVector,
    title: String,
    description: String,
    badgeText: String,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "camera-water-flow")
    val waveShift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "camera-wave-shift"
    )
    val waveDrift by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "camera-wave-drift"
    )
    val surfaceShape = RoundedCornerShape(22.dp)
    val badgeShape = RoundedCornerShape(999.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(surfaceShape)
            .background(
                Brush.linearGradient(
                    colors = gradientColors,
                    start = Offset(
                        x = -220f + (520f * waveShift),
                        y = 20f + (140f * waveDrift)
                    ),
                    end = Offset(
                        x = 520f + (520f * waveShift),
                        y = 280f + (140f * waveDrift)
                    )
                )
            )
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, surfaceShape)
            .clickable(onClick = onClick)
            .padding(18.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(
                    color = Color.White.copy(alpha = 0.25f),
                    shape = badgeShape
                )
                .border(1.dp, Color.White.copy(alpha = 0.4f), badgeShape)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                badgeText,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}
