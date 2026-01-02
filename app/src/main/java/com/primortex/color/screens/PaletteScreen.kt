// com/primortex/color/screens/PaletteScreen.kt
package com.primortex.color.screens

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
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
import com.primortex.color.ui.components.ColorDetailsBottomSheet
import com.primortex.color.ui.components.ScreenScaffold
import com.primortex.color.ui.util.argbToHex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Preview(showBackground = true)
@Composable
fun previewPaletteScreen() {
    PaletteScreen(innerPadding = PaddingValues())
}

object ColorQueryResolver {

    fun search(query: String, limit: Int = 10): List<PickedColor> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()

        // HEX → nearest color lookup
        if (isHex(q)) {
            val rgb = q.removePrefix("#").toLong(16).toInt() and 0x00FFFFFF
            val argb = (0xFF shl 24) or rgb   // force alpha = 255
            val nearest = ColorNameLookup.nearestName(argb)
            Log.d("ColorSearch", "HEX pressed, top=${nearest.name} #${argb.toString(16)}")
            return listOf(
                PickedColor(
                    argb = argb,
                    name = nearest.name,
                )

            )
        }

        // NAME → name index search
        return ColorNameIndex.search(q, limit).map {
            PickedColor(
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
fun PaletteScreen(innerPadding: PaddingValues) {
    val clipboard = LocalClipboardManager.current

    val recents by RecentPicksService.history.collectAsState()
    val savedPalettes by PaletteService.palettes.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    val suggestions = remember(searchQuery, recents) {
        ColorQueryResolver.search(searchQuery, limit = 10)
    }
    var detailPick by remember { mutableStateOf<PickedColor?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val threshold = 20
    var showAllRecents by remember { mutableStateOf(false) }

    LaunchedEffect(recents.size) {
        if (recents.size <= threshold) showAllRecents = false
    }
    val hasMoreThanThreshold = recents.size > threshold
    val visibleRecents =
        if (!hasMoreThanThreshold || showAllRecents) recents else recents.take(threshold)
    hasMoreThanThreshold && showAllRecents

    ScreenScaffold(
        "Palette",
        innerPadding,
        snackbarHostState = snackbarHostState
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                ColorSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSubmit = {
                        val top = suggestions.firstOrNull() ?: return@ColorSearchBar
                        RecentPicksService.addPick(PickedColor(argb = top.argb, name = top.name))
                        scope.launch { snackbarHostState.showSnackbar("Added to recents") }
                        searchQuery = ""
                    },
                    onClear = { searchQuery = "" }
                )
            }

            if (suggestions.isNotEmpty()) {
                item { Spacer(Modifier.height(10.dp)) }

                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Suggestions", style = MaterialTheme.typography.titleSmall)
                    }
                }

                item { Spacer(Modifier.height(6.dp)) }

                items(
                    items = suggestions,
                    key = { it.argb }
                ) { s ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                detailPick = s
                                RecentPicksService.addPick(
                                    PickedColor(
                                        argb = s.argb,
                                        name = s.name
                                    )
                                )
                                scope.launch { snackbarHostState.showSnackbar("Added to recents") }
                                searchQuery = ""
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(s.argb))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        )

                        Spacer(Modifier.width(12.dp))

                        Column {
                            Text(
                                text = s.name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = argbToHex(s.argb),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
            item { Text("Recent colors", style = MaterialTheme.typography.titleMedium) }
            item { Spacer(Modifier.height(8.dp)) }

            if (recents.isEmpty()) {
                item {
                    Text(
                        "No recent colors yet. Tap 🧪 to pick a color.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 5 // same as your Fixed(5)
                    ) {
                        visibleRecents.forEach { pick ->
                            Swatch(
                                argb = pick.argb,
                                onClick = { detailPick = pick },
                                label = pick.name
                            )
                        }
                    }

                }

                if (hasMoreThanThreshold) {
                    item { Spacer(Modifier.height(10.dp)) }

                    item {
                        FilledTonalButton(
                            onClick = { showAllRecents = !showAllRecents },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (showAllRecents) "Show less" else "Show more")
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Saved palettes",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item { Spacer(Modifier.height(8.dp)) }

            if (savedPalettes.isEmpty()) {
                item {
                    Text(
                        "No saved palettes yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(savedPalettes, key = { it.id }) { p ->
                    PaletteCard(
                        palette = p,
                        onOpen = { }, // do not open for now
                        onCopy = {
                            clipboard.setText(
                                AnnotatedString(p.colors.joinToString(", ") { argbToHex(it.argb) })
                            )
                        },
                        onDelete = { PaletteService.delete(p.id) }
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    detailPick?.let { picked ->
        ColorDetailsBottomSheet(
            picked = picked,
            onDismiss = { detailPick = null },
            onOpenColorDetail = { s -> detailPick = s }, // tap similar colors to jump
            skipPartiallyExpanded = true
        )
    }
}

@Composable
private fun ColorSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit = {}
) {
    val prompts = remember {
        listOf(
            "Curious about 'Midnight blue'?",
            "Try 'Slate gray'",
            "Reveal the color behind '#007BA7'",
            "Ever seen 'Forest green'?",
            "What color hides in '#191970'?",
            "Search for 'Burnt sienna'",
            "Decode the mood of '#708090'",
            "What does 'Cerulean' look like?",
            "What does '#228B22' feel like?",
            "Uncover the shade '#E97451'"
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
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { onSubmit() },
                onDone = { onSubmit() } // in case keyboard shows Done
            ),
            placeholder = {
                AnimatedContent(
                    targetState = prompts[idx],
                    transitionSpec = {
                        ((slideInVertically { it / 6 } + fadeIn(tween(160)))
                                togetherWith (slideOutVertically { -it / 6 } + fadeOut(tween(160)))
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
                    Text(
                        palette.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                IconButton(onClick = onCopy) {
                    Icon(
                        Icons.Outlined.ContentCopy,
                        contentDescription = "Copy"
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete"
                    )
                }
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
        }
    }
}

