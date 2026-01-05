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
        val normalizedTag = normalizeLanguageTag(languageTag)

        val remote = fetchRemote(page, normalizedTag)
        if (!remote.isNullOrEmpty()) {
            saveCache(page, normalizedTag, remote)
            return remote
        }
        Log.d("InfoContentService", "Remote fallback")

        val cached = readCache(page, normalizedTag)
        if (cached != null) return cached

        return fallback
    }

    private suspend fun fetchRemote(
        page: InfoPage,
        languageTag: String
    ): List<InfoDetailSection>? {
        val url = "$BASE_URL/${page.path}/$languageTag.json"
        Log.d("InfoContentService", "Fetching from $url")

        return runCatching {
            val response: RemoteInfoContent = client.get(url).body()
            Log.d("InfoContentService", "Fetching from $response")
            response.toSections()
        }.getOrNull()
    }

    private fun saveCache(page: InfoPage, languageTag: String, sections: List<InfoDetailSection>) {
        val payload = RemoteInfoContent(
            sections = sections.map {
                RemoteInfoSection(
                    heading = it.heading,
                    paragraphs = it.paragraphs,
                    bullets = it.bullets
                )
            }
        )

        cache.edit().putString(cacheKey(page, languageTag), json.encodeToString(payload)).apply()
    }

    private fun readCache(page: InfoPage, languageTag: String): List<InfoDetailSection>? {
        val cached = cache.getString(cacheKey(page, languageTag), null) ?: return null
        return runCatching {
            json.decodeFromString(RemoteInfoContent.serializer(), cached).toSections()
        }
            .getOrNull()
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
private data class RemoteInfoContent(val sections: List<RemoteInfoSection> = emptyList())

@Serializable
private data class RemoteInfoSection(
    val heading: String = "",
    val paragraphs: List<String> = emptyList(),
    val bullets: List<String> = emptyList()
)

private fun RemoteInfoContent.toSections(): List<InfoDetailSection> = sections
    .filter { it.heading.isNotBlank() }
    .map { section ->
        InfoDetailSection(
            heading = section.heading,
            paragraphs = section.paragraphs,
            bullets = section.bullets
        )
    }
