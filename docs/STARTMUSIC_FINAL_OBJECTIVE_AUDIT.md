# StartMusicV3 — Final Objective Audit Report

**Date:** 2026-11-08  
**Product:** StartMusicV3  
**Constraint:** Audit only — **no destructive changes** in this pass.  
**Objective:** A professional, stable, responsive music app; native Windows desktop; proper Android with reliable background playback and media notifications; shared identity, design, features, and playback behavior where technically appropriate.

**Method:** Reconcile Phases 1–15 docs with current sources (`:commonUi`, `:player`, `:desktopApp`, `:androidApp`, packaging, README/AGENTS) and this machine’s environment (SDK path, prior `:desktopApp:run` / JVM test results). Parallel read-only audits corroborated: Windows readiness, Android Phase 7, playback Phase 8, storage Phase 9, security Phase 11, module/map audits.  
**Priority:** Existing functionality > stability > correctness > UI > extras (Phase 14–15).

---

## Executive verdict

| Goal | Verdict |
|------|---------|
| Shared product (KMP + Compose) | **Met in architecture** — one `App` / `AppGraph` / `Player` for Windows + Android (+ iOS shell). |
| StartMusicV3 identity + design language | **Mostly met** — Brand/Theme, launcher, window title, installers; Discover/YT Music **section** label “YT Music” remains a catalog surface, not the product name. |
| Windows as native desktop app | **Product-capable** — Compose Desktop JVM, tray, FFmpeg local + MSE YouTube, library scan, downloads, Inno packaging path. **Gap:** no Windows SMTC / lock-screen media session. |
| Android as proper app + background + notifications | **Wired in code** (`PlaybackService`, Media3 session, `AudioFocusCoordinator`, widgets). **Not verified on this machine** — Android SDK directory missing; `assembleDebug` fails. |
| Shared playback behavior | **Met in design** — single `:player` facade + platform engines. |
| Ready to “ship final” without further work | **No** — environment + SMTC + remaining QA must complete in Phase 15 order. |

**Do not rewrite the stack.** Continue ordered completion (Phase 15). Next critical gate: **Android SDK install → assemble/install → device checklist**.

---

## 1. What already works

### Shared (`:commonUi` + modules)

- Drawer navigation: Discover, YT Music, Library, Play again, Downloads, Settings, Feedback, …
- Home / search / library / playlists / queue / Now Playing / favorites / Play again
- One `Player` + `PlaybackConfig` (shuffle, repeat, audio-only, volume, …)
- Persistence: media-store, play-history, settings, downloads, logs
- Error/playability UX, HTTP timeout (`APP_HTTP_TIMEOUT`), HTTPS-first
- Brand tokens (`Brand` / `Theme` / `DesignMotion`), `app_name` = StartMusicV3

### Windows (`:desktopApp`)

- Native JVM window titled **StartMusicV3**; resize; keyboard shortcuts; Jump List
- Local playback (ffmpeg/libav) + YouTube MSE; tray continue; folder library
- Cookie vault (DPAPI); sign-in without Chromium
- Packaging: `packageName` / vendor StartMusicV3; `./gradlew :desktopApp:releaseInstaller`
- Prior live verification: launch, title, resize, navigation; JVM tests green after Phase 12 fixes

### Android (`:androidApp`) — code

- Shared `App` over ExoPlayer + MSE; WebView login/cookies
- `PlaybackService` (MediaLibraryService) → notification / lock-screen / BT / Auto transport
- `AudioFocusCoordinator` (focus, becoming-noisy, calls)
- SAF library scan; download FGS; widgets; PiP; deep links
- Launcher / splash / strings branded StartMusicV3

---

## 2. What is unfinished or blocked

