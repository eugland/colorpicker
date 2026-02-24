package com.primortex.color.service

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.charset.StandardCharsets
import kotlin.io.path.name
import kotlin.streams.toList
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorCatalogJsonIntegrityTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val resDir: Path = Paths.get("src", "main", "res")
    private val localeQualifierRegex = Regex("^[a-z]{2,3}(-r[A-Z]{2})?$")

    private fun rawDirs(): List<Path> =
        Files.list(resDir).use { stream ->
            stream
                .filter { Files.isDirectory(it) && it.fileName.toString().startsWith("raw") }
                .sorted()
                .toList()
        }

    private fun readSeeds(path: Path): List<ColorSeed> {
        val raw = String(Files.readAllBytes(path), StandardCharsets.UTF_8)
        return json.decodeFromString(ListSerializer(ColorSeed.serializer()), raw)
    }

    @Test
    fun everyRawDirectory_hasColorsJson() {
        rawDirs().forEach { dir ->
            val file = dir.resolve("colors.json")
            assertTrue("Missing colors.json for ${dir.fileName}", Files.exists(file))
        }
    }

    @Test
    fun allCatalogJsonFiles_areUtf8WithoutBom_andParseable() {
        rawDirs().forEach { dir ->
            val file = dir.resolve("colors.json")
            val bytes = Files.readAllBytes(file)
            val hasBom =
                bytes.size >= 3 &&
                    bytes[0] == 0xEF.toByte() &&
                    bytes[1] == 0xBB.toByte() &&
                    bytes[2] == 0xBF.toByte()
            assertFalse("BOM is not allowed in ${file.fileName} (${dir.fileName})", hasBom)

            val seeds = readSeeds(file)
            assertFalse("Catalog must not be empty: ${dir.fileName}", seeds.isEmpty())
        }
    }

    @Test
    fun allSeeds_haveNonBlankName_andValidHex() {
        val hexRegex = Regex("^#[0-9A-Fa-f]{6}$")
        rawDirs().forEach { dir ->
            val file = dir.resolve("colors.json")
            val seeds = readSeeds(file)
            seeds.forEachIndexed { index, seed ->
                assertTrue(
                    "Blank name at ${dir.fileName}/colors.json[$index]",
                    seed.name.isNotBlank()
                )
                assertTrue(
                    "Invalid hex '${seed.hex}' at ${dir.fileName}/colors.json[$index]",
                    hexRegex.matches(seed.hex)
                )
            }
        }
    }

    @Test
    fun localeCatalogCounts_matchExpectedPolicy() {
        val counts = rawDirs().associate { dir ->
            dir.name to readSeeds(dir.resolve("colors.json")).size
        }

        val base = counts.getValue("raw")
        assertEquals("Base catalog size changed unexpectedly", 1013, base)
        assertEquals(143, counts.getValue("raw-ja"))
        assertEquals(165, counts.getValue("raw-zh"))
        assertEquals(165, counts.getValue("raw-zh-rCN"))
        assertEquals(165, counts.getValue("raw-zh-rTW"))

        counts
            .filterKeys {
                it != "raw" &&
                    it != "raw-ja" &&
                    it != "raw-zh" &&
                    it != "raw-zh-rCN" &&
                    it != "raw-zh-rTW"
            }
            .forEach { (dir, count) ->
                assertEquals("Expected full catalog parity for $dir", base, count)
            }
    }

    @Test
    fun everyValuesLocale_hasMatchingRawLocaleCatalog() {
        val valueLocales = Files.list(resDir).use { stream ->
            stream
                .filter { Files.isDirectory(it) && it.fileName.toString().startsWith("values-") }
                .map { it.fileName.toString().removePrefix("values-") }
                .filter { qualifier -> localeQualifierRegex.matches(qualifier) }
                .sorted()
                .toList()
        }
        val rawLocales = Files.list(resDir).use { stream ->
            stream
                .filter { Files.isDirectory(it) && it.fileName.toString().startsWith("raw-") }
                .map { it.fileName.toString().removePrefix("raw-") }
                .sorted()
                .toList()
        }

        assertEquals(
            "Each values-* locale must have a matching raw-* colors catalog locale",
            valueLocales,
            rawLocales
        )
    }
}
