# StartMusicV3 — Existing Feature Audit

**Date:** 2026-11-07  
**Workspace:** `c:\Users\IT\Desktop\StartMusicV3`  
**Remote:** https://github.com/ElionKrasniqi0/StartMusicV3  
**Reference app (inspiration only):** https://github.com/ymusic-io/YMusicV4 · https://ymusic.io/

**Method:** Source inspection of this repository (Gradle modules, Android shell, `:commonUi`, `:player`, `:settings`, storage modules). Runtime playback on a device was **not** executed in this audit pass — working status for playback features is inferred from complete wiring in code, marked **Unknown (runtime)** where device verification is still required.

---

## Critical discovery

**This workspace is not a thin starter app.** The tree is a full Kotlin Multiplatform music client whose source, modules, package names (`io.ymusic.*`), GPL license text, README, engine host, and Compose UI match the **YMusic V4** product architecture described in `AGENTS.md` / module READMEs.

| Identity signal | Value in this repo |
|-----------------|--------------------|
| Root Gradle name | `YMusicV4` (`settings.gradle.kts`) — **not renamed** (deferred) |
| Version | `ymusic.versionName` in `gradle.properties` |
| Android `applicationId` | `io.ymusic.android` — **not renamed** (deferred) |
| Android / in-app display name | **StartMusicV3** (resources updated; packages still `io.ymusic.*`) |
| Theme / Application class | `Theme.YMusicV4`, `YMusicApplication` (class names deferred) |
| Design system | `Brand.kt` + StartMusicV3 violet palette in `Theme.kt` |
| Deep links | Still `ymusic://` / ymusic hosts — **deferred** (store/domain risk) |

**Implication for the StartMusicV3 transformation:** most “reference” UX features already exist here. The work is primarily **rebranding + visual redesign + selective productization**, not greenfield feature builds. Blindly “rebuilding” playback or navigation would destroy a working stack. Safe UI/branding phases are documented in `docs/STARTMUSIC_TRANSFORMATION.md`.

**License note:** Root license is **GPL-3.0** (with Apache-2.0 exceptions for `kmpCommon` / `kmpHttp`). Any redistribute/rebrand must stay GPL-compliant. This audit does not give legal advice.

---

## A. Existing application architecture

### A.1 High-level shape

```
┌─────────────────────────────────────────────────────────────────┐
│ Product shells                                                  │
│  :androidApp  ·  :desktopApp  ·  iosApp (+ :iosAppKotlin)       │
└────────────────────────────┬────────────────────────────────────┘
                             │ builds AppGraph
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ :commonUi — Compose Multiplatform App (drawer, feeds, player UI)│
│   App → AppRoot → sections + AppLayer stacks + MiniPlayer/NP    │
│   PlaybackCoordinator → :player.Player                          │
└───────┬───────────────────────┬───────────────────┬─────────────┘
        │                       │                   │
        ▼                       ▼                   ▼
 :dynamic-engine          :player               :media-store
  + :engine-js             MediaEngine SPI       + :media-scanner
  + :quickjs               Local + Yt/MSE        + :download
  (scraping / login)       + :mse / :yt-media    + :settings
```

### A.2 Modules (role)

| Module | Role |
|--------|------|
| `:commonUi` | Shared screens, theme, navigation, playback UI glue |
| `:androidApp` | Android product: `MainActivity`, `PlaybackService`, widgets, WebView login, SAF |
| `:desktopApp` / `iosApp` | Desktop / iOS shells over the same `App` |
| `:player` | `Player` facade + `MediaEngine` (ExoPlayer local / MSE YouTube) |
| `:mse` / `:yt-media` | Native MSE host + SABR/UMP download/cache |
| `:dynamic-engine` / `:engine-js` / `:quickjs` | Remote-updatable JS engine + QuickJS host |
| `:media-store` / `:media-scanner` | Local library DB + folder scan |
| `:download` | Offline download queue |
| `:settings` | Typed `SettingsStore` / `AppSettings` |
| `:image-loader`, `:log-store`, `:remote-config`, `:telemetry`, `:projectCommon`, `:kmp*` | Support libraries |

