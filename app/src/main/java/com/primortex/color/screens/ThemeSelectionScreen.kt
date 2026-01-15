package com.primortex.color.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.primortex.color.R
import com.primortex.color.service.SettingsService
import com.primortex.color.service.ThemeMode
import com.primortex.color.ui.components.ScreenScaffold
import androidx.compose.ui.res.stringResource as str

@Composable
fun ThemeSelectionScreen(
    innerPadding: PaddingValues,
    onBack: () -> Unit
) {
    val selectedTheme by SettingsService.themeMode.collectAsState()
    val listState = rememberLazyListState()

    ScreenScaffold(
        titleRes = R.string.choose_theme,
        innerPadding = innerPadding,
        onBack = onBack
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 12.dp),
            state = listState
        ) {
            item {
                Text(
                    stringResource(R.string.choose_theme_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(ThemeMode.entries) { mode ->
                ThemeOption(
                    mode = mode,
                    selected = mode == selectedTheme,
                    onSelect = {
                        SettingsService.setThemeMode(mode)
                    }
                )
            }
        }
    }
}

@Composable
private fun ThemeOption(
    mode: ThemeMode,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        onClick = onSelect
    ) {
        ListItem(
            headlineContent = { Text(str(mode.labelRes)) },
            supportingContent = {
                if (mode == ThemeMode.SYSTEM) {
                    Text(str(R.string.follows_your_system_theme))
                }
            },
            leadingContent = {
                Icon(
                    imageVector = Icons.Outlined.DarkMode,
                    contentDescription = null
                )
            },
            trailingContent = {
                if (selected) {
                    Text(
                        str(R.string.selected_str),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
