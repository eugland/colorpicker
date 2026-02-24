package com.primortex.color.i18n

import android.content.Context

object LanguageCache {
    private const val PREFS_NAME = "language_cache"
    private const val KEY_LANGUAGE_TAG = "language_tag"

    fun get(context: Context): String? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE_TAG, null)
    }

    fun set(context: Context, tag: String?) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (tag == null) remove(KEY_LANGUAGE_TAG) else putString(KEY_LANGUAGE_TAG, tag)
        }.apply()
    }
}
