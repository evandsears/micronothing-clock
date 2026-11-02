# Changelog

All notable changes to the MicroNothing Clock project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### 🐛 Fixed & Hardened (Clock & Event Synchronization)
- **Unified Cradle Bracket Baseline Underline**:
  - Fixed visual artifact where the countdown cradle bracket displayed two lines of differing thickness on the bottom under the text.
  - Replaced the overlapping vector asset tail and separate baseline stroke with a unified, contiguous vector path that maintains an authentic, uniform stroke thickness seamlessly from the corner curve all the way under the countdown text to the end cap.
- **Doze Mode & Battery Saver Reliability**:
  - Added `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` support, allowing users to disable aggressive battery optimization ("Unrestricted" mode) on Nothing OS, Pixel, Samsung, and other devices so background clock ticks never pause.
  - Added Unrestricted Battery Mode controls and status indicators in both `PermissionWalkthrough.kt` and `ClockPreviewScreen.kt` (System & Maintenance tab).
- **Instant Display Refresh on Screen Unlock (`ACTION_USER_PRESENT`)**:
  - Registered `android.intent.action.USER_PRESENT` in `AndroidManifest.xml` and `NothingClockWidgetProvider` to immediately re-render clock and upcoming event the exact millisecond the phone is unlocked.
- **Package Update Alarm Continuity (`ACTION_MY_PACKAGE_REPLACED`)**:
  - Registered `android.intent.action.MY_PACKAGE_REPLACED` to guarantee the 1-minute alarm chain is rescheduled immediately following app updates.
- **Calendar Event Real-Time Sync (`ACTION_PROVIDER_CHANGED`)**:
  - Registered `android.intent.action.PROVIDER_CHANGED` with `content://com.android.calendar` to trigger an instant widget refresh whenever an event is added, modified, or deleted in Google Calendar.
- **Event Duration & Multi-Meeting Logic (`CalendarRepository.kt`)**:
  - Fixed timed event evaluation to respect the actual event `END` timestamp rather than a hardcoded 1-hour limit.
  - Short meetings (15m, 30m) no longer stay stuck on "HAPPENING (NOW)" after they conclude, allowing subsequent meetings to display on time.
  - Long meetings (> 1 hr) remain correctly marked as happening until their true end time.
  - Cache expiration now immediately falls through to live query if calendar permission is granted instead of prematurely displaying generic placeholders.
- **Asynchronous Execution Guard (`goAsync()`)**:
  - Wrapped background broadcast execution in `WidgetAlarmReceiver` and `NothingClockWidgetProvider` with `goAsync()` to prevent Android from killing background coroutines mid-render.
  - Separated local calendar queries from network tasks (Google Tasks & Weather) so calendar events update in milliseconds without waiting for network synchronization.

### 🌟 Added
- **Dynamic Wallpaper-Adaptive Typography for Floating Glyph Preset**:
  - The Floating Glyph (no card background) widget now dynamically adapts its typography and pill elements to match the user's wallpaper, faithfully replicating Nothing OS 5.0 micrographics aesthetics.
  - Automatically samples system Monet dynamic color tokens (`system_accent1_100` / `system_accent1_900`) on Android 12+ (Nothing OS 2.0 / 3.0 / 5.0) and extracts ambient tones from `WallpaperManager` across Android 8.1+.
  - Subtly tints glyphs to harmonize with the wallpaper hue (e.g. ice-blue `#D0E6F2` on slate/twilight mountain wallpapers) while strictly guarding against oversaturation.
  - **Legibility Guard & Contrast Safety Engine**: Implements strict HSV chroma damping (saturation clamped to 6%–16%) and algorithmic WCAG AA/AAA contrast enforcement (>= 4.5:1 ratio). On vibrant or light wallpapers (such as neon green or bright yellow), text automatically promotes to deep obsidian (`#191C18`), completely preventing unreadable neon-on-neon combinations.
  - Coordinated solid badge styling: Day number and status pills match the dynamic text tone with contrasting inverted text.
  - **Cohesive Timezone Badge Typography**: Timezone badge text (e.g. `CST`, `PST`, `EST`, `GMT`) dynamically derives a rich, deeply saturated tone matching the wallpaper's exact hue (such as a deep velvet dark purple `#29203B` on light lavender rather than stark, harsh pure black), maintaining WCAG AAA contrast (>= 7:1) while feeling completely unified with the color scheme. On light wallpapers, text uses a soft luminous tint rather than harsh pure white.
  - Live Wallpaper Change Listener: Registered `ACTION_WALLPAPER_CHANGED` to immediately re-sample colors and update homescreen widgets the instant a new wallpaper is applied.
  - Dynamic in-app preview: In-app preview card now detects and renders against the device's actual wallpaper palette.

- **Dedicated Weather Tap Target (Google Weather Forecast)**:
  - Tapping specifically on the weather line on the home screen widget opens Google Weather for the full interactive weather forecast, matching official Android weather widgets.
  - Multi-tier resolution fallback seamlessly opens standalone Pixel Weather (`com.google.android.apps.weather`), Google App's exported Weather activity (`WeatherExportedActivity`), Google App's Velour proxy activity, or universal Google weather search in the browser.
  - Distinct hotspot zones dynamically adapt depending on whether the calendar row is visible (4-row mode) or hidden (2-row mode).
  - Added "Google Weather Forecast" action card in the in-app Weather configuration tab.

### ⚡ Changed & Improved
- **Active Event Countdown ("NOW")**:
  - Ongoing events occurring right now display `NOW` inside the authentic cradle bracket instead of `0MIN` / `0 minutes`.
  - The `NOW` status stays current for up to 1 hour from event start.
  - After 1 hour, the calendar row automatically defaults back to the next scheduled event, today's all-day event, or generic quippy phrases.
- **Daily Calendar Reset & Placeholder Defaulting**:
  - Event queries are now strictly bounded to today's local schedule, ensuring the calendar resets every day.
  - When no upcoming or all-day events are scheduled, the widget consistently defaults to generic placeholder phrases (`YOUR DAY IS OPEN`, `NOTHING SCHEDULED`, `ALL CLEAR TODAY`, etc.) rather than simulator test data.
  - Synchronous widget render cache updated to prevent flashing fallback placeholder events.

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
