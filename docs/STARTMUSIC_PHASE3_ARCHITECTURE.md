# StartMusicV3 — Phase 3 Application Architecture

**Date:** 2026-11-08  
**Product:** StartMusicV3  
**Scope:** Windows 10/11 + Android (phones/tablets)  
**Status:** **Decision locked — extend existing architecture; no framework migration**

**Companions:** [`STARTMUSIC_PHASE1_AUDIT.md`](STARTMUSIC_PHASE1_AUDIT.md) · [`STARTMUSIC_MULTIPLATFORM_ARCHITECTURE.md`](STARTMUSIC_MULTIPLATFORM_ARCHITECTURE.md) · [`AGENTS.md`](../AGENTS.md)

---

## 1. Decision

| Question | Answer |
|----------|--------|
| Primary stack | **Kotlin Multiplatform** already in this repo |
| Shared UI | **Compose Multiplatform** (`:commonUi`) |
| Windows | **`:desktopApp`** — JVM + Compose Desktop + native window |
| Android | **`:androidApp`** — AGP app + Compose + lifecycle + Media3 services |
| Flutter / React Native / Electron | **Rejected** — would discard a working music stack |
| Separate native rewrites | **Rejected** — triples UI/playback maintenance |

**Rule:** Prefer sharing business logic and reusable UI. Keep platform services, audio sessions, notifications, storage drivers, and permissions in the shells.

**Priority:** Existing functionality > Stability > Correctness > UI > Extras

---

## 2. Layered shape (as implemented)

```
┌──────────────────────────────────────────────────────────────┐
│ Product shells (platform entry + AppGraph construction)      │
│   :androidApp          :desktopApp (Windows JVM)             │
└────────────────────────────┬─────────────────────────────────┘
                             │ AppGraph
                             ▼
┌──────────────────────────────────────────────────────────────┐
│ :commonUi — Compose MP App (drawer, feeds, NP, settings)     │
│   Brand / Theme · PlaybackCoordinator · IncomingLinks        │
└───────┬──────────────────────┬───────────────────┬───────────┘
        ▼                      ▼                   ▼
   :player.Player        :dynamic-engine      :media-store
   MediaEngine SPI        + engine-js          :media-scanner
   Local + Yt/MSE         + quickjs            :download
                                               :settings
```

Root modules (from `settings.gradle.kts`): `:commonUi`, `:desktopApp`, `:androidApp`, `:player`, `:mse`, `:yt-media`, `:media-store`, `:media-scanner`, `:download`, `:dynamic-engine`, `:engine-js`, `:quickjs`, `:settings`, `:remote-config`, `:telemetry`, `:log-store`, `:image-loader`, `:kmpCommon`, `:kmpHttp*`, `:kmpFfmpeg`, `:projectCommon`, plus iOS/playground labs (out of Phase 3 focus).

---

## 3. What is shared

| Concern | Module / location | Notes |
|---------|-------------------|--------|
| Screens & navigation | `:commonUi` | Drawer `AppSection` + `AppLayer` stacks |
| Design system | `Brand.kt` / `Theme.kt` | One palette/type scale for Win + Android |
| Playback facade | `:player` `Player` / `PlaybackConfig` | UI never talks to Exo/libav directly |
| YouTube MSE path | `:player` `MsePlayer` + `:mse` + `:yt-media` | Shared across shells |
| Scrape / remote JS | `:dynamic-engine` / `:engine-js` / `:quickjs` | Host protocols stay versioned |
| Library models | `:media-store` (SQLDelight) | Content-addressed playlist membership |
| Offline queue API | `:download` | Shells supply storage + FGS/tray behavior |
| Settings descriptors | `:settings` `AppSettings` | Store impl is per shell |
| HTTP | `:kmpHttp` | OkHttp on JVM/Android |

---

## 4. Windows — platform-specific (keep in `:desktopApp`)

