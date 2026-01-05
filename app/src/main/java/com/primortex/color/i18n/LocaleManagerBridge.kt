package com.primortex.color.i18n

import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat

/**
 * Shim for reading/writing the platform per-app language without relying on
 * LocaleManagerCompat APIs that may not be available in older core versions.
 */
object LocaleManagerBridge {
    fun getApplicationLocales(context: Context): LocaleListCompat {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager =
                ContextCompat.getSystemService(context, android.app.LocaleManager::class.java)
            val locales = localeManager?.applicationLocales
            if (locales != null) return LocaleListCompat.wrap(locales)
        }

        return LocaleListCompat.getEmptyLocaleList()
    }

    fun setApplicationLocales(context: Context, locales: LocaleListCompat) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager =
                ContextCompat.getSystemService(context, android.app.LocaleManager::class.java)
            localeManager?.applicationLocales = locales.unwrap() as LocaleList
        }
    }
}
