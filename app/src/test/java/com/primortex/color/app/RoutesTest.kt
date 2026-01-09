package com.primortex.color.app

import java.net.URLDecoder
import kotlin.test.Test
import kotlin.test.assertEquals

class RoutesTest {
    @Test
    fun `detail route encodes and decodes name`() {
        val argb = 0xFFAABBCC.toInt()
        val route = Routes.Detail.to(argb, "Blue Violet")

        val query = route.substringAfter('?')
            .split('&')
            .associate { part ->
                val (key, value) = part.split('=')
                key to value
            }

        assertEquals("color/details", route.substringBefore('?'))
        assertEquals(argb.toString(), query["argb"])
        assertEquals(
            "Blue Violet",
            URLDecoder.decode(query["name"], "UTF-8")
        )
    }

    @Test
    fun `photo pick route encodes uri`() {
        val encoded = Routes.Camera.photoPickWith("content://media/picked image")
        val query = encoded.substringAfter('?')
            .split('&')
            .associate { part ->
                val (key, value) = part.split('=')
                key to value
            }

        assertEquals("cam/photoPick", encoded.substringBefore('?'))
        assertEquals(
            "content://media/picked image",
            URLDecoder.decode(query["uri"], "UTF-8")
        )
    }

    @Test
    fun `route constants stay consistent`() {
        assertEquals("tab/palette", Routes.Tab.PALETTE)
        assertEquals("tab/camera", Routes.Tab.CAMERA)
        assertEquals("tab/explore", Routes.Tab.EXPLORE)
        assertEquals("cam/live", Routes.Camera.LIVE)
        assertEquals("cam/photoPick?uri={uri}", Routes.Camera.PHOTO_PICK_ROUTE)
        assertEquals("color/details?argb={argb}&name={name}", Routes.Detail.COLOR_ROUTE)
        assertEquals("palette/details?id={id}&edit={edit}", Routes.Detail.PALETTE_ROUTE)
        assertEquals("info/copyright", Routes.Info.COPYRIGHT)
        assertEquals("info/privacy", Routes.Info.PRIVACY)
        assertEquals("info/usage", Routes.Info.USAGE)
        assertEquals("tool/slider", Routes.Tool.SLIDER)
        assertEquals("settings/language", Routes.Settings.LANGUAGE)
    }
}
