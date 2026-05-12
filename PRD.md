# Cathode — Product Requirements Document

**Product:** Cathode — open-source IPTV viewer for Fire TV
**Owner:** bulchandani
**Repo:** github.com/bulchandani/cathode
**Target release:** v1.0

---

## Problem Statement

Fire TV owners with legitimate IPTV subscriptions have no good open-source viewer. The mature options — TiviMate, IPTV Smarters, Smart IPTV, Perfect Player — are all closed-source, often ad-supported, and several have been pulled from the Amazon Appstore at random. Power users who want a clean lean-back experience, predictable behaviour across firmware updates, and the ability to inspect or modify the player they're trusting with their stream credentials currently have nothing to install. **Cathode is the open-source IPTV viewer that runs on a stock Fire TV Stick and respects the remote.**

## Goals

1. **Play live, VOD, and series content from any Xtream Codes provider on a stock Fire TV Stick (gen 2+) via sideload, with sub-3-second channel switching on a stable network.**
2. **Be sideload-installable in under 60 seconds via the AFTVnews Downloader app** — single short URL, no Amazon Appstore dependency, no developer-mode gymnastics beyond the standard "Install unknown apps" toggle.
3. **Self-update from GitHub Releases without user intervention beyond accepting the install prompt** — so a public sideload audience stays on the latest build without re-typing URLs.
4. **Hold 60fps on a Fire TV Stick Lite with the full CRT visual identity enabled** — the aesthetic is the product; if it can't run smoothly on the cheapest hardware in the lineup, it's the wrong aesthetic.
5. **Ship a feature surface that lets a TiviMate user switch over without missing anything they care about for daily viewing** (not power-user features like DVR — see Non-Goals).

## Non-Goals (v1)

