# StartMusicV3 — Database, Platforms, Sources, Build & CI

**Date:** 2026-11-08  
**Repo truth:** inspected schemas, shells, `release.yml`, player/desktop media session code  
**Architecture decision:** [`STARTMUSIC_MULTIPLATFORM_ARCHITECTURE.md`](STARTMUSIC_MULTIPLATFORM_ARCHITECTURE.md)  
**Product name:** StartMusicV3 (configurable)  

**No architecture rewrite in this document.** No migrations applied. No secrets documented.

---

# 9. Database and user data

## 9.1 Existing technology

| Store | Technology | Module / location |
|-------|------------|-------------------|
| Local library | **SQLDelight → SQLite** | `:media-store` |
| Downloads queue | **SQLDelight → SQLite** | `:download` |
| Player segment/blob cache | **SQLDelight** | `:yt-media` |
| App logs | **SQLDelight** | `:log-store` |
| Settings (Android) | SharedPreferences (`PrefsSettingsStore`) | `:androidApp` |
| Settings + cookies (desktop) | SQLite `app.db` (`AppStore` / CookieVault) | `:desktopApp` |
| Play history / Play again | SQLite (commonUi play-history) | `:commonUi` |

**Do not migrate** these stores to Room, Core Data, Realm, or Flutter storage as part of multi-platform work. SQLDelight **already** targets commonMain with platform drivers.

## 9.2 Library schema (media-store)

Tables (commonMain `.sq`):

| Table | Purpose |
|-------|---------|
| `bucket` | Folder tree (uri, parent, soft-delete, excluded) |
| `mediaItem` | File row: uri (unique), title/artist/album/genre, duration, codecs, `youtubeVid`, fingerprint (`fileSize`/`lastModified`), soft-delete |
| `playlist` | User playlist names |
| `playlistItem` | Membership by content **`code`** (not raw row FK) — `youtubeVid` or `@fileSize:duration` |

Migrations present: `media-store/.../migrations/1.sqm` … `4.sqm` (SQLDelight versioned migrations).  
Downloads: `download/.../db/1.sqm` … `4.sqm`.  
yt-media cache: `1.sqm`, `2.sqm`.

## 9.3 File storage / metadata

- **Local files:** referenced by URI; metadata extracted by `:media-scanner` + `:kmpFfmpeg` (no-op on iOS).  
- **Android:** SAF document trees.  
- **Desktop:** filesystem URIs via okio DocumentsProvider.  
- **Downloads:** completed files on disk + rows in download DB + optional media-store registration.  
- **Artwork:** URLs / local art helpers via `:image-loader` (not a second DB).

## 9.4 “Favorites”

There is **no separate `favorites` SQL table** in media-store. Closest behaviors:

- User **playlists** (local DB)  
- Smart lists (Recent / Top tracks) via play-history  
- Signed-in **YouTube account library** (engine feeds — not local favorites rows)

Do not invent a favorites table without a product decision and a **non-destructive** additive migration.

## 9.5 Cross-platform sharing of the database

| Question | Answer |
|----------|--------|
| Can schema/models be shared? | **Yes** — already commonMain SQLDelight |
| Can one DB file be shared across phones/PCs? | **No** (different devices/paths); sync would be a separate product feature |
| Can iOS use the same schema? | **Yes in principle** — driver compiles for iosArm64; **graph currently passes `mediaStore = null`** |
| Settings shared across OS? | Same **keys** (`AppSettings`); different **backends** per shell |

## 9.6 Preserve / do not destroy

**Preserve:** track metadata, playlists, settings, download tasks, play history, cookies (platform vaults).  

**Forbidden without an explicit safe plan:** clearing DBs, dropping tables, renumbering migrations, “fresh install” resets, framework swaps.

**If a migration is ever required:** explain why → list affected rows → additive SQLDelight `.sqm` → preserve records → test upgrade from prior DB file. **Do not proceed if preservation cannot be established.**

**Current recommendation:** **No migration required** for multi-platform expansion. Wire iOS to existing media-store; keep Android/desktop DBs untouched.

---

# 10. Windows implementation

## 10.1 Correct architecture (existing)

| Layer | Choice in this repo |
|-------|---------------------|
| Desktop framework | **Compose Multiplatform Desktop** (`:desktopApp`) |
| Language | Kotlin / JVM |
| Audio (local) | `JvmLocalPlayer` + libav (`file_av.c`) → **waveOut** on Windows |
| Audio/video (YouTube) | Shared `MsePlayer` / MSE |
| File access | okio filesystem + media-scanner |
| UI | Shared `:commonUi` App with desktop window / layout-hole patterns |
| Packaging | Compose `jpackage` + **Inno Setup** (Windows block in `desktopApp/build.gradle.kts`) |
| Settings/cookies | SQLite `app.db`; cookie key via **DPAPI** on Windows |

