# StartMusicV3 — Multi-Platform Expansion Strategy

**Date:** 2026-11-08  
**Repo truth:** inspected sources under `c:\Users\IT\Desktop\StartMusicV3`  
**Related:** [`STARTMUSIC_AUDIT.md`](STARTMUSIC_AUDIT.md), [`STARTMUSIC_TRANSFORMATION.md`](STARTMUSIC_TRANSFORMATION.md)  
**Product name (configurable):** StartMusicV3  

**Priority:** Existing functionality > Stability > Correctness > UI quality > Additional features  

---

## Executive verdict (do not guess)

**StartMusicV3 is already a Kotlin Multiplatform + Compose Multiplatform music product** with three product shells:

| Platform | Shell | Status in this tree |
|----------|--------|---------------------|
| **Android** | `:androidApp` | Full product shell (ExoPlayer + MSE, FGS, MediaSession, widgets) |
| **Windows** | `:desktopApp` (Compose Desktop / JVM) | Full product shell on Windows (and macOS/Linux hosts) via `./gradlew :desktopApp:run` + jpackage |
| **iOS** | `iosApp` + `:iosAppKotlin` | Same shared `App` from `:commonUi`; AVFoundation local + MSE YouTube; **Library `mediaStore` currently null**; Xcode/toolchain caveats |

**Recommended architecture decision:**

| Option | Verdict |
|--------|---------|
| **A — Kotlin Multiplatform** | **Already in use.** Keep and harden. |
| **B — Compose Multiplatform** | **Already in use** for shared UI (`:commonUi`). Keep; polish branding/UX. |
| **C — Flutter** | **Reject.** Would discard working KMP engines, MediaSession, QuickJS/MSE, SQLDelight, and desktop/iOS shells. Not a UI tweak. |
| **D — Separate native apps** | **Unnecessary.** Would duplicate business rules and UI; consistency cost is high. Only consider for a hypothetical greenfield platform this repo does not need. |

**Do not** “convert Android to multiplatform.” It already is.  
**Do not** replace Android services with Windows/iOS copies of Android APIs. Each shell already uses platform-appropriate engines.

---

## 1. What the repository actually is

### Language / framework (facts)

- **Kotlin** Multiplatform (not Java-only, not Flutter, not React Native).
- **Compose Multiplatform** shared UI in `:commonUi` (Android + JVM desktop + iOS).
- Android shell is a plain AGP application hosting Compose (`MainActivity` → `App(graph)`).
- Desktop is Compose Desktop JVM (`desktopApp` → `MainKt` → `App(graph)`).
- iOS is SwiftUI `@main` hosting `MainViewController()` from the Kotlin framework (`iosApp/README.md`).

### Evidence paths

| Concern | Path |
|---------|------|
| Root product name / modules | `settings.gradle.kts` (`YMusicV4`, includes `:androidApp`, `:desktopApp`, `:iosAppKotlin`, `:commonUi`, `:player`, …) |
| Shared UI | `commonUi/` + `commonUi/README.md` |
| Android shell | `androidApp/`, `androidApp/README.md` |
| Desktop / Windows | `desktopApp/`, `desktopApp/README.md`, `compose.desktop { windows { … } }` in `desktopApp/build.gradle.kts` |
| iOS shell | `iosApp/README.md`, `iosAppKotlin/` |
| Playback SPI | `player/README.md` — ExoPlayer (Android), libav (JVM), AVFoundation (Apple), MSE YouTube |
| Project map | Root `AGENTS.md` |

### Shared vs platform-specific (current)

```
                    ┌─────────────────────────────┐
                    │  :commonUi  App(graph)      │
                    │  Discover / Library / NP …  │
                    └─────────────┬───────────────┘
                                  │ AppGraph
          ┌───────────────────────┼───────────────────────┐
          ▼                       ▼                       ▼
   :androidApp              :desktopApp                 iosApp
   ExoPlayer local          JvmLocalPlayer (ffmpeg)     AVFoundation local
   MSE YouTube              MSE YouTube                 MSE YouTube
   PlaybackService FGS      No Android FGS              AVAudioSession / bg audio
   WebView cookies          CookieVault + QR link       Cookie / device-link seams
   SAF media-store          File-backed media-store     mediaStore = null (today)
```

Shared business/UI: feeds, search chrome, queue UX, settings descriptors, playlists logic (where store exists), branding (`Brand` / strings).  
Platform-specific: audio engines, notifications/MediaSession, windowing, cookie storage, library scanning providers, installers.

---

## 2. Platform requirements mapped to current code

### Windows (via `:desktopApp`)

