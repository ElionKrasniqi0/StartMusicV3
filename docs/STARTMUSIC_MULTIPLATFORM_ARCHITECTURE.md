# StartMusicV3 — Multi-Platform Architecture Decision Report

**Date:** 2026-11-08  
**Repository:** https://github.com/ElionKrasniqi0/StartMusicV3 (local: `c:\Users\IT\Desktop\StartMusicV3`)  
**Reference (inspiration only):** https://github.com/ymusic-io/YMusicV4 · https://ymusic.io/  
**Configurable product name:** StartMusicV3  

**Companion docs:**  
[`STARTMUSIC_AUDIT.md`](STARTMUSIC_AUDIT.md) · [`STARTMUSIC_TRANSFORMATION.md`](STARTMUSIC_TRANSFORMATION.md) · [`STARTMUSIC_MULTIPLATFORM.md`](STARTMUSIC_MULTIPLATFORM.md)

**Method:** Source inspection of Gradle modules, shell READMEs, `:player`, `:commonUi`, `:media-store`, Android/desktop/iOS wiring.  
**Constraint for this document:** architecture description and plan only — **no architecture modification** in the same step as authoring this report.

**Priority:** Existing functionality > Stability > Correctness > UI quality > Additional features  

---

## A. Existing architecture

### A.1 Language and framework (repository facts)

| Question | Finding |
|----------|---------|
| Language | **Kotlin** (Multiplatform) |
| Primary UI | **Compose Multiplatform** in `:commonUi` |
| Android UI host | Compose in `MainActivity` (AGP application module) |
| Desktop UI host | Compose Desktop JVM (`:desktopApp`) |
| iOS UI host | SwiftUI `@main` → Kotlin `MainViewController` → shared Compose `App` |
| Flutter / RN / Java-only | **Not** the product architecture |

Root modules and name: `settings.gradle.kts` (`rootProject.name = "YMusicV4"`; includes `:androidApp`, `:desktopApp`, `:iosAppKotlin`, `:commonUi`, `:player`, `:media-store`, `:download`, `:dynamic-engine`, …).

### A.2 High-level shape

```
Product shells (platform entry + AppGraph construction)
  :androidApp  |  :desktopApp (Windows/macOS/Linux JVM)  |  iosApp + :iosAppKotlin
                              │
                              ▼
                    :commonUi  App(graph)
                    drawer sections · feeds · library · NP · settings
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
        :player.Player   :dynamic-engine   :media-store / :download / :settings
        MediaEngine SPI   + engine-js        SQLDelight + SettingsStore
        Local + Yt/MSE    + quickjs
```

### A.3 UI architecture

- **Shared:** Compose Multiplatform screens in `commonUi/src/commonMain/...` (`App.kt`, Discover, Library, Now Playing, Settings, Search, Downloads).
- **Navigation:** Drawer `AppSection` + per-section `AppLayer` stacks — **not** Navigation-Compose; **not** forced identical chrome on every form factor (desktop uses layout-hole / wider patterns; mobile uses drawer + mini player).
- **Design tokens:** `Theme.kt` / `Brand.kt` (StartMusicV3 palette); strings via compose-resources (~50 locales).
- **Android-only XML:** home-screen widgets (`RemoteViews`), themes/manifest — not the main in-app UI.
- **iOS-only Swift:** thin `iOSApp.swift` host; UI is still shared Compose.

### A.4 Playback engine

| Layer | Role | Location |
|-------|------|----------|
| Shared facade | `Player` / `PlayerImpl`, queue, shuffle, repeat, `PlaybackConfig` | `:player` commonMain |
| Shared SPI | `MediaEngine`, `LocalPlayer`, `YtEngine` | `:player` |
| Shared YouTube | `MsePlayer` + QuickJS `player.js` + `:mse` | common / native |
| Android local | Media3 **ExoPlayer** (`AndroidLocalPlayer`) | `player` androidMain |
| Desktop local (Windows included) | **JvmLocalPlayer** + libav `file_av.c` (waveOut on Windows) | `player` jvmMain + `:mse` |
| Apple local | **AVFoundation** `AppleLocalPlayer` + `AVAudioSession` on iOS | `player` appleMain / iosMain |
| Android session shell | `PlaybackService` (Media3 `MediaLibraryService`), notification, MediaSession, audio focus | `:androidApp` only |

Shared playback **interface already exists** (`Player`: play/pause/seek/next/previous, `status`/`progress`/`queue`, shuffle/loop, config). Platform engines implement `MediaEngine`; the facade is the single UI-facing API. **Do not add a second player stack.**

### A.5 Database and storage

