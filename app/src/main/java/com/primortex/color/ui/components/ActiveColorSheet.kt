package com.primortex.color.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.primortex.color.R
import com.primortex.color.app.PickedColor


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveColorSheet(
    picked: PickedColor,
    recents: List<PickedColor>,
    onTapPick: (PickedColor) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(top = 8.dp)
    ) {
        ColorRow(
            pick = picked,
            onTapPick = { pick -> onTapPick(pick) },
            showViewMore = true
        )

        Divider(Modifier.padding(top = 6.dp))
        Text(
            stringResource(R.string.recent_colors),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // Expanded list: user pulls up to see more
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(recents) { pick ->
                ColorRow(pick, onTapPick)
            }
        }
    }
}

@Composable
fun ColorRow(
    pick: PickedColor,
    onTapPick: (PickedColor) -> Unit,
    showViewMore: Boolean = false
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onTapPick(pick) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(pick.argb))
        )

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                pick.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                String.format("#%06X", pick.argb and 0xFFFFFF),
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (showViewMore) {
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(R.string.view_more),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
