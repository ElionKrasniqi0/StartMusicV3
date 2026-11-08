# StartMusicV3 — Phase 15 Implementation Order

**Date:** 2026-11-08  
**Verdict:** Work proceeds in **ordered steps**. Do not attempt to implement everything in one uncontrolled operation. This document is mandatory alongside [Phase 14 rules](STARTMUSIC_PHASE14_DEVELOPMENT_RULES.md).

## After every major step

1. Run the **appropriate build** (`:desktopApp:run` / compile, `:androidApp:assembleDebug`, etc.).
2. Run **relevant tests**.
3. **Fix errors** (root causes — do not hide failures).
4. **Summarize modified files**.
5. Explain **what works**.
6. Explain **what remains unfinished**.

If an important architectural decision is required, **explain the options before making a destructive change**.

## Ordered steps

| Step | Goal | Prior track mapping | Status (this machine / track) |
|------|------|---------------------|-------------------------------|
| **1** | Complete repository audit and report | Phase 1 | **Done** — [`STARTMUSIC_PHASE1_AUDIT.md`](STARTMUSIC_PHASE1_AUDIT.md) |
| **2** | Verify the current Windows app; preserve working state | Phase 1 / 6 / 12 | **Done (partial QA)** — `:desktopApp:run` verified; full playback matrix may still need manual pass |
| **3** | Inspect Android SDK config; verify Android build capability | Phase 7 / 12 | **Blocked** — `sdk.dir` points at a missing SDK; re-run when installed |
| **4** | Consistent StartMusicV3 branding and design system | Phase 2 / 10 | **Done** — Brand / Theme / DesignMotion; residual “YT Music” section label is catalog surface |
| **5** | Shared navigation and main interface | Phase 4 / 5 | **Done** — drawer + Discover / Library / NP / Settings |
| **6** | Audio playback engine and shared playback state | Phase 8 | **Done (keep)** — `:player` facade; audio-only / engines preserved |
| **7** | Windows-specific functionality | Phase 6 | **Mostly done** — chrome, shortcuts, packaging; **SMTC unfinished** |
| **8** | Android-specific functionality and background playback | Phase 7 | **Code done; build/device unverified** here — MediaSession / focus / noisy wired |
| **9** | Search, library, playlists, queue, settings | Phase 5 / 9 | **Done** — favorites / history / clear actions filled |
| **10** | Persistence and error handling | Phase 9 / 11 | **Done** — stores + timeouts/playability; Play-again JSON hardened |
| **11** | Test Windows and Android independently | Phase 12 | **Windows partial; Android blocked** (no SDK) |
| **12** | Prepare Windows distribution and Android APK | Phase 6 packaging docs | **Windows path documented** (`releaseInstaller`); **APK blocked** without SDK + Android ffmpeg packs |
| **13** | Update documentation | Phase 13–15 | **Done for entry docs** — README + rules + this order; refresh when Steps 3/7/11/12 advance |

## How to use this order going forward

- Finish **open items in step order** (next critical: **Step 3** Android SDK → then re-verify **Step 8 / 11 / 12**).
- Do not start Windows SMTC (**Step 7 remainder**) in the same uncontrolled batch as Android SDK work — one concern per change set; keep platforms green (Phase 14 §§4–5).
- Shared playback / UI changes (**Steps 5–6, 9–10**) require both desktop and Android compile checks when the environment allows.

## Suggested commands per step type

| Focus | Build | Tests |
|-------|-------|-------|
| Shared / UI | `:desktopApp:compileKotlinJvm` or `:desktopApp:run` | `:commonUi:jvmTest`, `:settings:jvmTest` |
| Player | same + `:player:jvmTest` | `:player:jvmTest` |
| Windows | `:desktopApp:run`, optionally `:desktopApp:releaseInstaller` | desktop smoke + Phase 12 checklist |
| Android | `:androidApp:assembleDebug` / `installDebug` | device checklist (Phase 12) |
| Docs only | — | link-check mentally; no need to disable tests |

## Architectural decisions

Before a destructive or stack-level change (new engine, replace FFmpeg, rewrite shells, rename packages for branding):

1. State the **options** and tradeoffs.
2. Prefer the **additive** option behind an existing SPI.
3. Proceed only after explicit agreement when the change is hard to reverse.