| Store | Technology | Scope |
|-------|------------|--------|
| Local library | SQLDelight (`:media-store`) — buckets, media items, playlists (content-addressed membership) | Android + desktop wired; **iOS graph sets `mediaStore = null`** today |
| Downloads | SQLDelight (`:download`) | Android/desktop; iOS format-limited (no ffmpeg → M4A-oriented) |
| Settings | `SettingsStore` / `AppSettings` — Android SharedPreferences; desktop SQLite `AppStore` | Per shell |
| Play history / Play again | SQLite via commonUi play-history | Where shell provides DB |
| Cookies | Android WebView `CookieManager`; desktop `CookieVault` (DPAPI on Windows) | Platform-specific |

### A.6 Notable dependencies (selected)

- Kotlin Multiplatform + Compose Multiplatform  
- Media3 (Android playback / session)  
- SQLDelight  
- OkHttp / `:kmpHttp`  
- QuickJS + engine-js / player.js (remote-updatable)  
- ffmpeg packs (sibling `ffmpeg-build`; required for desktop local + Android metadata/mux; **no-op on iOS** per project docs)  
- Image loading via `:image-loader`  

### A.7 Android-specific components (must stay Android)

- `PlaybackService` / Media3 MediaSession / notification / MediaButtonReceiver  
- `AudioFocusCoordinator`  
- `DownloadForegroundService`  
- WebView login + `WebViewCookieJar`  
- SAF folder pick / scanner provider  
- Home-screen widgets, Android Auto browse tree  
- Manifest permissions (`FOREGROUND_SERVICE_*`, `POST_NOTIFICATIONS`, media read, etc.)  
- `PrefsSettingsStore`  

These must **not** be forced into commonMain or reused on Windows/iOS.

---

## B. Platform compatibility

### B.1 Windows (via `:desktopApp` JVM)

| Aspect | Assessment |
|--------|------------|
| **Existing compatibility** | **Yes — first-class.** Compose Desktop app; `windows { }` packaging in `desktopApp/build.gradle.kts`; Windows DPAPI cookie key; waveOut audio path in libav stack. |
| **Required implementation** | Run/verify on Windows; confirm media keys / System Media Transport Controls if product requires them; keep ffmpeg/MinGW toolchain per root README; StartMusicV3 branding already mostly shared. |
| **Unsupported / N/A** | Android FGS, MediaSession, SAF, widgets, Auto. |
| **Dependencies** | JDK with jpackage (packaging), MinGW natives, sibling ffmpeg pack, Compose Desktop. |
| **Technical risks** | Missing ffmpeg/Mingw → local playback fails; SMTC/media-key parity may be incomplete vs Android (verify before claiming); single-instance / `ymusic://` on Windows noted incomplete in commonUi docs. |

### B.2 Android (`:androidApp`)

| Aspect | Assessment |
|--------|------------|
| **Existing compatibility** | **Yes — foundation product.** Full shell over shared UI. |
| **Required implementation** | **Preserve.** Branding/UI polish only unless a justified migration plan exists. Verify playback after UI diffs. |
| **Unsupported** | N/A for Android core features already shipped in tree. |
| **Dependencies** | AGP 9, Media3, NDK/ffmpeg packs for some paths, WebView. |
| **Technical risks** | Breaking `PlaybackService` or cookie ownership; package/`applicationId` rename without migration; treating README “scanner not wired” as truth (implementation exists in `YMusicApplication`). |

### B.3 iOS (`iosApp` + `:iosAppKotlin`)

| Aspect | Assessment |
|--------|------------|
| **Existing compatibility** | **Partial–substantial.** Shared Compose `App`; AVFoundation local + MSE YouTube; `AVAudioSessionCategoryPlayback`; background audio mode in Info.plist. |
| **Required implementation** | Wire `mediaStore` (today `null` in `MainViewController.kt`); verify lock-screen / Now Playing controls on device; shippable Xcode 26 link (avoid App Store with `iosAllowUndefined`); finish branding assets/copy. |
| **Unsupported / gaps** | Local library empty until store wired; limited local video surface; no Intel iosX64 Simulator; ffmpeg no-op → download format limits. |
| **Dependencies** | Xcode (preferably 26 / iOS 26 SDK), Kotlin/Native, Compose MP iOS targets. |
| **Technical risks** | Toolchain/SDK mismatch; claiming Library works while `mediaStore == null`; dynamic_lookup builds not store-ready. |

---

## C. Architecture comparison

Evaluated against **this repository**, not popularity.

