package com.primortex.color.service

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ColorApiServiceTest {

    @Test
    fun `get prominent color names`() = runTest {
        val service = ColorApiService()

        val colors = listOf(
            "FF0000", // Red
            "00FF00", // Green
            "0000FF", // Blue
            "FFFF00", // Yellow
            "FFA500", // Orange
            "800080", // Purple
            "00FFFF", // Cyan
            "FF69B4", // Hot Pink
            "8B4513", // Saddle Brown
            "000000"  // Black
        )

        for (hex in colors) {
            val name = service.getColorName(hex)
            println("#$hex -> $name")
            assertTrue(name.isNotBlank())
        }
    }
}
