package com.primortex.color.info

import com.primortex.color.screens.InfoDetailSection
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class InfoContentServiceTest {
    @Test
    fun `normalizes language tags to primary`() {
        val service = InfoContentService(FakeCache())

        assertEquals("zh", service.normalizeLanguageTag("zh-CN"))
        assertEquals("zh", service.normalizeLanguageTag("zh_TW"))
        assertEquals("en", service.normalizeLanguageTag("  "))
        assertEquals("en", service.normalizeLanguageTag(null))
    }

    @Test
    fun `does not refresh when cache is fresh`() = runTest {
        val cache = FakeCache()
        val clock = FakeClock(nowMillis = 1_000L)
        val client = mockClient(responseJson(version = 1, heading = "Cached"))
        val service = InfoContentService(cache, client = client, clock = clock)
        val cachedSections = listOf(
            InfoDetailSection(
                heading = "Cached",
                paragraphs = listOf("Paragraph"),
                bullets = listOf("Bullet")
            )
        )

        service.loadSections(
            page = InfoPage.COPYRIGHT,
            languageTag = "en",
            fallback = emptyList()
        ) { }

        val freshClient = mockClient(responseJson(version = 2), onRequest = { error("unexpected") })
        val freshService = InfoContentService(cache, client = freshClient, clock = clock)

        clock.nowMillis = 1_000L + (6L * 24 * 60 * 60 * 1000)

        val updates = mutableListOf<List<InfoDetailSection>>()
        freshService.loadSections(
            page = InfoPage.COPYRIGHT,
            languageTag = "en",
            fallback = emptyList()
        ) { updated -> updates.add(updated) }

        assertEquals(listOf(cachedSections), updates)
    }

    @Test
    fun `refreshes when cache is stale and updates sections`() = runTest {
        val cache = FakeCache()
        val clock = FakeClock(nowMillis = 1_000L)
        val initialClient = mockClient(responseJson(version = 1, heading = "Old"))
        val service = InfoContentService(cache, client = initialClient, clock = clock)

        service.loadSections(
            page = InfoPage.PRIVACY,
            languageTag = "en",
            fallback = emptyList()
        ) { }

        clock.nowMillis = 1_000L + (8L * 24 * 60 * 60 * 1000)
        val refreshedClient = mockClient(responseJson(version = 2, heading = "New"))
        val refreshedService = InfoContentService(cache, client = refreshedClient, clock = clock)

        val updates = mutableListOf<List<InfoDetailSection>>()
        refreshedService.loadSections(
            page = InfoPage.PRIVACY,
            languageTag = "en",
            fallback = emptyList()
        ) { updated -> updates.add(updated) }

        assertEquals(2, updates.size)
        assertEquals("Old", updates.first().first().heading)
        assertEquals("New", updates.last().first().heading)
    }
}

private class FakeCache : InfoContentCache {
    private val storage = mutableMapOf<String, String>()

    override fun read(key: String): String? = storage[key]

    override fun write(key: String, payload: String) {
        storage[key] = payload
    }
}

private class FakeClock(var nowMillis: Long) : Clock {
    override fun nowMillis(): Long = nowMillis
}

private fun mockClient(
    payload: String,
    onRequest: () -> Unit = {}
): HttpClient {
    val engine = MockEngine {
        onRequest()
        respond(
            content = payload,
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
    }
    return HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
}

private fun responseJson(
    version: Int,
    heading: String = "Heading"
): String = """
    {
      "version": $version,
      "sections": [
        {
          "heading": "$heading",
          "paragraphs": ["Paragraph"],
          "bullets": ["Bullet"]
        }
      ]
    }
""".trimIndent()