| Requirement | Current support | Notes |
|-------------|-----------------|-------|
| Desktop UI | Yes | Compose Desktop window; resizable |
| Library / scan | Yes | File-backed `:media-store` + scanner |
| Playback local + YouTube | Yes | libav + MSE |
| Search / playlists / queue / settings | Yes | Shared `:commonUi` |
| Album art | Yes | `:image-loader` |
| Keyboard | Partial–Yes | List keyboard nav, shortcuts seams in commonUi |
| Windows media controls | Investigate | OS integration may be limited vs Android MediaSession; verify on a Windows run |
| Installer | Yes | jpackage + Inno Setup path in `desktopApp/build.gradle.kts` |

**Not Android services.** Desktop does not use `PlaybackService`; it drives `Player` in-process.

### Android (`:androidApp`)

| Requirement | Current support | Notes |
|-------------|-----------------|-------|
| Existing functionality | Yes | Preserve; do not replace |
| Background / notification / MediaSession | Yes | `PlaybackService` |
| Library / SAF | Yes in `YMusicApplication` (androidApp README “not wired” is **stale**) |
| Permissions / lifecycle | Yes | Manifest + Application/Activity |

### iOS (`iosApp`)

| Requirement | Current support | Notes |
|-------------|-----------------|-------|
| Shared Compose App | Yes | Same `App(graph)` |
| Local + YouTube audio | Yes (engines present) | Music default = audio-only |
| Background audio | Declared | `UIBackgroundModes: audio` in Info.plist |
| Lock screen controls | Investigate | Depends on AVFoundation / Now Playing info wiring — verify on device |
| Library | **Gap** | `mediaStore` null → empty Library until wired |
| Local video surface | Limited | README: YouTube via `copyFrame`; local video surface incomplete |
| Toolchain | Constrained | Xcode 26 preferred; Intel Simulator unsupported; `iosAllowUndefined` workaround for older Xcode |

---

## 3. Architecture decision detail

### Option A — KMP (KEEP)

**Pros (already realized):** one `Player` API, one UI tree, shared settings/download/engine models, less behavioral drift.  
**Cons / work remaining:** finish iOS library; harden Windows media integration; keep expect/actual boundaries clean; never call Android APIs from commonMain.

**Safe expansion = extend existing modules**, not invent a second stack.

### Option B — Compose MP (KEEP)

Already the UI layer. StartMusicV3 branding/theme belongs here.  
**Do not** rewrite navigation to a different framework.  
**Do not** replace Compose with Android XML / SwiftUI full UI (except thin iOS host).

### Option C — Flutter (REJECT)

| Cost | Detail |
|------|--------|
| Rewrite | Entire UI + glue |
| Playback | ExoPlayer/MSE/QuickJS/ffmpeg bindings would need re-hosting or FFI |
| Services | Android FGS/MediaSession rewritten |
| Desktop/iOS shells | Throw away existing `:desktopApp` / `iosApp` |
| Risk | Beautiful UI, broken music — violates project priority |

Only reconsider if product leadership abandons this codebase (out of scope).

### Option D — Separate native apps (REJECT for now)

Would mean three UIs and three playback stacks with a “shared spec” only on paper. This repo already solved consistency via `:commonUi` + `:player`. Separate natives add cost without unlocking Windows/iOS—they’re already present.

---

## 4. What must NOT be done

1. Do not migrate to Flutter or React Native.  
2. Do not delete `:desktopApp` or `iosApp` to “start over.”  
3. Do not run Android `PlaybackService` / MediaSession on Windows or iOS.  
4. Do not assume SAF / WebView cookie APIs exist on desktop/iOS (they have their own cookie/library seams).  
5. Do not rename `applicationId` / bundle IDs / URL schemes in the same change as UI polish (breaks updates and deep links).  
6. Do not claim iOS Library or Windows SMTC parity until verified on device/OS.  
7. Do not copy YMusic branding; keep configurable **StartMusicV3** identity (`Brand` + `app_name`).

---

## 5. Safest implementation strategy (incremental)

### Phase MP-0 — Baseline (no architecture change)

- Treat KMP + Compose MP as settled.  
- Keep StartMusicV3 branding in shared UI (`Brand.kt`, resources).  
- Document per-platform run commands (below).

### Phase MP-1 — Android preserve + verify

- Run/install Android; verify play/pause/seek/queue/notification/lock screen.  
- Fix only regressions caused by branding/UI diffs.  
- **Do not** replace playback service.

### Phase MP-2 — Windows desktop verify + polish

- On this Windows host: `./gradlew :desktopApp:run` (needs JDK with jpackage for packaging; ffmpeg sibling pack for local decode).  
- Verify library scan, YouTube play, queue, window resize, keyboard.  
- Investigate Windows media key / SMTC support **only if missing**; smallest additive change.  
- Package via existing `compose.desktop` / Inno path when ready.

