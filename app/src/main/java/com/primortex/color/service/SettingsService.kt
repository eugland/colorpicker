package com.primortex.color.service

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

enum class CrosshairSize(val label: String) {
    Small("Small"),
    Medium("Medium"),
    Large("Large")
}

enum class CrosshairShape(val label: String) {
    Circle("Circle"),
    Square("Square"),
    Cross("Crosshair")
}

enum class ThemeMode(val label: String) {
    SYSTEM("System default"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class PickerSensitivity(val label: String) {
    Low("Low"),
    Medium("Medium"),
    High("High")
}

enum class AppLanguage(val label: String) {
    SystemDefault("system_default"),
    English("english"),
    Spanish("spanish"),
    French("french"),
    German("german"),
    Italian("italian"),
    Portuguese("portuguese"),
    Russian("russian"),
    ChineseSimplified("chinese_simplified"),
    ChineseTraditional("chinese_traditional"),
    Japanese("japanese"),
    Korean("korean"),
    Arabic("arabic"),
    Hindi("hindi"),
    Bengali("bengali"),
    Urdu("urdu"),
    Indonesian("indonesian"),
    Vietnamese("vietnamese"),
    Turkish("turkish"),
    Dutch("dutch"),
    Swedish("swedish"),
    Norwegian("norwegian"),
    Danish("danish"),
    Finnish("finnish"),
    Greek("greek"),
    Polish("polish"),
    Czech("czech"),
    Hungarian("hungarian"),
    Romanian("romanian"),
    Thai("thai"),
    Filipino("filipino"),
    Malay("malay"),
    Hebrew("hebrew"),
    Ukrainian("ukrainian")
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
