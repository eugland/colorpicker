package com.primortex.color.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.primortex.color.i18n.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.primortex.color.app.PickedColor
import com.primortex.color.R

@Composable
fun PaletteBar(
    modifier: Modifier,
    palette: List<PickedColor>,
    onAddColor: () -> Unit,
    onAddPalette: () -> Unit,
    onClearPalette: () -> Unit = {}
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClearPalette,
                enabled = palette.isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.palette_bar_clear),
                    tint = if (palette.isNotEmpty())
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }

            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(14.dp)
                    )
                    .clickable(onClick = onAddPalette)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Palette,
                    contentDescription = stringResource(R.string.palette_bar_palette),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                StackedColorsPreview(colors = palette)

            }

            Spacer(Modifier.width(10.dp))

            // ➕ ADD COLOR (far right)
            IconButton(
                onClick = onAddColor,
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Colorize,
                    contentDescription = stringResource(R.string.palette_bar_add_color),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun StackedColorsPreview(
    colors: List<PickedColor>,
    modifier: Modifier = Modifier,
    circleSize: Dp = 28.dp,
    maxVisible: Int = 10
) {
    BoxWithConstraints(modifier.height(circleSize)) {
        val width = maxWidth
        //         val visible = colors.take(maxVisible)
        val visible = colors
        val count = visible.size

        val spacing = if (count <= 1) 0.dp
        else ((width - circleSize) / (count - 1))
            .coerceAtMost(circleSize * 0.6f)

        Box(Modifier.fillMaxWidth()) {
            visible.forEachIndexed { i, c ->
                Box(
                    Modifier
                        .offset(x = spacing * i)
                        .size(circleSize)
                        .clip(CircleShape)
                        .background(Color(c.argb))
                        .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
                )
            }
        }
    }
}


