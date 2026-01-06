package com.primortex.color.service

import android.content.Context

/**
 * Application-level service locator to share color name utilities and cached buckets.
 */
object ColorServices {
    private lateinit var bucketService: ColorBucketService
    private lateinit var nameService: ColorNameService

    fun init(context: Context) {
        if (::nameService.isInitialized) return
        bucketService = ColorBucketService(context.applicationContext)
        nameService = ColorNameService(bucketService = bucketService)
    }

    fun ensure(context: Context) {
        if (!::nameService.isInitialized) {
            init(context)
        }
    }

    val colorNames: ColorNameService
        get() {
            if (!::nameService.isInitialized) {
                nameService = ColorNameService()
            }
            return nameService
        }

    val colorBuckets: ColorBucketService
        get() {
            check(::bucketService.isInitialized) { "ColorServices.init must be called first" }
            return bucketService
        }
}
