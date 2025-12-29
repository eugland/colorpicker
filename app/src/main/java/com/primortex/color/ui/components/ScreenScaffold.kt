package com.primortex.color.ui.components


import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.primortex.color.ui.util.argbToHex

@Composable
fun ScreenScaffold(
    title: String,
    innerPadding: PaddingValues,
    selectedArgb: Int,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.padding(innerPadding).fillMaxSize().padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ColorSwatch(Color(selectedArgb))
            Spacer(Modifier.width(12.dp))
            Text("Current: ${argbToHex(selectedArgb)}")
        }
        Spacer(Modifier.height(16.dp))
        content()
    }
}
