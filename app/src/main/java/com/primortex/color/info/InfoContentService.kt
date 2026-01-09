package com.primortex.color.info

import android.content.Context
import android.util.Log
import com.primortex.color.screens.InfoDetailSection
import com.primortex.color.service.ApiService
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

interface InfoContentCache {
    fun read(key: String): String?
    fun write(key: String, payload: String)
}

interface Clock {
    fun nowMillis(): Long
}

object SystemClock : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}

class InfoContentService(
    private val cache: InfoContentCache,
    private val client: HttpClient = ApiService.defaultClient(),
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val clock: Clock = SystemClock
) {
    constructor(
        context: Context,
        client: HttpClient = ApiService.defaultClient(),
        json: Json = Json { ignoreUnknownKeys = true },
        clock: Clock = SystemClock
    ) : this(
        cache = SharedPreferencesInfoContentCache(
            context.applicationContext.getSharedPreferences("info_content_cache", Context.MODE_PRIVATE)
        ),
        client = client,
        json = json,
        clock = clock
    )

    suspend fun loadSections(
        page: InfoPage,
        languageTag: String?,
        fallback: List<InfoDetailSection>,
        onUpdate: (List<InfoDetailSection>) -> Unit
    ) {
        val normalizedTag = normalizeLanguageTag(languageTag)
        val cached = readCache(page, normalizedTag)
        onUpdate(cached?.sections ?: fallback)

        val shouldRefresh = cached == null || isStale(cached)
        if (!shouldRefresh) return

        val remote = fetchRemote(page, normalizedTag) ?: return

        val hasChanged = cached?.version != remote.version || cached.sections != remote.sections
        saveCache(page, normalizedTag, remote)

        if (hasChanged) {
            onUpdate(remote.sections)
        }
    }

    private suspend fun fetchRemote(
        page: InfoPage,
        languageTag: String
    ): RemoteContent? {
        val url = "$BASE_URL/${page.path}/$languageTag.json"
        Log.d("InfoContentService", "Fetching from $url")

        return runCatching {
            val response: RemoteInfoContent = client.get(url).body()

            Log.d("InfoContentService", "Fetching from $response")
            val sections = response.sections.toSections()
            if (sections.isEmpty()) null else RemoteContent(response.version, sections)
        }.getOrNull()
    }

    private fun saveCache(page: InfoPage, languageTag: String, content: RemoteContent) {
        val payload = CachedPayload(
            version = content.version,
            sections = content.sections.map {
                RemoteInfoSection(
                    heading = it.heading,
                    paragraphs = it.paragraphs,
                    bullets = it.bullets
                )
            },
            fetchedAt = clock.nowMillis()
        )

        cache.write(cacheKey(page, languageTag), json.encodeToString(payload))
    }

    private fun readCache(page: InfoPage, languageTag: String): CachedContent? {
        val cached = cache.read(cacheKey(page, languageTag)) ?: return null
        return runCatching {
            val content = json.decodeFromString(CachedPayload.serializer(), cached)
            val sections = content.sections.toSections()
            if (sections.isEmpty()) null else CachedContent(
                content.version,
                sections,
                content.fetchedAt
            )
        }.getOrNull()

    }

    private fun isStale(cachedContent: CachedContent): Boolean {
        val age = clock.nowMillis() - cachedContent.fetchedAt
        return age >= DEFAULT_TTL_MILLIS
    }

    private fun cacheKey(page: InfoPage, languageTag: String): String = "${page.path}_$languageTag"

    internal fun normalizeLanguageTag(languageTag: String?): String {
        val cleaned = languageTag
            ?.trim()
            ?.replace('_', '-')
            ?.lowercase()
            ?.ifBlank { null }

        return cleaned?.substringBefore("-") ?: "en"
    }

    companion object {
        private const val BASE_URL = "https://eugland.github.io/color-picker-pages"
        private const val DEFAULT_TTL_MILLIS = 7L * 24 * 60 * 60 * 1000
    }
}

private class SharedPreferencesInfoContentCache(
    private val cache: android.content.SharedPreferences
) : InfoContentCache {
    override fun read(key: String): String? = cache.getString(key, null)

    override fun write(key: String, payload: String) {
        cache.edit().putString(key, payload).apply()
    }
}

enum class InfoPage(val path: String) {
    COPYRIGHT("copyright"),
    PRIVACY("privacy"),
    USAGE("usage")
}

@Serializable
private data class RemoteInfoContent(
    val version: Int = 0,
    val sections: List<RemoteInfoSection> = emptyList()
)

@Serializable
private data class RemoteInfoSection(
    val heading: String = "",
    val paragraphs: List<String> = emptyList(),
    val bullets: List<String> = emptyList()
)

@Serializable
private data class CachedPayload(
    val version: Int = 0,
    val sections: List<RemoteInfoSection> = emptyList(),
    val fetchedAt: Long = 0L
)

private data class RemoteContent(val version: Int, val sections: List<InfoDetailSection>)

private data class CachedContent(
    val version: Int,
    val sections: List<InfoDetailSection>,
    val fetchedAt: Long
)

private fun List<RemoteInfoSection>.toSections(): List<InfoDetailSection> =
    filter { it.heading.isNotBlank() }
        .map { section ->
            InfoDetailSection(
                heading = section.heading,
                paragraphs = section.paragraphs,
                bullets = section.bullets
            )
        }
