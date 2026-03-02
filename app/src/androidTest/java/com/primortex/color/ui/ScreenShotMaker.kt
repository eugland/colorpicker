package com.primortex.color.ui

import android.Manifest
import android.content.Context
import android.content.res.AssetManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.swipeUp
import androidx.core.os.LocaleListCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.primortex.color.MainActivity
import com.primortex.color.app.PickedColor
import com.primortex.color.data.enums.AppLanguage
import com.primortex.color.features.photopick.PhotoPickScreen
import com.primortex.color.features.photopick.PhotoPickUiAction
import com.primortex.color.features.photopick.PhotoPickUiState
import com.primortex.color.i18n.AppStrings
import com.primortex.color.i18n.LanguageCache
import com.primortex.color.i18n.LocaleManagerBridge
import com.primortex.color.service.ColorCatalogImportService
import com.primortex.color.service.ColorDetails
import com.primortex.color.service.ColorService
import com.primortex.color.service.Hsl
import com.primortex.color.service.Hsv
import com.primortex.color.service.Rgb
import com.primortex.color.ui.theme.ColorTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import java.io.File
import java.io.FileOutputStream

abstract class ScreenShotMakerBase {
    companion object {
        private const val LOG_TAG = "ScreenShotMaker"
    }

    protected val localeEn = "en"
    protected val localeJa = "ja"
    protected val localeZh = "zh"

    protected val supportedAppLocales: List<String> = AppLanguage.entries
        .mapNotNull { language ->
            language.languageTag?.trim()?.takeIf { it.isNotEmpty() }
        }
        .distinct()

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()
    private var stageCounter: Int = 0
    private var activeLocaleTag: String = localeEn

    protected fun captureSuiteFor(localeTag: String) = runWithLocale(
        localeTag = localeTag,
        forceResetAndSeedOnStart = true,
        cleanupAppDataOnExit = true
    ) { captureSuite() }

    protected fun captureStageFor(
        localeTag: String,
        stageName: String,
        block: () -> Unit
    ) {
        runWithLocale(localeTag) {
            logStage(stageName)
            block()
        }
    }

    protected fun captureSuite() {
        logStage("camera_screen")
        captureCameraScreen()
        logStage("color_slider_screen")
        captureColorSliderScreen()
        logStage("color_blind_enhancer_screen")
        captureColorBlindEnhancerScreen()
        logStage("live_picking_screen")
        captureLivePickingScreen()
        logStage("photo_picking_screen")
        capturePhotoPickingScreen()
        logStage("explore_screen")
        captureExploreScreen()
        logStage("palette_screen")
        capturePaletteScreen()
        logStage("palette_details_view")
        capturePaletteDetailsView()
        logStage("color_details_view")
        captureColorDetailsView()
    }

    protected fun captureCameraScreen() {
        openTab(TestTags.TAB_CAMERA)
        val outFile = captureCurrentScreen("camera_screen", folder = activeLocaleTag)
        assertFileLooksValid(outFile)
    }

    protected fun captureColorSliderScreen() {
        openTab(TestTags.TAB_CAMERA)
        clickTag(TestTags.CAMERA_COLOR_SLIDER_CARD)
        composeRule.waitForIdle()
        val outFile = captureCurrentScreen("color_slider_screen", folder = activeLocaleTag)
        assertFileLooksValid(outFile)
    }

