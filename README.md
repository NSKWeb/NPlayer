<div align="center">

# N Player

**A native Android media player and client for the Jellyfin server you run.**

[![Release](https://img.shields.io/github/v/release/NSKWeb/NPlayer?color=9ECAFF)](https://github.com/NSKWeb/NPlayer/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Kotlin · Jetpack Compose · Media3 · Hilt · Room · min SDK 26

</div>

---

N Player connects to a Jellyfin instance you host and control. It browses your
libraries, streams or downloads your media, and keeps "Continue watching" in sync
across your devices. It ships no content and talks only to the server URL you enter.

## Do I need an API key?

**No.** Jellyfin does not use API keys for client access.

You supply two things you already have:

1. **Server URL** — e.g. `https://jellyfin.example.com` or `http://192.168.1.10:8096`
2. **Username and password** — your normal Jellyfin account

At sign-in the app calls `POST /Users/AuthenticateByName`. Your server verifies the
credentials and returns an **access token**, which the app stores and attaches to
every later request. You never create or paste a key.

The only key you could generate yourself is a Jellyfin *server* API key
(Dashboard → API Keys). That is for server-side integrations and is **not** used here.

<div align="center">
  <img src="docs/auth-flow.svg" alt="Authentication flow: launch, session check, auth screen, AuthenticateByName, token persisted to DataStore" width="900">
</div>

## Screens at a glance

| Screen | What it does | State source |
|---|---|---|
| **Auth** | Enter server URL and credentials; URL is normalised to `https://` when the scheme is missing | `AuthViewModel` |
| **Home** | "Recently added" rail over a library grid of posters | `HomeViewModel` |
| **Detail** | Backdrop, title, year, runtime, synopsis, favourite toggle, Play | `DetailViewModel` |
| **Search** | Server-side search, debounced 300 ms, minimum two characters | `SearchViewModel` |
| **Downloads** | Live progress for everything kept offline | `DownloadsViewModel` |
| **Player** | Media3 `PlayerView` inside Compose, with a back affordance | `PlayerViewModel` |
| **Settings** | Server and account info, biometric lock toggle, sign out | `SettingsViewModel` |

The bottom bar carries Home, Search, Downloads, and Settings. Detail and Player are
full-screen destinations pushed on top, which is why the bar hides for them.

## Architecture

Four layers, one direction of dependency. Compose talks to ViewModels; ViewModels
talk to repositories; repositories own the network and persistence. Nothing above
the data layer knows about Retrofit, and nothing below the UI layer knows about Compose.

<div align="center">
  <img src="docs/architecture.svg" alt="Layered architecture: Compose UI, Hilt ViewModels, repositories, data layer with Retrofit and Room, and Media3 services talking to a Jellyfin server" width="900">
</div>

```
app/src/main/java/com/example/nplayer/
├── data/
│   ├── local/        Room database + DataStore position store
│   ├── prefs/        SessionStore, DeviceIdProvider
│   ├── remote/       JellyfinApi, interceptors, DTOs, Hilt network module
│   └── repository/   Auth + library repositories, ApiResult error mapping
├── di/               Hilt modules (database, DAOs)
├── downloads/        DownloadModule, download service, notifications
├── playback/         PlaybackService, PlaybackController, StreamUrls
├── sync/             LibrarySyncWorker + SyncScheduler
└── ui/               Compose: theme, navigation, components, seven screens
```

### How a request finds the right server

Retrofit is built once against a placeholder origin. The real host is not known
until the user types it, so two OkHttp interceptors do the work at request time:

- **`BaseUrlInterceptor`** rewrites scheme, host, and port onto the stored server
  origin while preserving path and query.
- **`AuthInterceptor`** adds `X-Emby-Authorization` with the client name, device id,
  and — once signed in — the token.
- **`JellyfinImageInterceptor`** does the same for Coil, which fetches artwork
  outside Retrofit.

This is why `JellyfinApi` declares only relative paths. There is no build-time
server baked into the app.

### Error handling

Every network call runs through one helper:

```kotlin
suspend fun <T> apiCall(block: suspend () -> T): ApiResult<T>
```

It maps `HttpException` and `IOException` onto `NETWORK`, `UNAUTHORIZED`, `SERVER`,
or `UNKNOWN`. Screens switch on that instead of catching exceptions, so a dropped
connection shows an inline retry rather than crashing the UI.

## Playback logic

Playback lives behind a Media3 `MediaSessionService`. The Activity and ViewModel
never hold an `ExoPlayer`; they hold a `MediaController` bound to the service. That
is what gives background playback, the media notification, lock-screen controls,
and correct audio focus for free.

<div align="center">
  <img src="docs/playback-sequence.svg" alt="Playback sequence: resolve item, build stream URL, restore resume position, start playback, then report progress every ten seconds" width="900">
</div>

### Direct play versus transcode

`StreamUrls` is a pure Kotlin object, which is what makes it unit-testable without
a device. Given a media source it decides:

| Container | Decision | URL |
|---|---|---|
| `mp4`, `mkv`, `webm`, `m4v`, `mov`, `mp3`, `aac`, `flac`, `ogg`, `opus`, `wav`, `m4a` | **Direct play** the original file | `/Videos/{id}/stream?static=true&mediaSourceId=…` |
| anything else, or no media source | **HLS transcode** | `/Videos/{id}/master.m3u8` with `videoCodec=h264&audioCodec=aac&transcodingProtocol=hls` |

Direct play avoids burning server CPU. Transcoding guarantees something playable
when the container is one ExoPlayer cannot demux.

### Resume position

Resume is stored twice, on purpose:

- **Locally** in DataStore (`nplayer_positions`) so playback resumes instantly and
  offline. Positions under five seconds are dropped rather than stored.
- **On the server** via `Sessions/Playing*`, so "Continue watching" is correct on
  every other Jellyfin client you use.

`PlaybackController` restores the local position as the start position and then
runs a ten-second loop that saves locally and posts `Sessions/Playing/Progress`.
Leaving the player cancels the loop and posts `Sessions/Playing/Stopped` with the
final position.

Media bytes themselves flow through `OkHttpDataSource`, so the same auth headers
that protect API calls also protect the video stream.

## Offline downloads

Downloads use Media3's own manager rather than a hand-rolled downloader, so
retry, resume, and constraint handling come from the platform.

<div align="center">
  <img src="docs/download-pipeline.svg" alt="Download pipeline: request into DownloadManager, bytes into SimpleCache, state mirrored into Room, surfaced through a foreground notification and the downloads screen" width="900">
</div>

Two design choices are worth calling out:

- **The UI observes Room, not Media3.** A `DownloadManager.Listener` mirrors every
  change into the `downloads` table. Screens read a single Flow, so Media3 types
  never leak upward and the list renders from one source of truth.
- **`NoOpCacheEvictor`.** Downloaded media is never silently evicted. The user asked
  to keep it offline, so only an explicit delete removes it.

Downloads default to `NETWORK_UNMETERED` — Wi-Fi only, so a queue cannot quietly
consume mobile data.

## Background sync

A `LibrarySyncWorker` runs every twelve hours, constrained to a connected network,
and writes a lightweight snapshot of the latest sixty items into Room. `pruneOlderThan`
removes cached rows untouched for thirty days, excluding anything downloaded.

The purpose is launch speed and graceful degradation: the app can render a
previously-seen library before the network answers, and something useful when it
does not answer at all.

WorkManager is initialised on demand through `Configuration.Provider`. The manifest
therefore removes `WorkManagerInitializer` from `InitializationProvider` — the
duplicate initialisation is a real lint error, not a warning.

## Design

<div align="center">
  <img src="docs/theme.svg" alt="Colour tokens: dark palette with #0B0B0F background, #9ECAFF primary, and the Material 3 role mapping" width="900">
</div>

### Colour

The app leans dark. The fallback palette sets a near-black canvas (`#0B0B0F`) with a
soft blue primary (`#9ECAFF`), which keeps poster art the brightest thing on screen —
the correct hierarchy for a media browser. `Theme.NPlayer` also paints the window
background black so there is no white flash between the splash and first frame.

On Android 12 and above, **dynamic colour is on by default** and the app inherits the
user's wallpaper palette. The fixed palette is the fallback for older devices, and
Material 3 derives the remaining roles from either source. That means the app looks
native to the device without a theme picker.

### Layout and components

- **`PosterCard`** renders at a 2:3 aspect ratio — the standard poster shape — with a
  two-line clamped title and an optional year subtitle.
- **The home grid** uses `GridCells.Adaptive(minSize = 120.dp)`, so a phone shows two
  or three columns and a tablet shows more, from the same code.
- **`LoadingBox` and `ErrorBox`** are shared, so every screen has identical loading and
  retry behaviour instead of seven different ones.
- **Edge-to-edge** is enabled in `MainActivity`; the `Scaffold` supplies insets, and
  `NavHost` receives the inner padding so content never sits under the system bars.

## Security and privacy

- **Optional biometric lock.** When enabled, `BiometricGate` hides all content behind
  `BiometricPrompt` using `BIOMETRIC_WEAK` or device credential. It is off by default.
- **No device-identity spoofing.** `DeviceIdProvider` reads the platform's own
  `ANDROID_ID` and prefixes it. This is resettable by the user and exists so a user can
  revoke one device from their server dashboard — the opposite of a rotating fingerprint.
- **Cleartext is disabled.** `usesCleartextTraffic="false"` means an `http://` server is
  rejected rather than silently allowed.
- **Credentials stay on device.** Only the server-issued token is stored, in DataStore.

## Repository layout

```
.
├── NPlayer/            Gradle project — open this in Android Studio
│   └── app/src/        Kotlin sources, Compose UI, resources
├── docs/               SVG diagrams used by this README
└── LICENSE
```

## Build

Requirements: JDK 17 or newer (CI uses 21), Android SDK with platform 35 and build-tools 35.

The Gradle project lives in `NPlayer/`, so build from there:

```bash
export ANDROID_HOME=/path/to/android-sdk
cd NPlayer

./gradlew :app:assembleDebug        # debug APK
./gradlew :app:testDebugUnitTest    # unit tests
./gradlew :app:assembleRelease      # minified release APK
```

Debug output lands at `NPlayer/app/build/outputs/apk/debug/app-debug.apk`.

CI builds and tests on every push and pull request, and uploads the test report and
APK as artifacts.

### Tests

`StreamUrlsTest` covers the decision logic that is easiest to get wrong and hardest to
notice: direct play versus transcode, URL normalisation, subtitle endpoints, and the
image-URL guard. Seven cases, no device required, because `StreamUrls` is a pure object
with no Android dependencies.

### Signing a release

Create `keystore.properties` in the project root (git-ignored):

```properties
storeFile=/absolute/path/to/keystore.jks
storePassword=…
keyAlias=…
keyPassword=…
```

When the file is absent the release APK is produced unsigned, so local builds and CI
work without secrets.

## Scope and intent

N Player is a client for media you are entitled to access on a Jellyfin server you
operate. It deliberately contains no third-party streaming endpoints, no device
identity spoofing, and no remote kill-switch or hidden host delivery.

## License

[MIT](LICENSE).
