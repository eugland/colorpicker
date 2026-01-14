package com.primortex.color.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.primortex.color.R
import com.primortex.color.service.CrosshairShape
import com.primortex.color.service.CrosshairSize
import com.primortex.color.service.PickerSensitivity
import com.primortex.color.service.SettingsService
import com.primortex.color.ui.components.CrosshairIndicator
import com.primortex.color.ui.components.ScreenScaffold

@Composable
fun CrosshairSettingsScreen(
    innerPadding: PaddingValues,
    onBack: () -> Unit
) {
    val crosshairSize by SettingsService.crosshairSize.collectAsState()
    val crosshairShape by SettingsService.crosshairShape.collectAsState()
    val pickerSensitivity by SettingsService.pickerSensitivity.collectAsState()

    ScreenScaffold(
        titleRes = R.string.picker_crosshair,
        innerPadding = innerPadding,
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader(stringResource(R.string.picker_crosshair_preview))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                CrosshairIndicator(
                    argb = MaterialTheme.colorScheme.primary.toArgb(),
                    size = crosshairSize,
                    shape = crosshairShape
                )
            }

            Text(
                stringResource(R.string.picker_crosshair_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SectionHeader(stringResource(R.string.size))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CrosshairSize.values().forEach { size ->
                    FilterChip(
                        selected = crosshairSize == size,
                        onClick = { SettingsService.setCrosshairSize(size) },
                        label = { Text(stringResource(size.labelRes)) }
                    )
                }
            }

            SectionHeader(stringResource(R.string.shape))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CrosshairShape.values().forEach { shape ->
                    FilterChip(
                        selected = crosshairShape == shape,
                        onClick = { SettingsService.setCrosshairShape(shape) },
                        label = { Text(stringResource(shape.labelRes)) },
                        leadingIcon = {
                            CrosshairIndicator(
                                argb = MaterialTheme.colorScheme.primary.toArgb(),
                                size = CrosshairSize.Small,
                                shape = shape,
                                displaySize = 22.dp
                            )
                        }
                    )
                }
            }

            SectionHeader(stringResource(R.string.sensitivity))
            Text(
                stringResource(R.string.sensitivity_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PickerSensitivity.values().forEach { sensitivity ->
                    FilterChip(
                        selected = pickerSensitivity == sensitivity,
                        onClick = { SettingsService.setPickerSensitivity(sensitivity) },
                        label = { Text(stringResource(sensitivity.labelRes)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider()
    }
}
