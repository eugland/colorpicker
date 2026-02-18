package com.primortex.color.service

import android.content.Context
import android.util.Log
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.primortex.color.analytics.AnalyticsTracker
import com.primortex.color.data.enums.AppLanguage
import com.primortex.color.data.enums.CrosshairShape
import com.primortex.color.data.enums.CrosshairSize
import com.primortex.color.data.enums.PickerSensitivity
import com.primortex.color.data.enums.ThemeMode
import com.primortex.color.i18n.AppStrings
import com.primortex.color.i18n.LanguageCache
import com.primortex.color.i18n.LocaleManagerBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

object SettingsService {
    private val KEY_CROSSHAIR_SIZE = stringPreferencesKey("crosshair_size")
    private val KEY_CROSSHAIR_SHAPE = stringPreferencesKey("crosshair_shape")
    private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    private val KEY_PICKER_SENSITIVITY = stringPreferencesKey("picker_sensitivity")
    private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
    private val KEY_COLOR_ASSETS = stringPreferencesKey("color_assets")
    private val KEY_FIRST_USE_LOGGED = booleanPreferencesKey("first_use_logged_v1")

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

    private val _availableColorAssets = MutableStateFlow<List<ColorAssetOption>>(emptyList())
    val availableColorAssets: StateFlow<List<ColorAssetOption>> = _availableColorAssets.asStateFlow()

    private val _selectedColorAssets = MutableStateFlow<Set<String>>(emptySet())
    val selectedColorAssets: StateFlow<Set<String>> = _selectedColorAssets.asStateFlow()

    fun init(context: Context) {
        if (::appContext.isInitialized) return

        appContext = context.applicationContext

        scope.launch {
            val prefs = appContext.settingsDataStore.data.first()
            val systemLocales = LocaleManagerBridge.getApplicationLocales(appContext)
            val systemTag = if (systemLocales.isEmpty) null else systemLocales[0]?.toLanguageTag()

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

            val storedLanguage = prefs[KEY_APP_LANGUAGE]
                ?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
                ?: AppLanguage.SystemDefault

            val resolvedLanguage = systemTag?.let { tag ->
                findLanguageForTag(tag) ?: AppLanguage.SystemDefault
            } ?: storedLanguage

            _appLanguage.value = resolvedLanguage

            val availableAssets = ColorCatalogImportService.availableAssets(appContext)
            _availableColorAssets.value = availableAssets
            val storedAssetIds = prefs[KEY_COLOR_ASSETS]
                ?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?.toSet()
                ?: emptySet()
            val selectedAssets = ColorCatalogImportService.normalizeSelection(appContext, storedAssetIds)
            _selectedColorAssets.value = selectedAssets

            val tagToCache = systemTag ?: resolvedLanguage.languageTag
            if (tagToCache != null) {
                LanguageCache.set(appContext, tagToCache)
            }

            syncPlatformLocale(resolvedLanguage)
            AppStrings.clear()
            Log.d("AppLanguage", "init: ${_appLanguage.value}")
            syncColorCatalogSelection(selectedAssets)

            if (prefs[KEY_FIRST_USE_LOGGED] != true) {
                AnalyticsTracker.logFirstUse()
                appContext.settingsDataStore.edit { updated ->
                    updated[KEY_FIRST_USE_LOGGED] = true
                }
            }
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
        Log.d("AppLanguage", "setAppLanguage: $language")
        LanguageCache.set(appContext, language.languageTag)
        syncPlatformLocale(language)
        AppStrings.clear()
        syncColorCatalogSelection(_selectedColorAssets.value)
        persist()
    }

    fun setSelectedColorAssets(selectedIds: Set<String>) {
        val next = ColorCatalogImportService.normalizeSelection(appContext, selectedIds)
        _selectedColorAssets.value = next
        syncColorCatalogSelection(next)
        persist()
    }

    private fun syncColorCatalogSelection(selectedAssets: Set<String>) {
        scope.launch {
            ColorServices.setCatalogSelection(selectedAssets)
        }
    }


    private fun syncPlatformLocale(language: AppLanguage) {
        val locales = language.languageTag
            ?.let { LocaleListCompat.forLanguageTags(it) }
            ?: LocaleListCompat.getEmptyLocaleList()

        LocaleManagerBridge.setApplicationLocales(appContext, locales)
    }

    private fun findLanguageForTag(tag: String): AppLanguage? {
        return AppLanguage.entries.firstOrNull { candidate ->
            candidate.languageTag != null && tag.equals(candidate.languageTag, ignoreCase = true)
        } ?: AppLanguage.entries.firstOrNull { candidate ->
            candidate.languageTag != null && tag.startsWith(
                candidate.languageTag,
                ignoreCase = true
            )
        }
    }

    private fun persist() {
        val size = _crosshairSize.value.name
        val shape = _crosshairShape.value.name
        val theme = _themeMode.value.name
        val sensitivity = _pickerSensitivity.value.name
        val language = _appLanguage.value.name
        val assets = _selectedColorAssets.value.joinToString(",")

        scope.launch {
            appContext.settingsDataStore.edit { prefs ->
                prefs[KEY_CROSSHAIR_SIZE] = size
                prefs[KEY_CROSSHAIR_SHAPE] = shape
                prefs[KEY_THEME_MODE] = theme
                prefs[KEY_PICKER_SENSITIVITY] = sensitivity
                prefs[KEY_APP_LANGUAGE] = language
                prefs[KEY_COLOR_ASSETS] = assets
            }
        }
    }

}