### A.3 Android entry / services

| Component | Path | Notes |
|-----------|------|--------|
| Application | `androidApp/.../YMusicApplication.kt` | Builds `AppGraph`, starts playback FGS when playing |
| UI host | `androidApp/.../MainActivity.kt` | `setContent { App(graph, …) }` |
| Playback FGS | `androidApp/.../PlaybackService.kt` | Media3 `MediaLibraryService` + custom notification |
| Downloads FGS | `DownloadForegroundService` | `dataSync` while queue active |
| Login | `AndroidLoginBrowser` + `WebViewCookieJar` | Cookies shared with engine HTTP |
| Other | `ShareActivity`, `VoiceSearchActivity`, `QrScanActivity`, player widgets, `ArtworkProvider` | |

### A.4 UI architecture (`:commonUi`)

- **No Navigation-Compose graph.** Routing = drawer `AppSection` + per-section `AppLayer` stacks (`App.kt`).
- **Theme:** `Theme.kt` — `DesignPalette` (light / dark / amoled) + Material 3; accent via `AppSettings.themeAccent` (default `#E8732A`).
- **Strings:** compose-resources, ~50 locales; rule = never hardcode UI text.
- **Android XML layouts:** widgets only — in-app UI is Compose.

### A.5 Settings persistence

| Platform | Store |
|----------|--------|
| Android | `PrefsSettingsStore` → SharedPreferences `app_settings` |
| Desktop | SQLite-backed `AppStoreSettings` |
| Keys | `settings/.../AppSettings.kt` (`theme`, `themeAccent`, `uiLanguage`, download/playback/telemetry keys, …) |

Playback quality / EQ / speed live primarily on `Player` / `PlaybackConfig`, with some keys restored via `AppGraph` (shuffle, loop, etc.) — not all knobs are `AppSettings` rows.

---

## B. Existing features

Status legend for **Works?**  
- **Wired:** complete code path exists end-to-end in this tree  
- **Unknown (runtime):** wired, but not device-verified in this audit  
- **Platform-limited:** wired on some shells only  
- **Partial / mock:** incomplete or stubbed  
- **Missing:** not found

