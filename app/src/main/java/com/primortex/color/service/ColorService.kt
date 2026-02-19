package com.primortex.color.service

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
    @Volatile private var dataset: ColorDataset = ColorDataset.from(colors)

    fun setColors(colors: List<ColorSeed>) {
        dataset = ColorDataset.from(colors)
    }

    fun nameFromArgb(argb: Int): String = dataset.nearestName(argb).name

    fun localNameFromArgb(argb: Int): String = nameFromArgb(argb)

    fun hexFromName(name: String): String? = dataset.hexFromName(name)

    fun search(query: String, limit: Int = 10): List<PickedColor> = dataset.search(query, limit)

    fun allColors(): List<PickedColor> = dataset.allColors()
}

private data class ColorDataset(
    val entries: List<ColorRecord>,
    val lookup: Map<String, ColorRecord>
) {

    private val nearestCache = android.util.LruCache<Int, ColorRecord>(512)
    fun nearestName(argb: Int): ColorRecord {
        val normalized = normalizeArgb(argb)
        nearestCache[normalized]?.let { return it }
        val targetLab = argbToLab(normalized)
        val result = entries.minByOrNull { deltaE76(targetLab, it.lab) } ?: ColorRecord(
            name = "",
            normalizedName = "",
            hex = "#000000",
            argb = normalized,
            lab = targetLab
        )
        nearestCache.put(normalized, result)
        return result
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
