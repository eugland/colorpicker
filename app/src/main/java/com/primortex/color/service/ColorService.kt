package com.primortex.color.service

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.primortex.color.app.PickedColor
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Unified color name pipeline backed by bundled assets with optional remote refresh + cache.
 */
class ColorService(
    context: Context,
    private val client: HttpClient = ApiService.defaultClient(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    private val appContext = context.applicationContext
    private val cache = appContext.getSharedPreferences("color_service_cache", Context.MODE_PRIVATE)

    private val bundledColors: List<ColorSeed> = loadBundledColors()

    @Volatile
    private var dataset: ColorDataset = ColorDataset.from(bundledColors)

    suspend fun refreshIfStale(languageTag: String?) {
        val normalizedTag = normalizeLanguageTag(languageTag)
        val cached = readCache(normalizedTag)
        if (cached != null && !isStale(cached.fetchedAt)) {
            updateDataset(cached.colors)
            return
        }

        val remote = fetchRemote(normalizedTag)
        if (remote != null) {
            saveCache(normalizedTag, remote)
            updateDataset(remote.colors)
            return
        }

        updateDataset(bundledColors)
    }

    fun nameFromHex(hex: String): String = nameFromArgb(hexToArgb(hex))

    fun nameFromArgb(argb: Int): String = dataset.nearestName(argb).name

    fun nameFromColor(color: Color): String = nameFromArgb(color.toArgb())

    fun localNameFromArgb(argb: Int): String = nameFromArgb(argb)

    fun hexFromName(name: String): String? = dataset.hexFromName(name)

    fun search(query: String, limit: Int = 10): List<PickedColor> = dataset.search(query, limit)

    fun allColors(): List<PickedColor> = dataset.allColors()

    private fun updateDataset(colors: List<ColorSeed>) {
        dataset = ColorDataset.from(colors)
        Log.d(
            "ColorService",
            "Color dataset updated: count=${colors.size}, " +
                    "names=${colors.take(10).joinToString { it.name }}"
        )
    }

    private suspend fun fetchRemote(languageTag: String): RemotePayload? {
        val url = "$BASE_URL/$languageTag.json"
        Log.d("ColorService", "Fetching colors from $url")

        return runCatching {
            val responseText: String = client.get(url).body()
            Log.d(
                "ColorService",
                "Remote response (first 100 chars):\n${responseText.take(100)}"
            )
            parsePayload(responseText)
        }
            .onFailure { Log.w("ColorService", "Remote color fetch failed", it) }
            .getOrNull()
            ?.takeIf { it.colors.isNotEmpty() }
    }

    private fun parsePayload(raw: String): RemotePayload? {
        return decodeRemotePayload(raw)
            ?: runCatching { json.decodeFromString(ListSerializer(ColorSeed.serializer()), raw) }
                .getOrNull()
                ?.let { RemotePayload(colors = it) }
    }

    private fun decodeRemotePayload(raw: String): RemotePayload? =
        runCatching { json.decodeFromString(RemotePayload.serializer(), raw) }.getOrNull()

    private fun saveCache(languageTag: String, payload: RemotePayload) {
        val cached = CachedPayload(
            version = payload.version,
            colors = payload.colors,
            fetchedAt = System.currentTimeMillis()
        )
        cache.edit().putString(cacheKey(languageTag), json.encodeToString(cached)).apply()
    }

    private fun readCache(languageTag: String): CachedPayload? {
        val cached = cache.getString(cacheKey(languageTag), null) ?: return null
        return runCatching { json.decodeFromString(CachedPayload.serializer(), cached) }
            .getOrNull()
            ?.takeIf { it.colors.isNotEmpty() }
    }

    private fun cacheKey(languageTag: String): String = "colors_$languageTag"

    private fun isStale(fetchedAt: Long): Boolean {
        val age = System.currentTimeMillis() - fetchedAt
        return age >= DEFAULT_TTL_MILLIS
    }

    private fun loadBundledColors(): List<ColorSeed> {
        return runCatching {
            appContext.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        }
            .mapCatching { raw ->
                parsePayload(raw)?.colors ?: json.decodeFromString(ListSerializer(ColorSeed.serializer()), raw)
            }
            .onFailure { Log.e("ColorService", "Failed to read bundled colors", it) }
            .getOrDefault(emptyList())
    }

    private fun normalizeLanguageTag(languageTag: String?): String {
        val cleaned = languageTag
            ?.trim()
            ?.replace('_', '-')
            ?.lowercase()
            ?.ifBlank { null }

        return cleaned?.substringBefore("-") ?: "en"
    }

    companion object {
        private const val ASSET_NAME = "colors.json"
        private const val BASE_URL = "https://eugland.github.io/color-picker-pages/colors"
        private const val DEFAULT_TTL_MILLIS = 7L * 24 * 60 * 60 * 1000
    }
}

@Serializable
private data class ColorSeed(
    val name: String = "",
    val hex: String = ""
)

@Serializable
private data class RemotePayload(
    val version: Int? = null,
    val colors: List<ColorSeed> = emptyList()
)

@Serializable
private data class CachedPayload(
    val version: Int? = null,
    val colors: List<ColorSeed> = emptyList(),
    val fetchedAt: Long = 0L
)

private data class ColorDataset(
    val entries: List<ColorRecord>,
    val lookup: Map<String, ColorRecord>
) {
    fun nearestName(argb: Int): ColorRecord {
        val normalized = normalizeArgb(argb)
        val targetLab = argbToLab(normalized)
        return entries.minByOrNull { deltaE76(targetLab, it.lab) } ?: ColorRecord(
            name = "",
            normalizedName = "",
            hex = "#000000",
            argb = normalized,
            lab = argbToLab(normalized)
        )
    }

    fun hexFromName(name: String): String? = lookup[normalizeName(name)]?.hex

    fun search(query: String, limit: Int): List<PickedColor> {
        val normalized = normalizeName(query)
        if (normalized.isBlank()) return emptyList()

        val startsWith = entries.filter { it.normalizedName.startsWith(normalized) }
        val contains = entries.filter {
            !it.normalizedName.startsWith(normalized) && it.normalizedName.contains(normalized)
        }

        return (startsWith + contains)
            .distinctBy { it.argb }
            .take(limit)
            .map { PickedColor(it.argb, it.name) }
    }

    fun allColors(): List<PickedColor> = entries.map { PickedColor(it.argb, it.name) }

    companion object {
        fun from(colors: List<ColorSeed>): ColorDataset {
            val records = buildList {
                colors.forEach { seed ->
                    val normalizedName = normalizeName(seed.name)
                    val argb = runCatching { hexToArgb(seed.hex) }.getOrNull() ?: return@forEach
                    if (normalizedName.isBlank()) return@forEach
                    val normalizedArgb = normalizeArgb(argb)
                    add(
                        ColorRecord(
                            name = seed.name.trim().ifBlank { normalizedName },
                            normalizedName = normalizedName,
                            hex = argbToHex(normalizedArgb),
                            argb = normalizedArgb,
                            lab = argbToLab(normalizedArgb)
                        )
                    )
                }
            }

            val lookup = records.associateBy { it.normalizedName }
            return ColorDataset(records, lookup)
        }

    }
}

private data class ColorRecord(
    val name: String,
    val normalizedName: String,
    val hex: String,
    val argb: Int,
    val lab: Lab
)