| Feature | Key sources | Works? | UI change needed for StartMusicV3? | Playback/backend change? | Break risk |
|---------|-------------|--------|--------------------------------|--------------------------|------------|
| Discover feeds (Home/Music/Recommended/…) | `App.kt`, `Feed.kt`, dynamic-engine yt commands | Wired / Unknown (runtime) | Yes (visual) | No | Medium if feed contracts touched |
| YT Music section | `YtMusicScreen.kt` | Wired / Unknown (runtime) | Yes | No | Medium |
| YouTube search | `SearchScreen.kt` | Wired / Unknown (runtime) | Yes | No | Medium |
| YouTube sign-in | `YtLoginScreen.kt`, WebView / cookie vault | Wired / Unknown (runtime) | Yes (branding copy) | **Do not replace** cookie/WebView model | **High** |
| Local Library (folders/songs/…) | `LibraryScreen.kt`, `:media-store`, scanner | Wired on Android/desktop; **null on iOS** | Yes | No (keep SAF/scanner) | High if DB schema changed |
| User playlists + smart Recent/Top | `LibraryScreen.kt`, `PlayHistoryStore` | Wired | Yes | No | Medium |
| Playlist import/backup/restore | `PlaylistTransfer.kt` | Wired (Android import seam) | Optional | No | Medium |
| Play / pause / seek / prev / next | `Player`, `NowPlaying.kt`, MediaSession | Wired / Unknown (runtime) | Yes (controls chrome) | **Do not replace engine** | **Critical** |
| Queue, shuffle, repeat | `player/.../Queue.kt`, `PlayerImpl` | Wired | Yes | No | High |
| Mini-player + Now Playing | `NowPlaying.kt`, `App.kt` | Wired | Yes (primary redesign surface) | No | Medium |
| Background playback + notification | `PlaybackService` | Wired / Unknown (runtime) | Notification copy/icon | No | High |
| Lock screen / headset / MediaSession | Media3 session + `MediaButtonReceiver` | Wired / Unknown (runtime) | No | No | High |
| Audio focus | `AudioFocusCoordinator.kt` | Wired | Settings labels only | No | High |
| Widgets | `PlayerWidgets.kt` | Wired | Branding/layout | No | Medium |
| Android Auto browse | `MediaBrowseTree.kt` | Wired / Unknown (runtime) | Labels | No | High |
| Downloads (SABR + ffmpeg mux) | `:download`, `DownloadsScreen.kt` | Wired Android/desktop; iOS M4A-only | Yes | No | High |
| Play again (resume sessions) | `PlayAgainScreen.kt`, `PlayAgainStore` | Wired | Yes | No | Medium |
| Sleep timer / EQ / speed-pitch | `SleepTimerDialog`, `EqualizerSheet`, Settings | Wired | Yes | No | Medium |
| SponsorBlock | `sponsorblock` package | **Partial — mock API** | Optional | Swap API later | Low |
| PiP | `enablePip`, `PipCoordinator` | Wired Android | Optional | No | Medium |
| Deep links (YT / ymusic hosts) | Manifest + `IncomingLinks` | Wired | Must retarget brand hosts | Engine `resolveLink` careful | High |
| In-app updates | Settings / update seams | Wired | Branding | No | Medium |
| Device link (phone → desktop cookies) | `DeviceLinkScreens.kt` | Wired | Branding | Crypto/API careful | High |
| Guest mode | `AppGraph.guest` | Wired | Copy | No | Low |
| Settings (theme/lang/downloads/…) | `SettingsScreen.kt` | Wired | Yes | No | Low–Medium |
| Telemetry / crash reports | `:telemetry`, Settings consent | Wired | Copy | No | Medium |
| Feedback | `FeedbackScreen.kt` | Wired | Retarget URLs | No | Low |
| Donate / Telegram drawer actions | `App.kt` drawer | Wired | **Remove or replace** for StartMusicV3 | N/A | Low |
| Multi-platform desktop/iOS shells | `desktopApp`, `iosApp` | Present; iOS library gap | Branding | Platform gaps remain | High if “Android-only rewrite” |

---

## C. Playback architecture

### C.1 Flow (as implemented)

```
User action
  (Discover / Library / Search / Now Playing / Notification / Widget / Auto / Share)
        │
        ▼
PlaybackCoordinator  (commonUi/PlaybackCoordinator.kt)
  · playNow / playLocal / playDownloaded / playAgain / …
        │
        ▼
Player / PlayerImpl  (:player)
  · queue (QueueController): shuffle, LoopMode, QueueSource pagination
  · PlaybackConfig (quality, audio-only="zero", volume, speed, EQ, …)
  · routes PlayableItem.LocalMedia → LocalPlayer
  · routes PlayableItem.YtMedia    → YtEngine (MsePlayer)
        │
        ├─ Android local: AndroidLocalPlayer (Media3 ExoPlayer)
        └─ YouTube: MsePlayer → DynamicEnginePlayer (player.js)
                    → QuickJS → :mse native decode
                    · DynamicEngineManager (engine.js) for scrape / PO / proxy
        │
        ▼
StateFlows: player.status · player.progress · player.queue · player.config
        │
        ├─ Compose UI (MiniPlayer / NowPlaying)
        └─ Android parallel:
              YMusicApplication starts PlaybackService when playing
              Media3PlayerAdapter ↔ same Player
              MediaLibrarySession → notification / lock screen / Auto
              AudioFocusCoordinator ducks/pauses via Player
```

### C.2 What must not be rewritten for UI work

- `Player` / `MediaEngine` SPI and engine selection  
- `PlaybackService` + MediaSession wiring  
- QuickJS / MSE / SABR stack  
- Cookie jar ownership (WebView on Android)  
- `PlaybackCoordinator` play APIs (extend, don’t duplicate)

### C.3 Audio-only / quality

Music default is audio-only via quality `"zero"` / `PlaybackConfig.audioOnly` (see `player/README.md` and `PlaybackStatus.kt`). Video qualities and captions are capability selectors on the active engine, surfaced in Now Playing.

