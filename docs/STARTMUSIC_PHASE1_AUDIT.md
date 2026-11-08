# StartMusicV3 — Phase 1 Complete Project Audit

**Date:** 2026-11-08  
**Product name:** StartMusicV3  
**Scope:** Windows 10/11 desktop + Android phones/tablets  
**Workspace:** `c:\Users\IT\Desktop\StartMusicV3`  
**Remote:** https://github.com/ElionKrasniqi0/StartMusicV3  

**Method:** Source inspection of Gradle modules, shell READMEs, `:commonUi`, `:player`, `:media-store`, `:media-scanner`, `:download`, `:dynamic-engine`, manifests, and desktop media-session code.  
**Constraint:** Audit only — no architecture rewrite, no deletion of working code.

**Runtime note:** Desktop `:desktopApp:run` was previously verified on this machine after building sibling `ffmpeg-build`. Full Android device/emulator verification was blocked here by a missing Android SDK at `local.properties` `sdk.dir`. Feature “works” below means **wired in code** unless marked otherwise.

**Priority for later phases:** Existing functionality > Stability > Correctness > UI quality > Extra features

---

## Executive summary

This repository is **not** a greenfield starter. It is a full **Kotlin Multiplatform + Compose Multiplatform** music client (Gradle root still named `YMusicV4`) with product shells for Android, desktop JVM (Windows/macOS/Linux), and iOS. User-facing branding has been moved toward **StartMusicV3**; packages/`applicationId` remain `io.ymusic.*` by design.

**Do not rebuild playback, navigation, or storage.** Improve and complete the existing stack.

| Target | Status |
|--------|--------|
| Windows desktop | Product-capable: shared UI + libav local + MSE YouTube + library scan + downloads + tray/installer. **Gap:** OS media keys / SMTC. |
| Android | Product-capable: ExoPlayer + MSE + MediaSession FGS + SAF library + widgets + WebView login. **Gap:** SDK/NDK env on this machine; stale README claim about unwired scanner. |

---

## 1. What already works

### 1.1 Shared application (`:commonUi`)

- Compose Multiplatform `App(graph)` with drawer sections: Discover, YtMusic, Library, Play Again, Downloads, Settings, Catalogue, Feedback (+ Donate/Telegram actions).
- Navigation = drawer `AppSection` + per-section `AppLayer` stacks (not Navigation-Compose).
- Mini-player + Now Playing, search, queue, sleep timer, equalizer chrome, downloads UI, About branding via `Brand.kt` / `app_name` = StartMusicV3.
- Playback UI talks to one shared `:player.Player` facade.

### 1.2 Playback stack (`:player` + `:mse` + `:yt-media` + engine)

| Layer | Role |
|-------|------|
| `Player` / `PlaybackConfig` | Queue, shuffle, loop, audio-only, EQ knobs |
| `MediaEngine` SPI | Narrow local / YouTube engines |
| Android local | Media3 **ExoPlayer** (`AndroidLocalPlayer`) |
| Windows/desktop local | **JvmLocalPlayer** + libav `file_av` (waveOut on Windows) |
| YouTube (all shells) | `MsePlayer` + QuickJS `player.js` + `:mse` host |
| Cache / SABR | `:yt-media` (knows nothing about WebView) |
| Scrape / remote JS | `:dynamic-engine` + `:engine-js` + `:quickjs` |

### 1.3 Windows desktop (`:desktopApp`)

- Entry: `desktopApp/.../Main.kt` → Compose window titled StartMusicV3.
- File-backed `media-store.db` + folder scan (okio / Windows folder picker).
- Tray + mini-player popup; close-to-tray when queue non-empty.
- Cookie vault (DPAPI on Windows) + QR / cURL sign-in (no Chromium).
- Single-instance handoff; Jump List (packaged); Inno Setup installer path.
- Packaging `packageName` / vendor = StartMusicV3.

### 1.4 Android (`:androidApp`)

- `YMusicApplication` builds `AppGraph`; `MainActivity` hosts shared `App`.
- `PlaybackService` (Media3 `MediaLibraryService`) + playback notification / MediaSession.
- `DownloadForegroundService` while download queue active.
- **Media store + SAF scanner are wired** (`mediaStoreAndroidDriver`, folder pick/rescan/prune).
- WebView login + `WebViewCookieJar`; device-link QR; widgets; Auto browse tree; PiP; deep links.

### 1.5 Storage / settings / downloads

- `:media-store` SQLDelight (buckets / items / playlists; content-addressed playlist membership).
- `:media-scanner` + ffmpeg metadata on Android SAF and desktop filesystem.
- `:download` offline queue (SABR); completion registers into media-store.
- `:settings` — Android SharedPreferences; desktop SQLite `AppStore`.

---

## 2. What is partially implemented

| Item | Notes |
|------|--------|
| Windows OS media surface | `DesktopMediaSession` API exists; **macOS only**. Windows SMTC / Linux MPRIS return `null`. |
| Windows `ymusic://` OS registration | Open-URI plumbing exists; Windows jpackage block does not register a custom scheme like macOS. |
| Progressive (non-SABR) download | Explicitly unfinished (`E70010` in download pipeline). |
| SponsorBlock | Service API when remote API present (older docs still say “mock”). |
| Branding vs identity | Display name StartMusicV3; packages, themes (`Theme.YMusicV4`), deep-link hosts still YMusic-oriented. |
| Desktop vs phone layout | Shared screens; desktop wide layout / holes exist but polish is ongoing. |
| Doc accuracy | Several READMEs/docs drift from code (see §6). |

---

## 3. What is missing (Windows + Android focus)

