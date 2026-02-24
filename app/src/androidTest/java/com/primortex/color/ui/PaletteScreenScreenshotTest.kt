package com.primortex.color.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.primortex.color.MainActivity
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PaletteScreenScreenshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun capturePaletteScreenToPng() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val targetContext = instrumentation.targetContext
        val arguments = InstrumentationRegistry.getArguments()
        val outputDir = arguments.getString("additionalTestOutputDir")
        val customFileName = arguments.getString("screenshotFileName")
        composeRule.waitForIdle()

        val scrollable = composeRule
            .onAllNodes(hasScrollAction(), useUnmergedTree = true)
            .onFirst()
        repeat(4) {
            scrollable.performTouchInput { swipeUp() }
            composeRule.waitForIdle()
        }

        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val outFile = saveBitmap(
            baseDir = outputDir?.let(::File) ?: targetContext.cacheDir,
            bitmap = bitmap,
            prefix = "palette_screen",
            customFileName = customFileName
        )

        assertTrue("Screenshot file should exist", outFile.exists())
        assertTrue("Screenshot file should not be empty", outFile.length() > 0L)
    }

    private fun saveBitmap(
        baseDir: File,
        bitmap: Bitmap,
        prefix: String,
        customFileName: String?
    ): File {
        val screenshotsDir = File(baseDir, "screenshots").apply { mkdirs() }
        val outFile = File(
            screenshotsDir,
            customFileName
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let(::sanitizeFileName)
                ?.let(::ensurePngExtension)
                ?: "${prefix}_${System.currentTimeMillis()}.png"
        )
        FileOutputStream(outFile).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
        }
        return outFile
    }

    private fun sanitizeFileName(value: String): String {
        return value.replace(Regex("[^A-Za-z0-9._-]"), "_")
    }

    private fun ensurePngExtension(value: String): String {
        return if (value.endsWith(".png", ignoreCase = true)) value else "$value.png"
    }
}
