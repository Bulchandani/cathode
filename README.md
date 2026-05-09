# Cathode

An IPTV viewer for Fire TV with a CRT/tube aesthetic.

> **Status:** early scaffolding. Hub UI only. No player or sources wired yet.

## Goals

- Lean-back Fire TV experience: Live TV, Movies, Series, Search, Favorites, Recents, Settings
- Source support: M3U/M3U8 playlists, Xtream Codes API, XMLTV EPG
- Distributed via sideload (AFTVnews Downloader) — no app-store review
- Open source, Apache-2.0

## Install on Fire TV (planned, once `v0.0.1` ships)

1. On your Fire TV, install the **Downloader** app from the Amazon Appstore
2. Open Downloader and enter:
   `https://github.com/Bulchandani/cathode/releases/latest/download/cathode.apk`
3. Install when prompted (you may need to allow apps from unknown sources)

## Build

CI builds the debug APK on every push to `main` and uploads it as a GitHub Actions artifact. Tagged releases (`vX.Y.Z`) publish APKs to GitHub Releases.

To build locally, you'll need JDK 17 and Android SDK 35:

```sh
./gradlew :app:assembleDebug
```

## Stack

- Kotlin + Jetpack Compose for TV
- Media3 / ExoPlayer (coming next)
- Min SDK 23 (Fire TV Stick gen 2+)
- Target SDK 35

## License

Apache-2.0 — see [LICENSE](LICENSE).
