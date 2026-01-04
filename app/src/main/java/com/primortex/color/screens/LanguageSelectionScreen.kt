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
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.primortex.color.service.AppLanguage
import com.primortex.color.service.SettingsService
import com.primortex.color.ui.components.ScreenScaffold

@Composable
fun LanguageSelectionScreen(
    innerPadding: PaddingValues,
    onBack: () -> Unit,
) {
    val selectedLanguage by SettingsService.appLanguage.collectAsState()
    val listState = rememberLazyListState()

    ScreenScaffold(
        title = "Choose language",
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
                    "Select the language you want to use in the app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(AppLanguage.entries) { language ->
                LanguageOption(
                    language = language,
                    selected = language == selectedLanguage,
                    onSelect = { SettingsService.setAppLanguage(language) }
                )
            }
        }
    }
}

@Composable
private fun LanguageOption(
    language: AppLanguage,
    selected: Boolean,
    onSelect: () -> Unit
) {
    ListItem(
        headlineContent = { Text(language.label) },
        supportingContent = {
            if (language == AppLanguage.SystemDefault) {
                Text("Follows your device language")
            }
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Outlined.Language,
                contentDescription = null
            )
        },
        trailingContent = {
            if (selected) {
                Text(
                    "Selected",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        modifier = Modifier
            .padding(vertical = 2.dp)
            .clickable(onClick = onSelect)
    )
}

