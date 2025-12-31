package com.primortex.color.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.primortex.color.ui.components.ScreenScaffold

@Composable
fun ExploreScreen(innerPadding: PaddingValues) {
    ScreenScaffold("Explore", innerPadding) {
        Text("Settings here")
    }
}
