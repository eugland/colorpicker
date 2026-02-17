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
        val context = appContext
            ?: error("AppStrings is not initialized. Call AppStrings.init(context) in Application.onCreate().")

        if (formatArgs.isEmpty()) {
            return stringCache.getOrPut(id.toString()) { context.getString(id) }
        }

        val key = buildString {
            append(id)
            formatArgs.forEach {
                append('|')
                append(it.toString())
            }
        }
        return stringCache.getOrPut(key) { context.getString(id, *formatArgs) }
    }

}

@Composable
fun stringResource(@StringRes id: Int, vararg formatArgs: Any): String {
    return AppStrings.get(id, *formatArgs)
}
