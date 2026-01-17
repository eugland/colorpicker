package com.primortex.color.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.primortex.color.R
import com.primortex.color.app.Palette
import com.primortex.color.app.PickedColor
import com.primortex.color.service.PaletteService
import com.primortex.color.service.argbToHex
import com.primortex.color.service.rgbDistSq
import com.primortex.color.ui.LocalSnackbarService
import com.primortex.color.ui.components.ScreenScaffold

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PaletteDetailScreen(
    innerPadding: PaddingValues,
    paletteId: String,
    startInEditMode: Boolean,
    onBack: () -> Unit,
    onOpenColorDetail: (PickedColor) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val snackbarService = LocalSnackbarService.current

    val paletteLabel = stringResource(R.string.palette)
    val paletteUpdatedMessage = stringResource(R.string.palette_updated)
    val copiedAllHexMessage = stringResource(R.string.copied_all_hex)
    val exportedCssMessage = stringResource(R.string.exported_css)

    val palettes by PaletteService.palettes.collectAsState()
    val previewPalettes by PaletteService.previewPalettes.collectAsState()
    val palette = palettes.firstOrNull { it.id == paletteId }
        ?: previewPalettes.firstOrNull { it.id == paletteId }
    val isSaved = palettes.any { it.id == paletteId }
    var localPalette by rememberSaveable(paletteId) { mutableStateOf<Palette?>(null) }
    LaunchedEffect(palette?.id) {
        palette?.let { localPalette = it }
    }
    val paletteSnapshot = localPalette ?: palette

    var isEditing by rememberSaveable(paletteId) { mutableStateOf(startInEditMode) }
    var name by rememberSaveable(paletteId) { mutableStateOf(paletteSnapshot?.name.orEmpty()) }
    var tagsInput by rememberSaveable(paletteId) {
        mutableStateOf(
            paletteSnapshot?.tags?.joinToString(", ").orEmpty()
        )
    }
    var note by rememberSaveable(paletteId) { mutableStateOf(paletteSnapshot?.note.orEmpty()) }
    val editableColors = remember(paletteId) { mutableStateListOf<PickedColor>() }

    LaunchedEffect(paletteSnapshot?.id) {
        paletteSnapshot?.let {
            name = it.name
            tagsInput = it.tags.joinToString(", ")
            note = it.note
            editableColors.apply {
                clear()
                addAll(it.colors)
            }
        }
    }

    val listState = rememberLazyListState()
    val colorItemStartIndex = 3
    val dragState = rememberDragReorderState(listState, colorItemStartIndex) { from, to ->
        editableColors.move(from, to)
    }

    val gradientColors =
        remember(editableColors.toList()) { smoothGradient(editableColors.toList()) }

    val resetEditingState = {
        paletteSnapshot?.let {
            name = it.name
            tagsInput = it.tags.joinToString(", ")
            note = it.note
            editableColors.apply {
                clear()
                addAll(it.colors)
            }
        }
    }

    ScreenScaffold(
        titleRes = R.string.palette_details,
        innerPadding = innerPadding,
        onBack = onBack
    ) {
        if (paletteSnapshot == null) {
            Text(text = stringResource(R.string.palette_missing))
            return@ScreenScaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PaletteGradientPreview(colors = gradientColors)
            }

            item {
                PaletteHeader(
                    isEditing = isEditing,
                    name = name,
                    tagsInput = tagsInput,
                    note = note,
                    onNameChange = { name = it },
                    onTagsChange = { tagsInput = it },
                    onNoteChange = { note = it },
                    palette = paletteSnapshot,
                    isFavorite = isSaved,
                    onToggleEdit = {
                        if (isEditing) {
                            val tags = tagsInput.split(',').mapNotNull { tag ->
                                tag.trim().takeIf { it.isNotBlank() }
                            }
                            PaletteService.update(
                                id = paletteSnapshot.id,
                                name = name.ifBlank { paletteLabel },
                                tags = tags,
                                note = note,
                                colors = editableColors.toList()
                            )
                            snackbarService.showMessage(paletteUpdatedMessage)
                        }
                        isEditing = !isEditing
                    },
                    onToggleFavorite = { PaletteService.toggleSaved(paletteSnapshot) },
                    onCopyAll = {
                        clipboard.setText(AnnotatedString(editableColors.joinToString(", ") {
                            argbToHex(
                                it.argb
                            )
                        }))
                        snackbarService.showMessage(copiedAllHexMessage)
                    },
                    onExportCss = {
                        val css = buildString {
                            appendLine(":root {")
                            editableColors.forEachIndexed { index, color ->
                                append("    --color-${index + 1}: ${argbToHex(color.argb)};")
                                append('\n')
                            }
                            append("}")
                        }
                        clipboard.setText(AnnotatedString(css))
                        snackbarService.showMessage(exportedCssMessage)
                    },
                    onDelete = {
                        PaletteService.delete(paletteSnapshot.id)
                        onBack()
                    },
                    onCancelEdit = {
                        resetEditingState()
                        isEditing = false
                    }
                )
            }

            item {
                Text(
                    text = stringResource(R.string.palette_color_count, editableColors.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(editableColors.size) { index ->
                val color = editableColors[index]
                val isDragging = dragState.draggedIndex == index
                val dragOffset = if (isDragging) dragState.dragOffset else 0f
                val containerModifier = Modifier
                    .offset { IntOffset(0, dragOffset.toInt()) }
                    .zIndex(if (isDragging) 1f else 0f)

                val cardContent: @Composable () -> Unit = {
                    PaletteColorCard(
                        color = color,
                        isEditing = isEditing,
                        isDragging = isDragging,
                        onClick = { onOpenColorDetail(color) },
                        onDelete = { editableColors.remove(color) },
                        onDrag = { dragState.onDrag(it) },
                        onDragStart = { dragState.startDrag(index) },
                        onDragCancel = { dragState.endDrag() },
                        onDragEnd = { dragState.endDrag() }
                    )
                }

                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            editableColors.remove(color)
                            true
                        } else {
                            false
                        }
                    }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    enableDismissFromEndToStart = true,
                    backgroundContent = {
                        val isDismissed =
                            dismissState.targetValue != SwipeToDismissBoxValue.Settled
                        val targetColor = if (isDismissed) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                        val iconTint = if (isDismissed) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(targetColor),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = iconTint,
                                modifier = Modifier.padding(end = 16.dp)
                            )
                        }
                    },
                    content = {
                        Box { cardContent() }
                    },
                    modifier = containerModifier
                )
            }
        }
    }
}

