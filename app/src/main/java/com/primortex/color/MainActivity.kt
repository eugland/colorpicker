package com.primortex.color

import android.os.Bundle
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.primortex.color.app.ColorApp
import com.primortex.color.service.SettingsService
import com.primortex.color.service.ThemeMode
import com.primortex.color.ui.theme.ColorTheme

class MainActivity : ComponentActivity() {
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
