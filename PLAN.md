# Yoink — Build Plan (Android)

An Android app for downloading YouTube videos. In the YouTube app, tap **Share → Yoink**, pick a quality, and the video downloads to your phone. Every download appears in Yoink's own **Library**, where you can watch, share or delete it.

No server is needed. Everything runs on the phone.

## How it works

1. You're watching a video in the YouTube app (or YouTube in the browser).
2. Tap **Share**, then **Yoink**.
3. A small sheet slides up over YouTube with **Best**, **1080p**, **720p**, **480p** and **Audio (MP3)**.
4. Pick one. The sheet closes, you stay in YouTube, and a notification shows the progress.
5. When it's done, tap the notification or open Yoink to play it.

## Tech stack

| Piece | Choice |
|---|---|
| Language / UI | Kotlin, Jetpack Compose, Material 3 (with dynamic color and dark mode) |
| Downloader | [`youtubedl-android`](https://github.com/JunkFood02/youtubedl-android) (`library` + `ffmpeg` modules). It bundles yt-dlp, Python and ffmpeg so they run on the phone. Follow its current README for setup, including anything YouTube now requires (such as a JS runtime). |
| Background work | WorkManager with a foreground notification showing progress |
| Database | Room, to store the download history and library |
| Video player | Media3 ExoPlayer |
| Images | Coil, for thumbnails |
| Min / target SDK | minSdk 29 (Android 10), latest targetSdk |

**Languages:** Kotlin for the app, plain HTML/CSS/JavaScript for the website. No TypeScript, and no Node/npm tooling anywhere in the repo.

See `ARCHITECTURE.md` for how everything fits together. Two settings are required: `android:extractNativeLibs="true"` in the manifest and `jniLibs.useLegacyPackaging = true` in Gradle. Without them, the bundled Python and ffmpeg can't run.

## Screens

### 1. Share sheet (`ShareActivity`)
- A transparent activity that handles `ACTION_SEND` with `text/plain`, so Yoink appears in YouTube's Share menu. It also handles `ACTION_VIEW` for youtube.com and youtu.be links.
- Pulls the video ID out of the shared text and rebuilds a clean URL (drops `si=` tracking).
- Shows a Material bottom sheet right away with title and thumbnail (from YouTube's oEmbed endpoint and `i.ytimg.com`). Quality buttons unlock once yt-dlp has listed the formats; qualities the video doesn't have stay disabled.
- On tap: queue the download, show a toast that it started, and close. The user stays in YouTube.

### 2. Home
- Tabs: **Downloading** and **Library**
- **Downloading:** active and queued downloads, each with thumbnail, title, progress bar, speed, ETA and a cancel button
- **Library:** finished downloads as a grid or list with thumbnail, title, duration, size and date
  - Tap to play in the built-in player
  - Long-press for: Share, Open with another app, Delete
  - Search box and a filter for Video / Audio
- A **+** button to paste a link manually (for people who copy links instead)

### 3. Player
- Full-screen ExoPlayer that works in landscape. Background audio for MP3s.

### 4. Settings
- Default quality, which skips the sheet and downloads right away
- Save location: app folder only, or also show in the gallery (`Movies/Yoink` and `Music/Yoink` through MediaStore)
- **Update downloader** button, which calls yt-dlp's update inside the library, because YouTube changes often
- Light / dark / system theme

## Downloading

| Quality | yt-dlp options |
|---|---|
| Best | `-f "bv*+ba/b" --merge-output-format mp4` |
| 1080p / 720p / 480p | `-f "bv*[height<=H]+ba/b[height<=H]/b" -S "vcodec:h264,acodec:aac" --merge-output-format mp4` |
| Audio | `-f "ba/b" -x --audio-format mp3 --audio-quality 192K` |

- Always add `--no-playlist`.
- Save the thumbnail alongside the file for the library.
- Allow at most 2 downloads at once; queue the rest.
- On failure: a notification with a readable error and a **Retry** button.
- On first launch, check for yt-dlp updates in the background.

## Permissions

- `INTERNET`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `POST_NOTIFICATIONS` (ask on first download, Android 13+)
- No storage permission needed: use app-specific storage plus MediaStore for the gallery option.

## Build and release

- Use a GitHub Actions workflow that builds a **release APK** on every tag like `v1.0.0` and attaches it to a GitHub Release.
- Split APKs by ABI (`arm64-v8a`, `armeabi-v7a`, `x86_64`), because the bundled Python and ffmpeg are large. Most phones need `arm64-v8a`.
- Sign the release with a keystore stored in GitHub Secrets. The README explains how to make one.
- **Release files must use these exact names, with no version number in them.** The website links to `releases/latest/download/<name>`, which only works if the names never change:
  - `yoink-arm64-v8a.apk`
  - `yoink-armeabi-v7a.apk`
  - `yoink-universal.apk`
  - `SHA256SUMS.txt`

## App icon

A white downward arrow on a red rounded square. The source is `docs/logo.svg` (red gradient `#ff4d4d` → `#d90f1a`).
- Build an Android **adaptive icon**: the background layer is the red gradient and the foreground layer is the white arrow (vector drawable), kept inside the 66dp safe zone.
- Add a **monochrome** layer (the arrow only) for Android 13+ themed icons.
- Use the same red as the app's Material 3 seed color.

## Website (`docs/`)

`docs/index.html` is a finished download page: a big "Download for Android" button, install steps, how-to-use, other APK versions and FAQ. It's plain HTML with no build step. It already:
- links to the latest release files above
- shows the version, size and date from the GitHub API
- shows a QR code to desktop visitors and an "Android only" note to iPhone visitors

**Only change needed:** set `const REPO = "YOUR-USERNAME/yoink";` near the bottom of `docs/index.html` to the real GitHub repo.

**Hosting: Cloudflare Pages**, connected to the GitHub repo. Framework preset: *None*. Build command: *(empty)*. Output directory: `docs`. Every push to `main` redeploys. The APKs stay on GitHub Releases, because Cloudflare Pages caps each file at 25 MB.

## Project layout

```
app/src/main/java/.../yoink/
  ui/        (home, library, player, settings, share sheet)
  data/      (Room database, repository)
  download/  (WorkManager worker, yt-dlp wrapper)
.github/workflows/release.yml
docs/        (download website: index.html, logo.svg)
ARCHITECTURE.md
README.md
LICENSE      (GPL-3.0, required because the bundled downloader library and ffmpeg are GPL)
```

## README

- What it is, with screenshots of the Share sheet and the Library
- Install: a link to the download website, plus "or grab the APK from Releases"
- How to use: YouTube → Share → Yoink
- Note: not on Google Play, because Play doesn't allow YouTube downloaders
- Disclaimer: for personal use with content you have the right to download. Respect YouTube's Terms of Service and copyright.
