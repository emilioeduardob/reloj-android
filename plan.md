# Plan: Reloj Android — LaMetric-style Smart Clock

> Turn an old Android phone into an always-on, pixel-art dashboard that shows clock, Asunción weather, and USD/PYG exchange rate. Configure everything from a web UI served by the phone itself.

---

## 1. Goal

Build a single Android app that:

1. Runs continuously on an old Android phone as a desk clock / info display.
2. Shows **LaMetric-style pixel art visuals**: a grid of colored "LED" dots/pixels on a black background.
3. Rotates through configurable **faces**: Clock, Weather for Asunción, USD→PYG exchange rate.
4. Exposes a **web configuration UI** hosted on the phone at `http://<phone-ip>:8080`.
5. Keeps the **screen always on** while the app is in the foreground.
6. Is designed so new faces (news, X, Reddit, etc.) can be added later with minimal changes.

---

## 2. Decisions

| Decision | Value |
|----------|-------|
| **Target SDK** | `minSdk = 34` (Android 14). The old phone runs Android 14. |
| **Authentication** | None for MVP. The server is intended for a trusted home LAN only. |
| **Weather location** | Asunción, Paraguay (lat/lon fixed; can be overridden via settings later if desired). |
| **Web server port** | `8080`. |
| **Orientation** | Landscape only. |
| **Language / framework** | Kotlin + Jetpack Compose + Ktor embedded server. |

---

## 3. Technology Stack

| Layer | Choice | Reason |
|-------|--------|--------|
| **Language** | Kotlin | Modern, official, concise, coroutines. |
| **UI** | Jetpack Compose | A single `Canvas` composable is ideal for drawing a pixel grid. |
| **Embedded server** | Ktor (CIO engine) | Pure Kotlin, small footprint, easy JSON routes. |
| **Persistence** | DataStore (Preferences) | Simple key/value storage for settings. |
| **HTTP client** | Ktor Client or Retrofit + OkHttp | For weather and exchange-rate APIs. |
| **Concurrency** | Kotlin Coroutines / Flow | For face rotation, API polling, and reacting to settings. |

### Why native Android over cross-platform?

- Full control over wake locks, fullscreen immersive mode, and battery optimizations.
- Better performance on old hardware.
- Ktor integrates naturally with Kotlin data classes; no runtime bridge needed.

---

## 4. Visual Design (LaMetric-style)

LaMetric TIME is a rectangular dot-matrix display with:

- Black background.
- Bright, blocky pixels (rounded squares).
- Pixelated fonts and simple icons.
- Horizontal scrolling for text that does not fit.
- High contrast colors.

### Implementation

- Fixed virtual pixel grid, e.g. **32 rows × 64 columns**.
- Each virtual pixel rendered as a rounded rectangle on a Canvas.
- `PixelMatrix` data class represents the grid state.
- 3×5 or 5×7 pixel font maps for digits and letters.
- Optional subtle glow around lit pixels for an LED feel.

---

## 5. Architecture

```
┌─────────────────────────────────────────────┐
│                 MainActivity                │
│  (landscape, immersive fullscreen, keep on) │
└────────────────────┬────────────────────────┘
                     │
┌────────────────────▼────────────────────────┐
│           PixelClockScreen (Compose)        │
│          draws the virtual pixel grid       │
└────────────────────┬────────────────────────┘
                     │
        ┌────────────┼────────────┐
        ▼            ▼            ▼
┌──────────────┐ ┌──────────┐ ┌──────────────┐
│ FaceEngine   │ │  Ktor    │ │  Settings    │
│ (rotates     │ │  Server  │ │  Repository  │
│  faces)      │ │  :8080   │ │  (DataStore) │
└──────┬───────┘ └────┬─────┘ └──────┬───────┘
       │              │              │
       ▼              ▼              ▼
┌─────────────────────────────────────────────┐
│              Face Plugins (interface)       │
│  ClockFace  WeatherFace  ExchangeFace  ...  │
└─────────────────────────────────────────────┘
```

### Core abstractions

```kotlin
// Virtual pixel grid passed to the renderer
data class PixelMatrix(
    val width: Int,
    val height: Int,
    val pixels: List<Color> // size = width * height
)

// Every face implements this
interface Face {
    val id: String
    val name: String
    suspend fun render(settings: Settings): PixelMatrix
    suspend fun isAvailable(settings: Settings): Boolean
}

// Settings persisted via DataStore
data class Settings(
    val enabledFaces: List<String>,
    val rotationSeconds: Int,
    val weatherCity: String,      // default: "Asunción"
    val weatherLat: Double,       // default: -25.2867
    val weatherLon: Double,       // default: -57.3333
    val exchangeSource: String,   // default: "dolarpy"
    val serverPort: Int           // default: 8080
)
```

### Face rotation

- `FaceEngine` emits a new `PixelMatrix` every `rotationSeconds`.
- A face can also animate within its turn (e.g. scrolling text) by emitting frames at a fixed FPS.
- Engine loops: `enabledFaces` → render each for N seconds → repeat.

---

## 6. Faces

### MVP faces

