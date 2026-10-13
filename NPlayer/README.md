# N Player

A native Android media player and client for **your own Jellyfin server**.

N Player connects to a Jellyfin instance you run and control, then lets you browse
your libraries, stream or download your media, and pick up where you left off.
It ships no content of its own and talks only to the server URL you enter.

## Do I need an API key?

No. Jellyfin does not use API keys for normal client access.

Authentication is two pieces of information you already have:

1. **Server URL** — e.g. `https://jellyfin.example.com` or `http://192.168.1.10:8096`
2. **Username + password** — your normal Jellyfin account

On first launch the app probes the server (`GET /System/Info/Public`), then calls
`POST /Users/AuthenticateByName`. The server returns an **access token** that the
app stores and sends with every later request. That token is issued to you by
your own server at login; you never create or paste a key.

The only "API key" you might generate yourself is a Jellyfin *server* API key
(Dashboard → API Keys) — that is for server-side integrations and is **not**
required by this app.

## What it does

- **Connect** — enter your server URL, sign in, session persists across restarts.
- **Browse** — libraries, "latest" rails, and item detail with artwork and synopsis.
- **Search** — server-side search across movies, series, and episodes.
- **Play** — Media3/ExoPlayer playback with direct play when the container allows,
  otherwise an HLS transcode; resume position is reported back to Jellyfin so
  "Continue Watching" stays in sync across devices.
- **Download** — keep items offline via the Media3 download manager, with progress
  surfaced in the app.
- **Lock** — optional biometric gate over the whole app.
- **Sync** — a periodic WorkManager job refreshes the local library cache.

## Architecture

```
app/src/main/java/com/example/nplayer/
├── data/
│   ├── local/        Room database (cached items, downloads) + DataStore positions
│   ├── prefs/        Session + device id
│   ├── remote/       Retrofit Jellyfin API, interceptors, DTOs
│   └── repository/   Auth + library repositories, ApiResult wrapper
├── di/               Hilt modules
├── downloads/        Media3 DownloadManager, download service, notifications
├── playback/         MediaSessionService, playback controller, URL builders
├── sync/             Library sync worker + scheduler
└── ui/               Jetpack Compose screens (auth, home, detail, search,
                      downloads, settings, player) + theme
```

Stack: Kotlin, Jetpack Compose (Material 3), Hilt, Room, DataStore, Retrofit/OkHttp,
Media3 (ExoPlayer + session + offline downloads), WorkManager, Coil, BiometricPrompt.

## Build

Requirements: JDK 17+, Android SDK with platform 35 and build-tools 35.

```bash
export ANDROID_HOME=/path/to/android-sdk
./gradlew :app:assembleDebug        # debug APK
./gradlew :app:testDebugUnitTest    # unit tests
./gradlew :app:assembleRelease      # minified release APK
```

Debug output: `app/build/outputs/apk/debug/app-debug.apk`.

### Signing a release

Create `keystore.properties` in the project root (git-ignored):

```properties
storeFile=/absolute/path/to/keystore.jks
storePassword=…
keyAlias=…
keyPassword=…
```

When the file is absent the release APK is produced unsigned, so local builds and
CI work without secrets.

## Scope and intent

N Player is a client for media you are entitled to access on a Jellyfin server you
operate. It intentionally contains no third-party streaming endpoints, no device
identity spoofing, and no remote kill-switch or hidden host delivery.
