# Reloj Android

Turn an old Android phone into an always-on, LaMetric-style smart clock that
shows the time, Asunción weather, and the USD → PYG exchange rate. Everything is
configurable from a web UI served by the phone itself.

![Virtual pixel grid](app/src/main/res/drawable/ic_launcher_foreground.xml)

## Features

- **LaMetric-style pixel display** — 64×32 virtual LED grid rendered with
  rounded, glowing pixels on a black background.
- **Rotating faces**
  - Clock with date
  - Weather for Asunción, Paraguay (Open-Meteo)
  - USD → PYG exchange rate (DolarPy)
- **Web config UI** at `http://<phone-ip>:8080`
  - Toggle faces
  - Set rotation interval
  - Override weather city / coordinates
  - Live preview of the current display
- **Always-on, fullscreen, landscape** — keeps the screen awake and hides system
  bars.
- **Extensible** — new faces are added by implementing a single interface.

## Requirements

- Android 14 (API 34) or newer
- Landscape orientation
- Recommended: phone plugged in and battery optimization disabled

## Build

```bash
./gradlew assembleDebug
```

Install on a connected device:

```bash
./gradlew installDebug
```

## Configure

1. Open the app on the phone.
2. Find the phone’s IP on your Wi-Fi network.
3. Visit `http://<phone-ip>:8080` from another device on the same network.
4. Toggle faces, change the rotation interval, or adjust weather location.

## REST API

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/settings` | Current settings |
| POST | `/api/settings` | Update settings |
| GET | `/api/faces` | List available faces |
| GET | `/api/status` | Current face and server port |
| GET | `/api/preview` | Current pixel grid as hex colors |

## Architecture

```
MainActivity
  └── PixelClockScreen (Compose Canvas)
        └── FaceEngine
              ├── ClockFace
              ├── WeatherFace
              └── ExchangeFace
SettingsRepository (DataStore)
WebServer (Ktor :8080)
```

## Extending

Add a new face:

1. Implement `Face` in `app/src/main/java/com/example/relojandroid/faces/`.
2. Register it in `AppModule.provideFaces()`.
3. The web UI will discover it automatically via `/api/faces`.

See `AGENTS.md` for developer notes and `plan.md` for the original design.

## License

MIT — feel free to hack on it.