    protected fun captureColorBlindEnhancerScreen() {
        grantPermissionIfNeeded(Manifest.permission.CAMERA)
        openTab(TestTags.TAB_CAMERA)
        clickTag(TestTags.CAMERA_COLOR_BLIND_ENHANCER_CARD)
        composeRule.waitForIdle()
        Thread.sleep(450)
        // Default to thermal mode for screenshot capture.
        selectColorBlindMode("thermal")
        val thermalOut = captureCurrentScreen(
            prefix = "color_blind_enhancer_screen_thermal",
            folder = activeLocaleTag,
            includePlatformViews = true,
            avoidNearBlack = true
        )
        assertFileLooksValid(thermalOut)

        openColorBlindFilterGrid()
        val filterOpenOut = captureCurrentScreen(
            prefix = "color_blind_enhancer_screen_filter_open",
            folder = activeLocaleTag,
            includePlatformViews = true,
            avoidNearBlack = true
        )
        assertFileLooksValid(filterOpenOut)
        selectColorBlindMode("thermal")

        listOf(
            "xray",
            "intrinsic",
            "edge",
            "monochrome"
        ).forEach { mode ->
            selectColorBlindMode(mode)
            val outFile = captureCurrentScreen(
                prefix = "color_blind_enhancer_screen_$mode",
                folder = activeLocaleTag,
                includePlatformViews = true,
                avoidNearBlack = true
            )
            assertFileLooksValid(outFile)
        }
    }

    protected fun captureLivePickingScreen() {
        grantPermissionIfNeeded(Manifest.permission.CAMERA)
        openTab(TestTags.TAB_CAMERA)
        clickTag(TestTags.CAMERA_START_PICKING_CARD)
        composeRule.waitForIdle()
        Thread.sleep(450)
        val outFile = captureCurrentScreen(
            prefix = "colorpicking_livepicking",
            folder = activeLocaleTag,
            includePlatformViews = true,
            avoidNearBlack = true
        )
        assertFileLooksValid(outFile)
    }

    protected fun capturePhotoPickingScreen() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val ctx = instrumentation.targetContext
        val photoFile = copyAssetToCacheFile(
            assets = instrumentation.context.assets,
            cacheDir = ctx.cacheDir,
            assetName = "pink_daisy.jpg"
        )
        val testColorService = ColorService(
            ColorCatalogImportService().loadLocaleSeeds(ctx, activeLocaleTag)
        )
        val initialArgb = 0xFF7B8266.toInt()
        val initialPick = PickedColor(
            argb = initialArgb,
            name = testColorService.localNameFromArgb(initialArgb)
        )