| Requirement | Existing implementation |
|-------------|-------------------------|
| JVM desktop | `:desktopApp` Compose Desktop application |
| Compose Desktop UI | Hosts shared `App(graph)` |
| Native Windows window | Compose/AWT window; title **StartMusicV3** |
| Keyboard / mouse | Desktop shortcuts, tray, Jump List |
| Local playback | `JvmLocalPlayer` + libav (`file_av` / waveOut) |
| YouTube | Shared `MsePlayer` + `VideoUnderlay` Canvas |
| Library | JDBC `media-store.db` + filesystem scanner |
| Auth | CookieVault (DPAPI) + QR / cURL (no WebView) |
| Packaging | jpackage + Inno → `StartMusicV3.exe` / installer |
| Toolchain | JDK 21, MinGW natives, sibling `ffmpeg-build` |

**Not applicable on Windows:** Android FGS, MediaSession notification, SAF, widgets, Auto.

**Known additive gap (later phase, not a stack change):** Windows SMTC / OS media keys (`DesktopMediaSession` is macOS-only today).

---

## 5. Android — platform-specific (keep in `:androidApp`)

| Requirement | Existing implementation |
|-------------|-------------------------|
| Native Android app | AGP 9 `:androidApp` |
| Compose UI | `MainActivity` → shared `App(graph)` |
| Lifecycle | Application-scoped graph; Activity for SAF / permissions |
| Media playback services | `PlaybackService` (Media3 `MediaLibraryService`) |
| Notification / background | Playback FGS + MediaSession; download FGS |
| Local playback | ExoPlayer (`AndroidLocalPlayer`) |
| YouTube | Shared `MsePlayer` + TextureView surface |
| Library | SQLDelight + **SAF** scanner (wired in `YMusicApplication`) |
| Auth | WebView login + `WebViewCookieJar` |
| Other | Widgets, Auto browse, PiP, deep links |

**Must not move into commonMain:** `PlaybackService`, audio focus, notification channels, WebView cookie ownership, SAF grants, widgets.

---

## 6. Module ownership rules (maintainability)

1. **UI chrome / brand** → `:commonUi` only (`Brand`, resources, screens).  
2. **Playback behavior** → `:player` (+ engines); shells only construct engines and surfaces.  
3. **Library schema** → `:media-store`; scanners via `:media-scanner` providers.  
4. **Downloads** → `:download` queue; shells register completion + OS foreground work.  
5. **Engine wire / natives** → follow `engine-js` backward-compat checklist; no casual renames.  
6. **New features** → prefer `commonMain` + thin `expect`/`actual` or shell callbacks on `AppGraph`.  
7. **No second player stack** and **no second navigation framework** without an explicit approved migration.

---

## 7. Explicit non-goals for Phase 3

- Do **not** introduce Flutter, React Native, Electron, or a parallel UI toolkit.  
- Do **not** rewrite Windows as WinUI/WPF or Android as a separate non-Compose app.  
- Do **not** merge Android services into shared code “for reuse.”  
- Do **not** rename `applicationId` / packages / deep-link schemes in this phase.  
- Do **not** delete working modules to “simplify.”

---

## 8. Scalability path (without changing architecture)

| Growth | Approach |
|--------|----------|
| New screen | `:commonUi` + strings in all locales |
| New setting | `AppSettings` + shell store already persists |
| New playback capability | Extend `MediaEngine` capability interfaces / `PlaybackConfig` |
| New OS integration (e.g. SMTC) | Additive desktop backend behind existing `DesktopMediaSession` SPI |
| New storage location | Shell `DocumentsProvider` / download storage — keep SQLDelight models shared |

---

## 9. Verdict

StartMusicV3 **already has** the modular KMP architecture Phase 3 asks for:

- Shared business logic and Compose UI where appropriate  
- Windows JVM / Compose Desktop shell with native window and input  
- Android shell with lifecycle, Media3 playback service, notifications, background playback  

**Action for later phases:** implement features and polish **on this architecture**. Architecture work here is **documentation and discipline**, not a migration.
