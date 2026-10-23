# Architecture

Yoink is an Android app that downloads YouTube videos **entirely on-device**. There's no backend: the extraction engine (yt-dlp), its Python interpreter, a JavaScript runtime and ffmpeg all ship inside the APK and run as native child processes of the app.

```
┌──────────────────────────── YouTube app ─────────────────────────────┐
│  Share ▸ Yoink                                                      │
└──────────────┬───────────────────────────────────────────────────────┘
               │ Intent ACTION_SEND (text/plain, EXTRA_TEXT = youtu.be/<id>?si=…)
               ▼
┌──────────────────────────── Yoink ──────────────────────────────────┐
│ ShareActivity (translucent)                                          │
│   ├─ parse + normalize URL → videoId                                 │
│   ├─ instant preview: oEmbed (title/channel) + i.ytimg.com thumbnail │
│   ├─ background: yt-dlp --dump-single-json → available heights      │
│   └─ user picks quality → enqueue DownloadWorker, finish()           │
│                                                                      │
│ DownloadWorker (WorkManager, foreground, type=dataSync)              │
│   ├─ yt-dlp (embedded CPython) ── InnerTube API ──► YouTube          │
│   │     ├─ JS runtime solves player challenges (sig / n)             │
│   │     ├─ chunked HTTP range downloads: video-only + audio-only     │
│   │     └─ ffmpeg: stream-copy mux → MP4  (or transcode → MP3)       │
│   ├─ stdout progress → setProgress() + notification                  │
│   └─ move to final storage → write row to Room                       │
│                                                                      │
│ UI (Compose): Downloading / Library / Player (Media3) / Settings     │
│   └─ observes Room Flow + WorkInfo Flow                              │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 1. Entry point: the Share intent

An app can't add a button inside the official YouTube app. The system **Share sheet** is the supported integration point.

- `ShareActivity` declares an `intent-filter` for `ACTION_SEND` / `text/plain`, plus `ACTION_VIEW` for `youtube.com`, `m.youtube.com` and `youtu.be` links.
- Manifest flags: `Theme.Translucent.NoTitleBar`, `excludeFromRecents="true"`, `taskAffinity=""` and `launchMode="singleTask"`. The sheet floats over YouTube, doesn't drag Yoink's main task forward, and leaves no trace in Recents.
- URL parsing: one regex extracts the 11-character video ID from `youtu.be/<id>`, `/watch?v=<id>`, `/shorts/<id>`, `/live/<id>` and `/embed/<id>`. The URL is rebuilt as `https://www.youtube.com/watch?v=<id>`, which drops tracking parameters like `si=` and playlist context.

### Two-phase preview (no perceived latency)

A full yt-dlp metadata extraction takes 2–5 seconds on a phone, so the sheet loads in two phases:

1. **Instant (~100 ms):** `GET https://www.youtube.com/oembed?url=…&format=json` returns the title and channel without an API key. The thumbnail comes from `https://i.ytimg.com/vi/<id>/hqdefault.jpg`.
2. **Background:** `yt-dlp --dump-single-json --no-playlist` returns the full format list. Quality buttons stay disabled until the available `height` values are known, then unavailable ones stay greyed out.

---

## 2. The embedded runtime

