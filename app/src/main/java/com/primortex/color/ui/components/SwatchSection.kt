package com.primortex.color.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.primortex.color.R
import com.primortex.color.app.PickedColor

@Composable
fun SwatchSection(
    title: String,
    picks: List<PickedColor>,
    emptyMessage: String,
    onSwatchClick: (PickedColor) -> Unit,
    modifier: Modifier = Modifier,
    threshold: Int = 10,
    actions: (@Composable () -> Unit)? = null,
) {
    var showAll by remember { mutableStateOf(false) }

    LaunchedEffect(picks.size) {
        if (picks.size <= threshold) showAll = false
    }

    val hasMoreThanThreshold = picks.size > threshold
    val visiblePicks = if (!hasMoreThanThreshold || showAll) picks else picks.take(threshold)

    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            actions?.invoke()
        }
        Spacer(Modifier.height(8.dp))

        if (picks.isEmpty()) {
            Text(
                emptyMessage,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp), // SpaceBetween
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                visiblePicks.forEach { pick ->
                    Swatch(
                        argb = pick.argb,
                        onClick = { onSwatchClick(pick) },
                        label = pick.name
                    )
                }
            }

            if (hasMoreThanThreshold) {
                Spacer(Modifier.height(10.dp))

                FilledTonalButton(
                    onClick = { showAll = !showAll },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (showAll) stringResource(R.string.show_less) else stringResource(R.string.show_more))
                }
            }
        }
    }
}

@Composable
fun Swatch(argb: Int, onClick: () -> Unit, label: String) {
    val cellW = 72.dp

    Column(
        modifier = Modifier.width(cellW),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(argb))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .clickable(onClick = onClick)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,                        // 👈 key
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp,
            modifier = Modifier.fillMaxWidth()   // 👈 uses the fixed cell width
        )
    }
}
