package com.micro.nothingclock.data

import android.content.Context
import android.content.SharedPreferences

enum class ClockThemePreset(val id: String, val displayName: String) {
    OS5_BLUEPRINT("os5_blueprint", "Blueprint Grid"),
    MATERIAL_YOU("material_you", "Material You"),
    FROSTED_GLASS("frosted_glass", "Frosted Glass"),
    AMOLED_DARK("dark", "AMOLED Dark"),
    NOTHING_RED("red", "MicroNothing Red"),
    TRANSPARENT_FLOATING("floating", "Floating Glyph")
}

data class WidgetConfig(
    val themePreset: ClockThemePreset = ClockThemePreset.OS5_BLUEPRINT,
    val cardOpacityPercent: Int = 100,
    val cornerRadiusDp: Int = 28,
    val is24Hour: Boolean = false,
    val showCursor: Boolean = false,
    val useLiveCalendar: Boolean = false,
    val enableQuippyPlaceholders: Boolean = true,
    val fallbackEventTitle: String = "WEEKLY DESIGN SYNC",
    val fallbackMinutes: Int = 6,
    val tempCelsius: Int = 72,
    val tempUnit: String = "°F",
    val weatherCondition: String = "SUNNY",
    val useLiveWeather: Boolean = false,
    val weatherRefreshIntervalMinutes: Int = 15
)

class WidgetPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nothing_clock_prefs", Context.MODE_PRIVATE)

    fun loadConfig(): WidgetConfig {
        val presetId = prefs.getString("theme_preset", ClockThemePreset.OS5_BLUEPRINT.id)
        val preset = ClockThemePreset.entries.find { it.id == presetId } ?: ClockThemePreset.OS5_BLUEPRINT
        val unit = prefs.getString("temp_unit", "°F") ?: "°F"
        val defaultTemp = if (unit.contains("F", ignoreCase = true)) 72 else 22
        return WidgetConfig(
            themePreset = preset,
            cardOpacityPercent = prefs.getInt("card_opacity", 100),
            cornerRadiusDp = prefs.getInt("corner_radius", 28),
            is24Hour = prefs.getBoolean("is_24_hour", false),
            showCursor = prefs.getBoolean("show_cursor", false),
            useLiveCalendar = prefs.getBoolean("use_live_calendar", false),
            enableQuippyPlaceholders = prefs.getBoolean("enable_quippy_placeholders", true),
            fallbackEventTitle = prefs.getString("fallback_title", "WEEKLY DESIGN SYNC") ?: "WEEKLY DESIGN SYNC",
            fallbackMinutes = prefs.getInt("fallback_minutes", 6),
            tempCelsius = prefs.getInt("temp_celsius", defaultTemp),
            tempUnit = unit,
            weatherCondition = prefs.getString("weather_condition", "SUNNY") ?: "SUNNY",
            useLiveWeather = prefs.getBoolean("use_live_weather", false),
            weatherRefreshIntervalMinutes = prefs.getInt("weather_refresh_interval", 15)
        )
    }

    fun saveConfig(config: WidgetConfig) {
        prefs.edit()
            .putString("theme_preset", config.themePreset.id)
            .putInt("card_opacity", config.cardOpacityPercent)
            .putInt("corner_radius", config.cornerRadiusDp)
            .putBoolean("is_24_hour", config.is24Hour)
            .putBoolean("show_cursor", config.showCursor)
            .putBoolean("use_live_calendar", config.useLiveCalendar)
            .putBoolean("enable_quippy_placeholders", config.enableQuippyPlaceholders)
            .putString("fallback_title", config.fallbackEventTitle)
            .putInt("fallback_minutes", config.fallbackMinutes)
            .putInt("temp_celsius", config.tempCelsius)
            .putString("temp_unit", config.tempUnit)
            .putString("weather_condition", config.weatherCondition)
            .putBoolean("use_live_weather", config.useLiveWeather)
            .putInt("weather_refresh_interval", config.weatherRefreshIntervalMinutes)
            .apply()
    }
}
