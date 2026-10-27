package com.micro.nothingclock.data

import android.content.Context
import android.content.SharedPreferences

enum class AppThemeMode(val id: String, val displayName: String) {
    SYSTEM("system", "Follow System"),
    LIGHT("light", "Light Mode"),
    DARK("dark", "Dark Mode")
}

enum class ClockThemePreset(val id: String, val displayName: String, val description: String = "") {
    OS5_BLUEPRINT("os5_blueprint", "Blueprint Grid", "Technical architectural blueprint with subtle grid"),
    MATERIAL_YOU("material_you", "Material You", "Expressive dynamic palette harmonized with wallpaper"),
    FROSTED_GLASS("frosted_glass", "Frosted Glass", "Translucent frosted aesthetic with glass border"),
    AMOLED_DARK("dark", "AMOLED Dark", "Pure obsidian dark background with maximum battery saving"),
    NOTHING_RED("red", "MicroNothing Red", "Signature deep ruby red glyphs and accents"),
    TRANSPARENT_FLOATING("floating", "Floating Glyph", "Ultra-minimal floating time elements without container card")
}

enum class WidgetFontWeight(
    val id: String,
    val displayName: String,
    val weight: Int,
    val description: String = ""
) {
    LIGHT("light", "Light", 300, "Thin technical aesthetic"),
    REGULAR("regular", "Regular", 400, "Clean standard weight"),
    MEDIUM("medium", "Medium", 500, "Nothing OS 5.0 micrographics weight"),
    SEMI_BOLD("semi_bold", "Semi-Bold", 600, "Crisp high-contrast weight"),
    BOLD("bold", "Bold", 700, "Classic punchy weight (Default)")
}

data class WidgetConfig(
    val themePreset: ClockThemePreset = ClockThemePreset.OS5_BLUEPRINT,
    val fontWeight: WidgetFontWeight = WidgetFontWeight.BOLD,
    val cardOpacityPercent: Int = 100,
    val cornerRadiusDp: Int = 28,
    val is24Hour: Boolean = false,
    val showCursor: Boolean = false,
    val showCalendar: Boolean = true,
    val useLiveCalendar: Boolean = false,
    val enableQuippyPlaceholders: Boolean = true,
    val fallbackEventTitle: String = "WEEKLY DESIGN SYNC",
    val fallbackMinutes: Int = 6,
    val tempCelsius: Int = 72,
    val tempUnit: String = "°F",
    val weatherCondition: String = "SUNNY",
    val useLiveWeather: Boolean = false,
    val weatherRefreshIntervalMinutes: Int = 15,
    val appThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true
)

class WidgetPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nothing_clock_prefs", Context.MODE_PRIVATE)

    fun loadConfig(): WidgetConfig {
        val presetId = prefs.getString("theme_preset", ClockThemePreset.OS5_BLUEPRINT.id)
        val preset = ClockThemePreset.entries.find { it.id == presetId } ?: ClockThemePreset.OS5_BLUEPRINT
        val fontWeightId = prefs.getString("font_weight", WidgetFontWeight.BOLD.id)
        val fontWeight = WidgetFontWeight.entries.find { it.id == fontWeightId } ?: WidgetFontWeight.BOLD
        val unit = prefs.getString("temp_unit", "°F") ?: "°F"
        val defaultTemp = if (unit.contains("F", ignoreCase = true)) 72 else 22
        val themeModeId = prefs.getString("app_theme_mode", AppThemeMode.SYSTEM.id)
        val appThemeMode = AppThemeMode.entries.find { it.id == themeModeId } ?: AppThemeMode.SYSTEM
        val useDynamicColor = prefs.getBoolean("use_dynamic_color", true)

        return WidgetConfig(
            themePreset = preset,
            fontWeight = fontWeight,
            cardOpacityPercent = prefs.getInt("card_opacity", 100),
            cornerRadiusDp = prefs.getInt("corner_radius", 28),
            is24Hour = prefs.getBoolean("is_24_hour", false),
            showCursor = prefs.getBoolean("show_cursor", false),
            showCalendar = prefs.getBoolean("show_calendar", true),
            useLiveCalendar = prefs.getBoolean("use_live_calendar", false),
            enableQuippyPlaceholders = prefs.getBoolean("enable_quippy_placeholders", true),
            fallbackEventTitle = prefs.getString("fallback_title", "WEEKLY DESIGN SYNC") ?: "WEEKLY DESIGN SYNC",
            fallbackMinutes = prefs.getInt("fallback_minutes", 6),
            tempCelsius = prefs.getInt("temp_celsius", defaultTemp),
            tempUnit = unit,
            weatherCondition = prefs.getString("weather_condition", "SUNNY") ?: "SUNNY",
            useLiveWeather = prefs.getBoolean("use_live_weather", false),
            weatherRefreshIntervalMinutes = prefs.getInt("weather_refresh_interval", 15),
            appThemeMode = appThemeMode,
            useDynamicColor = useDynamicColor
        )
    }

    fun saveConfig(config: WidgetConfig) {
        prefs.edit()
            .putString("theme_preset", config.themePreset.id)
            .putString("font_weight", config.fontWeight.id)
            .putInt("card_opacity", config.cardOpacityPercent)
            .putInt("corner_radius", config.cornerRadiusDp)
            .putBoolean("is_24_hour", config.is24Hour)
            .putBoolean("show_cursor", config.showCursor)
            .putBoolean("show_calendar", config.showCalendar)
            .putBoolean("use_live_calendar", config.useLiveCalendar)
            .putBoolean("enable_quippy_placeholders", config.enableQuippyPlaceholders)
            .putString("fallback_title", config.fallbackEventTitle)
            .putInt("fallback_minutes", config.fallbackMinutes)
            .putInt("temp_celsius", config.tempCelsius)
            .putString("temp_unit", config.tempUnit)
            .putString("weather_condition", config.weatherCondition)
            .putBoolean("use_live_weather", config.useLiveWeather)
            .putInt("weather_refresh_interval", config.weatherRefreshIntervalMinutes)
            .putString("app_theme_mode", config.appThemeMode.id)
            .putBoolean("use_dynamic_color", config.useDynamicColor)
            .apply()
    }

    fun hasCompletedWalkthrough(): Boolean {
        return prefs.getBoolean("has_completed_walkthrough", false)
    }

    fun setWalkthroughCompleted(completed: Boolean = true) {
        prefs.edit().putBoolean("has_completed_walkthrough", completed).apply()
    }

    fun resetWalkthrough() {
        prefs.edit().putBoolean("has_completed_walkthrough", false).apply()
    }
}
