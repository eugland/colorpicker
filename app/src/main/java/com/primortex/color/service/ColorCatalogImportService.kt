package com.primortex.color.service

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.Locale

object ColorCatalogImportService {
    private val json = Json { ignoreUnknownKeys = true }

    fun loadLocaleSeeds(context: Context, languageTag: String? = null): List<ColorSeed> {
        val resourceContext = localizedContext(context, languageTag)
        val resId = resourceContext.resources.getIdentifier("colors", "raw", resourceContext.packageName)
        if (resId == 0) return emptyList()

        val raw = runCatching {
            resourceContext.resources.openRawResource(resId).bufferedReader().use { it.readText() }
        }.getOrElse { return emptyList() }

        return runCatching {
            json.decodeFromString(ListSerializer(ColorSeed.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    private fun localizedContext(context: Context, languageTag: String?): Context {
        val tag = languageTag?.trim()?.takeIf { it.isNotBlank() } ?: return context
        val locale = Locale.forLanguageTag(tag)
        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.setLocale(locale)
        }
        return context.createConfigurationContext(config)
    }
}