| Item | Impact on final objective | Notes |
|------|---------------------------|--------|
| **Android SDK missing on this host** | Blocks APK, install, background/notification **proof** | `local.properties` → `C:/Users/IT/AppData/Local/Android/Sdk` **does not exist** |
| **Android FFmpeg ABI packs** | Blocks green native Android link if only desktop pack present | Need `FfmpegBuild.ANDROID_ABIS` from sibling `ffmpeg-build` |
| **Windows SMTC** | Windows not fully “OS media citizen” vs Android MediaSession | `DesktopMediaSession` = macOS only; focused Media* keys exist |
| **Full Windows playback QA matrix** | Stability claim incomplete | Seek/volume/queue play-through not fully signed off in Phase 12 |
| **Large uncommitted working tree** | Delivery risk | Many modified/untracked files from Phases 2–15; needs ordered commits when asked — not part of this audit |
| **iOS** | Out of primary Windows/Android objective | Shell exists; not the focus of this audit |

---

## 3. Shared identity & behavior (objective check)

| Concern | Windows | Android | Shared? |
|---------|---------|---------|---------|
| Product name | Window / installer StartMusicV3 | `app_name` StartMusicV3 | Yes |
| Design language | Compose Brand/Theme | Same + system bars | Yes (`:commonUi`) |
| Core features | Discover/YT/Library/NP/… | Same | Yes |
| Playback API | `:player` | `:player` | Yes |
| Local engine | ffmpeg/libav | ExoPlayer | Platform-appropriate |
| YouTube engine | MSE + QuickJS | MSE + QuickJS | Yes |
| Background / OS media | Tray + focused keys; **no SMTC** | FGS + MediaSession | Partial parity |

---

## 4. Architecture decision (non-destructive)

**Options if someone proposes a rewrite to “finish” the objective:**

| Option | Pros | Cons | Recommendation |
|--------|------|------|----------------|
| **A. Continue current KMP stack** | Matches Phase 3–15; most features exist | Finish SDK, SMTC, QA | **Preferred** |
| **B. Flutter/Electron rewrite** | — | Destroys working Windows/Android; violates Phase 14 | **Reject** |
| **C. Rename all `io.ymusic` packages** | Cosmetic purity | Mass churn; breaks nothing user-visible | **Defer** unless explicitly required |

No destructive change is warranted before Steps 3 / 11 / 12 complete.

---

## 5. Recommended next work (Phase 15 order)

1. **Step 3** — Install Android SDK (+ NDK 28.2.13676358) at `sdk.dir`; build Android ffmpeg packs; `./gradlew :androidApp:assembleDebug`.
2. **Step 8 / 11** — Device/emulator: background playback, notification controls, lock screen, audio focus, headphones, rotation.
3. **Step 7 remainder** — Windows SMTC **only** as additive `DesktopMediaSession` backend (options first; do not disturb macOS).
4. **Step 12** — `releaseInstaller` (Windows) + signed/release APK when SDK green.
5. **Step 13** — Refresh README known limitations when the above land.

---

## 6. Follow-up from parallel audits (doc-only)

Stale docs corrected so future work does not re-wire finished features:

- `commonUi/README.md` — SponsorBlock is `ServiceSponsorBlockApi` (not mock); Windows/Linux have single-instance (protocol scheme registration still missing).
- `TERMS.md` — StartMusicV3 branding + trusted-release / no-circumvention posture (was YMusic/ymusic.io).
- `androidApp/README.md` — already stated SAF/media-store as wired (no change needed).

Code gaps remain deferred per Phase 15 (SDK, SMTC, desktop `FILE_MISSING` mapping, Exo focus alignment) — **not** applied in the audit-only constraint.

## 7. Modified files (this consolidation)

- `docs/STARTMUSIC_FINAL_OBJECTIVE_AUDIT.md`
- `commonUi/README.md`, `TERMS.md`, `.gitignore` (allowlist)

---

## 8. What works / what remains (summary)

**Works today (with prior verification):** shared StartMusicV3 app architecture; Windows desktop run + branding + packaging path; Android background/notification **implementation**; shared player and feature set in code; docs/rules/order (Phases 13–15).

**Remains unfinished for the final objective:** Android build/runtime proof on this machine; Windows SMTC; complete independent QA on both platforms; distribution artifacts produced from a clean verified tree.