---

## D. Feature comparison (user-facing vs YMusic V4 / ymusic.io)

Because **this repository already is a YMusic V4-class codebase**, comparison is against the **reference product experience**, not against an empty app.

| User-facing capability | Status in StartMusicV3 tree |
|------------------------|-----------------------------|
| Browse / Discover YouTube music content | **Already implemented** — needs UI/brand polish |
| YT Music–style hub | **Already implemented** — needs UI/brand polish |
| Search with suggestions | **Already implemented** — needs UI/brand polish |
| Account sign-in | **Already implemented** — needs rebrand of copy/WebView chrome |
| Local library + folder pick/scan | **Already implemented** (Android/desktop); iOS **Missing** |
| Playlists (user + smart) | **Already implemented** — needs UI polish |
| Full player (artwork, queue, transport) | **Implemented but needs UI improvement** (StartMusicV3 identity) |
| Mini-player | **Implemented but needs UI improvement** |
| Background / notification / lock screen | **Already implemented** — Unknown (runtime) |
| Downloads / offline | **Already implemented** — needs UI/brand; iOS format limits |
| Settings (theme, language, playback, downloads) | **Already implemented** — theme palette must change for StartMusicV3 |
| Light / dark / system (+ amoled) themes | **Already implemented** — keep engine; change palettes |
| Shuffle / repeat / queue edit | **Already implemented** |
| EQ / speed / sleep timer | **Already implemented** |
| Widgets / Android Auto | **Already implemented** — Unknown (runtime) |
| SponsorBlock skip | **Partially implemented** (mock API) |
| Independent StartMusicV3 branding | **Missing** (still YMusic everywhere) |
| Centralized non-YMusic brand config | **Missing** (strings/themes scattered; accent exists) |
| Original logo / launcher icon for StartMusicV3 | **Missing** (YMusic assets) |
| Remove YMusic donate/support package coupling | **Requires product decision** — currently wired |
| Replace `ymusic://` / ymusic.io App Links | **Requires risky architectural / store changes** if done carelessly |
| Rename `io.ymusic.*` packages / applicationId | **Requires risky architectural changes** — defer; not needed for UX redesign |
| Rebuild playback on a new stack | **Not applicable / must not do** |

---

## E. Transformation strategy

### E.1 Safe to redesign first (low–medium risk)

1. **Visual design system** in `commonUi/.../Theme.kt` — new StartMusicV3 palette (violet/indigo/cyan direction), keep light/dark/amoled + `themeAccent` plumbing.  
2. **Compose UI chrome** — drawer, list rows, cards, Now Playing / mini-player visuals, empty/loading/error states — without changing play APIs.  
3. **User-visible brand strings** — compose-resources `app_name` and related copy → **StartMusicV3** (all locales). Android `app_name`, themes label, About links.  
4. **Original logo / mipmap icons** — replace drawables; do not copy YMusic marks.  
5. **Central brand constants** — small `Brand` / resource-backed name + default accent, wired into existing `DesignPalette` / `app_name` (no new architecture).  
6. **Drawer product links** — replace or hide Donate / Telegram / Feedback targets that point at YMusic infrastructure until new URLs exist.

### E.2 Leave unchanged (for now)

- `:player`, `:mse`, `:yt-media`, `:quickjs`, `:dynamic-engine` playback/scrape contracts  
- `PlaybackService` / MediaSession / audio focus behavior  
- SQLDelight schemas (`media-store`, downloads, play-history)  
- Cookie / login protocol and device-link crypto  
- Remote `engine.js` / `player.js` update pipeline (backward-compat rules in `engine-js/README.md`)  
- Kotlin package namespaces `io.ymusic.*` and `applicationId` until a deliberate migration plan exists  

### E.3 Must test after each UI/brand tranche

- Play / pause / next / prev from UI, notification, widget  
- YouTube stream + local file + downloaded file  
- Queue shuffle/repeat and Now Playing seek  
- Background continuity (screen off)  
- Theme switch (system / light / dark / amoled) + accent  
- Sign-in still attaches cookies (smoke)  
- Library scan + playlist open  
- Download enqueue → complete → play from Library  

