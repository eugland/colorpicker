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
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.primortex.color.R
import com.primortex.color.app.Palette
import com.primortex.color.app.PickedColor
import com.primortex.color.service.ColorService
import com.primortex.color.service.ColorServices
import com.primortex.color.service.PaletteService
import com.primortex.color.service.RecentPicksService
import com.primortex.color.service.argbToHex
import com.primortex.color.ui.components.ColorDetailsBottomSheet
import com.primortex.color.ui.components.ScreenScaffold
import com.primortex.color.ui.components.SwatchSection
import com.primortex.color.ui.components.WaterCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext


object ColorQueryResolver {

    fun search(nameService: ColorService, query: String, limit: Int = 10): List<PickedColor> {
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
fun PaletteScreen(
    innerPadding: PaddingValues,
    onOpenPalette: (Palette) -> Unit,
    onOpenLiveCameraPicker: () -> Unit,
    onOpenRecentColors: () -> Unit,
    onOpenSavedColors: () -> Unit,
    onOpenSavedPalettes: () -> Unit,
    onOpenColorDetail: (PickedColor) -> Unit
) {
    val ctx = LocalContext.current
    val colorNameService = remember(ctx) {
        ColorServices.ensure(ctx)
        ColorServices.colors
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
    var showClearPalettesDialog by remember { mutableStateOf(false) }
    val paletteThreshold = 3

    ScreenScaffold(
        R.string.palette,
        innerPadding
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
                        RecentPicksService.addPick(
                            PickedColor(argb = top.argb, name = top.name),
                            source = "palette_search"
                        )
                        searchQuery = ""
                    },
                    onClear = { searchQuery = "" },
                    onOpenLiveCameraPicker = onOpenLiveCameraPicker
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

                items(suggestions) { s ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                detailPick = s
                                RecentPicksService.addPick(
                                    PickedColor(
                                        argb = s.argb,
                                        name = s.name
                                    ),
                                    source = "palette_search"
                                )
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
                    threshold = 10,
                    showFooterToggle = false,
                    showEndSeeMore = true,
                    onEndSeeMore = onOpenRecentColors,
                    swatchSize = 52.dp,
                    swatchShape = CircleShape,
                    swatchLabelBelow = true,
                    actions = {
                        if (recents.isNotEmpty()) {
                            TextButton(onClick = onOpenRecentColors) {
                                Text(stringResource(R.string.see_more))
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
                    threshold = 6,
                    showFooterToggle = false,
                    showEndSeeMore = true,
                    onEndSeeMore = onOpenSavedColors,
                    actions = {
                        if (savedColors.isNotEmpty()) {
                            TextButton(onClick = onOpenSavedColors) {
                                Text(stringResource(R.string.see_more))
                            }
                        }
                    }
                )
            }

            item { Spacer(Modifier.height(16.dp)) }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        stringResource(R.string.saved_palettes),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (savedPalettes.isNotEmpty()) {
                        TextButton(onClick = onOpenSavedPalettes) {
                            Text(stringResource(R.string.see_more))
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }

            if (savedPalettes.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_saved_palettes),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val hasMoreThanThreshold = savedPalettes.size > paletteThreshold
                val visiblePalettes = if (hasMoreThanThreshold) {
                    savedPalettes.take(paletteThreshold)
                } else {
                    savedPalettes
                }
                items(visiblePalettes) { p ->
                    PaletteCard(
                        palette = p,
                        onOpen = { onOpenPalette(p) }
                    )
                    Spacer(Modifier.height(10.dp))
                }
                if (hasMoreThanThreshold) {
                    item { Spacer(Modifier.height(6.dp)) }
                    item {
                        FilledTonalButton(
                            onClick = onOpenSavedPalettes,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(stringResource(R.string.show_more))
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    detailPick?.let { picked ->
        ColorDetailsBottomSheet(
            picked = picked,
            onDismiss = { detailPick = null },
            onOpenColorDetail = onOpenColorDetail,
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
fun PaletteListScreen(
    innerPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenPalette: (Palette) -> Unit,
) {
    val savedPalettes by PaletteService.palettes.collectAsState()
    var query by remember { mutableStateOf("") }
    val filteredPalettes = remember(savedPalettes, query) {
        val lowered = query.trim().lowercase()
        if (lowered.isBlank()) {
            savedPalettes
        } else {
            savedPalettes.filter { palette ->
                palette.name.lowercase().contains(lowered) ||
                        palette.tags.any { it.lowercase().contains(lowered) } ||
                        palette.note.lowercase().contains(lowered)
            }
        }
    }

    ScreenScaffold(
        titleRes = R.string.saved_palettes,
        innerPadding = innerPadding,
        onBack = onBack
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.clear)
                        )
                    }
                }
            },
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        if (filteredPalettes.isEmpty()) {
            val emptyText = if (query.isBlank()) {
                stringResource(R.string.no_saved_palettes)
            } else {
                stringResource(R.string.no_matching_palettes)
            }
            Text(
                text = emptyText,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredPalettes) { palette ->
                    PaletteCard(
                        palette = palette,
                        onOpen = { onOpenPalette(palette) }
                    )
                }
            }
        }
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
    onSubmit: () -> Unit = {},
    onOpenLiveCameraPicker: () -> Unit
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
        WaterCard(
            icon = Icons.Filled.PhotoCamera,
            title = stringResource(R.string.pick_color_here),
            description = stringResource(R.string.pick_color_here_description),
            gradientColors = listOf(
                Color(0xFF1B6EF3),
                Color(0xFF2FD1C6),
                Color(0xFF5EC8FF),
                Color(0xFF3561E8)
            ),
            onClick = onOpenLiveCameraPicker
        )
        Spacer(Modifier.height(16.dp))

        Text(
            stringResource(R.string.quick_color_lookup),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(12.dp))

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
                items(palette.colors.take(10)) { p ->
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
