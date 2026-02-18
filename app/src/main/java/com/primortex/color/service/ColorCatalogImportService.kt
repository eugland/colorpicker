package com.primortex.color.service

import android.content.Context
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

data class ColorAssetOption(
    val id: String,
    val fileName: String,
    val languageTag: String
)

object ColorCatalogImportService {
    private const val ASSET_DIR = "colors"
    private const val DEFAULT_ASSET_ID = "colors"
    private val json = Json { ignoreUnknownKeys = true }

    fun availableAssets(context: Context): List<ColorAssetOption> {
        val files = runCatching { context.assets.list(ASSET_DIR)?.toList().orEmpty() }
            .getOrDefault(emptyList())
            .filter { it.endsWith(".json", ignoreCase = true) }
            .sorted()

        return files.map { file ->
            val id = file.removeSuffix(".json")
            ColorAssetOption(
                id = id,
                fileName = file,
                languageTag = languageFromAssetId(id)
            )
        }
    }

    fun defaultSelection(available: List<ColorAssetOption>): Set<String> {
        if (available.isEmpty()) return emptySet()
        val preferred = available.firstOrNull { it.id == DEFAULT_ASSET_ID }?.id
        return setOf(preferred ?: available.first().id)
    }

    fun bootstrapDefault(context: Context): Set<String> {
        val available = availableAssets(context)
        return defaultSelection(available)
    }

    fun normalizeSelection(context: Context, selectedAssetIds: Set<String>): Set<String> {
        val options = availableAssets(context)
        val allowed = options.map { it.id }.toSet()
        val normalized = selectedAssetIds.intersect(allowed)
        return if (normalized.isNotEmpty()) normalized else defaultSelection(options)
    }

    fun loadSelectedSeeds(context: Context, selectedAssetIds: Set<String>): List<ColorSeed> {
        val options = availableAssets(context)
        val byId = options.associateBy { it.id }
        val normalizedSelection = normalizeSelection(context, selectedAssetIds)
        return normalizedSelection
            .mapNotNull { byId[it] }
            .flatMap { option -> loadAssetSeeds(context, option.fileName) }
    }

    private fun loadAssetSeeds(context: Context, fileName: String): List<ColorSeed> {
        val assetPath = "$ASSET_DIR/$fileName"
        val raw = runCatching {
            context.assets.open(assetPath).bufferedReader().use { it.readText() }
        }.getOrElse { return emptyList() }

        return runCatching {
            json.decodeFromString(ListSerializer(ColorSeed.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    private fun languageFromAssetId(assetId: String): String {
        if (!assetId.startsWith("colors-")) return "en"
        return assetId.removePrefix("colors-").lowercase().ifBlank { "en" }
    }
}
