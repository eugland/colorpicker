package com.primortex.color.service

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.primortex.color.app.PickedColor

/**
 * Unified entry point for color name lookups across local indexes, dynamic buckets, and remote API.
 */
class ColorNameService(
    private val apiService: ColorApiService = ColorApiService(),
    private val lookup: ColorNameLookup = ColorNameLookup,
    private val nameIndex: ColorNameIndex = ColorNameIndex,
    private val bucketService: ColorBucketService? = null
) {

    enum class Strategy {
        /** Prefer dynamic bucket -> built-in lookup */
        DynamicThenLocal,
        /** Only use built-in lookup */
        LocalOnly,
        /** Built-in lookup first, then remote API */
        LocalThenRemote,
        /** Skip local lookup and use remote API only */
        RemoteOnly
    }

    suspend fun nameFromHex(hex: String, strategy: Strategy = Strategy.DynamicThenLocal): String {
        val argb = hexToArgb(hex)
        return nameFromArgb(argb, strategy)
    }

    suspend fun nameFromArgb(argb: Int, strategy: Strategy = Strategy.DynamicThenLocal): String {
        val normalizedArgb = argb or (0xFF shl 24)

        if (strategy != Strategy.RemoteOnly) {
            bucketService?.lookupName(normalizedArgb)?.let { return it }
            val local = lookup.nearestName(normalizedArgb).name
            if (strategy == Strategy.LocalOnly || strategy == Strategy.DynamicThenLocal) return local
            // LocalThenRemote falls through to remote if available
        }

        return apiService.getColorName(normalizedArgb)
    }

    fun localNameFromArgb(argb: Int): String {
        val normalizedArgb = argb or (0xFF shl 24)
        bucketService?.lookupName(normalizedArgb)?.let { return it }
        return lookup.nearestName(normalizedArgb).name
    }

    suspend fun nameFromColor(color: Color, strategy: Strategy = Strategy.DynamicThenLocal): String {
        return nameFromArgb(color.toArgb(), strategy)
    }

    fun hexFromName(name: String): String? {
        bucketService?.lookupArgbByName(name)?.let { return argbToHex(it) }
        val indexed = nameIndex.search(name, limit = 1).firstOrNull { it.name.equals(name, ignoreCase = true) }
        return indexed?.let { argbToHex(it.argb) }
    }

    fun search(query: String, limit: Int = 10): List<PickedColor> {
        val bucketResults = bucketService?.search(query, limit).orEmpty()
        if (bucketResults.size >= limit) return bucketResults.take(limit)

        val remaining = limit - bucketResults.size
        val indexedResults = nameIndex.search(query, remaining).map { PickedColor(it.argb, it.name) }

        return (bucketResults + indexedResults)
            .distinctBy { it.argb }
            .take(limit)
    }

    private fun hexToArgb(hex: String): Int {
        val clean = hex.trim().removePrefix("#")
        require(clean.matches(Regex("^[0-9A-Fa-f]{6}$"))) { "Invalid hex: $hex" }
        return (0xFF shl 24) or clean.toInt(16)
    }

    private fun argbToHex(argb: Int): String {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = (argb) and 0xFF
        return "#%02X%02X%02X".format(r, g, b)
    }
}