private fun smoothGradient(colors: List<PickedColor>): List<Color> {
    if (colors.isEmpty()) return emptyList()

    val remaining = colors.toMutableList()
    val ordered = mutableListOf<PickedColor>()

    val start = remaining.minByOrNull { candidate ->
        remaining.sumOf { rgbDistSq(candidate.argb, it.argb).toLong() }
    } ?: return emptyList()

    ordered.add(start)
    remaining.remove(start)

    var current = start
    while (remaining.isNotEmpty()) {
        val next = remaining.minByOrNull { rgbDistSq(current.argb, it.argb) } ?: break
        ordered.add(next)
        remaining.remove(next)
        current = next
    }

    return ordered.map { color -> Color(color.argb) }
}


@Composable
private fun PaletteGradientPreview(colors: List<Color>) {
    val resolvedColors = if (colors.isNotEmpty()) {
        colors
    } else {
        listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.surface
        )
    }

    val brush = if (resolvedColors.size == 1) {
        SolidColor(resolvedColors.first())
    } else {
        Brush.horizontalGradient(resolvedColors)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        tonalElevation = 4.dp,
        shape = MaterialTheme.shapes.large
    ) {
        Box(Modifier.background(brush))
    }
}

@Composable
private fun PaletteHeader(
    isEditing: Boolean,
    name: String,
    tagsInput: String,
    note: String,
    onNameChange: (String) -> Unit,
    onTagsChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    palette: Palette,
    isFavorite: Boolean,
    onToggleEdit: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCopyAll: () -> Unit,
    onExportCss: () -> Unit,
    onDelete: () -> Unit,
    onCancelEdit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (isEditing) {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.palette_name)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = tagsInput,
                onValueChange = onTagsChange,
                label = { Text(stringResource(R.string.palette_tags_hint)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                label = { Text(stringResource(R.string.palette_description_label)) },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                name.ifBlank { stringResource(R.string.palette) },
                style = MaterialTheme.typography.headlineSmall
            )
            if (palette.tags.isNotEmpty()) {
                Text(
                    text = palette.tags.joinToString(" • "),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (palette.note.isNotBlank()) {
                Text(
                    text = palette.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        ActionRow(
            isEditing = isEditing,
            isFavorite = isFavorite,
            onToggleEdit = onToggleEdit,
            onToggleFavorite = onToggleFavorite,
            onCopyAll = onCopyAll,
            onExportCss = onExportCss,
            onDelete = onDelete,
            onCancelEdit = onCancelEdit
        )
    }
}

@Composable
private fun ActionRow(
    isEditing: Boolean,
    isFavorite: Boolean,
    onToggleEdit: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCopyAll: () -> Unit,
    onExportCss: () -> Unit,
    onDelete: () -> Unit,
    onCancelEdit: () -> Unit,
) {
    var copyMenuExpanded by remember { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isEditing) {
            if (isFavorite) {
                Button(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.saved))
                }
            } else {
                OutlinedButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.save))
                }
            }
        }

        OutlinedButton(onClick = onToggleEdit) {
            Icon(
                imageVector = if (isEditing) Icons.Outlined.Favorite else Icons.Outlined.Edit,
                contentDescription = null
            )
            Spacer(Modifier.width(8.dp))
            Text(if (isEditing) stringResource(R.string.save_changes) else stringResource(R.string.edit_palette))
        }

        if (isEditing) {
            OutlinedButton(onClick = onCancelEdit) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.cancel))
            }
        }

        if (!isEditing) {
            Box {
                OutlinedButton(onClick = { copyMenuExpanded = true }) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.copy))
                }

                DropdownMenu(
                    expanded = copyMenuExpanded,
                    onDismissRequest = { copyMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.copy_all_hex)) },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.ContentCopy,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            copyMenuExpanded = false
                            onCopyAll()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.export_css)) },
                        leadingIcon = { Icon(Icons.Outlined.Code, contentDescription = null) },
                        onClick = {
                            copyMenuExpanded = false
                            onExportCss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteColorCard(
    color: PickedColor,
    isEditing: Boolean,
    isDragging: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragStart: () -> Unit,
    onDragCancel: () -> Unit,
    onDragEnd: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isEditing) { onClick() },
        tonalElevation = if (isDragging) 8.dp else 2.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(color.argb), shape = MaterialTheme.shapes.small)
            )

            Column(Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            color.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(argbToHex(color.argb), style = MaterialTheme.typography.bodyMedium)
                    }
                    if (!isEditing) {
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.view_more_indicator),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (isEditing) {
                val dragHandleModifier = Modifier.pointerInput(isEditing, color.argb) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { onDragStart() },
                        onDragCancel = { onDragCancel() },
                        onDragEnd = { onDragEnd() },
                        onDrag = { change, dragAmount ->
                            onDrag(dragAmount.y)
                            change.consume()
                        }
                    )
                }

                IconButton(onClick = onDelete, modifier = dragHandleModifier) {
                    Icon(
                        Icons.Outlined.DeleteForever,
                        contentDescription = stringResource(R.string.delete)
                    )
                }
            }
        }
    }
}

