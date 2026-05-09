# Cathode

An IPTV viewer for Fire TV (and any Android device) with a CRT/tube aesthetic.

> **Status:** alpha. Hub + Media3 player + a "paste any URL" stream tester wired. Real Xtream Codes / M3U / EPG flows coming next.

## Goals

- Lean-back Fire TV experience: Live TV, Movies, Series, Search, Favorites, Recents, Settings
- Source support: M3U/M3U8 playlists, Xtream Codes API, XMLTV EPG
- Distributed via sideload (AFTVnews Downloader) — no app-store review
- Open source, Apache-2.0

## Install on Fire TV

1. Install the **Downloader** app from the Amazon Appstore on your Fire TV
2. Open Downloader and enter:
   - `is.gd/cathode` (short, easy to type with the remote), or
   - `https://github.com/Bulchandani/cathode/releases/latest/download/cathode.apk` (the long form)
3. Install when prompted — you may need to allow installs from unknown sources

## Install on Android tablet / phone

1. Open [`is.gd/cathode`](https://is.gd/cathode) in your browser → APK downloads
2. Tap the downloaded APK file → Install
3. Allow your browser to install unknown apps if Android prompts you

The app locks to landscape and runs the same UI on tablets, phones, and Fire TV.

## Build

CI builds the debug APK on every push to `main` and uploads it as a GitHub Actions artifact. It also renders the Hub UI to a PNG snapshot via Roborazzi (no emulator required) — see the `cathode-ui-snapshots` artifact on each run. Tagged releases (`vX.Y.Z`) publish APKs to GitHub Releases.

To build locally, you'll need JDK 17 and Android SDK 35:

```sh
./gradlew :app:assembleDebug
```

## Stack

- Kotlin + Jetpack Compose (foundation primitives + tv-material3 + material3)
- Media3 / ExoPlayer (HLS / DASH / MPEG-TS)
- Min SDK 23 (Fire TV Stick gen 2+, Android 6.0+)
- Target SDK 35

## License

Apache-2.0 — see [LICENSE](LICENSE).
