package com.primortex.color.service

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.Locale
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ColorCatalogImportServiceTest {

    private val service = ColorCatalogImportService()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val json = Json { ignoreUnknownKeys = true }

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
        val representativeTags = listOf("en", "fr", "ja", "zh", "zh-CN", "zh-TW")
        representativeTags.forEach { tag ->
            assertEquals(
                expectedCatalogSizeForTag(tag),
                service.loadLocaleSeeds(context, tag).size
            )
        }
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

    private fun expectedCatalogSizeForTag(languageTag: String): Int {
        val localizedContext = localizedContext(context, languageTag)
        val resId = localizedContext.resources.getIdentifier("colors", "raw", localizedContext.packageName)
        check(resId != 0) { "Missing raw/colors resource for locale tag: $languageTag" }
        val raw = localizedContext.resources.openRawResource(resId).bufferedReader().use { it.readText() }
        return json.decodeFromString(ListSerializer(ColorSeed.serializer()), raw).size
    }

    private fun localizedContext(base: Context, languageTag: String): Context {
        val locale = Locale.forLanguageTag(languageTag)
        val configuration = Configuration(base.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            configuration.setLocale(locale)
        }
        return base.createConfigurationContext(configuration)
    }
}
