package com.primortex.color

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.primortex.color.app.ColorApp
import com.primortex.color.service.SettingsService
import com.primortex.color.service.ThemeMode
import com.primortex.color.ui.theme.ColorTheme

// Extend AppCompatActivity so AppCompatDelegate's locale handling applies to the Compose UI.
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by SettingsService.themeMode.collectAsStateWithLifecycle()
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
                ColorApp()
            }
        }
    }
}
