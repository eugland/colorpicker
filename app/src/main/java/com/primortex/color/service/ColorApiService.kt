package com.primortex.color.service

import android.graphics.Color
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


class ColorApiService(
    private val client: HttpClient = defaultClient()
) {
    suspend fun getColorName(hex: String): String {
        val clean = normalizeHex(hex)

        val resp: ColorApiResponse = client.get("https://www.thecolorapi.com/id") {
            parameter("hex", clean)
        }.body()

        return resp.name.value
    }

    // Accept Android-style ARGB int (0xAARRGGBB). Alpha is ignored.
    suspend fun getColorName(argb: Int): String {
        val hex = argbToRgbHex(argb)
        return getColorName(hex=hex)
    }

    // Accept Compose Color. Alpha is ignored.
    suspend fun getColorName(color: Color): String {
        return getColorName(color.toArgb())
    }

    private fun normalizeHex(hex: String): String {
        val clean = hex.trim().removePrefix("#")
        require(clean.matches(Regex("^[0-9A-Fa-f]{6}$"))) { "Invalid hex: $hex" }
        return clean.uppercase()
    }

    private fun argbToRgbHex(argb: Int): String {
        val rgb = argb and 0x00FFFFFF // drop alpha
        return "%06X".format(rgb)
    }

    companion object {
        fun defaultClient(): HttpClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
    }
}

@Serializable data class ColorApiResponse(val name: NameObj)
@Serializable data class NameObj(
    val value: String,
    @SerialName("closest_named_hex") val closestNamedHex: String? = null,
    @SerialName("exact_match_name") val exactMatchName: Boolean? = null,
    val distance: Int? = null
)
