package com.primortex.color.service

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ColorCatalogImportServiceTest {

    private val service = ColorCatalogImportService()
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun loadLocaleSeeds_defaultCatalog_isNotEmpty() {
        val seeds = service.loadLocaleSeeds(context)
        assertFalse(seeds.isEmpty())
    }

    @Test
    fun loadLocaleSeeds_knownLocales_areNotEmpty() {
        val locales = listOf("ja", "fr", "es", "it", "zh", "zh-CN", "zh-TW")
        locales.forEach { tag ->
            val seeds = service.loadLocaleSeeds(context, tag)
            assertTrue("Expected non-empty color seeds for locale tag: $tag", seeds.isNotEmpty())
        }
    }

    @Test
    fun loadLocaleSeeds_representativeCatalogSizes_matchCurrentContract() {
        assertEquals(1013, service.loadLocaleSeeds(context).size)
        assertEquals(1013, service.loadLocaleSeeds(context, "fr").size)
        assertEquals(143, service.loadLocaleSeeds(context, "ja").size)
        assertEquals(165, service.loadLocaleSeeds(context, "zh").size)
        assertEquals(165, service.loadLocaleSeeds(context, "zh-CN").size)
        assertEquals(165, service.loadLocaleSeeds(context, "zh-TW").size)
    }

    @Test
    fun loadLocaleSeeds_allConfiguredLocales_areUsable() {
        val allTags = listOf(
            "ar", "bn", "cs", "da", "de", "el", "es", "fi", "fil", "fr",
            "he", "hi", "hu", "id", "it", "ja", "ko", "ms", "nb", "nl",
            "pl", "pt", "ro", "ru", "sv", "th", "tr", "uk", "ur", "vi",
            "zh", "zh-CN", "zh-TW"
        )
        allTags.forEach { tag ->
            val seeds = service.loadLocaleSeeds(context, tag)
            assertTrue("Expected non-empty seeds for locale tag: $tag", seeds.isNotEmpty())
        }
    }

    @Test
    fun loadLocaleSeeds_unknownLocale_fallsBackToUsableCatalog() {
        val seeds = service.loadLocaleSeeds(context, "zz-ZZ")
        assertFalse(seeds.isEmpty())
    }
}
