# Changelog

All notable changes to the MicroNothing Clock project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.2.0] - 2026-10-26 (Version Code: 3)

### 🚀 Highlights
- **Interactive Setup & Permission Walkthrough**: A new 4-step guided onboarding experience to configure exact timing alarms, calendar synchronization, location/weather access, and one-tap homescreen widget placement.
- **Dynamic Variable Typography**: Selectable font weights ranging from Light (300) to Bold (700) using Android's native variable font engine (`'wght'` variation axis) on Geist Mono.
- **Day & Night Weather Specs**: Day/night solar cycle awareness with new authentic Nothing OS vector graphics for Clear Sky and Clear Night.
- **Smart Countdown & Extended Meeting Support**: Meeting status now elegantly indicates multi-hour countdowns (e.g., `1 HOUR`, `3 HOURS`) and all-day milestones with authentic hand tally gestures.

---

### 🌟 Added
- **Interactive Setup Walkthrough (`PermissionWalkthrough.kt`)**:
  - Step 1: Timing & Exact Alarms (`SCHEDULE_EXACT_ALARM`) for battery-conscious, zero-drift widget ticks.
  - Step 2: Device Calendar integration (`READ_CALENDAR`) with on-device privacy guarantee.
  - Step 3: Location-based live weather fetching (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`).
  - Step 4: Widget pinning shortcut (`AppWidgetManager.requestPinAppWidget`) with home screen preview.
  - Animated horizontal slide and fade transitions with progress indicator pills.
  - Ability to reopen the setup walkthrough at any time from the customizer or header.
- **Variable Font Weight Engine (`WidgetFontWeight`)**:
  - Added five weight presets: `LIGHT` (300), `REGULAR` (400), `MEDIUM` (500), `SEMI_BOLD` (600), and `BOLD` (700).
  - Implemented dynamic runtime font instantiation via `Typeface.Builder` variation settings `'wght'` on API 26+ and API 28+ `Typeface.create`, backed by static fallback caching.
  - Dynamic underline cradle bracket stroke thickness auto-scaling to match the active font weight.
- **Day/Night Weather Differentiation**:
  - Open-Meteo `is_day` solar position integration in `WeatherRepository`.
  - Added vector drawables `ic_nothing_weather_clear` and `ic_nothing_weather_clear_night` sourced from authentic Nothing OS specs.
- **Calendar Visibility Toggle**:
  - Option to hide the calendar row completely for users who prefer an ultra-clean clock + weather face.
- **App Theme Switcher & Material You Support**:
  - Added `AppThemeMode` settings: Follow System, Light Mode, and Dark Mode.
  - Dynamic color toggle (`useDynamicColor`) for harmonizing in-app UI with wallpaper tones.
  - Reactive edge-to-edge status and navigation bar styling responding to theme changes.
- **Launcher Widget Preview**:
  - High-resolution preview layout (`widget_nothing_clock_preview.xml`) and asset (`widget_preview.png`) in launcher widget selector.

---

### ⚡ Changed & Improved
- **Meeting Countdown Logic**:
  - Expanded event search horizon to display meetings occurring later in the day or within 6 hours.
  - Formatted countdown displays `1 HOUR` or `X HOURS` instead of overflow minute values when starting in > 60 minutes.
- **Production Build Optimizations**:
  - Enabled R8 minification (`isMinifyEnabled = true`) and resource shrinking (`isShrinkResources = true`).
  - Hardened ProGuard rules (`proguard-rules.pro`) for Compose UI, variable font resources, and Glance/RemoteViews providers.
- **Target SDK Compatibility**:
  - Validated and targeted Android 16 (API 36).
- **Interactive Web Preview (`preview.html`)**:
  - Updated web customizer to mirror the latest font weights, themes, calendar toggles, and weather modes.

---

## [1.0.1] - Earlier Release (Version Code: 2)

- Initial release of MicroNothing Clock widget and customizer.
- Native high-resolution canvas renderer (`NothingClockRenderer`) reproducing Nothing OS 5.0 lockscreen aesthetics.
- Blueprint Grid, AMOLED Dark, MicroNothing Red, Frosted Glass, and Floating Glyph theme presets.
- Open-Meteo live weather client with temperature bar gauge and unit switcher.
- Device calendar sync with quippy placeholder generation.
- Exact alarm tick receiver for zero-battery-drain minute updates.
- Android 16 (API 36) support.