### Phase MP-3 — iOS harden (highest remaining gap)

1. Wire `mediaStore` + scanner/provider for iOS **or** explicitly ship Library-disabled with clear empty state (today’s empty Library is a known gap).  
2. Confirm background audio + Now Playing / lock controls on a real device.  
3. Align branding (display name already StartMusicV3; icons/usage strings still may say YMusic).  
4. Prefer Xcode 26 for shippable links; treat `iosAllowUndefined` as **dev-only**.

### Phase MP-4 — Unified product identity

- Keep one `app_name` / `Brand` for all platforms.  
- Defer package/bundle/scheme renames until a coordinated release.  
- Retarget donate/update/website URLs when StartMusicV3 infra exists.

### Phase MP-5 — Optional feature parity matrix

Only add features that exist on one platform to another when the **platform audio/session APIs** support them safely (e.g. widgets = Android-only; tray mini-player = desktop-only).

---

## 6. Feature parity matrix (user-facing)

| Feature | Android | Windows (desktop JVM) | iOS |
|---------|---------|------------------------|-----|
| Shared Compose UI | Yes | Yes | Yes |
| Discover / YT Music / Search | Yes | Yes | Yes |
| Local library | Yes | Yes | **No store wired** |
| Playlists (user) | Yes | Yes | Blocked on store |
| Player + mini player | Yes | Yes | Yes (surface caveats) |
| Queue / shuffle / repeat | Yes | Yes | Yes |
| Downloads | Yes | Yes | M4A-only / no ffmpeg |
| Background playback | FGS + MediaSession | Process + OS audio | Audio background mode |
| Lock screen / media keys | MediaSession | **Verify** | **Verify** |
| Sign-in | WebView cookies | CookieVault / QR link | Device-link / seams |
| Widgets | Yes | N/A | N/A |
| StartMusicV3 branding | In progress | Shared UI | Display name; icons TBD |

---

## 7. Risks specific to multi-platform work

| Risk | Mitigation |
|------|------------|
| Treating “add Windows/iOS” as greenfield | Refuse; extend shells that exist |
| Flutter migration proposal | Reject with this doc |
| iOS App Store build with `dynamic_lookup` | Only ship with proper Xcode 26 SDK link |
| ffmpeg / MinGW missing on Windows | Follow root README; sibling `ffmpeg-build` |
| EOL / Spotless noise looking like PlaybackService rewrites | Path-scoped commits; don’t “fix” service via reformat |
| Feature fake on iOS Library | Wire store or keep honest empty state |

---

## 8. How to run each platform (from this repo)

```text
# Android
./gradlew :androidApp:installDebug

# Windows / desktop JVM (run on the machine’s OS)
./gradlew :desktopApp:run

# iOS (on macOS + Xcode; see iosApp/README.md)
# open iosApp/iosApp.xcodeproj after ruby gen_xcodeproj.rb
# Gradle: :iosAppKotlin:embedAndSignAppleFrameworkForXcode …
```

Packaging Windows installer: see `desktopApp/build.gradle.kts` (`compose.desktop` / Inno Setup).

---

## 9. Files that matter for multi-platform work (no rewrite list)

**Touch for branding / shared UX:** `:commonUi` (already StartMusicV3 direction).  

**Touch for Windows behavior:** `desktopApp/src/jvmMain/...`, desktop expect/actuals, `mse` desktop video surface.  

**Touch for iOS Library:** `iosAppKotlin` graph wiring, `media-store` / scanner Apple provider, `iosApp` Info.plist branding.  

**Do not casually rewrite:** `PlaybackService.kt`, `:player` SPI contracts, `:quickjs` / `:mse` natives, SQLDelight schemas, engine-js wire protocol (compat rules).

---

## 10. Decision record

| Question | Answer from repo |
|----------|------------------|
| Is it Kotlin? | **Yes** (KMP) |
| Shared code? | **Yes** (`:commonUi`, `:player`, stores, engines…) |
| Desktop already? | **Yes** (`:desktopApp`, includes Windows) |
| iOS already? | **Yes** (shell + shared UI; gaps remain) |
| Safest path | **Keep KMP + Compose MP; verify and close gaps** |
| Flutter? | **No** |
| Separate native trio? | **No** |

---

## Next concrete actions (when implementation resumes)

1. **Windows:** run `:desktopApp:run` on this PC; note any playback/media-key gaps.  
2. **Android:** installDebug + playback checklist (stability first).  
3. **iOS:** plan `mediaStore` wiring as a dedicated, non-UI-breaking task.  
4. Commit StartMusicV3 UI/docs with **path-scoped** adds (avoid CRLF mass diffs).  

No destructive architecture change is required to “become” multi-platform — the product **is** multi-platform. The work is **stabilize, brand, and close documented gaps**.