| Face | Data source | Notes |
|------|-------------|-------|
| **Clock** | Device clock | Show HH:MM, optional seconds, date. |
| **Weather** | [Open-Meteo](https://open-meteo.com) | Default coordinates for Asunción, PY. Show temperature and small icon. |
| **Dólar PYG** | [DolarPy API](https://github.com/melizeche/dolarpy) | Show USD → PYG buy/sell rate. |

### Future faces

| Face | Possible source |
|------|-----------------|
| News | RSS feed / NewsAPI |
| X (Twitter) | Official API or alternative |
| Reddit | Reddit JSON endpoints |
| Spotify now playing | Spotify Web API |
| Custom message | User types text in web UI |

Adding a new face means implementing the `Face` interface and registering it in a central list. The web UI discovers available faces automatically.

---

## 7. Web Configuration UI

### Embedded server

- Run **Ktor CIO** on port `8080`.
- Serve a small static HTML/JS control panel from `assets/web/`.
- Expose JSON REST endpoints.

### API endpoints

```
GET  /api/settings          → current settings
POST /api/settings          → update settings
GET  /api/faces             → list all faces with id/name/enabled
GET  /api/status            → device IP, uptime, current face
GET  /api/preview           → optional snapshot of current matrix
```

### Control panel (served at `/`)

- Toggle switches for each face.
- Slider for rotation interval (5s – 5min).
- Display current server IP and port.
- Live preview of current display.
- Advanced: optional weather city/lat/lon override.

### Security

No authentication in MVP. Intended for trusted home LAN only.

---

## 8. Power & Display Management

### Keep screen always on

```kotlin
WindowCompat.setKeepScreenOn(window, true)
```

Backup wake lock in case the OS tries to doze:

```kotlin
val wakeLock = powerManager.newWakeLock(
    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
    "RelojAndroid::ClockWakeLock"
)
```

### Fullscreen / kiosk

- Hide status bar and navigation bar with `WindowInsetsController`.
- Sticky immersive mode.
- Landscape-only in `AndroidManifest.xml`.

### Battery optimization

- Request `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
- Recommend keeping phone plugged in; optionally show a charging indicator.

---

## 9. Project Structure

```
reloj-android/
├── app/
│   ├── src/main/java/com/example/relojandroid/
│   │   ├── MainActivity.kt
│   │   ├── RelojApplication.kt
│   │   ├── di/
│   │   │   └── AppModule.kt
│   │   ├── data/
│   │   │   ├── SettingsRepository.kt
│   │   │   ├── WeatherApi.kt
│   │   │   └── ExchangeApi.kt
│   │   ├── engine/
│   │   │   ├── FaceEngine.kt
│   │   │   ├── PixelMatrix.kt
│   │   │   └── Face.kt
│   │   ├── faces/
│   │   │   ├── ClockFace.kt
│   │   │   ├── WeatherFace.kt
│   │   │   └── ExchangeFace.kt
│   │   ├── server/
│   │   │   ├── WebServer.kt
│   │   │   └── routes/
│   │   └── ui/
│   │       ├── PixelClockScreen.kt
│   │       └── theme/
│   ├── src/main/assets/
│   │   └── web/               # static control panel files
│   └── src/main/AndroidManifest.xml
├── gradle/libs.versions.toml
├── build.gradle.kts
└── plan.md
```

---

## 10. Implementation Roadmap

### Milestone 0 — Scaffold

- Create Android project with Kotlin + Compose.
- `minSdk = 34`, `targetSdk = 34`, landscape-only in manifest.
- Add dependencies: Compose, Ktor server (CIO), Ktor client, DataStore, Coroutines.

### Milestone 1 — Pixel renderer

- Build `PixelMatrix` and Compose `Canvas` that draws rounded rectangles.
- Add a 3×5 / 5×7 pixel font utility.
- Render a static clock face as proof of concept.

### Milestone 2 — Face engine

- Define `Face` interface.
- Implement `ClockFace`.
- Build `FaceEngine` that rotates faces every N seconds using Flow.
- Wire engine to UI.

### Milestone 3 — Settings persistence

- DataStore `SettingsRepository`.
- Defaults: enabled faces = all, rotationSeconds = 10, weather = Asunción, port = 8080.
- Make UI react to settings changes.

### Milestone 4 — Weather & Exchange faces

- Integrate Open-Meteo with Asunción coordinates.
- Integrate DolarPy API.
- Add pixel icons (sun, cloud, dollar sign).

### Milestone 5 — Embedded web server

- Start Ktor server in Application/Service on port 8080.
- Serve static web UI from assets.
- Implement REST endpoints for settings and faces.
- Add live preview endpoint.

### Milestone 6 — Always-on & polish

- Keep screen on, fullscreen immersive.
- Handle battery optimizations.
- Add error state rendering (e.g. "NO NET").
- Add brightness control via settings.

### Milestone 7 — Future faces

- Add RSS/News face.
- Add Reddit face.
- Investigate X integration limits.

---

## 11. Suggested Next Step

Generate the Android project scaffold (Gradle files, `MainActivity.kt`, dependencies) and implement the pixel renderer with a working clock face. Once that renders correctly, add the face engine and settings persistence before moving to networking and the web server.
