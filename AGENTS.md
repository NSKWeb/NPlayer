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

Both variants are emitted as `app/build/outputs/apk/<variant>/NPlayer.apk`, not the AGP
default `app-<variant>.apk`. That name is the release asset name, so the stable
`releases/latest/download/NPlayer.apk` link, the README, the install page, and the QR
code (`docs/qr-apk.svg`) all depend on it. If you rename the artifact, update those too
and regenerate the QR.

CI (`.github/workflows/android.yml`) runs `testDebugUnitTest` then `assembleDebug` on
every push and pull request. `release.yml` builds and publishes the debug APK as a
release asset on a `v*` tag.

### Player controls

`ui/player/PlayerScreen.kt` is a Compose overlay over a `PlayerView` with
`useController = false`; the stock Media3 controller is not used. All player state and
the gesture handlers live in `ui/player/PlayerViewModel.kt`. Pure, unit-tested helpers
(enums, speed stepping, clock formatting, A–B model) are in
`playback/PlayerSettings.kt` — put new pure logic there, not in the composable.

Remaining Arc-Player-parity work, not yet started: equalizer, picture-in-picture,
playlists, folder/SD-card browsing, private folder, subtitle downloader (needs
`INTERNET`, which the app deliberately does not request), and FFmpeg for RMVB/WMV/DivX.

## Conventions

- Only add a dependency when it earns its place. The dependency set is deliberately
  small: Media3, DataStore, Coil (+ `coil-video` for video thumbnails), biometric, Hilt.
- Keep Android-framework types out of anything meant to be unit-tested; the pure helpers
  in `data/media/MediaItem.kt` are the model for that.
- A `content://` URI is the contract between the library, playback, and thumbnails. Do
  not introduce a second identity for an item.
- `README.md` claims must match the code. If you change behaviour (permissions, theme
  options, screens), update the README and the relevant `docs/*.svg` in the same change.
