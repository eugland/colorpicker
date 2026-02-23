package com.primortex.color

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.primortex.color.analytics.AnalyticsTracker
import com.primortex.color.app.ColorApp
import com.primortex.color.app.StartupViewModel
import com.primortex.color.i18n.LanguageCache
import com.primortex.color.i18n.LocaleManagerBridge
import com.primortex.color.i18n.LocaleUtil
import com.primortex.color.data.enums.ThemeMode
import com.primortex.color.service.SeedService
import com.primortex.color.service.SettingsService
import com.primortex.color.ui.theme.ColorTheme
import com.primortex.color.ui.components.AnimatedSplashHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val startupViewModel: StartupViewModel by viewModels()
    @Inject
    lateinit var settingsService: SettingsService
    @Inject
    lateinit var analyticsTracker: AnalyticsTracker
    @Inject
    lateinit var seedService: SeedService

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
            LaunchedEffect(Unit) {
                seedService.seedOnInit()
            }
            val themeMode by settingsService.themeMode.collectAsStateWithLifecycle()
            val startupReady by startupViewModel.isReady.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
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
                    ColorApp(
                        settingsService = settingsService,
                        onLanguageChanged = ::recreate,
                        onScreenViewed = { screenName, screenClass ->
                            analyticsTracker.logScreenView(screenName, screenClass)
                        }
                    )
                }
            }
        }
    }
}


