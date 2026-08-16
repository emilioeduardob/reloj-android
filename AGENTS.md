# Agent Guide: reloj-android

## Project overview

Native Android smart-clock app written in Kotlin + Jetpack Compose. It turns an
old Android phone into an always-on, LaMetric-style pixel dashboard that cycles
through clock, weather, calendar, USD/PYG exchange-rate, and daily kanji faces.
A built-in Ktor web server serves a configuration UI on port `8080`.

See `plan.md` for the original product plan.

## Technology stack

- Kotlin 2.2.10
- Jetpack Compose (Canvas-based pixel renderer)
- Android Gradle Plugin 9.2.1 / Gradle 9.4.1
- minSdk / targetSdk = 34, compileSdk = 35
- Ktor 3.x server (CIO) + Ktor 3.x client (CIO)
- DataStore Preferences for settings
- Kotlinx Serialization

## Project layout

```
app/src/main/java/com/example/relojandroid/
├── MainActivity.kt              # Fullscreen landscape activity + gesture host
├── RelojApplication.kt          # Application: starts engine + web server
├── AppModule.kt                 # Face registry + icon repository provider
├── data/
│   ├── Settings.kt              # Serializable settings data class
│   ├── SettingsRepository.kt    # DataStore persistence
│   ├── WeatherApi.kt            # Open-Meteo client
│   ├── ExchangeApi.kt           # DolarPy (dolar.melizeche.com) client
│   ├── KanjiApi.kt              # kanjiapi.dev client
│   ├── IconRepository.kt        # LaMetric icon cache + download manager
│   ├── LaMetricIcon.kt          # LaMetric icon data models
│   └── LaMetricIconApi.kt       # LaMetric icon catalog client
├── engine/
│   ├── PixelMatrix.kt           # Virtual LED grid (default 37×8)
│   ├── PixelFont.kt             # 3×5 / 5×7 pixel font extensions
│   ├── TextPixelRenderer.kt     # Bitmap-sampled text for any Unicode glyph
│   ├── Face.kt                  # Face plugin interface
│   ├── FaceEngine.kt            # Rotates enabled faces, handles swipe nav
│   └── LaMetricIconAnimator.kt  # Animated icon frame player
├── faces/
│   ├── ClockFace.kt
│   ├── WeatherFace.kt
│   ├── ExchangeFace.kt
│   ├── CalendarFace.kt
│   └── KanjiOfDayFace.kt
├── server/
│   └── WebServer.kt             # Ktor server + REST routes
└── ui/
    ├── PixelClockScreen.kt      # Compose screen + canvas renderer + gestures
    └── theme/
        ├── Theme.kt
        └── Type.kt

app/src/main/assets/web/index.html  # Config panel served at /
```

## Build commands

```bash
./gradlew assembleDebug
./gradlew installDebug            # with a device attached
```

## Important notes for agents

- **Do not commit `android-sdk/`, `.gradle/`, or build outputs.** They are
  already ignored in `.gitignore`.
- The Android SDK used on the original dev machine (a Raspberry Pi) was the
  command-line tools installed locally; CI or other machines should provide
  their own `ANDROID_HOME`.
- AAPT2 in the official Android SDK is x86-64 only. Native ARM64 Linux builds
  require emulation or a remote x86-64 builder.
- The app targets Android 14 (API 34) in landscape mode only.
- Keep the `Face` interface stable when adding new faces; register new faces in
  `AppModule.provideFaces()`.
- The web UI discovers faces via `GET /api/faces`; the backend list is the
  source of truth.
- Network permissions and cleartext traffic are enabled because some external
  APIs (DolarPy) are served over HTTP.
- `gradle.properties` keeps `android.builtInKotlin=false` and `android.newDsl=false`
  because the current Kotlin Android plugin / AGP 9.2.1 combination requires them.
  Do not remove them unless you also migrate the build to AGP's built-in Kotlin
  support.

## Touch interactions

- **Swipe left/right** on the clock screen jumps to the next/previous enabled
  face. `FaceEngine` listens for navigation commands while rendering and skips
  the current turn immediately.
- **Tap** the screen calls `Face.onTap()` on the current face. `KanjiOfDayFace`
  uses this to cycle to the next kanji in the list, overriding the daily
  selection until the list changes or the app restarts.

## Common extension points

- New face: implement `Face`, register in `AppModule`, add toggle in web UI
  (it auto-discovers).
- New settings field: add to `Settings.kt`, persist in `SettingsRepository`,
  expose in `WebServer` routes, and add a control in `index.html`.
- Weather source: replace or extend `WeatherApi`.
- Exchange source: replace or extend `ExchangeApi`; the default provider key is
  `"bcp"`.
- Kanji source: replace or extend `KanjiApi`; the default list is `"joyo"`.
- New touch behavior: implement `Face.onTap()` or add new gestures in
  `PixelClockScreen` and route them through `FaceEngine`.
