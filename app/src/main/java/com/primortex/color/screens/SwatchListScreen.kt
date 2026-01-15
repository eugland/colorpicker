package com.primortex.color.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.primortex.color.R
import com.primortex.color.app.PickedColor
import com.primortex.color.service.RecentPicksService
import com.primortex.color.service.argbToHex
import com.primortex.color.ui.components.ScreenScaffold

enum class SwatchListType {
    RECENT,
    SAVED
}

@Composable
fun SwatchListScreen(
    innerPadding: PaddingValues,
    type: SwatchListType,
    onBack: () -> Unit,
    onOpenColorDetail: (PickedColor) -> Unit,
) {
    val titleRes = when (type) {
        SwatchListType.RECENT -> R.string.recent_colors
        SwatchListType.SAVED -> R.string.saved_colors
    }
    val picks by when (type) {
        SwatchListType.RECENT -> RecentPicksService.history.collectAsState()
        SwatchListType.SAVED -> RecentPicksService.saved.collectAsState()
    }

    var query by remember { mutableStateOf("") }
    val filtered = remember(picks, query) {
        val lowered = query.trim().lowercase()
        if (lowered.isBlank()) {
            picks
        } else {
            picks.filter {
                it.name.lowercase().contains(lowered) ||
                    argbToHex(it.argb).lowercase().contains(lowered)
            }
        }
    }

    ScreenScaffold(titleRes, innerPadding, onBack = onBack) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = stringResource(R.string.clear)
                        )
                    }
                }
            },
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            val emptyText = if (query.isBlank()) {
                when (type) {
                    SwatchListType.RECENT -> stringResource(R.string.no_recent_colors)
                    SwatchListType.SAVED -> stringResource(R.string.no_saved_colors)
                }
            } else {
                stringResource(R.string.no_matching_colors)
            }
            Text(
                text = emptyText,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.argb }) { pick ->
                    val displayName = pick.name.ifBlank { argbToHex(pick.argb) }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 2.dp,
                        onClick = { onOpenColorDetail(pick) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Spacer(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(pick.argb))
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant,
                                            CircleShape
                                        )
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = argbToHex(pick.argb),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.more_details),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
