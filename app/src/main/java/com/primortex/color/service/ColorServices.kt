package com.primortex.color.service

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application-level service locator to share the color pipeline.
 */
object ColorServices {
    private lateinit var colorService: ColorService
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(context: Context) {
        if (::colorService.isInitialized) return
        val appContext = context.applicationContext
        colorService = ColorService(appContext)

        val localeTag = appContext.resources.configuration.locales[0].toLanguageTag()
        scope.launch { colorService.refreshIfStale(localeTag) }
    }

    fun ensure(context: Context) {
        if (!::colorService.isInitialized) {
            init(context)
        }
    }

    val colors: ColorService
        get() {
            check(::colorService.isInitialized) { "ColorServices.init must be called first" }
            return colorService
        }
}