| Criterion | Kotlin Multiplatform | Compose Multiplatform | Flutter | Separate native apps |
|-----------|----------------------|-------------------------|---------|----------------------|
| Fits existing code | **Already adopted** | **Already adopted for UI** | Would discard stack | Would fork three UIs |
| Playback | Shared `Player` + expect engines | N/A (UI only) | New audio bridge required | Triple playback stacks |
| Database | Shared SQLDelight models | N/A | New persistence | Drift risk |
| Windows | Via existing JVM desktop | Via Compose Desktop | Possible but greenfield | New WinUI/WPF/etc. app |
| Android preserve | **Natural** | **Natural** | Migration = rewrite | Keep Android, rebuild others |
| iOS | Existing shell | Existing shared UI | Rewrite | New SwiftUI app |
| Maintenance | One shared core | One design system | Two worlds until complete | Highest ongoing cost |
| Build complexity | High but **already paid** | Same | New CI + skill tax | Three CI matrices |
| Risk to working music | Low if we **extend** | Low if we **theme** UI | **Critical** | High consistency risk |

### Recommendation table

| Option | Verdict |
|--------|---------|
| KMP | **Recommended — already the architecture** |
| Compose MP | **Recommended — already the shared UI** |
| Flutter | **Not recommended** |
| Separate native apps | **Not recommended** given existing shared shells |

---

## D. Recommended architecture

### Choice

**Keep and harden the existing Kotlin Multiplatform + Compose Multiplatform product** with three shells:

1. **Android** — preserve Media3/`PlaybackService` path  
2. **Windows** — preserve `:desktopApp` JVM + libav/MSE path  
3. **iOS** — preserve shared Compose + AVFoundation/MSE; close Library and device QA gaps  

Configurable identity: **StartMusicV3** via resources + `Brand.kt` (not a hard-coded scatter of names).

### Why (decision drivers)

1. **Existing code** already implements Windows + Android + iOS entry points and a shared `Player` SPI.  
2. **Existing functionality** (queue, feeds, downloads, settings) lives in shared modules — rewriting would regress music.  
3. **Playback requirements** are already split correctly (ExoPlayer / libav / AVFoundation + shared MSE YouTube).  
4. **Database** models are already multiplatform SQLDelight.  
5. **Platform support** for Windows is not theoretical — `desktopApp` packages Windows installers.  
6. **Maintenance** is lower than Flutter migration or three native UIs.  
7. **Build complexity** is already invested (ffmpeg, QuickJS, CMP); switching stacks resets that investment.  
8. **Long-term** development should close iOS Library and verify media controls — not invent a fourth architecture.

### What “shared” means here

Share **models, rules, UI screens/tokens, `Player` facade, settings descriptors**.  
Keep **services, audio sessions, notifications, storage drivers, permissions** platform-specific.

---

## E. Migration risk

> There is no mandatory “migration” to multiplatform — the product **is** multiplatform. Risk is about **changing** the wrong layers.

### E.1 High-risk components

| Component | Why |
|-----------|-----|
| `PlaybackService` / MediaSession / notification | Android background contract; easy to break lock-screen / Auto |
| `:player` engine contracts / MSE / QuickJS natives | Affects all platforms; engine-js backward compat rules |
| SQLDelight schemas | User libraries/playlists/downloads |
| Cookie / login ownership | Sign-in and YouTube access |
| `applicationId` / bundle ID / URL schemes | Breaks updates and deep links |
| Flutter or full UI framework swap | Destroys working stack |

### E.2 Medium-risk components

| Component | Why |
|-----------|-----|
| iOS `mediaStore` wiring | Needed for Library; must use correct native DB/files APIs |
| Desktop media-key / SMTC integration | New OS surface; additive if done carefully |
| Download format matrix on iOS | ffmpeg absence |
| Navigation redesign (e.g. bottom nav) | Easy to break back stacks |
| Remote update / engine hosts | Compat with old app versions |

### E.3 Low-risk components

| Component | Why |
|-----------|-----|
| StartMusicV3 colors / logo / `app_name` | Design tokens + resources |
| List/card spacing, empty states | Presentation only |
| Settings section chrome | Same `AppSettings` keys |
| About branding header | Display only |

### E.4 Must remain platform-specific

| Platform | Keep local |
|----------|------------|
| **Android** | PlaybackService, MediaSession, notification, audio focus, WebView cookies, SAF, widgets, Auto, FGS downloads |
| **Windows / desktop** | JVM window lifecycle, libav/waveOut, CookieVault/DPAPI, file scanner provider, jpackage/Inno, optional tray |
| **iOS** | AVAudioSession, AVPlayer surfaces, Swift host, Info.plist background modes, Face ID/camera for device-link, future mediaStore driver |

### E.5 Safe to share (already mostly shared)

- Track/album/artist/playlist **models** (`media-store`, feed items)  
- Playlist membership rules (content-addressed codes)  
- Sorting/filtering/search **session** patterns in UI + engine commands  
- Queue / shuffle / repeat **behavior** inside `:player`  
- `AppSettings` descriptors and theme tokens  
- Compose screens + StartMusicV3 design system  
- Conceptual playback operations via existing `Player` API  

