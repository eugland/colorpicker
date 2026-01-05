package com.primortex.color

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.os.LocaleManagerCompat
import com.primortex.color.app.ColorApp
import com.primortex.color.i18n.LanguageCache
import com.primortex.color.i18n.LocaleUtil
import com.primortex.color.service.SettingsService
import com.primortex.color.service.ThemeMode
import com.primortex.color.ui.theme.ColorTheme

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val cachedTag = LanguageCache.get(newBase)
        val systemLocales = LocaleManagerCompat.getApplicationLocales(newBase)
        val systemTag = if (systemLocales.isEmpty) null else systemLocales[0]?.toLanguageTag()
        val tag = cachedTag ?: systemTag

        if (cachedTag == null && systemTag != null) {
            LanguageCache.set(newBase, systemTag)
        }

        super.attachBaseContext(LocaleUtil.wrap(newBase, tag))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by SettingsService.themeMode.collectAsStateWithLifecycle()
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
                ColorApp(onLanguageChanged = { activity?.recreate() })
            }
        }
    }
}
