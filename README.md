<p align="center">
  <img src="docs/logo.svg" width="96" alt="Yoink logo">
</p>

<h1 align="center">Yoink</h1>

<p align="center">
  Download YouTube videos on Android, right from the Share menu.<br>
  Everything runs on your phone. No server, no account, no ads.
</p>

---

In the YouTube app, tap **Share → Yoink**, pick a quality, and keep watching. The video downloads in the background and shows up in Yoink's **Library**, where you can play, share or delete it.

<!--
  SCREENSHOTS PLACEHOLDER
  1. Add two images to a screenshots/ folder: share-sheet.png and library.png
  2. Replace the two placeholder cells in the table below with:
       <img src="screenshots/share-sheet.png" width="280" alt="Share sheet">
       <img src="screenshots/library.png" width="280" alt="Library">
-->
| Share sheet | Library |
|:---:|:---:|
| **[Screenshot placeholder: the quality sheet over YouTube]** | **[Screenshot placeholder: the Library tab]** |

## Features

- **Share sheet:** shows the title and thumbnail instantly, then unlocks **Best**, **1080p**, **720p**, **480p** and **Audio (MP3)** once it knows which qualities the video has.
- **Background downloads** with a progress notification (speed and time left), **Cancel**, and **Retry** if something goes wrong. Up to two downloads run at once; the rest wait their turn.
- **Library** with search, a Video / Audio filter, and list or grid view. Long-press to share, open in another app, or delete.
- **Built-in player** in full screen and landscape. MP3s keep playing in the background.
- **Paste a link** with the **+** button if you copy links instead of sharing them.
- **Settings:** a default quality that skips the sheet, the option to show downloads in your gallery (`Movies/Yoink`, `Music/Yoink`), and an **Update downloader** button.
- **Make it yours:** ready-made themes, any color (or your wallpaper's), palette styles, contrast, pure black for OLED, corner styles and fonts. Save your own looks as themes and switch between them.

## Install

**[Download Yoink from the website](https://yoink-7rk.pages.dev/)**

Or grab the APK from [GitHub Releases](https://github.com/hishamhashmi100/Yoink/releases/latest):

| File | For |
|---|---|
| `yoink-arm64-v8a.apk` | Almost every phone from the last several years. **Start here.** |
| `yoink-armeabi-v7a.apk` | Older 32-bit phones |
| `yoink-universal.apk` | Works everywhere (including x86_64 emulators and Chromebooks), but larger |
| `SHA256SUMS.txt` | Checksums to verify your download |

Requires Android 10 or newer. Your browser will ask you to allow installing apps from unknown sources the first time.

> **Why isn't it on Google Play?** Google Play doesn't allow apps that download from YouTube, so Yoink is distributed here instead.

## How to use

1. Open a video in the YouTube app (or on youtube.com in your browser).
2. Tap **Share**, then **Yoink**. You may need to tap **More** the first time.
3. Pick a quality. The sheet closes and you stay in YouTube.
4. When the notification says it's done, tap it to watch, or open Yoink.

If downloads start failing, open **Settings → Update downloader**. YouTube changes often and the downloader can be updated without a new app version.

## Updates

- **The downloader (yt-dlp)** updates itself once a day in the background, so most YouTube changes are fixed without a new version of the app.
- **The app itself** checks GitHub Releases once a day. When a newer version is out, a card appears on the home screen (and a notification). Tap **Update**, then confirm in Android's installer. The first time, Android asks you to allow Yoink to install apps. You can also check by hand in **Settings → App updates**.

## How it works

Yoink bundles [yt-dlp](https://github.com/yt-dlp/yt-dlp) together with Python, ffmpeg and the QuickJS JavaScript engine through [youtubedl-android](https://github.com/JunkFood02/youtubedl-android). Downloads come from your own internet connection, and ffmpeg combines the video and audio streams into an MP4 (or converts to MP3). See [ARCHITECTURE.md](ARCHITECTURE.md) for details.

Built with Kotlin, Jetpack Compose and Material 3, WorkManager, Room, Media3 ExoPlayer and Coil.

## Building from source

You need JDK 17 and the Android SDK.

```sh
./gradlew assembleDebug
```

The APKs land in `app/build/outputs/apk/debug/`. Debug builds install as **Yoink Debug** (`…yoink.debug`), so they can sit next to the release version.

Every push runs the **Build** workflow, which uploads the `arm64-v8a` debug APK as a workflow artifact for quick testing on a phone.

## Releases

Pushing a tag like `v1.0.0` runs `.github/workflows/release.yml`. It builds the release APKs, signs them, and attaches them to a GitHub Release with fixed names (`yoink-arm64-v8a.apk`, `yoink-armeabi-v7a.apk`, `yoink-universal.apk`, `SHA256SUMS.txt`). The website links to `releases/latest/download/<name>`, so those names must never change.

```sh
git tag v1.0.0
git push origin v1.0.0
```

To ship a fix, tag a higher version (`v1.0.1`, `v1.1.0`, …). Installed copies find it within a day and offer the update. Updates only install over the old version when they're signed with the **same keystore**, so keep it backed up.

### Signing releases

Create a keystore once and keep it safe. If you lose it, you can't publish updates that install over existing copies.

```sh
keytool -genkeypair -v -keystore yoink-release.jks -alias yoink \
  -keyalg RSA -keysize 4096 -validity 10000
```

Encode it as base64:

```sh
base64 -w 0 yoink-release.jks > yoink-release.jks.b64   # Linux
base64 -i yoink-release.jks -o yoink-release.jks.b64    # macOS
```

Then add these secrets under **Settings → Secrets and variables → Actions** in the GitHub repo:

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | Contents of `yoink-release.jks.b64` |
| `KEYSTORE_PASSWORD` | The keystore password |
| `KEY_ALIAS` | `yoink` (or the alias you chose) |
| `KEY_PASSWORD` | The key password (the same as the keystore password unless you set a different one) |

Never commit the keystore. `*.jks` is in `.gitignore`.

## Website

The download page lives in [`docs/`](docs/) as plain HTML with no build step. It's hosted on **Cloudflare Pages**, connected to this repo:

- Framework preset: **None**
- Build command: *(empty)*
- Build output directory: `docs`

Every push to `main` redeploys it. The APKs stay on GitHub Releases, because Cloudflare Pages limits files to 25 MB.

## Disclaimer

Yoink is for personal use with content you have the right to download. Respect YouTube's Terms of Service and the rights of creators. Yoink isn't affiliated with or endorsed by YouTube or Google.

## License

[GPL-3.0](LICENSE). The bundled youtubedl-android library and ffmpeg build are GPL-licensed, so the app is too. yt-dlp is released under the Unlicense.
