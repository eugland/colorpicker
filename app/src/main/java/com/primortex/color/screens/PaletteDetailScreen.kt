package com.primortex.color.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.primortex.color.R
import com.primortex.color.app.Palette
import com.primortex.color.app.PickedColor
import com.primortex.color.service.PaletteService
import com.primortex.color.ui.components.ScreenScaffold
import com.primortex.color.ui.util.argbToHex
import com.primortex.color.ui.util.argbToHslString
import com.primortex.color.ui.util.argbToRgbString
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PaletteDetailScreen(
    innerPadding: PaddingValues,
    paletteId: String,
    startInEditMode: Boolean,
    onBack: () -> Unit,
    onOpenColorDetail: (PickedColor) -> Unit,
) {
    val snackbarHostState = rememberSnackbarHostState()
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val palettes by PaletteService.palettes.collectAsState()
    val palette = palettes.firstOrNull { it.id == paletteId }

    var isEditing by rememberSaveable(paletteId) { mutableStateOf(startInEditMode) }
    var name by rememberSaveable(paletteId) { mutableStateOf(palette?.name.orEmpty()) }
    var tagsInput by rememberSaveable(paletteId) { mutableStateOf(palette?.tags?.joinToString(", ").orEmpty()) }
    var note by rememberSaveable(paletteId) { mutableStateOf(palette?.note.orEmpty()) }
    val editableColors = remember(paletteId) { mutableStateListOf<PickedColor>() }

    LaunchedEffect(palette?.id) {
        palette?.let {
            name = it.name
            tagsInput = it.tags.joinToString(", ")
            note = it.note
            editableColors.apply {
                clear()
                addAll(it.colors)
            }
        }
    }

    val reorderState = rememberReorderableLazyListState(onMove = { from, to ->
        editableColors.move(from.index, to.index)
    })

    ScreenScaffold(
        titleRes = R.string.palette_details,
        innerPadding = innerPadding,
        onBack = onBack,
        snackbarHostState = snackbarHostState,
    ) {
        if (palette == null) {
            Text(text = stringResource(R.string.palette_missing))
            return@ScreenScaffold
        }

        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            PaletteHeader(
                isEditing = isEditing,
                name = name,
                tagsInput = tagsInput,
                note = note,
                onNameChange = { name = it },
                onTagsChange = { tagsInput = it },
                onNoteChange = { note = it },
                palette = palette,
                onToggleEdit = {
                    if (isEditing) {
                        val tags = tagsInput.split(',').mapNotNull { tag ->
                            tag.trim().takeIf { it.isNotBlank() }
                        }
                        PaletteService.update(
                            id = palette.id,
                            name = name.ifBlank { stringResource(R.string.palette) },
                            tags = tags,
                            note = note,
                            colors = editableColors.toList()
                        )
                        scope.launch { snackbarHostState.showSnackbar(stringResource(R.string.palette_updated)) }
                    }
                    isEditing = !isEditing
                },
                onCopyAll = {
                    clipboard.setText(AnnotatedString(editableColors.joinToString(", ") { argbToHex(it.argb) }))
                    scope.launch { snackbarHostState.showSnackbar(stringResource(R.string.copied_all_hex)) }
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
                    scope.launch { snackbarHostState.showSnackbar(stringResource(R.string.exported_css)) }
                },
                onDelete = {
                    PaletteService.delete(palette.id)
                    onBack()
                }
            )

            Text(
                text = stringResource(R.string.palette_color_count, editableColors.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .then(
                        if (isEditing) Modifier
                            .reorderable(reorderState)
                            .detectReorderAfterLongPress(reorderState)
                        else Modifier
                    ),
                state = reorderState.listState,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(editableColors.size, key = { editableColors[it].argb }) { index ->
                    val color = editableColors[index]
                    ReorderableItem(reorderState, key = color.argb) { isDragging ->
                        val dragHandleModifier = if (isEditing) Modifier.detectReorderAfterLongPress(reorderState) else Modifier
                        PaletteColorCard(
                            color = color,
                            isEditing = isEditing,
                            isDragging = isDragging,
                            onRemove = { editableColors.remove(color) },
                            onClick = { onOpenColorDetail(color) },
                            dragHandleModifier = dragHandleModifier
                        )
                    }
                }
            }
        }
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
    onToggleEdit: () -> Unit,
    onCopyAll: () -> Unit,
    onExportCss: () -> Unit,
    onDelete: () -> Unit,
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
            Text(name.ifBlank { stringResource(R.string.palette) }, style = MaterialTheme.typography.headlineSmall)
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
            onToggleEdit = onToggleEdit,
            onCopyAll = onCopyAll,
            onExportCss = onExportCss,
            onDelete = onDelete
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionRow(
    isEditing: Boolean,
    onToggleEdit: () -> Unit,
    onCopyAll: () -> Unit,
    onExportCss: () -> Unit,
    onDelete: () -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AssistChip(
            onClick = onToggleEdit,
            label = { Text(if (isEditing) stringResource(R.string.save_changes) else stringResource(R.string.edit_palette)) },
            leadingIcon = {
                Icon(
                    imageVector = if (isEditing) Icons.Outlined.Save else Icons.Outlined.Edit,
                    contentDescription = null
                )
            }
        )

        AssistChip(
            onClick = onCopyAll,
            label = { Text(stringResource(R.string.copy_all_hex)) },
            leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) }
        )

        AssistChip(
            onClick = onExportCss,
            label = { Text(stringResource(R.string.export_css)) },
            leadingIcon = { Icon(Icons.Outlined.Code, contentDescription = null) }
        )

        AssistChip(
            onClick = onDelete,
            label = { Text(stringResource(R.string.remove_from_favourites)) },
            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                labelColor = MaterialTheme.colorScheme.onErrorContainer,
                leadingIconContentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        )
    }
}

@Composable
private fun PaletteColorCard(
    color: PickedColor,
    isEditing: Boolean,
    isDragging: Boolean,
    onRemove: () -> Unit,
    onClick: () -> Unit,
    dragHandleModifier: Modifier,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                Text(color.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(argbToHex(color.argb), style = MaterialTheme.typography.bodyMedium)
                Text(argbToRgbString(color.argb), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(argbToHslString(color.argb), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.view_more_indicator),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (isEditing) {
                Column(horizontalAlignment = Alignment.End) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.delete))
                    }
                    IconButton(onClick = {}, modifier = dragHandleModifier) {
                        Icon(Icons.Outlined.DragHandle, contentDescription = stringResource(R.string.reorder))
                    }
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
