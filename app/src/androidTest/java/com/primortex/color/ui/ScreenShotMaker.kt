package com.primortex.color.ui

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.Color
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.primortex.color.MainActivity
import com.primortex.color.app.PickedColor
import com.primortex.color.features.photopick.PhotoPickScreen
import com.primortex.color.features.photopick.PhotoPickUiAction
import com.primortex.color.features.photopick.PhotoPickUiState
import com.primortex.color.service.ColorDetails
import com.primortex.color.service.Hsl
import com.primortex.color.service.Hsv
import com.primortex.color.service.Rgb
import com.primortex.color.ui.theme.ColorTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class ScreenShotMaker {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun captureCameraScreenToPng() {
        openTab("Camera")
        val outFile = captureCurrentScreen("camera_screen")
        assertFileLooksValid(outFile)
    }

    @Test
    fun captureLivePickingScreenToPng() {
        openTab("Camera")
        clickText("Start picking!")
        Thread.sleep(100)
        composeRule.waitForIdle()
        val outFile = captureCurrentScreen("colorpicking_livepicking")
        assertFileLooksValid(outFile)
    }


    @Test
    fun capturePhotoPickingScreenToPng() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val ctx = instrumentation.targetContext
        val photoFile = copyAssetToCacheFile(
            assets = instrumentation.context.assets,
            cacheDir = ctx.cacheDir,
            assetName = "pink_daisy.jpg"
        )
        val samplePick = PickedColor(argb = 0xFF4A90E2.toInt(), name = "Sample Blue")

        composeRule.activity.setContent {
            ColorTheme(darkTheme = false) {
                PhotoPickScreen(
                    uiState = PhotoPickUiState(
                        pickedColor = samplePick,
                        recents = listOf(samplePick),
                        palette = listOf(samplePick),
                        frozen = false,
                        detailPick = null
                    ),
                    photoUri = photoFile.toURI().toString(),
                    onBack = {},
                    onAction = { _: PhotoPickUiAction -> },
                    detailsFor = { _: Int -> emptyColorDetails() },
                    onOpenColorDetail = {}
                )
            }
        }
        composeRule.waitForIdle()
        Thread.sleep(100)
        composeRule.waitForIdle()
        val outFile = captureCurrentScreen("colorpicking_photopicking")
        assertFileLooksValid(outFile)
    }

    @Test
    fun captureExploreScreenToPng() {
        openTab("Explore")
        val outFile = captureCurrentScreen("explore_screen")
        assertFileLooksValid(outFile)
    }

    @Test
    fun capturePaletteScreenToPng() {
        openTab("Palette")
        val outFile = captureCurrentScreen("palette_screen")
        assertFileLooksValid(outFile)
    }

    @Test
    fun capturePaletteDetailsViewToPng() {
        openTab("Palette")
        scrollDown(3)
        if (!clickTextIfPresent("Show more")) {
            clickLastSeeMore()
        }
        clickTextIfPresent("Modern UI Neutrals")
        val outFile = captureCurrentScreen("palette_details_view")
        assertFileLooksValid(outFile)
    }

    @Test
    fun captureColorDetailsViewToPng() {
        openTab("Palette")
        assertFileLooksValid(captureCurrentScreen("palette_screen"))
        scrollDown(3)
        if (!clickTextIfPresent("Show more")) {
            clickLastSeeMore()
        }
        clickTextIfPresent("Modern UI Neutrals")
        if (!clickTextIfPresent("Blue Jeans")) {
            clickNthViewMore(4)
        } else if (!clickTextIfPresent("View more", substring = true)) {
            clickNthViewMore(4)
        }
        val outFile = captureCurrentScreen("color_details_view")
        assertFileLooksValid(outFile)
    }

    private fun openTab(tabName: String) {
        clickText(tabName)
        composeRule.waitForIdle()
    }

    private fun clickText(text: String, substring: Boolean = false) {
        composeRule.onAllNodesWithText(text, substring = substring, ignoreCase = true)
            .onFirst()
            .performClick()
    }

    private fun clickTextIfPresent(text: String, substring: Boolean = false): Boolean {
        val nodes = composeRule.onAllNodesWithText(text, substring = substring, ignoreCase = true)
            .fetchSemanticsNodes()
        if (nodes.isEmpty()) return false
        composeRule.onAllNodesWithText(text, substring = substring, ignoreCase = true)
            .onFirst()
            .performClick()
        composeRule.waitForIdle()
        return true
    }

    private fun clickLastSeeMore() {
        val nodes =
            composeRule.onAllNodesWithText("See more", ignoreCase = true).fetchSemanticsNodes()
        if (nodes.isNotEmpty()) {
            composeRule.onAllNodesWithText("See more", ignoreCase = true)[nodes.lastIndex]
                .performClick()
        }
        composeRule.waitForIdle()
    }

    private fun clickNthViewMore(position1Based: Int) {
        val index = (position1Based - 1).coerceAtLeast(0)
        val nodes = composeRule
            .onAllNodesWithText("View more", substring = true, ignoreCase = true)
            .fetchSemanticsNodes()
        if (nodes.isNotEmpty()) {
            val safeIndex = index.coerceAtMost(nodes.lastIndex)
            composeRule
                .onAllNodesWithText("View more", substring = true, ignoreCase = true)[safeIndex]
                .performClick()
            composeRule.waitForIdle()
        }
    }

    private fun scrollDown(times: Int) {
        val scrollable = composeRule
            .onAllNodes(hasScrollAction(), useUnmergedTree = true)
            .onFirst()
        repeat(times) {
            scrollable.performTouchInput { swipeUp() }
            composeRule.waitForIdle()
        }
    }

    private fun captureCurrentScreen(prefix: String): File {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val arguments = InstrumentationRegistry.getArguments()
        val outputDir = arguments.getString("additionalTestOutputDir")
        val customFileName = arguments.getString("screenshotFileName")
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        return saveBitmap(
            baseDir = outputDir?.let(::File) ?: targetContext.cacheDir,
            bitmap = bitmap,
            prefix = prefix,
            customFileName = customFileName
        )
    }

    private fun assertFileLooksValid(outFile: File) {
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

    private fun createSampleImageFile(baseDir: File): File {
        val bitmap = Bitmap.createBitmap(500, 900, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#2F80ED"))
        val outFile = File(baseDir, "screenshot_sample_photo.png")
        FileOutputStream(outFile).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
        }
        return outFile
    }

    private fun copyAssetToCacheFile(
        assets: AssetManager,
        cacheDir: File,
        assetName: String
    ): File {
        val outFile = File(cacheDir, assetName)
        assets.open(assetName).use { input ->
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
                output.flush()
            }
        }
        return outFile
    }

    private fun emptyColorDetails(): ColorDetails {
        return ColorDetails(
            argb = 0xFF000000.toInt(),
            name = "Sample",
            hex = "#000000",
            rgb = Rgb(0, 0, 0),
            hsv = Hsv(0f, 0f, 0f),
            hsl = Hsl(0f, 0f, 0f),
            similarColors = emptyList(),
            complements = emptyList(),
            splitComplements = emptyList(),
            tetrads = emptyList(),
            squares = emptyList(),
            triads = emptyList(),
            analogous = emptyList(),
            tints = emptyList(),
            shades = emptyList(),
            tones = emptyList(),
            luminance = 0f,
            isDark = true,
            recommendedOnColor = 0xFFFFFFFF.toInt()
        )
    }
}