private fun <T> MutableList<T>.move(from: Int, to: Int) {
    if (from == to) return
    val item = removeAt(from)
    add(if (to > from) to - 1 else to, item)
}

@Composable
private fun rememberDragReorderState(
    listState: LazyListState,
    colorItemStartIndex: Int,
    onMove: (Int, Int) -> Unit
): DragReorderState {
    return remember(listState, onMove, colorItemStartIndex) {
        DragReorderState(listState, colorItemStartIndex, onMove)
    }
}

private class DragReorderState(
    val listState: LazyListState,
    private val colorItemStartIndex: Int,
    private val onMove: (Int, Int) -> Unit
) {
    var draggedIndex by mutableStateOf<Int?>(null)
        private set

    var dragOffset by mutableStateOf(0f)
        private set

    fun startDrag(index: Int) {
        draggedIndex = index
        dragOffset = 0f
    }

    fun onDrag(deltaY: Float) {
        if (draggedIndex == null) return
        dragOffset += deltaY
        reorderIfNeeded()
    }

    fun endDrag() {
        draggedIndex = null
        dragOffset = 0f
    }

    private fun reorderIfNeeded() {
        val currentIndex = draggedIndex ?: return
        val layoutInfo = listState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo
        val visibleColorItems = visibleItems.filter { it.index >= colorItemStartIndex }
        val draggedItem =
            visibleColorItems.firstOrNull { it.index - colorItemStartIndex == currentIndex }
                ?: return

        val draggedMiddle = draggedItem.offset + dragOffset + (draggedItem.size / 2f)
        val target = visibleColorItems.minByOrNull { item ->
            kotlin.math.abs(draggedMiddle - (item.offset + (item.size / 2f)))
        }?.index ?: return

        val targetIndex = target - colorItemStartIndex
        if (targetIndex != currentIndex) {
            onMove(currentIndex, targetIndex)
            draggedIndex = targetIndex
        }
    }
}