| Out of scope | Why |
|---|---|
| **DVR / recording** | Storage management on a Fire TV Stick is fragile; this is its own product. Park for v2. |
| **Catchup / time-shift** (Xtream's 1–7 day rewind) | Provider-dependent, format-fragmented, low daily-use value. Park. |
| **Casting (Chromecast / AirPlay)** | Fire TV is the screen — casting *off* it is a different product. |
| **Multi-user profiles** | A sideloaded TV app on a household device doesn't need this in v1. |
| **Localization beyond English** | English-only ships; translations are community-driven later. |
| **Amazon Appstore submission** | Review unpredictability + content-policy risk on an IPTV viewer. Sideload-first is intentional. |
| **Picture-in-picture browsing while playing** | Compose-for-TV PiP is rough at this version; revisit when the framework matures. |
| **Multi-view tiling** (2–4 channels at once) | Hardware-strained on Stick Lite; design + perf both unproven. |

## Personas & User Stories

### Persona 1 — "The cord-cutter household head"
Owns a Fire TV Stick, pays an Xtream Codes provider, lives in the remote. Doesn't read changelogs. Will give up if onboarding takes more than two screens.

- As a Fire TV owner, I want to install Cathode by typing one short URL into Downloader so that I'm watching within a minute.
- As an Xtream subscriber, I want to enter my host/user/pass once and have Live, Movies, and Series populate so that I don't have to set up each section separately.
- As a viewer, I want channel-up/channel-down on the remote to behave like a TV so that I can flip without opening menus.
- As a viewer, I want to type a 4-digit channel number on the remote and jump to it so that I don't scroll through 800 channels.
- As a viewer, I want to press Back from playback once to return to the list and twice to exit the app so that the remote behaves predictably.

### Persona 2 — "The picky power user"
Came from TiviMate or Perfect Player. Cares about audio sync, subtitle tracks, aspect ratio, and stream debugging. Will report bugs.

- As a power user, I want to nudge audio sync ±50ms during playback without pausing so that I can fix lipsync drift on misencoded streams.
- As a power user, I want to swap audio and subtitle tracks mid-stream so that I can switch language without restarting.
- As a power user, I want to see codec, bitrate, resolution, FPS, and dropped frames on demand so that I can diagnose stutter.
- As a power user, I want a manual stream-URL tester so that I can paste a URL from VLC and prove whether the player or the source is at fault.
- As a power user, I want to copy the current stream URL or open it in an external player so that I have an escape hatch when Cathode fails on a specific stream.

### Persona 3 — "The multi-source juggler"
Has more than one IPTV subscription (one for sports, one for everything else). Switches sources weekly.

- As a multi-source user, I want to save multiple Xtream sources and switch the active one with one selection so that I don't re-type credentials.
- As a multi-source user, I want each saved source to remember its own EPG and channel cache so that switching is instant after the first sync.
- As a household user, I want a 4-digit PIN gate on Settings so that someone else can't see or change credentials.

### Persona 4 — "The auditor"
Doesn't trust closed-source IPTV apps with subscription credentials. Will read the source. Open-source is the entire reason they're here.

- As a security-minded user, I want credentials stored in EncryptedSharedPreferences and never logged so that a stolen device doesn't leak my account.
- As an auditor, I want the build to be reproducible from a public Git tag so that the APK on Releases matches the source.
- As an auditor, I want zero analytics, zero third-party crash reporting, and no network calls beyond the user's chosen provider plus the GitHub update check so that the app does only what it advertises.

## Requirements

### Must-Have (P0) — required for v1.0

**Onboarding & sources**
- Add Xtream Codes source by host + user + pass, with smart host normalization that auto-detects HTTPS on `:443` / `:8443`.
- Save multiple sources; one active at a time; switch active source from Settings.
- Source switch invalidates EPG cache and stream-URL index so the new source's data loads cleanly without a stale-data flash.
- Credentials stored in EncryptedSharedPreferences. Never written to logs, never sent anywhere except the user's own provider.

**Live TV**
- Channel list with channel name, number, and now-playing program (when EPG is present).
- Tap a channel → full-screen playback in <3s on a stable network.
- **Stream URL resolution from the provider's M3U** rather than constructed client-side. Many Xtream providers serve their API on `http://` but only accept stream playback on `https://:443`; constructing URLs from the API host fails with HTTP 405 on those providers. Fetching the M3U (`/get.php?type=m3u_plus`) and indexing `streamId → URL` from it is the only reliable path.
- Number quick-select: type a 4-digit channel number from the player → jump to that channel.
- Last-channel toggle: flip to previous channel with a single button.
- Channel-up / channel-down navigation from the player without opening the list.

**VOD & Series**
- Movies grid with poster, title, year. Click → playback with resume-from-position.
- Series catalog → seasons → episodes, with resume per episode.
- Skip-intro on series: drag-handle in the OSD to set per-series intro skip duration, persisted.

**On-stream OSD** (overlay, does not pause playback)
- Audio track switcher (all tracks reported by Media3).
- Subtitle track switcher (off + all tracks).
- Audio sync offset ±2000ms in 50ms steps, applied via a custom `BaseAudioProcessor` so nudges take effect mid-stream without restart. Persisted per stream URL.
- Aspect ratio (Auto / 16:9 / 4:3 / Fill / Crop / Original).
- Stream info panel: codec, bitrate, resolution, FPS, dropped frames, buffer health.
- Sleep timer (Off / 15 / 30 / 60 / 90 min).
- Copy URL chip.
- Open in external player chip (`ACTION_VIEW` intent with the stream URL).
- Auto-hide overlay 3s after playback starts; reappears on any D-pad press.

**Cross-cutting**
- Cross-source search across channels, movies, and series. Triggers at ≥2 characters.
- Voice search via Fire TV's `SEARCH` intent — query auto-populates and routes to the search section.
- Favorites and Recents persist across launches and across source switches (scoped per source).
- 4-digit Parental PIN gate on Settings. PIN stored as SHA-256 hash.
- Three CRT theme modes (Full vintage / Moderate default / Modern dark) with respect for the system "Reduce motion" flag.
- Three buffer profiles (Snappy / Default / Patient) for variable-quality networks.
- In-app log viewer reachable via 5-tap gesture in Settings, with a one-tap "copy log bundle" for issue reports.
- Manual stream-URL tester: paste any URL → play directly, bypassing all source/index logic.

**Distribution & update**
- GitHub Actions workflow on tag `v*`: build → sign with production keystore → upload `cathode-v{version}.apk` and `cathode.apk` (latest alias) to the Release.
- In-app self-update: on launch (cached 24h), check `releases/latest`; if remote tag > local `BuildConfig.VERSION_NAME`, prompt to update; on accept, download APK to cache and fire `ACTION_VIEW` install intent.
- Single short URL for Downloader (e.g. `is.gd/cathode`) that resolves to `releases/latest/download/cathode.apk`.
- **Production signing keystore generated, backed up, and committed to repo secrets before v1.0.** This is one-way: re-keying after v1.0 means existing users cannot auto-update.

**Acceptance criteria for v1.0 ship**
- Given a fresh Fire TV Stick 4K Max and an Xtream Codes account, when the user types the Downloader URL, then they're playing a live channel within 90 seconds end-to-end.
- Given any provider Cathode fails on, when the user opens the in-app log viewer, then they can copy a log bundle that's enough for a GitHub issue.
- Given an existing v0.x install, when v1.0 is released, then the in-app update prompt fires within 24h of next launch.
- Given playback on a Fire TV Stick Lite with Full CRT mode, then frame rate stays ≥58fps measured by `adb shell dumpsys gfxinfo`.
- Given a provider that only serves streams over HTTPS on port 443 while exposing the API on plain HTTP, then live channels play without HTTP 405 errors.

### Nice-to-Have (P1) — fast-follow in v1.x

- EPG program detail modal (synopsis, cast, full air time).
- Hide categories — per-source category mute list.
- Headers customization screen — User-Agent, Referer, force HTTP/1.1, Range header toggle. For provider-specific compatibility issues that survive the M3U-based URL fix.
- M3U-only sources as a first-class source type (separate from Xtream).
- Channel logos via Coil with graceful fallback when missing.
- Skip-outro (companion to skip-intro).

### Future Considerations (P2) — design must not foreclose

- **Catchup / time-shift** — keep the player session model open enough that a "seek to N hours ago" command can be added without a refactor.
- **DVR / recording** — keep the Media3 pipeline reachable enough that a `MediaSource` tap-off is feasible later.
- **Multi-view tiling** — keep the player a Composable that can be instanced more than once.
- **Multi-user profiles** — keep RecentsStore and FavoritesRepo keyed in a way that a `profileId` can be added without a migration.
- **Localization** — keep all user-facing strings in `strings.xml`. Cleanup before adding the first translation.
- **Amazon Appstore submission** — keep manifest + permissions clean enough that submission would not require code changes, only review.

## Success Metrics

Cathode is open-source software distributed via sideload. There are no analytics, no telemetry, no in-app metrics. **Success has to be measured from public signals.**

### Leading indicators (first 30 days post-v1.0)

| Metric | Source | Target | Stretch |
|---|---|---|---|
| GitHub stars | repo | 200 | 1,000 |
| Releases-page APK downloads | GitHub Releases API | 500 | 5,000 |
| Distinct issue reporters | GitHub Issues | ≥10 | ≥50 |
| Crash reports (local-file) attached to issues | issues w/ logs | ≥5 useful, ≤2 same-bug-different-reporter on any single bug | — |
| AFTVnews / Reddit r/firetv mentions | manual scan | 1 organic post | front-page post |

### Lagging indicators (90+ days)

| Metric | Source | Target |
|---|---|---|
| Recurring contributors (≥2 merged PRs each) | git log | ≥3 |
| Provider-compatibility issues closed as "works" | issue labels | ≥80% of opened |
| Active fork count | GitHub | ≥5 |
| Time-to-first-playback for new users | issue self-reports + manual onboarding tests | ≤90s p50 |

### Anti-metrics (intentionally not optimised)
- Daily active users — we don't measure them.
- Session length — viewers should watch what they want for as long as they want; this is not engagement-bait.
- Feature adoption — we don't know what users press. We rely on issues for product signal.

## Open Questions

| Question | Owner | Blocking? |
|---|---|---|
| **Provider compatibility surface area.** The M3U-based URL resolution covers the dominant 405 case (API on `http://`, streams on `https://:443`). Are there providers that 405 even with the correct URL — and if so, what's the minimum set of header customizations (UA, Referer, HTTP/1.1, Range) needed to cover them? | engineering | Yes — blocks v1.0 |
| **Production signing keystore.** Generated where, backed up where, recovered how if the primary copy is lost? Re-keying post-v1.0 is not an option. | bulchandani | Yes — blocks v1.0 |
| Should the "Cathode" name + visual identity be trademarked, or stay unprotected with the source under Apache-2.0? | bulchandani | No — post-v1.0 |
| Does the in-app update flow need to handle Fire OS 6 (older sticks) install permission differences vs Fire OS 7+? | engineering | No — non-blocking, test on hardware |
| Is `is.gd/cathode` durable enough as the canonical sideload URL, or do we need our own redirect on a domain we own? | bulchandani | No — `is.gd` is fine for v1.0 |
| Compose-for-TV is still moving fast; do we pin a known-good version or track latest? | engineering | No — currently pinned, decision documented |
| Should crash logs auto-attach to GitHub issues with a one-tap flow, or stay copy-paste-only? | engineering / privacy | No — post-v1.0 |

## Timeline Considerations

This is a personal open-source project on a sideloaded distribution channel — there are no hard external deadlines. Phasing is driven by provider-compatibility validation and signing-keystore work.

| Phase | Trigger | Outcome |
|---|---|---|
| **Alpha** | All P0 features in place, debug-keystore builds | Internal testing on dev's primary Xtream provider + at least one secondary provider for compatibility coverage |
| **Beta** | Production keystore in CI; auto-update flow verified end-to-end on hardware | Public sideload URL active; invite-only or limited announcement to gather provider-compatibility issues |
| **v1.0** | All P0 acceptance criteria green + 7-day soak with no new P0 bugs | Public announcement on r/firetv and AFTVnews; canonical short URL goes live |
| **v1.x** | Post-launch | P1 backlog burn-down based on issue volume and provider-compatibility findings |

**Hard dependency:** v1.0 cannot ship until the production keystore is in CI. Re-keying after v1.0 means existing users can't auto-update — they'd have to uninstall + reinstall, which violates Goal #3.
