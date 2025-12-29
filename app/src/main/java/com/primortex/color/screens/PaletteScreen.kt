package com.primortex.color.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.primortex.color.ui.components.ColorSwatch
import com.primortex.color.ui.components.ScreenScaffold
import com.primortex.color.ui.util.argbToHex

@Composable
fun PaletteScreen(innerPadding: PaddingValues) {
    val selectedArgb = Color.Red.toArgb()
    val base = Color(selectedArgb)
    ScreenScaffold("Palette", innerPadding, selectedArgb) {
        Text("Base: ${argbToHex(selectedArgb)}")
        Spacer(Modifier.height(12.dp))
        Row {
            ColorSwatch(base)
            ColorSwatch(base.copy(alpha = 0.8f))
            ColorSwatch(base.copy(alpha = 0.6f))
            ColorSwatch(base.copy(alpha = 0.4f))
        }
        Spacer(Modifier.height(12.dp))
        Text("Phase 3: complementary/analogous/triad accents + copy/export.")
    }
}
