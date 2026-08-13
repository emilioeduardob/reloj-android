# Agent Guide: reloj-android

## Project overview

Native Android smart-clock app written in Kotlin + Jetpack Compose. It turns an
old Android phone into an always-on, LaMetric-style pixel dashboard that cycles
through clock, weather, and USD/PYG exchange-rate faces. A built-in Ktor web
server serves a configuration UI on port `8080`.

See `plan.md` for the original product plan.

## Technology stack

- Kotlin 2.0.20
- Jetpack Compose (Canvas-based pixel renderer)
- Android Gradle Plugin 8.5.2 / Gradle 8.7
- minSdk / targetSdk / compileSdk = 34
- Ktor server (CIO) + Ktor client (CIO)
- DataStore Preferences for settings
- Kotlinx Serialization

## Project layout

```
app/src/main/java/com/example/relojandroid/
├── MainActivity.kt              # Fullscreen landscape activity
├── RelojApplication.kt          # Application: starts engine + web server
├── AppModule.kt                 # Simple face registry
├── data/
│   ├── Settings.kt              # Serializable settings data class
│   ├── SettingsRepository.kt    # DataStore persistence
│   ├── WeatherApi.kt            # Open-Meteo client
│   └── ExchangeApi.kt           # DolarPy (dolar.melizeche.com) client
├── engine/
│   ├── PixelMatrix.kt           # 64x32 virtual LED grid
│   ├── PixelFont.kt             # 3x5 pixel font extensions
│   ├── Face.kt                  # Face plugin interface
│   └── FaceEngine.kt            # Rotates enabled faces
├── faces/
│   ├── ClockFace.kt
│   ├── WeatherFace.kt
│   └── ExchangeFace.kt
├── server/
│   └── WebServer.kt             # Ktor server + REST routes
└── ui/
    ├── PixelClockScreen.kt      # Compose screen + canvas renderer
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

## Common extension points

- New face: implement `Face`, register in `AppModule`, add toggle in web UI
  (it auto-discovers).
- New settings field: add to `Settings.kt`, persist in `SettingsRepository`,
  expose in `WebServer` routes, and add a control in `index.html`.
- Weather source: replace or extend `WeatherApi`.
- Exchange source: replace or extend `ExchangeApi`; the default provider key is
  `"bcp"`.
