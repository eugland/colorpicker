package com.primortex.color.i18n

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.languageCacheDataStore by preferencesDataStore(name = "language_cache")

object LanguageCache {
    private val KEY_LANGUAGE_TAG = stringPreferencesKey("language_tag")

    fun get(context: Context): String? = runBlocking {
        context.languageCacheDataStore.data.first()[KEY_LANGUAGE_TAG]
    }

    fun set(context: Context, tag: String?) {
        runBlocking {
            context.languageCacheDataStore.edit { prefs ->
                if (tag == null) {
                    prefs.remove(KEY_LANGUAGE_TAG)
                } else {
                    prefs[KEY_LANGUAGE_TAG] = tag
                }
            }
        }
    }
}

