package com.primortex.color.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.primortex.color.R
import com.primortex.color.app.PickedColor
import com.primortex.color.service.recommendedOnColor

@Composable
fun SwatchSection(
    title: String,
    picks: List<PickedColor>,
    emptyMessage: String,
    onSwatchClick: (PickedColor) -> Unit,
    modifier: Modifier = Modifier,
    threshold: Int = 10,
    showFooterToggle: Boolean = true,
    showEndSeeMore: Boolean = false,
    onEndSeeMore: (() -> Unit)? = null,
    swatchSize: Dp = 96.dp,
    swatchShape: Shape = RoundedCornerShape(10.dp),
    swatchLabelBelow: Boolean = false,
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
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(visiblePicks, key = { it.argb }) { pick ->
                    Swatch(
                        argb = pick.argb,
                        onClick = { onSwatchClick(pick) },
                        label = pick.name,
                        size = swatchSize,
                        shape = swatchShape,
                        labelBelow = swatchLabelBelow
                    )
                }
                if (hasMoreThanThreshold && showEndSeeMore && onEndSeeMore != null) {
                    item {
                        Box(
                            modifier = Modifier
                                .size(swatchSize)
                                .clip(swatchShape)
                                .clickable(onClick = onEndSeeMore)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    swatchShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = stringResource(R.string.see_more),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            if (hasMoreThanThreshold && showFooterToggle) {
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
fun Swatch(
    argb: Int,
    onClick: () -> Unit,
    label: String,
    size: Dp = 96.dp,
    shape: Shape = RoundedCornerShape(10.dp),
    labelBelow: Boolean = false
) {
    val onColor = remember(argb) { recommendedOnColor(argb) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(Color(argb))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
            contentAlignment = Alignment.Center
        ) {
            if (!labelBelow) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(onColor),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    lineHeight = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                )
            }
        }
        if (labelBelow) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )
        }
    }
}
