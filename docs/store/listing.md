# Play Store listing copy

Ready to paste into Play Console. Character limits are enforced by the console, so
these are written to fit.

## App name (30 max)

```
N Player — local video player
```

## Short description (80 max)

```
Play the videos and music on your phone. No account, no server, no ads.
```

## Full description (4000 max)

```
N Player plays the video and music that is already on your phone. There is no
account to make, no server to point it at, and no feed to scroll. Open it and
your files are there.

IT PLAYS WHAT YOU ALREADY HAVE
Video and audio are read straight from your device through Android's own media
library. Nothing is uploaded, and the app does not request internet access at
all, so it cannot send anything anywhere even in principle.

BUILT FOR LONG FILES
The seek bar shows the time left, so you always know how much of a film is
remaining, and a 10-second skip moves you precisely when you need it.

MADE TO BE EASY ON THE EYES
Rounded artwork cards with the duration on each one, clear section headings,
and a deep, dark palette that lets your thumbnails be the brightest thing on
screen. Prefer your own wallpaper's colours? Turn on Match my wallpaper and
the app takes on your phone's palette instead.

FIND ANYTHING QUICKLY
- Browse by folder and see how many items each one holds, all the way down.
- Search across every video and audio file by name.
- Keep favourites one tap away.
- Build playlists and play them as a queue.

PLAY YOUR WAY
- Picture-in-picture, so a video keeps playing while you use another app.
- A built-in equalizer with presets.
- Variable speed from 0.25x to 4x.
- Shuffle, repeat, and A-B repeat for a section.
- A sleep timer that stops playback when you drift off.
- Night mode, rotation lock, aspect fitting, and a mirror flip.
- Resume from where you stopped, automatically.

YOUR BUSINESS IS YOUR OWN
- No account, no sign-up, no tracking, no ads.
- No internet permission, so nothing leaves the device.
- A private folder hides chosen items from every other list.
- An optional biometric lock keeps the whole app behind your fingerprint.

REQUIREMENTS
Android 8.0 (Oreo) or newer.

N Player is open source under the MIT licence.
```

## Category and tags

- Application type: App (not game)
- Category: Video Players & Editors
- Tags: video player, media player, offline, local files, no ads

## Data safety answers

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | No |
| Is all of the user data collected by your app encrypted in transit? | Not applicable — no data is collected |
| Do you provide a way for users to request that their data be deleted? | Not applicable — no data is collected |

Because the app declares no `INTERNET` permission and stores everything in its own
DataStore, the "no data collected" answer is accurate and verifiable.

## Content rating questionnaire

- Violence, sexuality, language, controlled substances: none.
- User-generated content sharing: no.
- The app plays user-supplied media, so declare that it can display any content the
  user already owns, but does not host or share it.

## Assets in this folder

| File | Use |
|---|---|
| `screenshots/phone-1-home.png` | Phone screenshot 1 |
| `screenshots/phone-2-player.png` | Phone screenshot 2 — the player controls |
| `screenshots/phone-3-detail.png` | Phone screenshot 3 |
| `screenshots/phone-4-search.png` | Phone screenshot 4 |
| `screenshots/phone-5-folders.png` | Phone screenshot 5 |
| `screenshots/phone-6-settings.png` | Phone screenshot 6 |
| `screenshots/feature-graphic.png` | Feature graphic (1024x500) |

All six screenshots are 1080x1920 (9:16) PNG, which meets Play's phone screenshot
requirement of 2 to 8 images with a short side of at least 320 px.

The icon still has to be supplied: a 512x512 32-bit PNG. `docs/` has no source icon
at that size, so export one before the first upload.

Regenerate the screenshots after a UI change with:

```bash
cd docs/store && python3 generate_screenshots.py
```