### E.4 Additional implementation (later phases)

| Item | Notes |
|------|--------|
| Real SponsorBlock client | Replace mock when ready |
| New deep-link hosts / scheme | After domain + Play Store association ready |
| applicationId / bundle ID migration | Breaks updates for existing installs — product decision |
| iOS Library / ffmpeg gaps | Separate platform workstreams |
| Legal/About / TERMS rebrand | Align with StartMusicV3 ownership + GPL obligations |
| Remote-config / Sentry / update endpoints | Point off ymusic-io infra when independent infra exists |

### E.5 Do not implement yet

- New duplicate `Player` / `MediaPlayer` / second foreground service  
- Room DB migration off SQLDelight “for cleanliness”  
- Navigation-Compose rewrite of `AppSection` / `AppLayer`  
- Full package rename sweep  
- Claiming feature parity with ymusic.io marketing until runtime QA on this fork  

### E.6 Recommended phase order (next work)

| Phase | Scope | Goal |
|-------|--------|------|
| **0 — this doc** | Audit only | Shared ground truth |
| **1 — Brand foundation** | Name resource, default palette, logo/icon, launcher label | **Done (UI)** — StartMusicV3 palette/logo/name |
| **2 — Design system pass** | Buttons, rows, sheets, settings cards, typography consistency | **In progress** — Brand radii + settings cards |
| **3 — Player surfaces** | Mini-player + Now Playing visual redesign | **Done (UI)** — accent transport, mini-player chrome |
| **4 — Section polish** | Discover / Library / Downloads / Settings | **Done (UI)** — rows, empty states, About brand |
| **5 — Detach YMusic infra** | Links, donate, update hosts, schemes | Not started (deep links / package IDs deferred) |
| **6 — Runtime QA matrix** | Device checklist above | Not started — Unknown (runtime) |

---

## Technical quality notes (non-blocking)

| Observation | Severity for StartMusicV3 UI work |
|-------------|-------------------------------|
| `androidApp/README.md` claims media scanner “not wired” but `YMusicApplication` implements SAF scan | Doc drift — ignore README claim |
| `docs/` was gitignored; `STARTMUSIC_AUDIT.md` allowlisted via `.gitignore` exception | Process |
| SponsorBlock mock | Feature incomplete, unrelated to first redesign |
| Build/ffmpeg sibling dependency | Build environment — not a UI blocker |
| Runtime build/play verification | **Not done** in this audit — treat playback as Unknown (runtime) until tested |

---

## Inventory of primary UI files (quick map)

| Area | Path under `commonUi/src/commonMain/kotlin/io/ymusic/ui/` |
|------|-------------------------------------------------------------|
| Root / nav | `App.kt`, `Drawer.kt`, `AppGraph.kt` |
| Theme | `Theme.kt`, `AccentPicker.kt` |
| Discover / feeds | `Feed.kt`, `AccountLibrary.kt` |
| YT Music | `YtMusicScreen.kt` |
| Search | `SearchScreen.kt`, `LibrarySearch.kt` |
| Library | `LibraryScreen.kt`, `DetailScreens.kt` |
| Player UI | `NowPlaying.kt`, `SleepTimerDialog.kt`, `EqualizerSheet.kt` |
| Downloads | `DownloadsScreen.kt`, `DownloadSettings.kt`, `QuickDownload*.kt` |
| Settings | `SettingsScreen.kt`, `DeviceLinkScreens.kt` |
| Auth | `YtLoginScreen.kt` |
| Playback API | `PlaybackCoordinator.kt` |

Android playback shell: `androidApp/src/main/kotlin/io/ymusic/android/PlaybackService.kt`, `AudioFocusCoordinator.kt`, `YMusicApplication.kt`.

---

## Verdict

StartMusicV3 already contains a **complete premium music-player architecture** (shared Compose UI + Media3 session + local/YouTube engines + library + downloads). The transformation to **StartMusicV3** should proceed as **identity + design-system + player chrome**, preserving the existing playback and data stack. The highest risk is treating this tree as an empty app and replacing services/engines that already implement the desired behavior.
