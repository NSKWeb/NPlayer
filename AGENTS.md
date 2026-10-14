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

`ui/player/PlayerScreen.kt` splits into `VideoPlayerScreen` and `AudioPlayerScreen`; the
top-level `PlayerScreen` owns the lifecycle (`onEnter`/`onLeave`, orientation reset) and
picks the surface from the item's `MediaKind`. Video is a Compose overlay over a
`PlayerView` with `useController = false`; the stock Media3 controller is not used.
`AudioPlayerScreen.kt` is the separate music-player screen (artwork, metadata, transport,
shuffle/repeat, speed, sleep timer, equalizer, up-next queue) and deliberately omits
video-only controls. All player state and the gesture handlers live in
`ui/player/PlayerViewModel.kt`. Pure, unit-tested helpers
(enums, speed stepping, clock formatting, A–B model) are in
`playback/PlayerSettings.kt` — put new pure logic there, not in the composable. The
queue is built by `playback/QueueBuilder.kt` (`QueueBuilderTest`), which turns the item's
folder or playlist into the ordered list handed to Media3, so next/previous and
auto-advance follow the same order. The
quick-jump steps and their clamping live in `playback/SeekSteps.kt` (`SeekStepsTest`):
the overlay's row of +20m/+40m/−20m/−40m buttons and the ±10 s buttons all route
through `PlayerViewModel.seekBy`, which clamps to the media bounds.

Rotating the phone is the fullscreen gesture: `Activity.toggleOrientation()` in
`PlayerScreen.kt` flips between following the device and a locked landscape, and
`Activity.resetOrientation()` runs when the player leaves so the lock never outlives it.

`PlayerView`'s own subtitle view is hidden (`subtitleView.visibility = View.GONE`) and
captions are drawn by the Compose overlay instead, so they stay on screen when the
controls auto-hide. If you re-enable Media3's subtitle rendering, remove the overlay.

### Subtitles

Subtitle detection is name-based and local — no network, no permission beyond the media
read. The rules are pure and unit-tested in `data/media/SubtitleFiles.kt` (matching a
sidecar to a media file, accepted extensions, language tags) and `playback/
SubtitleTracks.kt` (`SubtitleTrack` model and the sidecar-then-embedded list builder).
Keep new matching logic there.

The flow: `MediaStoreRepository.scanSubtitles()` indexes subtitle files by name;
`MediaLibrary.sidecarsFor(item)` returns those in the item's folder that match;
`PlaybackController` attaches them as `SubtitleConfiguration`s and reads embedded text
tracks back out of `currentTracks`. `PlayerViewModel` auto-selects a lone sidecar and
mirrors the active cue text into `PlayerUiState.subtitleText`.

Selection is by Media3 `TrackSelectionParameters` — a text override plus
`setTrackTypeDisabled(TRACK_TYPE_TEXT, true)` to turn subtitles off. A sidecar's track
group id is not stable across Media3 versions, so `selectedSubtitleTrack()` matches
embedded tracks by group id and treats any other selected text track as the sidecar.
When the viewer turns subtitles off, `subtitlesUserDisabled` stops auto-select turning
them back on. `.sub` is intentionally not an accepted extension (usually MicroDVD,
which the extractor cannot parse).

### Theme

`ui/theme/Theme.kt` owns the palette and typography. Dynamic colour is **off** by
default: the app keeps its own near-black/blue identity unless the user turns on
"Match my wallpaper" in Settings. `AppSettings.dynamicColor` is read in `MainActivity`
and passed to `NPlayerTheme`; the toggle only shows on API 31+.

### Release APK size

The release APK is padded to exactly 7,000,000 bytes (7.00 MB) on purpose. A placeholder
asset, `app/src/main/assets/release/placeholder.bin`, holds random bytes, which barely
compress, so its length controls the final APK size. Minification and resource shrinking
stay on, so nothing functional is stripped.

`tools/pad_asset.py` regenerates the asset. To retune after a size drift, add the signed
difference (built APK size minus 7,000,000) to `PAD_TARGET` and rebuild. The last byte is
not reachable exactly because DEFLATE compression is not linear, so the APK settles at
6,999,999 bytes, which is still 7.00 MB.

If you would rather not carry the padding, delete the asset, `tools/pad_asset.py`, and
this section; the release build is otherwise unchanged.

### Background behaviour

`PlaybackService` is a foreground service, so it keeps running while audio plays — that
is intended. It stops itself when playback ends with no next item and repeat off
(`idleStopListener`), and it also stops on task removal (`onTaskRemoved`), so dismissing
the app from recents ends playback and removes the notification. The app does not offer
background playback once it is dismissed; that was a deliberate product decision, so do
not make `onTaskRemoved` keep the player alive.

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

## Thumbnails

- Video artwork goes through Coil's `VideoFrameDecoder` (`coil-video`). By default it
  decodes **frame 0**, and plenty of encodes open on a black fade-in, slate, or encoder
  leader. Against the near-black card that looks exactly like a missing thumbnail. Always
  route card artwork through `ui/components/Components.kt:thumbnailRequest`, which asks
  for a frame a fraction of the way in via `videoFramePercent`. Do not pass a raw
  `item.uri` straight to `AsyncImage` for a video.
- R8 does **not** strip the decoder, so do not go looking for a keep rule first.
  `VideoFrameDecoder$Factory` survives as an obfuscated class and `newImageLoader`
  instantiates it. Verify with `dexdump -d` from build-tools on the release dex, not by
  grepping the dex for the original class name.
- `ImageLoaderFactory` is a Coil interface, not a Hilt one. `NPlayerApplication`
  implements it and must stay `open`; the interface is what makes `Coil.imageLoader()`
  pick up the custom loader, and it is checked at runtime.
- `README.md`'s Screens section embeds the generated store PNGs
  (`docs/store/screenshots/`), not the older `docs/screen-*.svg` mockups. Regenerate the
  PNGs with `docs/store/generate_screenshots.py` after a UI change and keep the two sets
  from diverging.
