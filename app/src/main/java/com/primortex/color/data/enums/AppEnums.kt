package com.primortex.color.data.enums

import androidx.annotation.StringRes
import com.primortex.color.R

enum class PaletteSchemeType {
    Complementary,
    Analogous,
    SplitComplementary,
    Triadic,
    Tetradic,
    Square
}

enum class SwatchListType {
    RECENT,
    SAVED
}

enum class CrosshairSize(@param:StringRes @field:StringRes val labelRes: Int) {
    Small(R.string.crosshair_size_small),
    Medium(R.string.crosshair_size_medium),
    Large(R.string.crosshair_size_large)
}

enum class CrosshairShape(@param:StringRes @field:StringRes val labelRes: Int) {
    Circle(R.string.crosshair_shape_circle),
    Square(R.string.crosshair_shape_square),
    Cross(R.string.crosshair_shape_cross)
}

enum class ThemeMode(@param:StringRes @field:StringRes val labelRes: Int) {
    SYSTEM(R.string.theme_system_default),
    LIGHT(R.string.theme_light),
    DARK(R.string.theme_dark),
}

enum class PickerSensitivity(@param:StringRes @field:StringRes val labelRes: Int) {
    Low(R.string.picker_sensitivity_low),
    Medium(R.string.picker_sensitivity_medium),
    High(R.string.picker_sensitivity_high)
}

enum class AppLanguage(
    val languageTag: String?,
    @param:StringRes @field:StringRes val labelRes: Int
) {
    SystemDefault(null, R.string.language_system_default),

    English("en", R.string.language_english),
    Spanish("es", R.string.language_spanish),
    French("fr", R.string.language_french),
    German("de", R.string.language_german),
    Italian("it", R.string.language_italian),
    Portuguese("pt", R.string.language_portuguese),
    Russian("ru", R.string.language_russian),

    ChineseSimplified("zh", R.string.language_chinese_simplified),
    ChineseTraditional("zh-Hant", R.string.language_chinese_traditional),

    Japanese("ja", R.string.language_japanese),
    Korean("ko", R.string.language_korean),
    Arabic("ar", R.string.language_arabic),
    Hindi("hi", R.string.language_hindi),
    Bengali("bn", R.string.language_bengali),
    Urdu("ur", R.string.language_urdu),

    Indonesian("id", R.string.language_indonesian),
    Vietnamese("vi", R.string.language_vietnamese),
    Turkish("tr", R.string.language_turkish),
    Dutch("nl", R.string.language_dutch),
    Swedish("sv", R.string.language_swedish),
    Norwegian("nb", R.string.language_norwegian),
    Danish("da", R.string.language_danish),
    Finnish("fi", R.string.language_finnish),

    Greek("el", R.string.language_greek),
    Polish("pl", R.string.language_polish),
    Czech("cs", R.string.language_czech),
    Hungarian("hu", R.string.language_hungarian),
    Romanian("ro", R.string.language_romanian),
    Thai("th", R.string.language_thai),

    Filipino("fil", R.string.language_filipino),
    Malay("ms", R.string.language_malay),
    Hebrew("he", R.string.language_hebrew),
    Ukrainian("uk", R.string.language_ukrainian)
}


