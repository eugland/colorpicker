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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.primortex.color.R
import com.primortex.color.app.Palette
import com.primortex.color.app.PickedColor
import com.primortex.color.service.ColorNameService
import com.primortex.color.service.ColorServices
import com.primortex.color.service.PaletteService
import com.primortex.color.service.RecentPicksService
import com.primortex.color.ui.components.ColorDetailsBottomSheet
import com.primortex.color.ui.components.ScreenScaffold
import com.primortex.color.ui.components.SwatchSection
import com.primortex.color.ui.util.argbToHex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


object ColorQueryResolver {

    fun search(nameService: ColorNameService, query: String, limit: Int = 10): List<PickedColor> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()

        // HEX -> nearest color lookup
        if (isHex(q)) {
            val rgb = q.removePrefix("#").toLong(16).toInt() and 0x00FFFFFF
            val argb = (0xFF shl 24) or rgb   // force alpha = 255
            val nearest = nameService.localNameFromArgb(argb)
            Log.d("ColorSearch", "HEX pressed, top=${nearest} #${argb.toString(16)}")
            return listOf(
                PickedColor(
                    argb = argb,
                    name = nearest,
                ),

            )
        }

        // NAME -> name index search
        return nameService.search(q, limit)
    }

    private fun isHex(input: String): Boolean {
        val s = input.removePrefix("#")
        return s.length in setOf(3, 6, 8) &&
                s.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaletteScreen(innerPadding: PaddingValues, onOpenPalette: (Palette)->Unit) {
    val clipboard = LocalClipboardManager.current
    val ctx = LocalContext.current
    val colorNameService = remember(ctx) {
        ColorServices.ensure(ctx)
        ColorServices.colorNames
    }

    val recents by RecentPicksService.history.collectAsState()
    val savedColors by RecentPicksService.saved.collectAsState()
    val savedPalettes by PaletteService.palettes.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<PickedColor>>(emptyList()) }
    LaunchedEffect(searchQuery) {
        val q = searchQuery
        delay(120) // debounce typing
        suggestions = withContext(Dispatchers.Default) {
            ColorQueryResolver.search(colorNameService, q, limit = 10)
        }
    }
    var detailPick by remember { mutableStateOf<PickedColor?>(null) }
    var showClearRecentsDialog by remember { mutableStateOf(false) }
    var showClearSavedDialog by remember { mutableStateOf(false) }
    var showClearPalettesDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ScreenScaffold(
        R.string.palette,
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
                        scope.launch {
                            snackbarHostState.showSnackbar(stringResource(R.string.added_to_recents))
                        }
                        searchQuery = ""
                    },
                    onClear = { searchQuery = "" }
                )
            }

            if (suggestions.isNotEmpty()) {
                item { Spacer(Modifier.height(10.dp)) }

                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.suggestions),
                            style = MaterialTheme.typography.titleSmall
                        )
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
                                scope.launch {
                                    snackbarHostState.showSnackbar(stringResource(R.string.added_to_recents))
                                }
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
            item {
                SwatchSection(
                    title = stringResource(R.string.recent_colors),
                    picks = recents,
                    emptyMessage = stringResource(R.string.no_recent_colors),
                    onSwatchClick = { pick -> detailPick = pick },
                    actions = {
                        if (recents.isNotEmpty()) {
                            TextButton(onClick = { showClearRecentsDialog = true }) {
                                Text(stringResource(R.string.clear))
                            }
                        }
                    }
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
            item {
                SwatchSection(
                    title = stringResource(R.string.saved_colors),
                    picks = savedColors,
                    emptyMessage = stringResource(R.string.no_saved_colors),
                    onSwatchClick = { pick -> detailPick = pick },
                    actions = {
                        if (savedColors.isNotEmpty()) {
                            TextButton(onClick = { showClearSavedDialog = true }) {
                                Text(stringResource(R.string.clear))
                            }
                        }
                    }
                )
            }

            item { Spacer(Modifier.height(16.dp)) }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.saved_palettes),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (savedPalettes.isNotEmpty()) {
                        TextButton(onClick = { showClearPalettesDialog = true }) {
                            Text(stringResource(R.string.clear))
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }

            if (savedPalettes.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_saved_palettes),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(savedPalettes, key = { it.id }) { p ->
                    PaletteCard(
                        palette = p,
                        onOpen = { onOpenPalette(p) }
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

    if (showClearRecentsDialog) {
        ConfirmClearDialog(
            title = stringResource(R.string.clear_recent_colors_title),
            description = stringResource(R.string.clear_recent_colors_description),
            onConfirm = {
                RecentPicksService.clear()
                showClearRecentsDialog = false
            },
            onDismiss = { showClearRecentsDialog = false }
        )
    }

    if (showClearSavedDialog) {
        ConfirmClearDialog(
            title = stringResource(R.string.clear_saved_colors_title),
            description = stringResource(R.string.clear_saved_colors_description),
            onConfirm = {
                RecentPicksService.clearSaved()
                showClearSavedDialog = false
            },
            onDismiss = { showClearSavedDialog = false }
        )
    }

    if (showClearPalettesDialog) {
        ConfirmClearDialog(
            title = stringResource(R.string.clear_saved_palettes_title),
            description = stringResource(R.string.clear_saved_palettes_description),
            onConfirm = {
                PaletteService.clear()
                showClearPalettesDialog = false
            },
            onDismiss = { showClearPalettesDialog = false }
        )
    }
}

@Composable
private fun ConfirmClearDialog(
    title: String,
    description: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(description) },
        confirmButton = {
            Button(onClick = onConfirm) { Text(stringResource(R.string.clear)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
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
            R.string.search_hint_one,
            R.string.search_hint_two,
            R.string.search_hint_three,
            R.string.search_hint_four,
            R.string.search_hint_five,
            R.string.search_hint_six,
            R.string.search_hint_seven,
            R.string.search_hint_eight,
            R.string.search_hint_nine,
            R.string.search_hint_ten
        )
    }.map { stringResource(it) }
    var focused by remember { mutableStateOf(false) }
    var idx by remember { mutableIntStateOf(0) }

    LaunchedEffect(focused, prompts) {
        if (focused || query.isNotBlank())
            idx = 0
        while (true) {
            delay(2200L)
            idx = (idx + 1) % prompts.size
        }
    }

    Column {
        Text(
            stringResource(R.string.quick_color_lookup),
            style = MaterialTheme.typography.titleMedium
        )
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
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.clear)
                        )
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
private fun PaletteCard(
    palette: Palette,
    onOpen: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Column(Modifier.padding(12.dp)) {
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

