package com.primortex.color.i18n

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import java.util.concurrent.ConcurrentHashMap

object AppStrings {
    @Volatile
    private var appContext: Context? = null

    private val stringCache = ConcurrentHashMap<String, String>()

    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
    }

    fun clear() {
        stringCache.clear()
    }

    fun get(@StringRes id: Int, vararg formatArgs: Any): String {
        val baseContext = appContext
            ?: error("AppStrings is not initialized. Call AppStrings.init(context) in Application.onCreate().")
        val localeKey = resolveLocaleKey(baseContext)
        val localizedContext = if (localeKey.isBlank()) {
            baseContext
        } else {
            LocaleUtil.wrap(baseContext, localeKey)
        }

        if (formatArgs.isEmpty()) {
            val key = "$localeKey|$id"
            return stringCache.getOrPut(key) { localizedContext.getString(id) }
        }

        val key = buildString {
            append(localeKey)
            append('|')
            append(id)
            formatArgs.forEach {
                append('|')
                append(it.toString())
            }
        }
        return stringCache.getOrPut(key) { localizedContext.getString(id, *formatArgs) }
    }

    private fun resolveLocaleKey(context: Context): String {
        LanguageCache.get(context)?.let { return it }
        val applicationLocales = LocaleManagerBridge.getApplicationLocales(context)
        if (!applicationLocales.isEmpty) {
            return applicationLocales[0]?.toLanguageTag().orEmpty()
        }
        return context.resources.configuration.locales[0]?.toLanguageTag().orEmpty()
    }
}

@Composable
fun stringResource(@StringRes id: Int, vararg formatArgs: Any): String {
    return AppStrings.get(id, *formatArgs)
}

