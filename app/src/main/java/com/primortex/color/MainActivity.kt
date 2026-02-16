package com.primortex.color

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.primortex.color.app.ColorApp
import com.primortex.color.app.StartupViewModel
import com.primortex.color.i18n.LanguageCache
import com.primortex.color.i18n.LocaleManagerBridge
import com.primortex.color.i18n.LocaleUtil
import com.primortex.color.service.SettingsService
import com.primortex.color.data.enums.ThemeMode
import com.primortex.color.ui.theme.ColorTheme
import com.primortex.color.ui.components.AnimatedSplashHost

class MainActivity : ComponentActivity() {
    private val startupViewModel: StartupViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        val cachedTag = LanguageCache.get(newBase)
        val systemLocales = LocaleManagerBridge.getApplicationLocales(newBase)
        val systemTag = if (systemLocales.isEmpty) null else systemLocales[0]?.toLanguageTag()
        val tag = cachedTag ?: systemTag

        if (cachedTag == null && systemTag != null) {
            LanguageCache.set(newBase, systemTag)
        }

        super.attachBaseContext(LocaleUtil.wrap(newBase, tag))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { !startupViewModel.isReady.value }
        enableEdgeToEdge()
        setContent {
            val themeMode by SettingsService.themeMode.collectAsStateWithLifecycle()
            val startupReady by startupViewModel.isReady.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val activity = LocalContext.current as? Activity
            val darkTheme by remember {
                derivedStateOf {
                    when (themeMode) {
                        ThemeMode.DARK -> true
                        ThemeMode.LIGHT -> false
                        ThemeMode.SYSTEM -> systemDark
                    }
                }
            }

            ColorTheme(darkTheme = darkTheme) {
                AnimatedSplashHost(showContent = startupReady) {
                    ColorApp(onLanguageChanged = { activity?.recreate() })
                }
            }
        }
    }
}

