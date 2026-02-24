package com.primortex.color.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun WaterCard(
    icon: ImageVector,
    title: String,
    description: String,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null
) {
    val transition = rememberInfiniteTransition(label = "water-card-flow")
    val waveShift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "water-card-shift"
    )
    val waveDrift by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "water-card-drift"
    )
    val surfaceShape = RoundedCornerShape(22.dp)
    val badgeShape = RoundedCornerShape(999.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(surfaceShape)
            .background(
                Brush.linearGradient(
                    colors = gradientColors,
                    start = Offset(
                        x = -220f + (520f * waveShift),
                        y = 20f + (140f * waveDrift)
                    ),
                    end = Offset(
                        x = 520f + (520f * waveShift),
                        y = 280f + (140f * waveDrift)
                    )
                )
            )
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, surfaceShape)
            .clickable(onClick = onClick)
            .padding(18.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (!badgeText.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(
                        color = Color.White.copy(alpha = 0.25f),
                        shape = badgeShape
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.4f), badgeShape)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

