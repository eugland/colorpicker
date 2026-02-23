package com.primortex.color.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.primortex.color.R
import com.primortex.color.i18n.stringResource

@Composable
fun AnimatedSplashHost(showContent: Boolean, content: @Composable () -> Unit) {
    Crossfade(targetState = showContent, label = "splash-crossfade") { ready ->
        if (ready) {
            content()
        } else {
            SplashScreen()
        }
    }
}

@Composable
private fun SplashScreen() {
    val colors = MaterialTheme.colorScheme
    val splashBase = colorResource(id = R.color.splash_background)
    val splashBottom = lerp(splashBase, Color.White, 0.22f)
    val transition = rememberInfiniteTransition(label = "splash-loop")
    val pulse by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing)
        ),
        label = "rotation"
    )
    val glow by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        splashBase,
                        splashBottom
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 36.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(260.dp)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    colors.secondary.copy(alpha = glow),
                                    Color.Transparent
                                ),
                                radius = size.maxDimension / 2
                            )
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .graphicsLayer(
                            rotationZ = rotation * 0.3f,
                            scaleX = pulse + 0.05f,
                            scaleY = pulse + 0.05f,
                            alpha = 0.5f
                        )
                        .background(
                            brush = Brush.radialGradient(
                                listOf(colors.tertiary.copy(alpha = 0.25f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )

                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(id = R.string.app_name),
                    modifier = Modifier
                        .size(156.dp)
                        .graphicsLayer(
                            scaleX = pulse,
                            scaleY = pulse,
                            rotationZ = rotation * 0.08f
                        )
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.splash_loading_message),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
