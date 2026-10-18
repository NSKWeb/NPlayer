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
      folder), `PlaybackSettingsStore` (player preferences: resume mode, seek step,
      orientation, overlay, subtitles, language, file arrangement), `PlaybackPositionStore`
      (resume positions), `ThemeMode`.
    - `data/media/SortOrder.kt` — the `FileSortOrder`/`FolderSortOrder` enums and their
      comparators. `MediaLibrary` mirrors the chosen order into an `Arrangement` flow and
      applies it in `videos`, `audio`, `itemsIn`, `foldersIn` and `search`.
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
release build, not debug, because it is several times smaller and faster.

### Signing

`app/build.gradle.kts` reads `keystore.properties` from the **project root** and signs
the release build with the release keystore when it is present, falling back to the
debug key otherwise so a fresh clone still builds and installs.

`storeFile` must resolve via `rootProject.file(...)`, not `file(...)`. The properties
file lives at `NPlayer/keystore.properties` while the module is `NPlayer/app`, so a bare
`file()` resolves inside `app/` and `validateSigningRelease` fails with *keystore file
not found*. This was a real bug: the config looked correct but never worked.

`release.keystore` and `keystore.properties` are git-ignored. CI supplies the same key
through repository secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
`KEY_PASSWORD`); `release.yml` decodes it, writes the properties file, and then
**fails the job if the built APK is debug-signed**, so a release cannot silently ship
with the wrong key.

The signature is the app's identity: an APK signed with one key cannot update an install
of another. Releases up to `v1.1.7` used the debug key, so the first release-signed
build needs an uninstall before it will install.

### Release APK size

The release APK carries a padding asset, `app/src/main/assets/release/placeholder.bin`.
It is not there to hit an exact byte count — the padding is left at its committed size
and the APK lands wherever it lands (about 7.05 MB). Do not tune it to a round number.

The asset holds high-entropy bytes, so it deflates to roughly 76% of its raw length
rather than compressing away, which is what makes it a usable size knob. It is *not*
incompressible — the earlier claim that it "barely compresses" was wrong. Minification
and resource shrinking stay on, so nothing functional is stripped.

`tools/pad_asset.py` regenerates the asset. To retune after a size drift, add the signed
difference (built APK size minus `PAD_TARGET`) to `PAD_TARGET` and rebuild. The last byte
is not reachable exactly because DEFLATE compression is not linear.

If you would rather not carry the padding, delete the asset, `tools/pad_asset.py`, and
this section; the release build is otherwise unchanged.

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

### Overlays over the video

`PlayerView` draws into a `SurfaceView`, which is composited by the system, not by
Compose. Anything Compose draws on top of it must go through `Modifier.background`
(or another draw modifier), because that writes into the window's own display list
and blends with the video normally.

A Material3 `Surface` is the exception that bites: it paints its colour inside a
render layer (`graphicsLayer`), and a translucent layer over a `SurfaceView` does not
blend with the video, so a scrim built from `Surface` renders see-through. The caption
scrim and the gesture hint both hit this; both now use `Modifier.background` on a `Box`,
matching the night-mode dim. Keep new overlays on that path.

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
- Player preferences live in `PlaybackSettingsStore`, separate from `AppSettingsStore`.
  A setting that only the player consults (resume mode, seek step, orientation, overlay,
  gestures, subtitle/audio language, file arrangement) belongs there; app-wide things
  (theme, biometric lock, favourites, private folder) stay in `AppSettingsStore`.
- The release APK is padded to ~7 MB by `app/src/main/assets/release/placeholder.bin`,
  regenerated with `tools/pad_asset.py` (`PAD_TARGET` is the raw byte count before
  base64). Adding code shrinks the placeholder's effective headroom, so retune it when
  the size drifts: build, compare the APK against 7,000,000, and adjust `PAD_TARGET` by
  the signed difference. The asset is regenerated from a fixed seed, so a given
  `PAD_TARGET` is reproducible; note that a 1-byte change to the raw count often does not
  change the base64 file size (it rounds to 3-byte groups).

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

## Seeking

Media3 reports `Player.getDuration()` as `C.TIME_UNSET` (a negative value) until the item
is prepared. Both halves of the video-seek bug came from treating that as a usable
number:

- `seekBy()` used to return early when the duration was unset, so a press of the ±10 s
  button made in the seconds after a video opened was **discarded silently** — no seek,
  no error. It now seeks straight away and lets `seekTarget()` apply the lower bound
  always and the upper bound only when a length exists.
- `dragSeekDelta()` scaled the swipe by the duration, which is 0 in that window, so
  `dx × 0` made the slide cover nothing. It falls back to `DRAG_SEEK_FALLBACK_MILLIS`
  (90 s) when the duration is missing, so the gesture always moves playback.

Neither symptom was recoverable by the user short of waiting or reopening the file, which
is why the bug read as "seeking is broken" rather than "seeking is briefly unavailable".
Guard against `duration <= 0` by *deferring or falling back*, never by dropping the
action. `Seek.kt` holds the pure maths and `SeekTest.kt` covers both cases.

The resume retry loop (`applySeekWhenReady`) is bounded to 100 × 100 ms. A file whose
length never arrives leaves the seek unapplied rather than polling for the life of the
screen.
