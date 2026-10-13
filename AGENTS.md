# AGENTS.md

N Player is a standalone Android media player: it plays the video and audio already on
the device. There is no server, account, or network layer, and the app does not request
the `INTERNET` permission.

## Layout

- `NPlayer/` — the Gradle project (open this directory, not the repo root).
  - `app/src/main/java/com/example/nplayer/` — Kotlin sources.
    - `data/media/` — `MediaStoreRepository` (scan), `MediaLibrary` (in-memory cache),
      `MediaItem` (domain model keyed by `content://` URI).
    - `data/prefs/` — `AppSettingsStore` (theme, biometric lock, favourites),
      `PlaybackPositionStore` (resume positions), `ThemeMode`.
    - `playback/` — `PlaybackService` (`MediaSessionService`), `PlaybackController`.
    - `ui/` — Compose: `theme/`, `navigation/`, `components/`, and one package per screen.
  - `app/src/test/` — JVM unit tests (no device required).
- `docs/` — SVG diagrams and screen mockups referenced by `README.md`.
- `docs/install/index.html` — the GitHub Pages download landing page.

## Build and test

Requires JDK 17+ (CI uses 21) and an Android SDK with platform 35 and build-tools 35.
Point Gradle at the SDK with `ANDROID_HOME` or `NPlayer/local.properties`
(`sdk.dir=/path/to/android-sdk`).

```bash
cd NPlayer
./gradlew :app:assembleDebug        # debug APK
./gradlew :app:testDebugUnitTest    # unit tests
./gradlew :app:assembleRelease      # minified release APK (R8 + resource shrinking)
```

CI (`.github/workflows/android.yml`) runs `testDebugUnitTest` then `assembleDebug` on
every push and pull request. `release.yml` builds and publishes the debug APK as a
release asset on a `v*` tag.

## Conventions

- Only add a dependency when it earns its place. The dependency set is deliberately
  small: Media3, DataStore, Coil (+ `coil-video` for video thumbnails), biometric, Hilt.
- Keep Android-framework types out of anything meant to be unit-tested; the pure helpers
  in `data/media/MediaItem.kt` are the model for that.
- A `content://` URI is the contract between the library, playback, and thumbnails. Do
  not introduce a second identity for an item.
- `README.md` claims must match the code. If you change behaviour (permissions, theme
  options, screens), update the README and the relevant `docs/*.svg` in the same change.
