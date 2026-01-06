package com.primortex.color.service

import android.content.Context
import com.primortex.color.app.PickedColor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Maintains a cached, versioned bucket of color names that can be refreshed from JSON payloads.
 *
 * Expected payload:
 * {"version":0,"color":[{"name":"Red","hex":"#FF0000"}]}
 */
class ColorBucketService(
    context: Context,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    private val appContext = context.applicationContext
    private val cache = appContext.getSharedPreferences("color_bucket_cache", Context.MODE_PRIVATE)

    @Volatile
    private var bucket: ColorBucket? = readCache()

    fun currentBucket(): ColorBucket? = bucket ?: readCache().also { bucket = it }

    /**
     * Update the bucket from the provided payload. If the version matches the cached one, no-op.
     */
    fun updateFromJsonPayload(payload: String): ColorBucket? {
        val remote = runCatching { json.decodeFromString(ColorPayload.serializer(), payload) }.getOrNull()
            ?: return currentBucket()

        val existing = currentBucket()
        if (existing != null && existing.version == remote.version) return existing

        val built = buildBucket(remote)
        bucket = built
        saveCache(remote)
        return built
    }

    fun lookupName(argb: Int): String? = currentBucket()?.argbToName?.get(normalizeArgb(argb))

    fun lookupArgbByName(name: String): Int? = currentBucket()?.nameToArgb?.get(normalizeName(name))

    fun search(query: String, limit: Int): List<PickedColor> {
        val b = currentBucket() ?: return emptyList()
        val normalized = normalizeName(query)
        return b.nameToArgb
            .filterKeys { it.contains(normalized) }
            .entries
            .take(limit)
            .map { PickedColor(argb = it.value, name = b.argbToName[it.value] ?: it.key) }
    }

    private fun saveCache(payload: ColorPayload) {
        cache.edit()
            .putString(CACHE_KEY, json.encodeToString(ColorPayload.serializer(), payload))
            .apply()
    }

    private fun readCache(): ColorBucket? {
        val cached = cache.getString(CACHE_KEY, null) ?: return null
        val payload = runCatching { json.decodeFromString(ColorPayload.serializer(), cached) }.getOrNull()
            ?: return null
        return buildBucket(payload)
    }

    private fun buildBucket(payload: ColorPayload): ColorBucket {
        val nameToArgb = mutableMapOf<String, Int>()
        val argbToName = mutableMapOf<Int, String>()

        payload.colors.forEach { entry ->
            val argb = runCatching { hexToArgb(entry.hex) }.getOrNull() ?: return@forEach
            val normalizedName = normalizeName(entry.name)
            val cleanedName = entry.name.trim().ifBlank { normalizedName }
            if (normalizedName.isBlank()) return@forEach

            val normalizedArgb = normalizeArgb(argb)
            nameToArgb[normalizedName] = normalizedArgb
            argbToName[normalizedArgb] = cleanedName
        }

        return ColorBucket(
            version = payload.version,
            nameToArgb = nameToArgb,
            argbToName = argbToName
        )
    }

    private fun hexToArgb(hex: String): Int {
        val clean = hex.trim().removePrefix("#")
        require(clean.matches(Regex("^[0-9A-Fa-f]{6}$"))) { "Invalid hex: $hex" }
        return (0xFF shl 24) or clean.toInt(16)
    }

    private fun normalizeArgb(argb: Int): Int = argb or (0xFF shl 24)

    private fun normalizeName(name: String): String = name.trim().lowercase()

    companion object {
        private const val CACHE_KEY = "color_bucket_payload"
    }
}

data class ColorBucket(
    val version: Int,
    val nameToArgb: Map<String, Int>,
    val argbToName: Map<Int, String>
)

@Serializable
data class ColorPayload(
    val version: Int = 0,
    @SerialName("color") val colors: List<ColorEntry> = emptyList()
)

@Serializable
data class ColorEntry(
    val name: String = "",
    val hex: String = ""
)
