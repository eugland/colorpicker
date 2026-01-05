package com.primortex.color.service

import android.content.Context
import androidx.annotation.StringRes
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.core.os.LocaleListCompat
import com.primortex.color.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

enum class CrosshairSize(@StringRes val labelRes: Int) {
    Small(R.string.crosshair_size_small),
    Medium(R.string.crosshair_size_medium),
    Large(R.string.crosshair_size_large)
}

enum class CrosshairShape(@StringRes val labelRes: Int) {
    Circle(R.string.crosshair_shape_circle),
    Square(R.string.crosshair_shape_square),
    Cross(R.string.crosshair_shape_cross)
}

enum class ThemeMode(val labelRes: Int) {
    DARK(R.string.theme_dark),
    LIGHT(R.string.theme_light),
    SYSTEM(R.string.theme_system_default)
}

enum class PickerSensitivity(@StringRes val labelRes: Int) {
    Low(R.string.picker_sensitivity_low),
    Medium(R.string.picker_sensitivity_medium),
    High(R.string.picker_sensitivity_high)
}

enum class AppLanguage(val languageTag: String?, @StringRes val labelRes: Int) {
    SystemDefault(null, R.string.language_system_default),

    English("en", R.string.language_english),
    Spanish("es", R.string.language_spanish),
    French("fr", R.string.language_french),
    German("de", R.string.language_german),
    Italian("it", R.string.language_italian),
    Portuguese("pt", R.string.language_portuguese),
    Russian("ru", R.string.language_russian),

    ChineseSimplified("zh-Hans", R.string.language_chinese_simplified),
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

object SettingsService {
    private val KEY_CROSSHAIR_SIZE = stringPreferencesKey("crosshair_size")
    private val KEY_CROSSHAIR_SHAPE = stringPreferencesKey("crosshair_shape")
    private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    private val KEY_PICKER_SENSITIVITY = stringPreferencesKey("picker_sensitivity")
    private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")

    private lateinit var appContext: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _crosshairSize = MutableStateFlow(CrosshairSize.Medium)
    val crosshairSize: StateFlow<CrosshairSize> = _crosshairSize

    private val _crosshairShape = MutableStateFlow(CrosshairShape.Circle)
    val crosshairShape: StateFlow<CrosshairShape> = _crosshairShape

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _pickerSensitivity = MutableStateFlow(PickerSensitivity.Medium)
    val pickerSensitivity: StateFlow<PickerSensitivity> = _pickerSensitivity.asStateFlow()

    private val _appLanguage = MutableStateFlow(AppLanguage.SystemDefault)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    fun init(context: Context) {
        if (::appContext.isInitialized) return

        appContext = context.applicationContext

        scope.launch {
            val prefs = appContext.settingsDataStore.data.first()

            _crosshairSize.value = prefs[KEY_CROSSHAIR_SIZE]
                ?.let { runCatching { CrosshairSize.valueOf(it) }.getOrNull() }
                ?: CrosshairSize.Medium

            _crosshairShape.value = prefs[KEY_CROSSHAIR_SHAPE]
                ?.let { runCatching { CrosshairShape.valueOf(it) }.getOrNull() }
                ?: CrosshairShape.Circle

            _themeMode.value = prefs[KEY_THEME_MODE]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM

            _pickerSensitivity.value = prefs[KEY_PICKER_SENSITIVITY]
                ?.let { runCatching { PickerSensitivity.valueOf(it) }.getOrNull() }
                ?: PickerSensitivity.Medium

            _appLanguage.value = prefs[KEY_APP_LANGUAGE]
                ?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
                ?: AppLanguage.SystemDefault
        }
    }

    fun setCrosshairSize(size: CrosshairSize) {
        _crosshairSize.value = size
        persist()
    }

    fun setCrosshairShape(shape: CrosshairShape) {
        _crosshairShape.value = shape
        persist()
    }


    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        persist()
    }

    fun setPickerSensitivity(sensitivity: PickerSensitivity) {
        _pickerSensitivity.value = sensitivity
        persist()
    }

    fun setAppLanguage(language: AppLanguage) {
        _appLanguage.value = language
        persist()
    }

    fun localeListFor(language: AppLanguage): LocaleListCompat {
        return language.languageTag?.let { LocaleListCompat.forLanguageTags(it) }
            ?: LocaleListCompat.getEmptyLocaleList()
    }


    private fun persist() {
        val size = _crosshairSize.value.name
        val shape = _crosshairShape.value.name
        val theme = _themeMode.value.name
        val sensitivity = _pickerSensitivity.value.name
        val language = _appLanguage.value.name

        scope.launch {
            appContext.settingsDataStore.edit { prefs ->
                prefs[KEY_CROSSHAIR_SIZE] = size
                prefs[KEY_CROSSHAIR_SHAPE] = shape
                prefs[KEY_THEME_MODE] = theme
                prefs[KEY_PICKER_SENSITIVITY] = sensitivity
                prefs[KEY_APP_LANGUAGE] = language
            }
        }
    }

}
