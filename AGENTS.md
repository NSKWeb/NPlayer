# AGENTS.md

N Player is a standalone Android media player: it plays the video and audio already on
the device. There is no server, account, or network layer, and the app does not request
the `INTERNET` permission.

## Layout

- `NPlayer/` — the Gradle project (open this directory, not the repo root).
  - `app/src/main/java/com/example/nplayer/` — Kotlin sources.
    - `data/media/` — `MediaStoreRepository` (scan), `MediaLibrary` (in-memory cache),
      `MediaItem` (domain model keyed by `content://` URI), `FolderTree` (pure tree logic).
    - `data/prefs/` — `AppSettingsStore` (theme, biometric lock, favourites, private
      folder), `PlaybackPositionStore` (resume positions), `ThemeMode`.
    - `data/playlists/` — `Playlist` (model), `PlaylistCodec` (pure JSON), `PlaylistStore`.
    - `playback/` — `PlaybackService` (`MediaSessionService`), `PlaybackController`,
      `EqualizerController` (platform equalizer bound to the player's audio session).
    - `ui/` — Compose: `theme/`, `navigation/`, `components/`, and one package per screen.
      `ui/playlists/` and `ui/privatefolder/` hold their own screen + view model.
  - `app/src/test/` — JVM unit tests (no device required). Robolectric tests exercise the
    real repository and library against a fake MediaStore; see `MediaStoreRepositoryTest`
    and `PrivateFolderTest`.
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
every push and pull request. `release.yml` builds `:app:assembleRelease` and publishes
that minified APK as a release asset on a `v*` tag — the published download is the
release build, not debug, because it is several times smaller and faster. Release
signing falls back to the debug key when `keystore.properties` is absent, so the asset
is installable without a keystore.

### Player controls

`ui/player/PlayerScreen.kt` is a Compose overlay over a `PlayerView` with
`useController = false`; the stock Media3 controller is not used. All player state and
the gesture handlers live in `ui/player/PlayerViewModel.kt`. Pure, unit-tested helpers
(enums, speed stepping, clock formatting, A–B model) are in
`playback/PlayerSettings.kt` — put new pure logic there, not in the composable. The
quick-jump steps and their clamping live in `playback/SeekSteps.kt` (`SeekStepsTest`):
the overlay's row of +20m/+40m/−20m/−40m buttons and the ±10 s buttons all route
through `PlayerViewModel.seekBy`, which clamps to the media bounds.

### Theme

`ui/theme/Theme.kt` owns the palette and typography. Dynamic colour is **off** by
default: the app keeps its own near-black/blue identity unless the user turns on
"Match my wallpaper" in Settings. `AppSettings.dynamicColor` is read in `MainActivity`
and passed to `NPlayerTheme`; the toggle only shows on API 31+.

### Background behaviour

`PlaybackService` is a foreground service, so it keeps running while audio plays — that
is intended. It stops itself when playback ends with no next item and repeat off
(`idleStopListener`), and on task removal when nothing is playing. Do not make it stop
on every task removal: that would kill background audio, which is the service's purpose.

Built so far from the Arc-Player-parity list: playlists (a playlist plays as one
Media3 queue), picture-in-picture, the equalizer, and the private folder. Still open:
the subtitle downloader (needs `INTERNET`, which the app deliberately does not request)
and FFmpeg for RMVB/WMV/DivX.

### Folders

`data/media/FolderTree.kt` holds the pure tree logic — path normalisation, storage-root
stripping, child listing with recursive counts, and breadcrumbs — and is unit-tested in
`FolderTreeTest`. Keep it free of Android types. `MediaStoreRepository` supplies the
containing folder from `RELATIVE_PATH` on API 29+, and falls back to the legacy absolute
`DATA` column before that, so older devices still get a real tree. The projection guards
on column presence so a device that lacks both columns degrades to the root instead of
throwing.

## Conventions

- Only add a dependency when it earns its place. The dependency set is deliberately
  small: Media3, DataStore, Coil (+ `coil-video` for video thumbnails), biometric, Hilt.
- Keep Android-framework types out of anything meant to be unit-tested; the pure helpers
  in `data/media/MediaItem.kt` are the model for that.
- A `content://` URI is the contract between the library, playback, and thumbnails. Do
  not introduce a second identity for an item.
- Never block the main thread on `PlaybackController.connect()`'s future. Binding the
  `MediaSessionService` completes on the main thread, so `.get()` there deadlocks and
  playback silently never starts. Use `connectAndAwait()`, which suspends instead.
- The private folder is a visibility filter, not a second store. Keys live in
  `AppSettingsStore.privateItems`; `MediaLibrary` withholds them from `videos`, `audio`,
  `itemsIn`, `foldersIn`, `search`, and `findVisible`. Only the gated private screen reads
  `allItems` or `find`. Any new listing must filter through `visible()` so a hidden item
  cannot leak back in through a favourite or playlist entry.
- `README.md` claims must match the code. If you change behaviour (permissions, theme
  options, screens), update the README and the relevant `docs/*.svg` in the same change.