Built on [`youtubedl-android`](https://github.com/JunkFood02/youtubedl-android), which packages:

| Component | Shipped as | Notes |
|---|---|---|
| CPython | `libpython.so` (+ stdlib zip) | Cross-compiled for Android ABIs |
| yt-dlp | Python zipapp in `noBackupFilesDir` | **Updatable at runtime** without an app update |
| ffmpeg / ffprobe | `libffmpeg.so` | Muxing, audio extraction |
| JS runtime | per library | Required by yt-dlp for YouTube (see §3) |

### Why executables are disguised as `.so` files

Since Android 10 (API 29), apps targeting modern SDKs **can't `exec()` files from writable storage** (W^X policy). The only app-owned location that is both on disk and executable is `nativeLibraryDir`, which the package installer fills with `lib*.so` files from the APK's `jniLibs`. So the interpreter and ffmpeg ship as "libraries" and are launched with `ProcessBuilder`.

For this to work, the libraries must be **extracted at install time**:

```xml
<application android:extractNativeLibs="true" …>
```
```kotlin
packaging { jniLibs { useLegacyPackaging = true } }
```

Without these, modern AGP keeps `.so` files uncompressed inside the APK, nothing exists on disk to exec, and every download fails.

yt-dlp itself is pure Python, so the interpreter only *reads* it, and it can live in writable storage. That's what makes in-app updates possible.

---

## 3. How yt-dlp actually gets the video

1. **Player API.** yt-dlp calls YouTube's internal **InnerTube** endpoint (`/youtubei/v1/player`) and presents itself as one or more official clients (web, mobile web, TV, Android, iOS variants). Each client gets different formats and protections, and yt-dlp tries several of them.
2. **`streamingData`.** The response holds:
   - `formats`: progressive (video and audio muxed). Today that's usually only 360p (itag 18).
   - `adaptiveFormats`: **DASH**, meaning separate video-only and audio-only streams for every resolution. All HD content lives here.
3. **Player challenges.** For many clients, stream URLs aren't usable as-is:
   - **Signature cipher (`s`):** must be transformed by a function obfuscated inside YouTube's player JavaScript (`base.js`) before it becomes the `sig` parameter.
   - **`n` parameter:** must also be transformed by player JS. If it isn't, YouTube throttles the stream to a crawl or returns 403.

   yt-dlp downloads the player JS and **executes those functions in a real JavaScript runtime**. Since late 2025, yt-dlp needs an external JS engine for this; it supports Deno, Node, Bun and QuickJS. On Android, the runtime has to come from the library or be bundled by the app. **This is the main technical risk: check the library's README for its current JS-runtime support before writing code.**
4. **Proof-of-Origin tokens.** Some clients also require PO tokens (BotGuard attestation). yt-dlp chooses clients that don't need them where possible. This changes often, which is why §6 (updates) matters.

### Format selection

| Choice | Selector | Result |
|---|---|---|
| Best | `bv*+ba/b` | Highest resolution; may be VP9/AV1 above 1080p |
| 1080/720/480 | `bv*[height<=H]+ba/b[height<=H]/b` with `-S vcodec:h264,acodec:aac` | **H.264 + AAC**: hardware-decoded on effectively every Android device |
| Audio | `ba/b -x --audio-format mp3` | MP3 via libmp3lame |

`-S vcodec:h264` is a *preference*, not a filter. It ranks avc1 streams above VP9/AV1 at each resolution, so you get the best H.264 stream within the height limit and still have a fallback if none exists.

### Transfer and mux

- YouTube throttles single large GETs, so yt-dlp downloads with **chunked HTTP range requests** (`--http-chunk-size 10M`). Partial files (`.part`) make downloads **resumable** if the process dies.
- The video-only and audio-only streams are combined by ffmpeg with **stream copy** (`-c copy`). This is a remux, not a re-encode: it takes seconds, uses little CPU and loses no quality.
- MP3 is the only path that actually re-encodes. (Offering M4A/AAC audio would avoid the transcode entirely; a good future option.)

### Why on-device beats a server

Requests come from the **user's own residential or mobile IP**. Datacenter IPs, which is where any server-based downloader runs, are the main target of YouTube's "Sign in to confirm you're not a bot" wall. Running on-device avoids that, and also means there's no hosting cost, no bandwidth bill and no central service to take down.

---

## 4. Background execution

- `DownloadWorker` is a `CoroutineWorker` that calls `setForeground(ForegroundInfo(…, FOREGROUND_SERVICE_TYPE_DATA_SYNC))`. Android 14+ requires the service type in the manifest plus the `FOREGROUND_SERVICE_DATA_SYNC` permission. Android 15 caps `dataSync` at about 6 hours per 24 hours, far more than this needs.
- **Concurrency:** a dedicated unique-work queue limits active downloads to 2. The rest wait as `ENQUEUED`.
- **Progress:** the library parses yt-dlp's stdout (`[download]  42.3% of 120.5MiB at 4.2MiB/s ETA 00:17`) into a callback `(progress, etaSeconds, line)`. The worker forwards it through `setProgress()` (for the UI) and a notification update throttled to about 1 per second (Android rate-limits faster updates).
- **Stages:** "Downloading video", then "Downloading audio", then "Merging", detected from yt-dlp's `[download] Destination:` and `[Merger]` lines.
- **Cancellation:** each job gets a `processId`; cancelling kills the child process (`destroyProcessById`) and deletes the `.part` files.
- **Failure:** stderr is mapped to readable errors (private, age-restricted, removed, network), and the notification offers **Retry**.

---

## 5. Storage and data model

The work happens in `cacheDir/<jobId>/` so a half-finished download never shows up anywhere. On success, the file moves to one of these:

| Setting | Destination | Permission |
|---|---|---|
| App only (default) | `getExternalFilesDir(DIRECTORY_MOVIES / MUSIC)` | none |
| Show in gallery | MediaStore → `Movies/Yoink`, `Music/Yoink` | none on API 29+ |

MediaStore writes use the **pending-item** pattern: insert with `IS_PENDING = 1`, stream the bytes through `ContentResolver.openOutputStream`, then set `IS_PENDING = 0`. This way the gallery never indexes a partial file.

**minSdk 29** (Android 10) keeps the storage path permission-free and matches the W^X model described above. It covers the vast majority of active devices.

**Room** entity `Download`:

```
id · videoId · title · channel · durationSec · thumbnailPath
fileUri · mimeType · quality · sizeBytes · status · error · createdAt
```

The repository exposes `Flow<List<Download>>`. Compose screens observe it together with `WorkManager.getWorkInfosForUniqueWorkFlow(…)`, so progress, completion and deletion update the UI live with no manual refresh.

---

## 6. Staying working when YouTube changes

YouTube changes its player JS and API regularly, and every downloader breaks from time to time. Yoink handles this without a new app release:

- `YoutubeDL.updateYoutubeDL(context, STABLE)` pulls the latest yt-dlp release from GitHub into app storage.
- A check runs on first launch (and optionally daily through a periodic `WorkManager` job). Settings also has a manual **Update downloader** button.
- Only changes to the native layer (Python, ffmpeg, JS runtime) need a new APK.

---

## 7. Build and release

- **Gradle (Kotlin DSL)**, version catalogs, JDK 17.
- **ABI splits** for `arm64-v8a`, `armeabi-v7a` and `x86_64`, because the bundled interpreter and ffmpeg dominate APK size. Most users need `arm64-v8a`. A universal APK is also published.
- **R8:** apply the keep rules from the library's README (it uses reflection-based JSON mapping).
- **CI (`.github/workflows/release.yml`):** pushing a `v*` tag runs these steps:
  1. `./gradlew assembleRelease`
  2. Decode the keystore from the `KEYSTORE_BASE64` secret and sign with `apksigner`
  3. Attach the APKs plus a `SHA-256SUMS` file to the GitHub Release
- **Not on Google Play.** Play policy prohibits YouTube downloaders, so releases are distributed through GitHub, as F-Droid-style apps commonly are.

---

## 8. Licensing

`youtubedl-android` and the bundled ffmpeg build are **GPL-licensed**, so the distributed APK is a GPL work. The repository is therefore licensed under **GPL-3.0**. yt-dlp itself is public domain (Unlicense).

The app name and icon must not use YouTube's name or logo.
