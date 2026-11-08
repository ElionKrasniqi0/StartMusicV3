# StartMusicV3 — Phase 14 Development Rules

**Date:** 2026-11-08  
**Verdict:** The sixteen rules below are **mandatory** for humans and agents working on this product track. They are enforced for Cursor agents via [`.cursor/rules/startmusic-development.mdc`](../.cursor/rules/startmusic-development.mdc) (`alwaysApply: true`) and summarized from [`AGENTS.md`](../AGENTS.md) / the root [`README.md`](../README.md).

## Rules

1. **Work with the existing repository.**  
   Extend the current Kotlin Multiplatform + Compose Multiplatform tree (`:commonUi`, `:player`, `:desktopApp`, `:androidApp`, …). Do not stand up a second app codebase.

2. **Do not delete working features.**  
   Prefer additive changes. Remove or gut behavior only when the user explicitly asks.

3. **Do not rewrite the entire project unnecessarily.**  
   No Flutter, Electron, or greenfield rewrite. Architecture is locked (Phase 3): fill gaps in place.

4. **Do not break Windows while developing Android.**  
   Shared and desktop paths must keep `./gradlew :desktopApp:run` and Windows packaging viable.

5. **Do not break Android while improving Windows.**  
   Shared UI/player/download contracts and `:androidApp` must remain buildable when the SDK is present.

6. **Keep common code shared where appropriate.**  
   Product UI and domain logic live in shared modules; shells stay thin.

7. **Keep platform-specific code isolated.**  
   Platform APIs belong in `androidMain` / `jvmMain` / `iosMain` or the product shells — not copied into `commonMain` behind `#ifdef` style hacks.

8. **Use clean architecture and maintainable code.**  
   Respect module boundaries and narrow SPIs (`MediaEngine`, stores, host protocols). Match existing style (Spotless / project conventions in `AGENTS.md`).

9. **Avoid unnecessary dependencies.**  
   Prefer existing in-tree libraries; justify any new Gradle dependency.

10. **Do not create fake functionality.**  
    No mock “features” that only look complete in the UI.

11. **Do not use placeholder buttons for features presented as complete.**  
    Incomplete work is documented as a limitation or left out — not shipped as a dead control.

12. **Do not claim a feature works without testing it.**  
    Code existence ≠ done. Test behavior; if the environment blocks (e.g. missing Android SDK), say so.

13. **Fix root causes instead of hiding errors.**  
    Do not swallow exceptions, `@Ignore` / delete failing tests, or disable checks to force a green build.

14. **Keep the UI responsive.**  
    No blocking I/O or heavy work on the UI or QuickJS engine thread; follow coroutine rules in `AGENTS.md`.

15. **Preserve the existing FFmpeg integration.**  
    Keep `:kmpFfmpeg` and sibling `ffmpeg-build` staging (`syncFfmpegHost`, desktop/Android ABI packs). Do not replace with a one-off binary pipeline.

16. **Use StartMusicV3 consistently as the product name.**  
    User-facing chrome, installers, README, and About — **StartMusicV3**. Historical `io.ymusic.*` package names may remain; they are not product branding.

## Alignment with earlier phases

| Rule theme | Earlier phase |
|------------|----------------|
| Existing repo / no rewrite | Phase 1–3 |
| Windows ↔ Android non-regression | Phase 6–7, 12 |
| Shared vs platform | Phase 3 |
| Honesty / testing | Phase 11–12 |
| FFmpeg | Phase 6, 8 |
| Branding | Phase 2, 13 |

## Enforcement

- Cursor: `.cursor/rules/startmusic-development.mdc`
- Humans / CI: follow `AGENTS.md` coding rules + root README known limitations
