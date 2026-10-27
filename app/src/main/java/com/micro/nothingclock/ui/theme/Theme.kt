package com.micro.nothingclock.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.micro.nothingclock.data.AppThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = NothingRedLight,
    onPrimary = Color.Black,
    primaryContainer = NothingRedContainerDark,
    onPrimaryContainer = NothingRedContainerLight,
    secondary = Color(0xFFE2E2E8),
    onSecondary = Color(0xFF1B1B1F),
    secondaryContainer = Color(0xFF2C2C34),
    onSecondaryContainer = Color(0xFFE2E2E8),
    tertiary = Color(0xFF70B6F6),
    onTertiary = Color(0xFF003258),
    tertiaryContainer = Color(0xFF00497D),
    onTertiaryContainer = Color(0xFFD1E4FF),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceContainerHigh,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val LightColorScheme = lightColorScheme(
    primary = NothingRed,
    onPrimary = Color.White,
    primaryContainer = NothingRedContainerLight,
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF262830),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E2EA),
    onSecondaryContainer = Color(0xFF141720),
    tertiary = Color(0xFF00639B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCEE5FF),
    onTertiaryContainer = Color(0xFF001D33),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceContainerHigh,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

@Composable
fun MicroNothingClockTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> systemInDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NothingTypography,
        content = content
    )
}
