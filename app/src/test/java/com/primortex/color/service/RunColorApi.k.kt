package com.primortex.color.service

import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class ColorApiSmokeTest {
    @Test
    fun runRealApi() = runBlocking {
        val name = ColorApiService().getColorName("7B8266")
        println("Color name = $name")
    }

    @Test
    fun runBetterApi() = runBlocking {
        val name = ColorApiService().getColorName("7B8266")
        println("Color name = $name")
    }
}