        composeRule.activity.setContent {
            var uiState by remember {
                mutableStateOf(
                PhotoPickUiState(
                    pickedColor = initialPick,
                    recents = emptyList(),
                    palette = emptyList(),
                    frozen = false,
                    detailPick = null
                )
                )
            }
            ColorTheme(darkTheme = false) {
                PhotoPickScreen(
                    uiState = uiState,
                    photoUri = photoFile.toURI().toString(),
                    onBack = {},
                    onAction = { action: PhotoPickUiAction ->
                        when (action) {
                            is PhotoPickUiAction.SampleColor -> {
                                val tappedPick = PickedColor(
                                    argb = action.argb,
                                    name = testColorService.localNameFromArgb(action.argb)
                                )
                                uiState = uiState.copy(
                                    pickedColor = tappedPick,
                                    recents = listOf(tappedPick) + uiState.recents,
                                    palette = if (uiState.palette.any { it.argb == tappedPick.argb }) {
                                        uiState.palette
                                    } else {
                                        uiState.palette + tappedPick
                                    }
                                )
                            }

                            PhotoPickUiAction.ToggleFreeze -> uiState = uiState.copy(frozen = !uiState.frozen)
                            PhotoPickUiAction.ShowCurrentDetails -> uiState = uiState.copy(detailPick = uiState.pickedColor)
                            is PhotoPickUiAction.ShowDetails -> uiState = uiState.copy(detailPick = action.pick)
                            PhotoPickUiAction.DismissDetails -> uiState = uiState.copy(detailPick = null)
                            PhotoPickUiAction.AddCurrentToPalette -> {
                                if (uiState.palette.none { it.argb == uiState.pickedColor.argb }) {
                                    uiState = uiState.copy(palette = uiState.palette + uiState.pickedColor)
                                }
                            }

                            PhotoPickUiAction.SavePalette -> Unit
                        }
                    },
                    detailsFor = { _: Int -> emptyColorDetails() },
                    onOpenColorDetail = {}
                )
            }
        }
        composeRule.waitForIdle()
        Thread.sleep(250)
        composeRule.onRoot(useUnmergedTree = true).performTouchInput {
            click(center)
        }
        composeRule.waitForIdle()
        Thread.sleep(100)
        composeRule.waitForIdle()
        val outFile = captureCurrentScreen(
            prefix = "colorpicking_photopicking",
            folder = activeLocaleTag,
            includePlatformViews = true,
            avoidNearBlack = true
        )
        assertFileLooksValid(outFile)
        relaunchMainActivityAndWait()
    }

    protected fun captureExploreScreen() {
        openTab(TestTags.TAB_EXPLORE)
        val outFile = captureCurrentScreen("explore_screen", folder = activeLocaleTag)
        assertFileLooksValid(outFile)
    }

    protected fun capturePaletteScreen() {
        openTab(TestTags.TAB_PALETTE)
        val outFile = captureCurrentScreen("palette_screen", folder = activeLocaleTag)
        assertFileLooksValid(outFile)
    }

    protected fun capturePaletteDetailsView() {
        openTab(TestTags.TAB_PALETTE)
        openFirstSavedPalette()
        val outFile = captureCurrentScreen("palette_details_view", folder = activeLocaleTag)
        assertFileLooksValid(outFile)
    }

    protected fun captureColorDetailsView() {
        openTab(TestTags.TAB_PALETTE)
        openFirstSavedPalette()
        clickTag("${TestTags.PALETTE_DETAIL_COLOR_CARD_PREFIX}0")
        val outFile = captureCurrentScreen("color_details_view", folder = activeLocaleTag)
        assertFileLooksValid(outFile)
    }

    private fun openTab(tabTag: String) {
        ensureComposeHierarchy("openTab:$tabTag")
        ensureBottomTabsVisible()
        waitForTag(tabTag)
        clickTag(tabTag)
        composeRule.waitForIdle()
    }

    private fun clickTag(tag: String) {
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).performClick()
    }

    private fun openColorBlindFilterGrid() {
        waitForTag(TestTags.COLOR_BLIND_FILTER_SELECTOR)
        clickTag(TestTags.COLOR_BLIND_FILTER_SELECTOR)
        waitForTag(TestTags.COLOR_BLIND_MODE_GRID)
    }

    private fun selectColorBlindMode(modeKey: String) {
        openColorBlindFilterGrid()
        clickTag("${TestTags.COLOR_BLIND_MODE_ITEM_PREFIX}$modeKey")
        composeRule.waitForIdle()
        Thread.sleep(220)
    }

    private fun hasTag(tag: String): Boolean {
        return try {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        } catch (_: IllegalStateException) {
            false
        }
    }

    private fun waitForTag(tag: String, timeoutMs: Long = 6_000L) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (hasTag(tag)) return
            composeRule.waitForIdle()
            Thread.sleep(120)
        }
        throw AssertionError("Timed out waiting for tag: $tag")
    }

    private fun ensureBottomTabsVisible() {
        val rootTabTag = TestTags.TAB_PALETTE
        ensureComposeHierarchy("ensureBottomTabsVisible")
        if (hasTag(rootTabTag)) return

        repeat(8) {
            if (hasTag(rootTabTag)) return
            composeRule.activity.runOnUiThread {
                composeRule.activity.onBackPressedDispatcher.onBackPressed()
            }
            composeRule.waitForIdle()
            Thread.sleep(120)
        }

        relaunchMainActivityAndWait()
        waitForTag(rootTabTag)
    }

    protected fun runWithLocale(
        localeTag: String,
        forceResetAndSeedOnStart: Boolean = false,
        cleanupAppDataOnExit: Boolean = false,
        block: () -> Unit
    ) {
        activeLocaleTag = localeTag
        stageCounter = 0
        Log.i(LOG_TAG, "===== BEGIN locale=$localeTag folder=$localeTag =====")
        if (forceResetAndSeedOnStart) {
            clearTargetAppData()
        }
        setAppLocale(localeTag)
        if (forceResetAndSeedOnStart) {
            forceSeedNow()
        }
        beforeLocaleCapture(localeTag)
        try {
            block()
        } finally {
            afterLocaleCapture(localeTag)
            if (cleanupAppDataOnExit) {
                clearTargetAppData()
            }
            Log.i(LOG_TAG, "===== END locale=$localeTag folder=$localeTag =====")
        }
    }

    protected open fun beforeLocaleCapture(localeTag: String) {
        // Hook for language-specific setup.
        // Example: create a fresh palette after switching locale so color names are localized.
    }

    protected open fun afterLocaleCapture(localeTag: String) {
        // Hook for language-specific teardown/cleanup.
    }

    private fun setAppLocale(languageTag: String) {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val locales = LocaleListCompat.forLanguageTags(languageTag)
        LocaleManagerBridge.setApplicationLocales(targetContext, locales)
        LanguageCache.set(targetContext, languageTag)
        AppStrings.clear()
        relaunchMainActivityAndWait()
    }

    protected fun logStage(stageName: String) {
        stageCounter += 1
        Log.i(
            LOG_TAG,
            "locale=$activeLocaleTag stage=$stageCounter name=$stageName"
        )
    }

    private fun hasComposeHierarchy(): Boolean {
        return try {
            composeRule.onRoot(useUnmergedTree = true).fetchSemanticsNode()
            true
        } catch (_: IllegalStateException) {
            false
        }
    }

    private fun ensureComposeHierarchy(reason: String) {
        if (hasComposeHierarchy()) return
        Log.w(LOG_TAG, "No compose hierarchy for '$reason'. Relaunching activity.")
        relaunchMainActivityAndWait()
    }

    protected fun relaunchMainActivityAndWait() {
        composeRule.activity.runOnUiThread { composeRule.activity.recreate() }
        repeat(30) {
            composeRule.waitForIdle()
            if (hasComposeHierarchy()) return
            Thread.sleep(120)
        }
        throw IllegalStateException("Compose hierarchy did not recover after activity recreate.")
    }

    private fun openFirstSavedPalette() {
        val targetTag = "${TestTags.PALETTE_SAVED_CARD_PREFIX}0"
        repeat(8) {
            val matches = composeRule.onAllNodesWithTag(targetTag, useUnmergedTree = true)
                .fetchSemanticsNodes()
            if (matches.isNotEmpty()) {
                clickTag(targetTag)
                composeRule.waitForIdle()
                return
            }
            scrollDown(1)
        }
        throw AssertionError("Could not find saved palette card with tag: $targetTag")
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

    private fun captureCurrentScreen(
        prefix: String,
        folder: String = "default",
        includePlatformViews: Boolean = false,
        avoidNearBlack: Boolean = false
    ): File {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val arguments = InstrumentationRegistry.getArguments()
        val outputDir = arguments.getString("additionalTestOutputDir")
        val customFileName = arguments.getString("screenshotFileName")
        val bitmap = captureBitmapWithRetries(
            includePlatformViews = includePlatformViews,
            avoidNearBlack = avoidNearBlack
        )
        return saveBitmap(
            baseDir = outputDir?.let(::File) ?: targetContext.cacheDir,
            bitmap = bitmap,
            folder = folder,
            prefix = prefix,
            customFileName = customFileName
        )
    }

    private fun captureBitmapWithRetries(
        includePlatformViews: Boolean,
        avoidNearBlack: Boolean
    ): Bitmap {
        val maxAttempts = if (avoidNearBlack) 10 else 1
        var lastBitmap: Bitmap? = null
        repeat(maxAttempts) { attempt ->
            composeRule.waitForIdle()
            val bitmap = if (includePlatformViews) {
                captureDeviceScreenBitmap()
            } else {
                composeRule.onRoot().captureToImage().asAndroidBitmap()
            }
            lastBitmap = bitmap
            if (!avoidNearBlack || !isLikelyNearBlack(bitmap)) {
                return bitmap
            }
            Log.w(
                LOG_TAG,
                "Captured near-black frame; retry ${attempt + 1}/$maxAttempts"
            )
            Thread.sleep(180)
        }
        return lastBitmap ?: throw IllegalStateException("Failed to capture screenshot bitmap.")
    }

    private fun captureDeviceScreenBitmap(): Bitmap {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        composeRule.waitForIdle()
        Thread.sleep(120)
        composeRule.waitForIdle()
        return instrumentation.uiAutomation.takeScreenshot()
            ?: throw IllegalStateException("Failed to capture device screenshot.")
    }

    private fun isLikelyNearBlack(bitmap: Bitmap): Boolean {
        val sampleX = 14
        val sampleY = 24
        val stepX = (bitmap.width / sampleX).coerceAtLeast(1)
        val stepY = (bitmap.height / sampleY).coerceAtLeast(1)
        var brightCount = 0
        var sampleCount = 0
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val luma = (0.2126f * r) + (0.7152f * g) + (0.0722f * b)
                if (luma > 22f) brightCount += 1
                sampleCount += 1
                x += stepX
            }
            y += stepY
        }
        if (sampleCount == 0) return true
        val brightRatio = brightCount.toFloat() / sampleCount.toFloat()
        return brightRatio < 0.03f
    }

    private fun assertFileLooksValid(outFile: File) {
        assertTrue("Screenshot file should exist", outFile.exists())
        assertTrue("Screenshot file should not be empty", outFile.length() > 0L)
    }

    private fun saveBitmap(
        baseDir: File,
        bitmap: Bitmap,
        folder: String,
        prefix: String,
        customFileName: String?
    ): File {
        val screenshotsDir = File(baseDir, "screenshots/$folder").apply { mkdirs() }
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

    private fun grantPermissionIfNeeded(permission: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val packageName = instrumentation.targetContext.packageName
        val pfd = instrumentation.uiAutomation.executeShellCommand(
            "pm grant $packageName $permission"
        )
        pfd.close()
    }

    private fun clearTargetAppData() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        clearAllUserTables(context, "palettes.db")
        clearAllUserTables(context, "recent_picks.db")
        clearSeedFlag(context)
    }

    private fun clearSeedFlag(context: Context) {
        context.getSharedPreferences("seed_flags", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("seeded_v1", false)
            .apply()
        Log.i(LOG_TAG, "Unset seed flag seed_flags/seeded_v1")
    }

    private fun clearAllUserTables(context: Context, dbName: String) {
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists()) {
            Log.i(LOG_TAG, "Database not found, skip clear: ${dbFile.absolutePath}")
            return
        }

        runCatching {
            SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READWRITE
            ).use { db ->
                db.beginTransaction()
                try {
                    val tableNames = mutableListOf<String>()
                    db.rawQuery(
                        """
                        SELECT name
                        FROM sqlite_master
                        WHERE type = 'table'
                          AND name NOT LIKE 'sqlite_%'
                          AND name != 'room_master_table'
                          AND name != 'android_metadata'
                        """.trimIndent(),
                        null
                    ).use { cursor ->
                        while (cursor.moveToNext()) {
                            tableNames += cursor.getString(0)
                        }
                    }
                    tableNames.forEach { table ->
                        db.execSQL("DELETE FROM `$table`")
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }
            Log.i(LOG_TAG, "Cleared Room user tables in $dbName")
        }.onFailure { error ->
            Log.w(LOG_TAG, "Failed clearing $dbName: ${error.message}")
        }
    }

    private fun forceSeedNow() {
        runBlocking {
            composeRule.activity.seedService.seedOnInit()
        }
        relaunchMainActivityAndWait()
        Log.i(LOG_TAG, "Force seed completed")
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
