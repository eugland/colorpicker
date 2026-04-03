package com.primortex.color

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class IconXmlRenderExportUnitTest {

    @Test
    fun exportPlayStoreIcons_fromXmlLayers() {
        val app = RuntimeEnvironment.getApplication()
        val outputDir = resolvePlayStoreDir()

        renderColorIcon(size = 4096, outFile = File(outputDir, "icon-4096-xml.png"))
        renderColorIcon(size = 512, outFile = File(outputDir, "icon-512-xml.png"))
        renderMonochromePreview(size = 512, outFile = File(outputDir, "icon-512-xml-monochrome.png"))
    }

    private fun renderColorIcon(size: Int, outFile: File) {
        val app = RuntimeEnvironment.getApplication()
        val background = requireNotNull(ContextCompat.getDrawable(app, R.drawable.ic_launcher_background))
        val foreground = requireNotNull(ContextCompat.getDrawable(app, R.drawable.ic_launcher_foreground))
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        background.setBounds(0, 0, size, size)
        background.draw(canvas)
        foreground.setBounds(0, 0, size, size)
        foreground.draw(canvas)

        writePng(bitmap, outFile)
    }

    private fun renderMonochromePreview(size: Int, outFile: File) {
        val app = RuntimeEnvironment.getApplication()
        val mono = requireNotNull(ContextCompat.getDrawable(app, R.drawable.ic_launcher_monochrome))
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(Color.WHITE)
        mono.setTint(Color.BLACK)
        mono.setBounds(0, 0, size, size)
        mono.draw(canvas)

        writePng(bitmap, outFile)
    }

    private fun writePng(bitmap: Bitmap, outFile: File) {
        outFile.parentFile?.mkdirs()
        FileOutputStream(outFile).use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                "Failed to write PNG: ${outFile.absolutePath}"
            }
        }
    }

    private fun resolvePlayStoreDir(): File {
        var dir = File(System.getProperty("user.dir"))
        repeat(4) {
            if (File(dir, "settings.gradle.kts").exists()) {
                return File(dir, "play_store").apply { mkdirs() }
            }
            dir = dir.parentFile ?: return@repeat
        }
        return File(System.getProperty("user.dir"), "play_store").apply { mkdirs() }
    }
}
