package com.primortex.color.service

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random
import android.graphics.Color
class ColorNameLookupTest {
    private fun argb(a: Int, r: Int, g: Int, b: Int): Int {
        return ((a and 0xFF) shl 24) or
                ((r and 0xFF) shl 16) or
                ((g and 0xFF) shl 8)  or
                (b and 0xFF)
    }
    @Test
    fun nearestName_randomColors_printResults() {
        repeat(100) { i ->
            val argb = argb(
                255,
                Random.nextInt(256),
                Random.nextInt(256),
                Random.nextInt(256)
            )

            val hex = String.format("#%06X", 0xFFFFFF and argb)

            val res = ColorNameLookup.nearestName(argb)
            val label = ColorNameLookup.prettyLabel(res)

            println(
                "%3d | %-8s | %-20s | ΔE=%.2f | idx=%d"
                    .format(i + 1, hex, res.name, res.deltaE, res.index)
            )

            // sanity checks
            assertNotNull(res.name)
            assertTrue(res.name.isNotBlank())
            assertTrue(res.deltaE >= 0f)
            assertTrue(label.contains(res.name, ignoreCase = true))
        }
    }
}