This is **not** WinUI/WPF/UWP and **not** Android services on Windows.

## 10.2 Feature readiness

| Feature | Status |
|---------|--------|
| Branding (StartMusicV3 shared UI) | Shared tokens/logo direction |
| Library / scan | Yes (file-backed store) |
| Search / player / queue / playlists / settings | Yes (shared UI + Player) |
| Play/pause/prev/next/progress | Yes (`Player`) |
| Resizable window / mouse | Yes |
| Keyboard shortcuts | Yes (`KeyboardShortcuts.kt`, shortcuts help) |
| Desktop-appropriate layout | Partially specialized (not a blind mobile stretch); continue polish carefully |
| **OS media controls (SMTC)** | **Not implemented** — `DesktopMediaSession.create` only returns a backend for **macOS**; Windows/Linux return `null` (documented in code) |

## 10.3 Build requirements (Windows developer machine)

From root `README.md` / desktop module:

- **JDK 17+** with **jpackage** (full JDK, not JBR-only) for packaging  
- **Node.js** (engine-js)  
- **CMake 3.20+**  
- **MSYS2 MinGW** for native libs (`mingw.dir` / `MINGW_HOME`)  
- Sibling **`ffmpeg-build`** checkout built for Windows  
- Optional: Inno Setup 6 for installer  

**Run:** `./gradlew :desktopApp:run`  
**Package:** Compose Desktop / project packaging tasks (see `desktopApp/build.gradle.kts`)

## 10.4 Implementation stance

- **Do not** re-architect Windows.  
- **Verify** run on this PC; fix branding/UI only as needed.  
- **SMTC / media keys:** optional future **additive** backend behind existing `DesktopMediaSession` interface — justify before implementing; do not fake OS controls in UI.

---

# 11. iOS implementation

## 11.1 Correct architecture (existing)

| Layer | Choice |
|-------|--------|
| Shared UI | Compose Multiplatform (`:commonUi`) |
| Host | SwiftUI `iOSApp` → `MainViewControllerKt.MainViewController()` (`YMusicApp` framework) |
| Local playback | AVFoundation `AppleLocalPlayer` |
| YouTube | MSE + QuickJS player.js |
| Audio session | `AVAudioSessionCategoryPlayback` in Kotlin (`AudioSession.ios.kt`) **and** Swift `iOSApp.init` |
| Background mode | `UIBackgroundModes: audio` in `Info.plist` |
| Library | **`mediaStore = null`** in `iosAppKotlin/.../MainViewController.kt` → empty Library |

## 11.2 Feature readiness

| Feature | Status |
|---------|--------|
| Player / search / settings / branding name | Shared UI present |
| Playlists / local library | **Blocked** until media-store wired |
| Favorites | Same as §9.4 (no local favorites table) |
| Background audio config | Declared + session category set |
| Lock screen / remote commands | **Not runtime-verified in this audit** — do **not** claim working without device proof |
| Interruptions / route changes | Needs device verification |
| Simulator | **No iosX64**; Apple Silicon `iosSimulatorArm64` only; Intel Mac → device |

## 11.3 Build requirements (honest)

| Requirement | Detail |
|-------------|--------|
| Host OS | **macOS** with Xcode — **cannot fully build/sign iOS on Windows-only** |
| Xcode / SDK | Prefer **Xcode 26 / iOS 26 SDK** for clean Compose MP link |
| Workaround | `-PiosAllowUndefined=true` for older Xcode — **dev/test only, not App Store** |
| Signing | Apple Team in Xcode (manual; CI secrets not auto-published here) |
| Gradle | `:iosAppKotlin:embedAndSignAppleFrameworkForXcode` from Xcode build phase |

See `iosApp/README.md`.

## 11.4 Background audio stance

Implement **only** via supported iOS mechanisms (already partially present: category + background mode).  

**Verification required before claims:** play → lock device → confirm audio + controls; interrupt with call/Siri; change route (Bluetooth). Until then: status = **configured, unverified**.

## 11.5 Implementation stance

1. Plan iOS `mediaStore` wiring (reuse SQLDelight schema — **additive platform driver use**, not schema wipe).  
2. Device-verify audio session / lock screen.  
3. Finish StartMusicV3 icons/usage strings.  
4. Do not use Android MediaSession on iOS.

---

# 12. Music sources and API compatibility

## 12.1 Existing integrations (from code)

| Source | Mechanism | Shared? |
|--------|-----------|---------|
| **YouTube / YouTube Music feeds & search** | `:dynamic-engine` + remote/bundled **engine.js** (InnerTube-style yt / ytmusic commands) | Yes — all platforms with engine host |
| **YouTube playback** | **player.js** + MSE (`MsePlayer`) + SABR/UMP via `:yt-media` | Yes — Android/desktop/iOS engines |
| **Local files** | media-scanner + ffmpeg metadata + `LocalPlayer` | Android/desktop; iOS gap until store |
| **Downloads / offline** | SABR download + optional ffmpeg mux | Android/desktop; iOS M4A-oriented / no ffmpeg |
| **Account library** | Signed-in engine feeds | Needs cookies (WebView / CookieVault / device-link) |

