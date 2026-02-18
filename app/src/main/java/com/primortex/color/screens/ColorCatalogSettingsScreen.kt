package com.primortex.color.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.primortex.color.R
import com.primortex.color.i18n.stringResource
import com.primortex.color.service.SettingsService
import com.primortex.color.ui.components.ScreenScaffold

@Composable
fun ColorCatalogSettingsScreen(
    innerPadding: PaddingValues,
    onBack: () -> Unit
) {
    val available by SettingsService.availableColorAssets.collectAsState()
    val selected by SettingsService.selectedColorAssets.collectAsState()

    ScreenScaffold(
        titleRes = R.string.color_catalogs,
        innerPadding = innerPadding,
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.color_catalogs_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (available.isEmpty()) {
                Text(
                    text = stringResource(R.string.color_catalogs_none_found),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                available.forEach { option ->
                    val isSelected = option.id in selected
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val next = selected.toMutableSet()
                            if (isSelected) next.remove(option.id) else next.add(option.id)
                            SettingsService.setSelectedColorAssets(next)
                        },
                        label = {
                            Text(
                                text = "${option.fileName} (${option.languageTag})",
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

