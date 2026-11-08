# StartMusicV3 — Multi-Platform Implementation Plan

**Date:** 2026-11-08  
**Foundation:** existing StartMusicV3 / KMP + Compose MP repository  
**Product name:** StartMusicV3 (configurable)  
**Architecture decision:** Keep existing KMP + Compose Multiplatform — see [`STARTMUSIC_MULTIPLATFORM_ARCHITECTURE.md`](STARTMUSIC_MULTIPLATFORM_ARCHITECTURE.md)  

**Rule:** Do not perform a full migration. Do not force a framework change. Do not break Android playback or destroy user data.

---

## Phase status (actual)

| Phase | Intent | Status |
|-------|--------|--------|
| **0** Inspect repository | Framework, shells, modules | **Done** |
| **1** Android / product audit | Features, playback, risks | **Done** — [`STARTMUSIC_AUDIT.md`](STARTMUSIC_AUDIT.md) |
| **2** Architecture report | Options A–D, recommendation | **Done** — architecture + platforms docs |
| **3** Select safest architecture | KMP + Compose MP | **Done** — selected; **no migration required** |
| **4** Shared vs platform-specific | Component map | **Done** — documented in architecture §E / this plan |
| **5** Shared application behavior | Player, queue, settings, feeds | **Done** — already implemented in shared modules |
| **6** Original design system | StartMusicV3 Brand / Theme | **Done (UI)** — [`STARTMUSIC_TRANSFORMATION.md`](STARTMUSIC_TRANSFORMATION.md); device polish ongoing |
| **7** Preserve & stabilize Android | No service rewrite; verify | **Partial** — code preserved; **device QA not complete** |
| **8** Windows support | Desktop shell | **Exists** — verify/run; SMTC gap documented |
| **9** iOS support | iOS shell | **Exists** — Library gap; device QA not complete |
| **10** Platform-specific playback | Engines already per-OS | **Exists** — do not duplicate; verify only |
| **11** Database & storage | SQLDelight; no destructive migrate | **Documented** — iOS store wiring pending approval |
| **12** Test all platforms | Independent matrices | **Not complete** — see testing doc |
| **13** Regression testing | After any change | **Ongoing requirement** |
| **14** Final architecture docs | This set of docs | **In progress** (this deliverable) |

**Important:** Phases 8–10 are **not** “build Windows/iOS from scratch.” Shells and playback backends **already exist**. Remaining work is stabilize, brand, close gaps, and verify.

---

## Recommended sequence going forward

### A. No major architecture migration (default)

1. Path-scoped commit of StartMusicV3 UI + docs (avoid EOL noise).  
2. **Android device checklist** (playback, notification, library).  
3. **Windows** `:desktopApp:run` smoke (library, play, resize, shortcuts).  
4. **Approve then implement** iOS `mediaStore` wiring (additive).  
5. Device-verify iOS background audio / lock controls before claiming them.  
6. Optional: Windows SMTC behind existing `DesktopMediaSession` API.

### B. Forbidden without explicit new approval

- Flutter / RN migration  
- Replacing `PlaybackService` or ExoPlayer  
- Clearing or rewriting SQLDelight schemas  
- Claiming all platforms tested when only one was  
- Publishing releases / expanding CI secrets automatically  

---

## Shared vs platform-specific (reuse map)

### Reuse (shared — already)

| Area | Where |
|------|--------|
| UI screens / navigation model | `:commonUi` |
| Design tokens / StartMusicV3 brand | `Brand.kt`, `Theme.kt`, compose-resources |
| `Player` facade, queue, shuffle, repeat | `:player` |
| YouTube engine + MSE player | `:dynamic-engine`, `:engine-js`, `:mse`, `MsePlayer` |
| Library models / playlist rules | `:media-store` |
| Download orchestration | `:download` |
| Settings descriptors | `:settings` / `AppSettings` |

### Must remain platform-specific

| Platform | Components |
|----------|------------|
| Android | `PlaybackService`, MediaSession, notification, audio focus, WebView cookies, SAF, widgets |
| Windows | JVM window, libav/waveOut, CookieVault/DPAPI, file scanner, jpackage; SMTC if added later |
| iOS | Swift host, `AVAudioSession`, AVPlayer surfaces, Info.plist background modes, future mediaStore driver |

### Changes required (safe)

| Change | Type |
|--------|------|
| Finish StartMusicV3 assets/copy consistency | Low risk UI |
| Android/Windows runtime verification | Testing |
| iOS `mediaStore = null` → real store | Medium — needs design approval before code |
| Windows SMTC | Optional medium — additive |
| Package ID / URL scheme rename | High — defer |

---

## Risks

| Risk | Mitigation |
|------|------------|
| Treating “implement Windows/iOS” as greenfield rewrite | Refuse; extend existing shells |
| Breaking Android playback during UI work | UI-only touch of `:commonUi`; never rewrite service casually |
| Data loss on iOS store wiring | Use existing SQLDelight schema; no wipe migrations |
| False “verified” claims | Track per-platform in testing doc |
| EOL/Spotless mass diffs | Path-scoped commits |

---

## Approval gate

**Major architecture migration:** not recommended and **not started**.  

**Next implementation** (after your approval of a specific item):

- [ ] Android device QA only  
- [ ] Windows smoke + UI polish  
- [ ] iOS mediaStore wiring plan (written) then code  
- [ ] Windows SMTC backend  

Wait for explicit approval before any of the medium/high items above.
