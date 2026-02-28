package com.primortex.color.features.camera

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.primortex.color.i18n.stringResource
import androidx.compose.ui.unit.dp
import com.primortex.color.R
import com.primortex.color.ui.TestTags
import com.primortex.color.ui.components.WaterCard

@Composable
fun CameraScreen(
    innerPadding: PaddingValues = PaddingValues(),
    onOpenLiveCameraPicker: () -> Unit,
    onOpenColorSlider: () -> Unit,
    onOpenColorBlindEnhancer: () -> Unit,
    onPickFromAlbum: (String) -> Unit
) {
    val isColorBlindEnhancerSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

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

        WaterCard(
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
            onClick = onOpenLiveCameraPicker,
            modifier = Modifier.testTag(TestTags.CAMERA_START_PICKING_CARD)
        )

        WaterCard(
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.tools), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))

        }

        Spacer(Modifier.height(6.dp))

        ColorSliderCard(onOpenColorSlider = onOpenColorSlider)
        ColorBlindEnhancerCard(
            onOpenColorBlindEnhancer = onOpenColorBlindEnhancer,
            isEnabled = isColorBlindEnhancerSupported
        )
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
private fun ColorBlindEnhancerCard(
    onOpenColorBlindEnhancer: () -> Unit,
    isEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        onClick = onOpenColorBlindEnhancer,
        enabled = isEnabled
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
            FilledTonalButton(onClick = onOpenColorBlindEnhancer, enabled = isEnabled) {
                Text(stringResource(R.string.open))
            }
        }
    }
}


