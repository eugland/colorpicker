package com.primortex.color.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import com.primortex.color.service.CrosshairShape
import com.primortex.color.service.CrosshairSize

@Composable
fun CrosshairIndicator(
    argb: Int,
    size: CrosshairSize,
    shape: CrosshairShape,
    modifier: Modifier = Modifier
) {
    val indicatorColor = Color(argb)
    val indicatorSize = size.toDp()
    val borderColor = MaterialTheme.colorScheme.surface

    when (shape) {
        CrosshairShape.Circle -> Box(
            modifier
                .size(indicatorSize)
                .background(indicatorColor, CircleShape)
                .border(2.dp, borderColor, CircleShape)
        )

        CrosshairShape.Square -> Box(
            modifier
                .size(indicatorSize)
                .background(indicatorColor, RoundedCornerShape(8.dp))
                .border(2.dp, borderColor, RoundedCornerShape(8.dp))
        )

        CrosshairShape.Cross -> Canvas(modifier.size(indicatorSize)) {
            val stroke = 3.dp.toPx()
            val inset = indicatorSize.toPx() * 0.2f
            val radius = indicatorSize.toPx() / 2f

            // Draw cross lines
            drawLine(
                color = borderColor,
                start = center.copy(x = center.x - radius + inset),
                end = center.copy(x = center.x + radius - inset),
                strokeWidth = stroke
            )
            drawLine(
                color = borderColor,
                start = center.copy(y = center.y - radius + inset),
                end = center.copy(y = center.y + radius - inset),
                strokeWidth = stroke
            )

            // Outline circle
            drawCircle(color = borderColor, radius = radius - inset / 2f, style = Stroke(width = stroke))

            // Focus dot
            drawCircle(color = indicatorColor, radius = stroke * 1.6f)
        }
    }
}

private fun CrosshairSize.toDp(): Dp = when (this) {
    CrosshairSize.Small -> 22.dp
    CrosshairSize.Medium -> 30.dp
    CrosshairSize.Large -> 38.dp
}

@Preview
@Composable
private fun CrosshairPreview() {
    CrosshairIndicator(
        argb = 0xFF5E81AC.toInt(),
        size = CrosshairSize.Medium,
        shape = CrosshairShape.Cross
    )
}
