# MicroNothing (Glanceable Technical Clock Face & Widget)

A precision-engineered Android clock app and homescreen widget recreating the technical lockscreen and ambient clock face aesthetics.

---

## 🔍 Visual Breakdown & Authentic Nothing OS 5.0 SVG Assets

Faithfully structured from the official Nothing OS 5.0 design assets (`MicroNothing Assets`):

```
┌────────────────────────────────────────────────────────────┐
│  SAT  ( 15 )  ( AUG )  <EST>  10:53                        │  <-- Row 1: Header / Date, Month, Timezone, Time
│                                                            │
│  I ◂  80°(F)   ☀️ SUNNY                                    │  <-- Row 2: Temp Bar Gauge, Temp, Unit, Weather Specs
│                                                            │
│  🗣 WEEKLY DESIGN SYNC  ( ➔ )                               │  <-- Row 3: Speaking Head, Calendar Event, Followup
│                                                            │
│  STARTING  ☝️ 🖐️  └───────────── 6MIN                       │  <-- Row 4: Gesture Tallies & Cradle Bracket
└────────────────────────────────────────────────────────────┘
```

1. **Row 1: Header / Date, Timezone & Time**
   - Monospace day of week (`SAT`) in Geist Mono Bold
   - Cutout date pill badge (`ic_nothing_day_1` .. `31`) from `MicroNothing Assets/Days/`
   - Outlined month pill badge (`ic_nothing_month_jan` .. `dec`) from `MicroNothing Assets/Months/`
   - Starburst timezone badge (`<EST>`) from `MicroNothing Assets/Timezone.svg`
   - Digital time (`10:53`) immediately following `<EST>`

2. **Row 2: Temperature & Weather Specs**
   - Dynamic thermometer gauge (`ic_nothing_temp_bar_100_80`, `70_60`, `50_30`, etc.) with triangle indicator based on temperature
   - Temperature value with degree symbol (`80°`)
   - Circled measure unit badge (`(F)`, `(C)`, `(K)`) from `MicroNothing Assets/Measure/`
   - Complete weather spec vector pairing weather icon and condition speech-bubble (`ic_nothing_weather_sunny`, `rain`, `cloud`, `snow`, `thunder`, `storm`, etc.) from `MicroNothing Assets/Specs/`

3. **Row 3: Next Calendar Meeting**
   - Speaking profile head with acoustic waves (`ic_nothing_speak`) from `MicroNothing Assets/Speak.svg`
   - Event title in uppercase monospace (`WEEKLY DESIGN SYNC`)
   - Pill action arrow badge (`ic_nothing_followup`) from `MicroNothing Assets/Followup.svg`

4. **Row 4: Dynamic Gesture Countdown & Cradle**
   - Status text: `STARTING`
   - Authentic hand gesture icons (`ic_nothing_marker_1` .. `5`) from `MicroNothing Assets/Markers/`. Cleanly displays one or two hands representing the remaining time (e.g., `35MIN` shows Hand 3 and Hand 5; `6MIN` shows Hand 1 and Hand 5; `5MIN` shows Hand 5; `30MIN` shows Hand 3).
   - Underline cradle bracket from `MicroNothing Assets/Union.svg` seamlessly supporting the `${minutesLeft}MIN` countdown text.

---

## ✨ Features

- **Homescreen Widget (`AppWidgetProvider`)**:
  - Resizable 4x2, 4x3, or 2x2 widget.
  - Native high-resolution canvas renderer (`NothingClockRenderer`) preserving pixel-perfect Nothing OS typography, pill badges, and speech bubbles across all Android launchers.
  - Hotspots: Tap time for Clock/Alarm, tap event for Calendar, tap card for Widget Customizer.
  - Minute-by-minute exact alarm updates with battery-conscious background scheduling.

- **In-App Customization Suite**:
  - **Themes**: Blueprint Grid, AMOLED Dark, Signature MicroNothing Red (`#D71920`), Frosted Glass, and Floating Face.
  - **Glassmorphism**: Slider to adjust background opacity and frosted glass blur.
  - **Calendar**: Sync live with Android device calendar (`READ_CALENDAR`) or configure custom mock events.
  - **Weather**: Fetch real-time local weather via Open-Meteo or set custom values.
  - **Quick Pinning**: One-tap "Add to Home Screen" shortcut.

---

## 🛠 Project Structure

- `app/src/main/java/com/micro/nothingclock/`
  - `MainActivity.kt`: Main launcher with live Compose preview and customizer.
  - `ui/screens/ClockPreviewScreen.kt`: Interactive customizer and widget manager.
  - `widget/NothingClockRenderer.kt`: Core drawing engine for the MicroNothing clock face.
  - `widget/NothingClockWidgetProvider.kt`: Android homescreen widget lifecycle & intents.
  - `widget/WidgetAlarmReceiver.kt`: Background exact alarm receiver for clock ticks.
  - `data/CalendarRepository.kt`: Device calendar queries.
  - `data/WeatherRepository.kt`: Weather API client.
  - `data/WidgetPreferences.kt`: User settings persistence.

---

## 🚀 How to Run in Android Studio

1. Open **Android Studio**.
2. Select **Open** and choose this directory (`MicroNothing Clock`).
3. Allow Gradle to sync dependencies.
4. Select your connected Android device or emulator (Android 8.0+ / API 26+).
5. Click **Run ▶**!