There is **no separate commercial streaming SDK** (Spotify/Apple Music) in this tree. Do not invent providers or fake results.

## 12.2 Authentication

- Android: WebView + shared `CookieManager`  
- Desktop: CookieVault + QR/cURL device-link  
- iOS: device-link / session seams (camera + Face ID strings in plist)  

Secrets: signing/Sentry via `local.properties` / `secrets.enc` — **never hardcode**; keep gitignored plaintext secrets.

## 12.3 Per-platform compatibility

| Source | Android | Windows | iOS |
|--------|---------|---------|-----|
| YouTube browse/search | Yes | Yes | Yes (engine) |
| YouTube stream play | Yes | Yes | Yes (MSE) |
| Local library | Yes | Yes | **No store wired** |
| Offline downloads | Yes | Yes | Limited formats |
| Sign-in | WebView | CookieVault | Device-link path |

**Limitations to document (not hide):** iOS local library; Windows SMTC absent; provider ToS/copyright — app must not claim to bypass restrictions; remote engine updates must keep host compat (`engine-js` checklist).

**Do not** duplicate entire source stacks per OS when the shared engine already runs.

---

# 13. Build system

## 13.1 Android

| Item | Guidance |
|------|----------|
| Config | Preserve existing Gradle / AGP 9 `:androidApp` |
| Command | `./gradlew :androidApp:installDebug` / `assembleRelease` (release needs signing secrets) |
| Needs | JDK 17+, Android SDK, NDK r28 (as CI), Node for engine-js, ffmpeg Android packs |

## 13.2 Windows

| Item | Guidance |
|------|----------|
| Framework | Compose Desktop (`:desktopApp`) |
| Command | `./gradlew :desktopApp:run` |
| Packaging | jpackage + Inno Setup (see module) |
| Needs | Full JDK, MSYS2 MinGW, CMake, Node, sibling ffmpeg Windows pack |

## 13.3 iOS

| Item | Guidance |
|------|----------|
| Environment | **macOS + Xcode** required |
| Command | Open `iosApp/iosApp.xcodeproj` (after `ruby gen_xcodeproj.rb`); Run on device/sim (Apple Silicon) |
| Signing | Xcode Team / certificates — local developer responsibility |
| **Windows-only machine** | Can edit shared Kotlin; **cannot** produce a fully signed iOS app locally |

**Do not invent successful iOS builds** from Windows CI or this Windows workspace without a macOS runner.

---

# 14. CI/CD

## 14.1 What already exists

File: `.github/workflows/release.yml`  

Triggered by **`release-*` tags** (not every push). Builds:

| Job area | Runner examples |
|----------|-----------------|
| ffmpeg packs | ubuntu, macos, windows (incl. ARM variants) |
| **android** | `ubuntu-24.04` → assembleRelease APKs |
| **macos** desktop | macos runners |
| **windows** desktop | windows runners |
| **linux** desktop | ubuntu runners |

Permissions: `contents: read` at workflow level; draft release / artifact upload patterns live in the workflow — **do not casually widen secrets exposure**.

## 14.2 iOS in CI

**No dedicated iOS app job** was found in `release.yml` job names (`android` / `macos` / `windows` / `linux` desktop). Shipping an iOS IPA would need a **macOS** runner, Apple signing secrets, and an explicit workflow — **not auto-created here**.

## 14.3 Policy for this project

| Action | Stance |
|--------|--------|
| Evaluate automated builds | **Already present** for Android + desktop via release tags |
| Auto-add CI dependencies | **Do not** without approval |
| Auto-publish store releases | **Do not** |
| Auto-upload binaries beyond existing draft/release design | **Do not** expand blindly |
| Secrets in logs / commits | **Forbidden** |

## 14.4 Environments summary

| Target | CI possible? | Notes |
|--------|--------------|-------|
| Android | Yes (existing) | Needs ffmpeg cache + signing for Play |
| Windows desktop | Yes (existing) | MSYS2 / Inno on runners |
| macOS/Linux desktop | Yes (existing) | |
| iOS app | Only with macOS + Apple creds | Not claimed working from Windows |

---

## Cross-cutting recommendations

1. **Database:** keep SQLDelight; no framework migration; wire iOS store without wiping Android/desktop data.  
2. **Windows:** run and verify; treat SMTC as optional additive work.  
3. **iOS:** Library wiring + **device** background/lock verification before any “works” claim.  
4. **Sources:** keep shared engine/MSE; document platform gaps; no fake APIs.  
5. **Build/CI:** document honestly; iOS needs macOS; do not invent CI success.  

**Priority remains:** existing functionality and user data integrity over new platforms polish.
