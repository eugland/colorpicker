package com.primortex.color.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.primortex.color.R
import com.primortex.color.app.Palette
import com.primortex.color.app.PickedColor
import com.primortex.color.data.enums.PaletteSchemeType
import com.primortex.color.i18n.AppStrings
import com.primortex.color.i18n.stringResource
import com.primortex.color.service.ColorDetails
import com.primortex.color.service.ColorDetailsService
import com.primortex.color.service.ColorService
import com.primortex.color.service.PaletteSelectionStore
import com.primortex.color.service.PaletteService
import com.primortex.color.service.RecentPicksService
import com.primortex.color.service.argbToHex
import com.primortex.color.ui.LocalSnackbarService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@Composable
fun ColorDetailsRoute(
    onBack: () -> Unit,
    onOpenPalette: (String, Boolean) -> Unit = { _, _ -> }
) {
    val viewModel: ColorDetailsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val snackbarService = LocalSnackbarService.current

    LaunchedEffect(viewModel, clipboard, snackbarService, onOpenPalette) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is ColorDetailsEffect.CopyHex -> clipboard.setText(AnnotatedString(effect.hex))
                is ColorDetailsEffect.ShowMessage -> snackbarService.showMessage(effect.message)
                is ColorDetailsEffect.OpenPalette -> onOpenPalette(effect.id, effect.edit)
            }
        }
    }

    ColorDetailsScreen(
        uiState = uiState,
        onBack = onBack,
        onAction = viewModel::onAction
    )

    if (uiState.showPalettePicker) {
        PalettePickerDialog(
            palettes = uiState.palettes,
            onDismiss = { viewModel.onAction(ColorDetailsUiAction.DismissPalettePicker) },
            onSelectPalette = { id -> viewModel.onAction(ColorDetailsUiAction.AddColorToPalette(id)) },
            onCreatePalette = { viewModel.onAction(ColorDetailsUiAction.CreatePaletteFromColor) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorDetailsScreen(
    uiState: ColorDetailsUiState,
    onBack: () -> Unit,
    onAction: (ColorDetailsUiAction) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        uiState.displayName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBackIosNew,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            ColorDetailsContent(uiState = uiState, onAction = onAction)
        }
    }
}

@HiltViewModel
class ColorDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val colorService: ColorService,
    private val colorDetailsService: ColorDetailsService,
    private val paletteService: PaletteService,
    private val recentPicksService: RecentPicksService,
    private val paletteSelectionStore: PaletteSelectionStore
) : ViewModel() {

    private val argb: Int = savedStateHandle.get<Int>("argb") ?: 0
    private val nameHint: String? = savedStateHandle.get<String>("name")
        ?.takeIf { it.isNotBlank() }
        ?.let { encoded ->
            runCatching { java.net.URLDecoder.decode(encoded, "UTF-8") }.getOrDefault(encoded)
        }

    private val details: ColorDetails = colorDetailsService.details(argb, similarLimit = 10)
    private val displayName = details.name.ifBlank {
        nameHint?.takeIf { it.isNotBlank() } ?: AppStrings.get(R.string.color_label)
    }
    private val pickedColor = PickedColor(details.argb, displayName)
    private val showPalettePicker = MutableStateFlow(false)

    private val _uiState = MutableStateFlow(
        buildUiState(
            palettes = paletteService.palettes.value,
            savedColors = recentPicksService.saved.value,
            pickerVisible = false
        )
    )
    val uiState: StateFlow<ColorDetailsUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<ColorDetailsEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<ColorDetailsEffect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(
                paletteService.palettes,
                recentPicksService.saved,
                showPalettePicker
            ) { palettes, savedColors, pickerVisible ->
                buildUiState(palettes, savedColors, pickerVisible)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onAction(action: ColorDetailsUiAction) {
        when (action) {
            ColorDetailsUiAction.CopyHexClicked -> {
                viewModelScope.launch {
                    _effects.emit(ColorDetailsEffect.CopyHex(details.hex))
                    _effects.emit(ColorDetailsEffect.ShowMessage(AppStrings.get(R.string.hex_copied)))
                }
            }

            ColorDetailsUiAction.ToggleSavedClicked -> {
                recentPicksService.toggleSaved(pickedColor)
                val message = if (_uiState.value.isSaved) {
                    AppStrings.get(R.string.removed_from_my_colors)
                } else {
                    AppStrings.get(R.string.saved_to_my_colors)
                }
                viewModelScope.launch {
                    _effects.emit(ColorDetailsEffect.ShowMessage(message))
                }
            }

            ColorDetailsUiAction.ShowPalettePicker -> showPalettePicker.value = true
            ColorDetailsUiAction.DismissPalettePicker -> showPalettePicker.value = false
            is ColorDetailsUiAction.AddColorToPalette -> addToPalette(action.paletteId)
            ColorDetailsUiAction.CreatePaletteFromColor -> createPaletteFromColor()
            ColorDetailsUiAction.OpenTintsPalette -> {
                openColorSetPalette(
                    suffix = AppStrings.get(R.string.tints_label),
                    colors = listOf(details.argb) + details.tints
                )
            }

            ColorDetailsUiAction.OpenShadesPalette -> {
                openColorSetPalette(
                    suffix = AppStrings.get(R.string.shades_label),
                    colors = listOf(details.argb) + details.shades
                )
            }

            ColorDetailsUiAction.OpenTonesPalette -> {
                openColorSetPalette(
                    suffix = AppStrings.get(R.string.tones_label),
                    colors = listOf(details.argb) + details.tones
                )
            }

            is ColorDetailsUiAction.OpenPaletteScheme -> {
                val scheme =
                    _uiState.value.paletteSchemes.firstOrNull { it.type == action.schemeType }
                if (scheme != null) {
                    openColorSetPalette(
                        suffix = paletteSchemeLabel(action.schemeType),
                        colors = scheme.colors
                    )
                }
            }
        }
    }

    private fun buildUiState(
        palettes: List<Palette>,
        savedColors: List<PickedColor>,
        pickerVisible: Boolean
    ): ColorDetailsUiState {
        val isSaved = savedColors.any { it.argb == pickedColor.argb }
        return ColorDetailsUiState(
            details = details,
            displayName = displayName,
            pickedColor = pickedColor,
            isSaved = isSaved,
            paletteSchemes = listOf(
                PaletteSchemeUiModel(
                    PaletteSchemeType.Complementary,
                    listOf(details.argb) + details.complements
                ),
                PaletteSchemeUiModel(
                    PaletteSchemeType.Analogous,
                    listOf(details.argb) + details.analogous
                ),
                PaletteSchemeUiModel(
                    PaletteSchemeType.SplitComplementary,
                    listOf(details.argb) + details.splitComplements
                ),
                PaletteSchemeUiModel(
                    PaletteSchemeType.Triadic,
                    listOf(details.argb) + details.triads
                ),
                PaletteSchemeUiModel(
                    PaletteSchemeType.Tetradic,
                    listOf(details.argb) + details.tetrads
                ),
                PaletteSchemeUiModel(
                    PaletteSchemeType.Square,
                    listOf(details.argb) + details.squares
                )
            ),
            palettes = palettes,
            showPalettePicker = pickerVisible
        )
    }

    private fun addToPalette(paletteId: String) {
        val palette = _uiState.value.palettes.firstOrNull { it.id == paletteId } ?: return
        val message = when {
            palette.colors.any { it.argb == pickedColor.argb } -> AppStrings.get(R.string.already_in_palette)
            palette.colors.size >= 10 -> AppStrings.get(R.string.palette_full)
            else -> {
                paletteService.update(id = palette.id, colors = palette.colors + pickedColor)
                showPalettePicker.value = false
                AppStrings.get(R.string.added_to_palette)
            }
        }
        viewModelScope.launch {
            _effects.emit(ColorDetailsEffect.ShowMessage(message))
        }
    }

    private fun createPaletteFromColor() {
        val newPaletteName = AppStrings.get(R.string.palette_name_with_color, pickedColor.name)
        val hasDuplicateName = _uiState.value.palettes.any { palette ->
            palette.name.equals(newPaletteName, ignoreCase = true)
        }

        val message = if (hasDuplicateName) {
            AppStrings.get(R.string.already_in_palette)
        } else {
            paletteService.create(
                name = newPaletteName,
                colors = listOf(pickedColor),
                tags = listOf("details"),
                creationSource = "color_details"
            )
            showPalettePicker.value = false
            AppStrings.get(R.string.palette_saved)
        }
        viewModelScope.launch {
            _effects.emit(ColorDetailsEffect.ShowMessage(message))
        }
    }

    private fun openColorSetPalette(suffix: String, colors: List<Int>) {
        val saved = paletteService.create(
            name = "$displayName $suffix",
            colors = colors.toPickedColors(colorService),
            tags = listOf("details"),
            saveOnCreate = false,
            creationSource = "color_details"
        )
        paletteSelectionStore.select(saved)
        viewModelScope.launch {
            _effects.emit(ColorDetailsEffect.OpenPalette(saved.id, false))
        }
    }

    private fun paletteSchemeLabel(type: PaletteSchemeType): String {
        return when (type) {
            PaletteSchemeType.Complementary -> AppStrings.get(R.string.palette_scheme_complementary)
            PaletteSchemeType.Analogous -> AppStrings.get(R.string.palette_scheme_analogous)
            PaletteSchemeType.SplitComplementary -> AppStrings.get(R.string.palette_scheme_split_complementary)
            PaletteSchemeType.Triadic -> AppStrings.get(R.string.palette_scheme_triadic)
            PaletteSchemeType.Tetradic -> AppStrings.get(R.string.palette_scheme_tetradic)
            PaletteSchemeType.Square -> AppStrings.get(R.string.palette_scheme_square)
        }
    }
}

data class PaletteSchemeUiModel(
    val type: PaletteSchemeType,
    val colors: List<Int>
)

data class ColorDetailsUiState(
    val details: ColorDetails,
    val displayName: String,
    val pickedColor: PickedColor,
    val isSaved: Boolean,
    val paletteSchemes: List<PaletteSchemeUiModel>,
    val palettes: List<Palette>,
    val showPalettePicker: Boolean = false
)

sealed interface ColorDetailsUiAction {
    data object CopyHexClicked : ColorDetailsUiAction
    data object ToggleSavedClicked : ColorDetailsUiAction
    data object ShowPalettePicker : ColorDetailsUiAction
    data object DismissPalettePicker : ColorDetailsUiAction
    data class AddColorToPalette(val paletteId: String) : ColorDetailsUiAction
    data object CreatePaletteFromColor : ColorDetailsUiAction
    data object OpenTintsPalette : ColorDetailsUiAction
    data object OpenShadesPalette : ColorDetailsUiAction
    data object OpenTonesPalette : ColorDetailsUiAction
    data class OpenPaletteScheme(val schemeType: PaletteSchemeType) : ColorDetailsUiAction
}

sealed interface ColorDetailsEffect {
    data class CopyHex(val hex: String) : ColorDetailsEffect
    data class ShowMessage(val message: String) : ColorDetailsEffect
    data class OpenPalette(val id: String, val edit: Boolean) : ColorDetailsEffect
}

private fun List<Int>.toPickedColors(colorService: ColorService): List<PickedColor> {
    return distinct().map { argb ->
        val name = colorService.localNameFromArgb(argb)
        PickedColor(argb, if (name.isBlank()) argbToHex(argb) else name)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorDetailsContent(
    uiState: ColorDetailsUiState,
    onAction: (ColorDetailsUiAction) -> Unit
) {
    ColorHeroCard(argb = uiState.details.argb)
    Spacer(Modifier.height(14.dp))

    ColorHeaderRow(
        displayName = uiState.displayName,
        hex = uiState.details.hex,
        argb = uiState.details.argb,
        onAction = onAction
    )
    Spacer(Modifier.height(12.dp))

    ColorActionsRow(isSaved = uiState.isSaved, onAction = onAction)
    Spacer(Modifier.height(14.dp))

    ColorInfoChips(uiState = uiState)
    Spacer(Modifier.height(12.dp))

    KeyValueGrid(items = metricsFor(uiState))
    Spacer(Modifier.height(14.dp))

    Text(stringResource(R.string.harmonies), style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    HarmonyRow(
        label = stringResource(R.string.harmony_complement),
        argbs = uiState.details.complements
    )
    Spacer(Modifier.height(8.dp))
    HarmonyRow(label = stringResource(R.string.harmony_triad), argbs = uiState.details.triads)
    Spacer(Modifier.height(8.dp))
    HarmonyRow(
        label = stringResource(R.string.harmony_analogous),
        argbs = uiState.details.analogous
    )

    Spacer(Modifier.height(14.dp))

    Text(
        stringResource(R.string.color_shades_tints_tones_title, uiState.displayName),
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(Modifier.height(10.dp))
    ShadeToneRow(
        label = stringResource(R.string.tints_label),
        argbs = uiState.details.tints,
        onOpenPalette = { onAction(ColorDetailsUiAction.OpenTintsPalette) }
    )
    Spacer(Modifier.height(8.dp))
    ShadeToneRow(
        label = stringResource(R.string.shades_label),
        argbs = uiState.details.shades,
        onOpenPalette = { onAction(ColorDetailsUiAction.OpenShadesPalette) }
    )
    Spacer(Modifier.height(8.dp))
    ShadeToneRow(
        label = stringResource(R.string.tones_label),
        argbs = uiState.details.tones,
        onOpenPalette = { onAction(ColorDetailsUiAction.OpenTonesPalette) }
    )

    Spacer(Modifier.height(16.dp))
    Text(
        stringResource(R.string.color_palettes_title, uiState.displayName),
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(Modifier.height(10.dp))
    uiState.paletteSchemes.forEach { scheme ->
        PaletteSchemeCard(
            scheme = scheme,
            onOpenPalette = { onAction(ColorDetailsUiAction.OpenPaletteScheme(scheme.type)) }
        )
        Spacer(Modifier.height(10.dp))
    }

    Text(stringResource(R.string.similar_colors), style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    SimilarColorsSection(similarColors = uiState.details.similarColors)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun metricsFor(uiState: ColorDetailsUiState): List<Pair<String, String>> {
    val details = uiState.details
    return listOf(
        stringResource(R.string.rgb_label) to "${details.rgb.r}, ${details.rgb.g}, ${details.rgb.b}",
        stringResource(R.string.hsv_label) to "${details.hsv.h.toInt()}\u00B0, ${(details.hsv.s * 100).toInt()}%, ${(details.hsv.v * 100).toInt()}%",
        stringResource(R.string.hsl_label) to "${details.hsl.h.toInt()}\u00B0, ${(details.hsl.s * 100).toInt()}%, ${(details.hsl.l * 100).toInt()}%"
    )
}

@Composable
private fun ColorHeroCard(argb: Int) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        val baseColor = Color(argb)
        val baseHsl = remember(argb) {
            FloatArray(3).apply { ColorUtils.colorToHSL(argb, this) }
        }
        val transition = rememberInfiniteTransition(label = "breathingTint")
        val lightnessShift by transition.animateFloat(
            initialValue = 0.06f,
            targetValue = 0.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 8000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "lightnessShift"
        )
        val tintColor = remember(baseHsl, lightnessShift) {
            Color(
                ColorUtils.HSLToColor(
                    floatArrayOf(
                        baseHsl[0],
                        baseHsl[1],
                        (baseHsl[2] + lightnessShift).coerceIn(0f, 1f)
                    )
                )
            )
        }
        val shadeColor = remember(baseHsl, lightnessShift) {
            Color(
                ColorUtils.HSLToColor(
                    floatArrayOf(
                        baseHsl[0],
                        baseHsl[1],
                        (baseHsl[2] - lightnessShift).coerceIn(0f, 1f)
                    )
                )
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(Brush.linearGradient(colors = listOf(tintColor, baseColor, shadeColor)))
        )
    }
}

@Composable
private fun ColorHeaderRow(
    displayName: String,
    hex: String,
    argb: Int,
    onAction: (ColorDetailsUiAction) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(64.dp)
                .background(Color(argb), CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                displayName,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                hex,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace
            )
        }
        IconButton(onClick = { onAction(ColorDetailsUiAction.CopyHexClicked) }) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = stringResource(R.string.copy_hex))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorActionsRow(
    isSaved: Boolean,
    onAction: (ColorDetailsUiAction) -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (isSaved) {
            Button(onClick = { onAction(ColorDetailsUiAction.ToggleSavedClicked) }) {
                Icon(Icons.Filled.Favorite, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.saved))
            }
        } else {
            OutlinedButton(onClick = { onAction(ColorDetailsUiAction.ToggleSavedClicked) }) {
                Icon(Icons.Outlined.FavoriteBorder, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.save))
            }
        }

        OutlinedButton(onClick = { onAction(ColorDetailsUiAction.ShowPalettePicker) }) {
            Text(stringResource(R.string.add_to_palette))
        }
    }
}

@Composable
private fun ColorInfoChips(uiState: ColorDetailsUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        AssistChip(
            onClick = {},
            label = {
                Text(
                    stringResource(
                        R.string.luma_label,
                        (uiState.details.luminance * 100).toInt()
                    )
                )
            }
        )
        AssistChip(
            onClick = {},
            label = {
                Text(
                    if (uiState.details.isDark) stringResource(R.string.dark_label) else stringResource(
                        R.string.light_label
                    )
                )
            }
        )
        AssistChip(
            onClick = {},
            label = {
                val textColor = if (uiState.details.recommendedOnColor == 0xFFFFFFFF.toInt()) {
                    stringResource(R.string.white_label)
                } else {
                    stringResource(R.string.black_label)
                }
                Text(stringResource(R.string.text_recommendation, textColor))
            }
        )
    }
}

@Composable
private fun SimilarColorsSection(similarColors: List<PickedColor>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        similarColors.forEach { s ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(84.dp)
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .background(Color(s.argb), CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = s.name,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = argbToHex(s.argb),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun HarmonyRow(label: String, argbs: List<Int>) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(90.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(argbs) { a ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .background(Color(a), CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        argbToHex(a),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun ShadeToneRow(
    label: String,
    argbs: List<Int>,
    onOpenPalette: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenPalette() }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.width(90.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(argbs) { a ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(28.dp)
                                .background(Color(a), CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            argbToHex(a),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteSchemeCard(
    scheme: PaletteSchemeUiModel,
    onOpenPalette: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenPalette() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(schemeTitle(scheme.type), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(scheme.colors) { a ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(34.dp)
                                .background(Color(a), CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            argbToHex(a),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun schemeTitle(type: PaletteSchemeType): String {
    return when (type) {
        PaletteSchemeType.Complementary -> stringResource(R.string.palette_scheme_complementary)
        PaletteSchemeType.Analogous -> stringResource(R.string.palette_scheme_analogous)
        PaletteSchemeType.SplitComplementary -> stringResource(R.string.palette_scheme_split_complementary)
        PaletteSchemeType.Triadic -> stringResource(R.string.palette_scheme_triadic)
        PaletteSchemeType.Tetradic -> stringResource(R.string.palette_scheme_tetradic)
        PaletteSchemeType.Square -> stringResource(R.string.palette_scheme_square)
    }
}

@Composable
private fun KeyValueGrid(items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (k, v) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        RoundedCornerShape(12.dp)
                    )
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
private fun PalettePickerDialog(
    palettes: List<Palette>,
    onDismiss: () -> Unit,
    onSelectPalette: (String) -> Unit,
    onCreatePalette: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_to_palette)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (palettes.isEmpty()) {
                    Text(stringResource(R.string.no_palettes_available))
                } else {
                    palettes.forEach { palette ->
                        TextButton(
                            onClick = { onSelectPalette(palette.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = palette.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = onCreatePalette,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.create_new_palette))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
