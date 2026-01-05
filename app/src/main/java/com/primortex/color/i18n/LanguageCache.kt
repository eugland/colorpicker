package com.primortex.color.i18n

import android.content.Context

object LanguageCache {
    private const val FILE = "language_cache"
    private const val KEY = "language_tag"

    fun get(context: Context): String? =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null)

    fun set(context: Context, tag: String?) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, tag)
            .apply()
    }
}
