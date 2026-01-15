package com.primortex.color.ui.components

import android.content.ClipData
import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.primortex.color.R
import com.primortex.color.app.PickedColor
import com.primortex.color.service.ColorDetails
import com.primortex.color.service.ColorDetailsService
import com.primortex.color.service.RecentPicksService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorDetailsBottomSheet(
    picked: PickedColor,
    onDismiss: () -> Unit,
    onOpenColorDetail: (PickedColor) -> Unit = {},
    skipPartiallyExpanded: Boolean = true
) {
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = skipPartiallyExpanded // ✅ opens “expanded” (no half height)
    )


    val details: ColorDetails = remember(picked.argb) {
        ColorDetailsService.details(picked.argb, similarLimit = 10)
    }
    val savedColors by RecentPicksService.saved.collectAsState()
    val isSaved = savedColors.any { it.argb == picked.argb }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(details.argb))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                )
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            details.name,
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    scope.launch {
                                        copyToClipboard(context, clipboard, R.string.copy_name, details.name)
                                    }
                                }
                        )
                        IconButton(onClick = {
                            scope.launch {
                                copyToClipboard(context, clipboard, R.string.copy_name, details.name)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = stringResource(R.string.copy_name)
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            details.hex,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    scope.launch {
                                        copyToClipboard(context, clipboard, R.string.copy_hex, details.hex)
                                    }
                                }
                        )
                        IconButton(onClick = {
                            scope.launch {
                                copyToClipboard(context, clipboard, R.string.copy_hex, details.hex)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = stringResource(R.string.copy_hex)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { RecentPicksService.toggleSaved(picked) }) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isSaved) {
                            stringResource(R.string.saved)
                        } else {
                            stringResource(R.string.save)
                        }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Info chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoPill(stringResource(R.string.luma_label, (details.luminance * 100).toInt()))
                InfoPill(
                    if (details.isDark) {
                        stringResource(R.string.dark_label)
                    } else {
                        stringResource(R.string.light_label)
                    }
                )
                InfoPill(
                    run {
                        val textColor = if (details.recommendedOnColor == 0xFFFFFFFF.toInt()) {
                            stringResource(R.string.white_label)
                        } else {
                            stringResource(R.string.black_label)
                        }
                        stringResource(R.string.text_recommendation, textColor)
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            // RGB / HSV / HSL
            KeyValueGrid(
                items = listOf(
                    stringResource(R.string.rgb_label) to "${details.rgb.r}, ${details.rgb.g}, ${details.rgb.b}",
                    stringResource(R.string.hsv_label) to "${details.hsv.h.toInt()}°, ${(details.hsv.s * 100).toInt()}%, ${(details.hsv.v * 100).toInt()}%",
                    stringResource(R.string.hsl_label) to "${details.hsl.h.toInt()}°, ${(details.hsl.s * 100).toInt()}%, ${(details.hsl.l * 100).toInt()}%"
                )
            )

            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    onDismiss()
                    onOpenColorDetail(picked)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    stringResource(R.string.show_more),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun KeyValueGrid(items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (k, v) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    k,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(52.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    v,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun InfoPill(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

private suspend fun copyToClipboard(
    context: Context,
    clipboard: Clipboard,
    @StringRes labelResId: Int,
    text: String
) {
    clipboard.setClipEntry(
        ClipData.newPlainText(
            context.getString(labelResId),
            text
        ).toClipEntry()
    )
}
