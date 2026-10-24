package com.micro.nothingclock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NothingRed,
    onPrimary = Color.White,
    secondary = Color.White,
    onSecondary = Color.Black,
    background = NothingDark,
    surface = NothingCard,
    onSurface = Color.White,
    outline = NothingBorder
)

@Composable
fun MicroNothingClockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = NothingTypography,
        content = content
    )
}