| Missing | Impact | Suggested later phase |
|---------|--------|----------------------|
| **Windows SMTC / media keys** | No lock-screen / hardware-key parity with Android MediaSession | Additive `DesktopMediaSession` Windows backend |
| Android SDK/NDK on this host | Cannot install/run APK here until SDK installed | Environment setup (not app rewrite) |
| Sibling `ffmpeg-build` pack | Blocks desktop local play, scan metadata, mux, MSE JNI | Keep sibling checkout; already required by README |
| Progressive download formats | Some non-SABR streams fail intentionally | Only if product needs them |
| Domain / deep-link rebrand | Links still `ymusic://` / ymusic.io hosts | Careful migration; do not break existing users casually |
| Package / applicationId rename | Still `io.ymusic.*` | Optional; high risk; not required for product name |

Out of scope for this Phase 1 focus but real: **iOS `mediaStore = null`** (Library local files unwired on iOS).

---

## 4. What needs improvement

1. **Correct stale docs** — especially `androidApp/README.md` (“Not wired yet — media store/SAF scanner”).
2. **Windows SMTC** — largest functional gap vs Android lock-screen controls.
3. **Verify Android runtime** on device/emulator after SDK install (`installDebug`).
4. **UI polish** under StartMusicV3 brand without touching engines.
5. **Keep ffmpeg/MinGW toolchain documented** — Windows natives are GCC/MinGW, not MSVC.
6. **Avoid dual player stacks** — never add a second playback architecture beside `:player`.
7. **Line-ending noise on Windows** — many working-tree `M` files are CRLF-only; do not commit them.

---

## 5. Shared vs platform-specific

### Keep shared

- `:commonUi` screens, theme/`Brand`, strings, playback chrome  
- `:player` facade + MSE YouTube path  
- `:media-store` / `:download` / `:settings` APIs  
- `:dynamic-engine` / `:engine-js` / `:quickjs` / `:yt-media` / `:image-loader` / `:log-store`

### Must stay platform-specific

| Android | Windows / desktop JVM |
|---------|------------------------|
| `PlaybackService`, notification, MediaSession, audio focus | Compose window + `VideoUnderlay` Canvas |
| `DownloadForegroundService` | AWT tray / Jump List |
| WebView cookie jar + in-app login | CookieVault + QR/cURL (no WebView) |
| SAF grants / DocumentsContract | Filesystem folder pick + JDBC media-store |
| Widgets, Auto, PiP, PackageInstaller updates | jpackage / Inno Setup / DPAPI |
| Manifest permissions | MinGW + sibling ffmpeg pack |

---

## 6. Documentation drift (audit corrections)

| Claim | Reality |
|-------|---------|
| `androidApp/README.md`: media store/SAF “Not wired yet” | **Wired** in `YMusicApplication.kt` |
| Some docs: SponsorBlock still “mock” | Prefer `ServiceSponsorBlockApi` when API client exists |
| `commonUi/README.md`: no Windows single-instance | Single-instance **exists** for Windows/Linux |
| Root Gradle / packages named YMusic | Intentional internal identity; user-facing name is StartMusicV3 |

---

## 7. Build / runtime risks (this machine)

| Risk | Detail |
|------|--------|
| ffmpeg | Desktop/Android natives expect `../ffmpeg-build/out/<slug>/pack` |
| MinGW | `local.properties` `mingw.dir=C:/msys64/mingw64` |
| Android SDK | `sdk.dir` points at a path that was **missing** during earlier checks |
| NDK | README/CI expect NDK r28 `28.2.13676358` for Android natives |
| `syncFfmpegHost` on Windows | Fixed to fall back when Unix `cp` is absent from PATH |

---

## 8. Unfinished markers

Few classic `TODO`/`FIXME` in product shells. Gaps are mostly **documented absences**:

- Windows/Linux: no `DesktopMediaSession` backend (`DesktopMediaSession.kt`).
- Download: progressive not supported (`E70010`).
- Player: several intentional **no-op** branches when engines/queue absent (normal SPI).

---

## 9. Recommended next phases (no work started here)

1. **Fix doc drift** (scanner README) — low risk.  
2. **Android env + `installDebug` smoke test** — verify MediaSession, SAF library, notifications.  
3. **Windows SMTC (additive)** — only after explicit approval; do not disturb macOS Now Playing.  
4. **UI polish** under StartMusicV3 — shared Compose only.  
5. **Defer** package renames, deep-link domain changes, Flutter/rewrite migrations.

---

## 10. Module map (quick reference)

```
:androidApp  |  :desktopApp (Windows JVM)  |  iosApp (out of Phase 1 focus)
                    │
                    ▼
              :commonUi  App(graph)
                    │
     ┌──────────────┼──────────────┐
     ▼              ▼              ▼
 :player      :dynamic-engine   :media-store / :download / :settings
 MediaEngine   + engine-js       SQLDelight + SettingsStore
 Local+MSE     + quickjs
```

Root includes (from `settings.gradle.kts`): `:commonUi`, `:desktopApp`, `:androidApp`, `:player`, `:mse`, `:yt-media`, `:media-store`, `:media-scanner`, `:download`, `:dynamic-engine`, `:engine-js`, `:quickjs`, `:settings`, `:remote-config`, `:telemetry`, `:log-store`, `:image-loader`, `:kmp*`, `:projectCommon`, playgrounds, `:iosAppKotlin`.

---

**Phase 1 verdict:** StartMusicV3 already has professional Windows and Android product shells over one shared music app. The job is **complete, verify, brand, and fill known gaps (especially Windows SMTC)** — not replace the architecture.
