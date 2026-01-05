package com.primortex.color.info

import android.content.Context
import android.util.Log
import com.primortex.color.screens.InfoDetailSection
import com.primortex.color.service.ColorApiService
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class InfoContentService(
    context: Context,
    private val client: HttpClient = ColorApiService.defaultClient(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    private val appContext = context.applicationContext
    private val cache = appContext.getSharedPreferences("info_content_cache", Context.MODE_PRIVATE)

    suspend fun getSections(
        page: InfoPage,
        languageTag: String?,
        fallback: List<InfoDetailSection>
    ): List<InfoDetailSection> {
        var current = fallback
        loadSections(page, languageTag, fallback) { sections -> current = sections }
        return current
    }

    suspend fun loadSections(
        page: InfoPage,
        languageTag: String?,
        fallback: List<InfoDetailSection>,
        onUpdate: (List<InfoDetailSection>) -> Unit
    ) {
        val normalizedTag = normalizeLanguageTag(languageTag)
        val cached = readCache(page, normalizedTag)
        onUpdate(cached?.sections ?: fallback)

        val remote = fetchRemote(page, normalizedTag) ?: return

        if (cached?.version == remote.version && cached.sections == remote.sections) return

        saveCache(page, normalizedTag, remote)
        onUpdate(remote.sections)
    }

    private suspend fun fetchRemote(
        page: InfoPage,
        languageTag: String
    ): CachedContent? {
        val url = "$BASE_URL/${page.path}/$languageTag.json"
        Log.d("InfoContentService", "Fetching from $url")

        return runCatching {
            val response: RemoteInfoContent = client.get(url).body()
            Log.d("InfoContentService", "Fetching from $response")
            response.toSections()
            val sections = response.toSections()
            if (sections.isEmpty()) null else CachedContent(response.version, sections)
        }.getOrNull()
    }

    private fun saveCache(page: InfoPage, languageTag: String, content: CachedContent) {
        val payload = RemoteInfoContent(
            version = content.version,
            sections = content.sections.map {
                RemoteInfoSection(
                    heading = it.heading,
                    paragraphs = it.paragraphs,
                    bullets = it.bullets
                )
            }
        )
        cache.edit().putString(cacheKey(page, languageTag), json.encodeToString(payload)).apply()
    }

    private fun readCache(page: InfoPage, languageTag: String): CachedContent? {
        val cached = cache.getString(cacheKey(page, languageTag), null) ?: return null
        return runCatching {
            val content = json.decodeFromString(RemoteInfoContent.serializer(), cached)
            val sections = content.toSections()
            if (sections.isEmpty()) null else CachedContent(content.version, sections)
        }.getOrNull()

    }

    private fun cacheKey(page: InfoPage, languageTag: String): String = "${page.path}_$languageTag"

    private fun normalizeLanguageTag(languageTag: String?): String {
        val cleaned = languageTag
            ?.trim()
            ?.replace('_', '-')
            ?.lowercase()
            ?.ifBlank { null }

        return cleaned?.substringBefore("-") ?: "en"
    }

    companion object {
        private const val BASE_URL = "https://eugland.github.io/color-picker-pages"
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

private data class CachedContent(val version: Int, val sections: List<InfoDetailSection>)

private fun RemoteInfoContent.toSections(): List<InfoDetailSection> = sections
    .filter { it.heading.isNotBlank() }
    .map { section ->
        InfoDetailSection(
            heading = section.heading,
            paragraphs = section.paragraphs,
            bullets = section.bullets
        )
    }