Do **not** share Android service classes into commonMain “for reuse.”

---

## F. Implementation plan (staged — no premature architecture rewrite)

### Stage 0 — Freeze architecture (this report)

- Accept KMP + Compose MP + three shells as the target.  
- **Do not** start Flutter or native-triple rewrites.  
- Document gaps honestly (iOS Library, media-control verification).

### Stage 1 — Preserve Android foundation

- Keep ExoPlayer + `PlaybackService` + MediaSession + audio focus + queue + library + downloads + settings.  
- Allow only UI/branding changes that call existing `PlaybackCoordinator` / `Player` APIs.  
- Test: play/pause/seek/next/prev, notification, background, library open, download play.

### Stage 2 — Windows (desktop) verification and polish

- Run `:desktopApp:run` on Windows with required ffmpeg/JDK.  
- Verify library scan, local + YouTube playback, queue, resize, keyboard.  
- Investigate media keys / SMTC **only if** missing; smallest additive OS integration.  
- Package with existing Compose Desktop / Inno pipeline when branding assets are ready.  
- **Do not** introduce Android MediaSession on Windows.

### Stage 3 — iOS completion path

1. Plan and implement `mediaStore` (+ files access) in iOS `AppGraph` — **explain before destructive schema moves** (none expected if reusing SQLDelight).  
2. Verify `AVAudioSession`, background audio, lock-screen controls on a physical device.  
3. Align icons / usage strings with StartMusicV3.  
4. Drop `iosAllowUndefined` for release builds once Xcode 26 SDK is available.  
5. **Do not** use Android MediaSession APIs.

### Stage 4 — Shared logic / UI (incremental)

- Continue sharing models, settings keys, queue behavior, design tokens.  
- Adapt layouts per form factor (desktop wide / mobile drawer / iPad) without forking product identity.  
- Avoid new abstractions that duplicate `Player` or `AppGraph`.

### Stage 5 — Platform-specific playback (maintain, don’t duplicate)

| Platform | Source of truth |
|----------|-----------------|
| Android | ExoPlayer engine + PlaybackService session |
| Windows | JvmLocalPlayer + process audio + shared `Player` facade |
| iOS | AppleLocalPlayer + AVAudioSession + shared `Player` facade |
| YouTube (all) | `MsePlayer` / MSE (shared), hosted per platform surface |

### Stage 6 — Testing matrix

| Gate | Android | Windows | iOS |
|------|---------|---------|-----|
| Compile / package | `installDebug` / assemble | `:desktopApp:run` / jpackage | Xcode device build |
| Playback transport | Required | Required | Required |
| Background / lock / media keys | Required | Verify OS support | Required where claimed |
| Library | Required | Required | After store wired |
| Regression vs pre-brand UI | Required | Smoke | Smoke |

Report only tests actually executed.

### Stage 7 — Build configuration

- Keep existing Gradle KMP targets and `kmpTargets` gating.  
- Keep ffmpeg sibling workflow.  
- Do not add a second UI toolkit to the same product flavors.

---

## Shared UI and original design (constraints)

- **Identity:** StartMusicV3 — configurable via `app_name` + `Brand.kt`.  
- **Share:** visual identity (color, type rhythm, control language, logo).  
- **Do not force:** identical navigation chrome on phone vs desktop vs iPad.  
- **Android:** preserve drawer/destinations and playback actions.  
- **Windows:** desktop window, mouse/keyboard, large layouts (existing desktop patterns).  
- **iOS:** safe areas, touch, Dynamic Type where Compose resources allow; respect platform audio lifecycle.

Reference apps inspire UX density and music-player patterns only — **not** branding or a code transplant.

---

## Decision record (summary)

| Decision | Outcome |
|----------|---------|
| Target architecture | **Existing KMP + Compose Multiplatform** |
| Android playback | **Preserve** — no second engine |
| Windows playback | **Existing JVM/libav + shared Player** |
| iOS playback | **Existing AVFoundation + shared Player** |
| Shared playback API | **Existing `Player` facade** — do not duplicate |
| Flutter / separate natives | **Rejected** for this codebase |
| Next engineering focus | Verify Windows/Android; wire iOS Library; finish StartMusicV3 identity without package renames |

---

## Explicit non-actions until a later approved plan

- No Flutter migration  
- No replacement of `PlaybackService`  
- No Android APIs in iOS/Windows playback  
- No forced move of Android services into shared modules  
- No `applicationId` / scheme rename bundled with UI work  
- No architecture rewrite in the same change set as this report  

**This report is complete.** Implementation work that follows must stay inside the recommended architecture and prefer the smallest change that preserves working music playback on every platform.
