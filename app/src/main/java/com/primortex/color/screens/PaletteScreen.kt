// com/primortex/color/screens/PaletteScreen.kt
package com.primortex.color.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.primortex.color.app.Palette
import com.primortex.color.app.PickedColor
import com.primortex.color.service.ColorNameIndex
import com.primortex.color.service.ColorNameLookup
import com.primortex.color.service.PaletteService
import com.primortex.color.service.RecentPicksService
import com.primortex.color.ui.components.ScreenScaffold
import com.primortex.color.ui.util.argbToHex
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.delay

@Preview(showBackground = true)
@Composable
fun previewPaletteScreen() {
    PaletteScreen(innerPadding = PaddingValues())
}

data class ColorHit(
    val argb: Int,
    val name: String
)

object ColorQueryResolver {

    fun search(query: String, limit: Int = 10): List<ColorHit> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()

        // HEX → nearest color lookup
        if (isHex(q)) {
            val colorInt = q.removePrefix("#").toLong(16).toInt()
            val nearest = ColorNameLookup.nearestName(colorInt)
            return listOf(
                ColorHit(
                    argb = colorInt,
                    name = nearest.name,
                )
            )
        }

        // NAME → name index search
        return ColorNameIndex.search(q, limit).map {
            ColorHit(
                argb = it.argb,
                name = it.name
            )
        }
    }

    private fun isHex(input: String): Boolean {
        val s = input.removePrefix("#")
        return s.length in setOf(3, 6, 8) &&
                s.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaletteScreen(
    innerPadding: PaddingValues,
    onOpenColorDetail: (argb: Int) -> Unit = {} // hook this to nav later
) {
    val clipboard = LocalClipboardManager.current

    val recents by RecentPicksService.history.collectAsState()
    val savedPalettes by PaletteService.palettes.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    val suggestions = remember(searchQuery, recents) {
        ColorQueryResolver.search(searchQuery, limit = 10)
    }

    var showBuilder by remember { mutableStateOf(false) }

    ScreenScaffold("Palette", innerPadding) {
        // ---- Search ----
        ColorSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onClear = { searchQuery = "" }
        )

        if (suggestions.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Suggestions", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.weight(1f))
                Text("Swipe →", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))

            LazyRow(

                horizontalArrangement = Arrangement.spacedBy(10.dp)

            ) {
                items(suggestions, key = { it.argb }) { hit ->
                    Swatch(
                        argb = hit.argb,
                        onClick = { onOpenColorDetail(hit.argb) },
                        label = hit.name
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                "Saved palettes",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            FilledTonalButton(onClick = { showBuilder = true }) {
                Text("Create palette")
            }
        }

        Spacer(Modifier.height(8.dp))

        if (savedPalettes.isEmpty()) {
            Text("No saved palettes yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                savedPalettes.forEach { p ->
                    PaletteCard(
                        palette = p,
                        onOpen = { /* optional: open builder prefilled */ showBuilder = true },
                        onCopy = { clipboard.setText(AnnotatedString(p.colors.joinToString(", ") { argbToHex(it.argb) })) },
                        onDelete = { PaletteService.delete(p.id) }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---- Recents ----
        Text("Recent colors", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        if (recents.isEmpty()) {
            Text(
                "No recent colors yet. Tap 🧪 to pick a color.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(recents, key = { it.argb }) { pick ->
                    Swatch(
                        argb = pick.argb,
                        onClick = { onOpenColorDetail(pick.argb) },
                        label = pick.name
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
    }

    if (showBuilder) {
        PaletteBuilderSheet(
            recents = recents,
            onDismiss = { showBuilder = false },
            onOpenColorDetail = onOpenColorDetail
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaletteBuilderSheet(
    recents: List<PickedColor>,
    onDismiss: () -> Unit,
    onOpenColorDetail: (Int) -> Unit
) {
    val clipboard = LocalClipboardManager.current

    val selected = remember { mutableStateListOf<PickedColor>() }

    var name by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    var query by remember { mutableStateOf("") }

    val suggestions = remember(query, recents) {
        ColorQueryResolver.search(query, limit = 12)
    }

    fun togglePick(p: PickedColor) {
        val idx = selected.indexOfFirst { it.argb == p.argb }
        if (idx >= 0) selected.removeAt(idx) else selected.add(0, p)
    }

    fun addHit(hit: ColorHit) {
        val p = PickedColor(argb = hit.argb, name = hit.name)
        togglePick(p)
    }

    fun selectedHexList(): String = selected.joinToString(", ") { argbToHex(it.argb) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp)
        ) {
            Text("Create palette", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))

            // Search inside builder
            ColorSearchBar(
                query = query,
                onQueryChange = { query = it },
                onClear = { query = "" }
            )

            if (suggestions.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("Add from search", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(suggestions, key = { it.argb }) { hit ->
                        Swatch(
                            argb = hit.argb,
                            onClick = { addHit(hit) },
                            label = hit.name
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Text("Pick from recents", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(recents, key = { it.argb }) { p ->
                    val isSelected = selected.any { it.argb == p.argb }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(6.dp)
                    ) {
                        Swatch(
                            argb = p.argb,
                            onClick = { togglePick(p) },
                            label = p.name
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (selected.isNotEmpty()) {
                Text("Selected", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(selected, key = { it.argb }) { p ->
                        Swatch(
                            argb = p.argb,
                            onClick = { onOpenColorDetail(p.argb) },
                            label = p.name
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(onClick = { selected.clear() }) { Text("Clear") }
                    FilledTonalButton(onClick = { clipboard.setText(AnnotatedString(selectedHexList())) }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Copy HEX")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Palette name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                placeholder = { Text("Tags (comma separated)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("Note") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        val t = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
                        if (selected.isNotEmpty()) {
                            PaletteService.create(name, selected.toList(), tags = t, note = note)
                        }
                        onDismiss()
                    },
                    enabled = selected.isNotEmpty(),
                ) {
                    Icon(Icons.Outlined.Save, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Save")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    val prompts = remember {
        listOf(
            "Curious about 'midnight blue'?",
            "Sunset in '#FFCC00'?",
            "Ever seen 'Japanese indigo'?",
            "What does 'forest fog' look like?",
            "Try 'muted sage'"
        )
    }

    var focused by remember { mutableStateOf(false) }
    var idx by remember { mutableIntStateOf(0) }

    LaunchedEffect(prompts) {
        idx = 0
        while (true) {
            delay(2200L)
            idx = (idx + 1) % prompts.size
        }
    }

    Column {
        Text("Quick color look up", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            leadingIcon = { Icon(Icons.Outlined.Palette, contentDescription = null) },
            placeholder = {
                AnimatedContent(
                    targetState = prompts[idx],
                    transitionSpec = {
                        (
                                (slideInVertically { it / 6 } + fadeIn(tween(160))) togetherWith
                                        (slideOutVertically { -it / 6 } + fadeOut(tween(160)))
                                ).using(SizeTransform(clip = true))
                    },
                    label = "search-placeholder"
                ) { t ->
                    Text(
                        text = t,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

            },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused },
            shape = RoundedCornerShape(18.dp)
        )
    }
}


@Composable
private fun Swatch(argb: Int, onClick: () -> Unit, label: String) {
    val cellW = 72.dp

    Column(
        modifier = Modifier.width(cellW),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(argb))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .clickable(onClick = onClick)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,                        // 👈 key
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp,
            modifier = Modifier.fillMaxWidth()   // 👈 uses the fixed cell width
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
                Text(
                    palette.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
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
