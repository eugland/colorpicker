package com.primortex.color.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.Gradient
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.primortex.color.service.RecentPicksService
import com.primortex.color.R

@Composable
fun CameraScreen(
    innerPadding: PaddingValues = PaddingValues(),
    onOpenLiveCameraPicker: () -> Unit,
    onOpenColorSlider: () -> Unit,
    onOpenColorBlindEnhancer: () -> Unit,
    onOpenIpLookup: () -> Unit,
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

        LivePickerHeroCard(
            onOpenLiveCameraPicker = onOpenLiveCameraPicker
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SourceTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.PhotoCamera,
                title = stringResource(R.string.live_camera),
                subtitle = stringResource(R.string.live_camera_subtitle),
                onClick = onOpenLiveCameraPicker
            )
            SourceTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Collections,
                title = stringResource(R.string.from_album),
                subtitle = stringResource(R.string.from_album_subtitle),
                onClick = { pickPhotoLauncher.launch("image/*") }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.tools), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))

        }

        ColorSliderCard(onOpenColorSlider = onOpenColorSlider)
        ColorBlindEnhancerCard(onOpenColorBlindEnhancer = onOpenColorBlindEnhancer)
        IpLookupCard(onOpenIpLookup = onOpenIpLookup)
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
                Text(stringResource(R.string.color_slider), style = MaterialTheme.typography.titleMedium)
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
private fun IpLookupCard(onOpenIpLookup: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        onClick = onOpenIpLookup
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Outlined.Security, contentDescription = null)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.ip_lookup),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    stringResource(R.string.ip_lookup_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(onClick = onOpenIpLookup) {
                Text(stringResource(R.string.open))
            }
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
                Text(stringResource(R.string.live_color_picker), style = MaterialTheme.typography.titleLarge)
            }

            Text(
                stringResource(R.string.live_color_picker_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onOpenLiveCameraPicker,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.start_live_picking))
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
