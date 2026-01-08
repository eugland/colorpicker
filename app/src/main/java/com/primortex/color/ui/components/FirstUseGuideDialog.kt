package com.primortex.color.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.primortex.color.R

@Composable
fun FirstUseGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.first_use_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.first_use_body),
                    style = MaterialTheme.typography.bodyMedium
                )
                GuideStep(text = stringResource(R.string.first_use_step_camera))
                GuideStep(text = stringResource(R.string.first_use_step_photo))
                GuideStep(text = stringResource(R.string.first_use_step_palette))
                GuideStep(text = stringResource(R.string.first_use_step_tap_color))
                GuideStep(text = stringResource(R.string.first_use_step_details))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.first_use_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.first_use_skip))
            }
        }
    )
}

@Composable
private fun GuideStep(text: String) {
    Column {
        Text(text = "• $text", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(2.dp))
    }
}
