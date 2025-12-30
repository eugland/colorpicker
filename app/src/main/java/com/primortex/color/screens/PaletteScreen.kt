// com/primortex/color/screens/PaletteScreen.kt
package com.primortex.color.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.primortex.color.app.Palette
import com.primortex.color.app.PickedColor
import com.primortex.color.service.PaletteService
import com.primortex.color.service.RecentPicksService
import com.primortex.color.ui.components.ScreenScaffold
import com.primortex.color.ui.util.argbToHex

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaletteScreen(innerPadding: PaddingValues) {
    val clipboard = LocalClipboardManager.current

    val recents by RecentPicksService.history.collectAsState()
    val savedPalettes by PaletteService.palettes.collectAsState()

    var query by remember { mutableStateOf(TextFieldValue("")) }

    val draft = remember { mutableStateListOf<PickedColor>() }
    var draftName by remember { mutableStateOf("") }
    var draftTags by remember { mutableStateOf("") }
    var draftNote by remember { mutableStateOf("") }

    fun addToDraft(p: PickedColor) {
        if (draft.none { it.argb == p.argb }) draft.add(0, p)
    }

    fun draftHexList(): String = draft.joinToString(", ") { argbToHex(it.argb) }

    val filteredRecents = remember(query.text, recents) {
        val q = query.text.trim()
        if (q.isBlank()) recents
        else {
            val uq = q.uppercase()
            recents.filter { p ->
                val hex = argbToHex(p.argb).uppercase()
                val name = (p.name ?: "").uppercase()
                val noHash = uq.removePrefix("#")
                name.contains(uq) || hex.removePrefix("#").contains(noHash)
            }
        }
    }

    ScreenScaffold("Palette", innerPadding, selectedArgb = (draft.firstOrNull()?.argb ?: 0xFF7B8266.toInt())) {

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Outlined.Palette, contentDescription = null) },
            placeholder = { Text("Search name, #hex…") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("Draft palette", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                if (draft.isEmpty()) {
                    Text(
                        "Tap a recent color to add it here.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(draft, key = { it.argb }) { p ->
                            Swatch(
                                argb = p.argb,
                                onClick = { },
                                label = p.name ?: argbToHex(p.argb)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(onClick = { draft.clear() }) { Text("Clear") }
                        FilledTonalButton(onClick = { clipboard.setText(AnnotatedString(draftHexList())) }) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Copy HEX")
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = draftName,
                        onValueChange = { draftName = it },
                        placeholder = { Text("Palette name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = draftTags,
                        onValueChange = { draftTags = it },
                        placeholder = { Text("Tags (comma separated)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = draftNote,
                        onValueChange = { draftNote = it },
                        placeholder = { Text("Note") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    Spacer(Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val tags = draftTags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
                            PaletteService.create(draftName, draft.toList(), tags = tags, note = draftNote)
                            draftName = ""
                            draftTags = ""
                            draftNote = ""
                        },
                        enabled = draft.isNotEmpty(),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Outlined.Save, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Save palette")
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Text("Saved palettes", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        if (savedPalettes.isEmpty()) {
            Text("No saved palettes yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                savedPalettes.forEach { p ->
                    PaletteCard(
                        palette = p,
                        onOpen = { draft.clear(); draft.addAll(p.colors) },
                        onCopy = { clipboard.setText(AnnotatedString(p.colors.joinToString(", ") { argbToHex(it.argb) })) },
                        onDelete = { PaletteService.delete(p.id) }
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Text("Recent colors", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        if (filteredRecents.isEmpty()) {
            Text(
                "No recent colors yet. Tap 🧪 to pick a color, or 🎨 to create a palette.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filteredRecents, key = { it.argb }) { pick ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Swatch(argb = pick.argb, onClick = { addToDraft(pick) }, label = pick.name ?: argbToHex(pick.argb))
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun Swatch(argb: Int, onClick: () -> Unit, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(argb))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .clickable(onClick = onClick)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 76.dp)
        )
    }
}

@Composable
private fun PaletteCard(
    palette: Palette,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(palette.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (palette.tags.isNotEmpty()) {
                        Text(
                            palette.tags.joinToString(" • "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = onCopy) { Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy") }
                IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = "Delete") }
            }

            if (palette.note.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(palette.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }

            Spacer(Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(palette.colors.take(10), key = { it.argb }) { p ->
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(p.argb))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .clickable(onClick = onOpen)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = onOpen) { Text("Open") }
                OutlinedButton(onClick = onCopy) { Text("Copy HEX") }
            }
        }
    }
}
