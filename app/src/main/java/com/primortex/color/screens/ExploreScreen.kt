package com.primortex.color.screens

import android.graphics.Color
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.primortex.color.ui.components.ScreenScaffold
import com.primortex.color.ui.util.argbToHex

@Composable
fun ExploreScreen(innerPadding: PaddingValues) {
    val selectedArgb = Color.GREEN
    ScreenScaffold("Explore", innerPadding, selectedArgb) {
        Text("Selected: ${argbToHex(selectedArgb)}")
        Text("Phase 4: name lookup, usage, brands/apps using it, references.")
    }
}
