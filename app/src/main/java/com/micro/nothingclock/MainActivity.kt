package com.micro.nothingclock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.micro.nothingclock.data.AppThemeMode
import com.micro.nothingclock.data.WidgetPreferences
import com.micro.nothingclock.ui.screens.ClockPreviewScreen
import com.micro.nothingclock.ui.theme.MicroNothingClockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember { WidgetPreferences(context) }
            var config by remember { mutableStateOf(prefs.loadConfig()) }

            val systemInDark = isSystemInDarkTheme()
            val isDark = when (config.appThemeMode) {
                AppThemeMode.SYSTEM -> systemInDark
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            DisposableEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    }
                )
                onDispose {}
            }

            MicroNothingClockTheme(
                themeMode = config.appThemeMode,
                dynamicColor = config.useDynamicColor
            ) {
                ClockPreviewScreen(
                    initialConfig = config,
                    onAppConfigChanged = { updated ->
                        config = updated
                        prefs.saveConfig(updated)
                    }
                )
            }
        }
    }
}
