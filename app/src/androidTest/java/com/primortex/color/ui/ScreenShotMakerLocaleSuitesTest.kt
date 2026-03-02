package com.primortex.color.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreenShotMakerLocaleSuitesTest : ScreenShotMakerBase() {

    @Test
    fun captureSuite_en() = captureSuiteFor(localeEn)

    @Test
    fun captureSuite_jp() = captureSuiteFor(localeJa)


    @Test
    fun captureSuite_zh() = captureSuiteFor(localeZh)


    @Test
    fun captureSuite_es() = captureSuiteFor("es")

    @Test
    fun captureSuite_fr() = captureSuiteFor("fr")

    @Test
    fun captureSuite_de() = captureSuiteFor("de")

    @Test
    fun captureSuite_it() = captureSuiteFor("it")

    @Test
    fun captureSuite_pt() = captureSuiteFor("pt")

    @Test
    fun captureSuite_ru() = captureSuiteFor("ru")

    @Test
    fun captureSuite_zh_Hant() = captureSuiteFor("zh-Hant")

    @Test
    fun captureSuite_ko() = captureSuiteFor("ko")

    @Test
    fun captureSuite_ar() = captureSuiteFor("ar")

    @Test
    fun captureSuite_hi() = captureSuiteFor("hi")

    @Test
    fun captureSuite_bn() = captureSuiteFor("bn")

    @Test
    fun captureSuite_ur() = captureSuiteFor("ur")

    @Test
    fun captureSuite_id() = captureSuiteFor("id")

    @Test
    fun captureSuite_vi() = captureSuiteFor("vi")

    @Test
    fun captureSuite_tr() = captureSuiteFor("tr")

    @Test
    fun captureSuite_nl() = captureSuiteFor("nl")

    @Test
    fun captureSuite_sv() = captureSuiteFor("sv")

    @Test
    fun captureSuite_nb() = captureSuiteFor("nb")

    @Test
    fun captureSuite_da() = captureSuiteFor("da")

    @Test
    fun captureSuite_fi() = captureSuiteFor("fi")

    @Test
    fun captureSuite_el() = captureSuiteFor("el")

    @Test
    fun captureSuite_pl() = captureSuiteFor("pl")

    @Test
    fun captureSuite_cs() = captureSuiteFor("cs")

    @Test
    fun captureSuite_hu() = captureSuiteFor("hu")

    @Test
    fun captureSuite_ro() = captureSuiteFor("ro")

    @Test
    fun captureSuite_th() = captureSuiteFor("th")

    @Test
    fun captureSuite_fil() = captureSuiteFor("fil")

    @Test
    fun captureSuite_ms() = captureSuiteFor("ms")

    @Test
    fun captureSuite_he() = captureSuiteFor("he")

    @Test
    fun captureSuite_uk() = captureSuiteFor("uk")
}
