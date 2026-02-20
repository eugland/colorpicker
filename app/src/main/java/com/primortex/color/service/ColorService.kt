package com.primortex.color.service

import android.util.LruCache
import com.primortex.color.app.PickedColor
import kotlinx.serialization.Serializable

@Serializable
data class ColorSeed(
    val name: String = "",
    val hex: String = ""
)

class ColorService(
    colors: List<ColorSeed>
) {
    @Volatile
    private var snapshot: ColorSnapshot = buildSnapshot(colors)
    private val nearestCache = LruCache<Int, ColorRecord>(512)

    fun setColors(colors: List<ColorSeed>) {
        snapshot = buildSnapshot(colors)
        nearestCache.evictAll()
    }

    fun nameFromArgb(argb: Int): String = nearestRecord(argb).name

    fun localNameFromArgb(argb: Int): String = nameFromArgb(argb)

    fun hexFromName(name: String): String? = snapshot.lookup[normalizeName(name)]?.hex

    fun search(query: String, limit: Int = 10): List<PickedColor> {
        val normalized = normalizeName(query)
        if (normalized.isBlank()) return emptyList()

        val startsWith = snapshot.entries.filter { it.normalizedName.startsWith(normalized) }
        val contains = snapshot.entries.filter {
            !it.normalizedName.startsWith(normalized) && it.normalizedName.contains(normalized)
        }

        return (startsWith + contains)
            .distinctBy { it.argb }
            .take(limit)
            .map { PickedColor(it.argb, it.name) }
    }

    fun allColors(): List<PickedColor> = snapshot.entries.map { PickedColor(it.argb, it.name) }

    private fun nearestRecord(argb: Int): ColorRecord {
        val normalized = normalizeArgb(argb)
        nearestCache[normalized]?.let { return it }
        val targetLab = argbToLab(normalized)
        val result = snapshot.entries.minByOrNull { deltaE76(targetLab, it.lab) } ?: ColorRecord(
            name = "",
            normalizedName = "",
            hex = "#000000",
            argb = normalized,
            lab = targetLab
        )
        nearestCache.put(normalized, result)
        return result
    }

    private fun buildSnapshot(colors: List<ColorSeed>): ColorSnapshot {
        val seen = mutableSetOf<String>()
        val records = buildList {
            colors.forEach { seed ->
                val normalizedName = normalizeName(seed.name)
                val argb = runCatching { hexToArgb(seed.hex) }.getOrNull() ?: return@forEach
                if (normalizedName.isBlank()) return@forEach
                val normalizedArgb = normalizeArgb(argb)
                val key = "$normalizedName|$normalizedArgb"
                if (!seen.add(key)) return@forEach
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

        return ColorSnapshot(
            entries = records,
            lookup = records.associateBy { it.normalizedName }
        )
    }
}

private data class ColorSnapshot(
    val entries: List<ColorRecord>,
    val lookup: Map<String, ColorRecord>
)

private data class ColorRecord(
    val name: String,
    val normalizedName: String,
    val hex: String,
    val argb: Int,
    val lab: Lab
)

