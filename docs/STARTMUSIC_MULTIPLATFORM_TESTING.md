# StartMusicV3 — Multi-Platform Testing Log

**Date:** 2026-11-08  
**Rule:** Do not claim a platform verified unless that platform was tested.  
**Rule:** Do not claim device features (background, lock screen, notifications) from compile-only success.

---

## Executive status

| Platform | Build verified (this effort) | Runtime / device verified |
|----------|------------------------------|---------------------------|
| **Android** | Not re-run as full `assembleDebug` in latest docs pass | **No** |
| **Windows desktop** | Not run (`:desktopApp:run`) in latest docs pass | **No** |
| **iOS** | **Cannot** fully build/sign on Windows-only agent | **No** |
| **Shared JVM UI compile** | **Yes** — `:commonUi:compileKotlinJvm` **SUCCESS** (earlier session) | N/A |
| **Spotless** | **Yes** — `spotlessApply` **SUCCESS** (earlier session) | N/A |

**Conclusion:** Multi-platform **runtime** verification is **incomplete**. Only shared Kotlin/Compose JVM compilation was confirmed. Treat playback stability on all three platforms as **unproven** until the matrices below are executed.

---

## Android matrix

| Check | Required | Result |
|-------|----------|--------|
| Gradle debug build / install | Yes | **Not executed** (this log) |
| Play / pause | Yes | Unverified |
| Background playback | Yes | Unverified |
| MediaSession | Yes | Unverified |
| Notifications | Yes | Unverified |
| Search | Yes | Unverified |
| Library | Yes | Unverified |
| Playlists | Yes | Unverified |
| Navigation | Yes | Unverified |

**Command when ready:** `./gradlew :androidApp:installDebug` (device/emulator attached).

---

## Windows matrix

| Check | Required | Result |
|-------|----------|--------|
| `:desktopApp:run` / package | Yes | **Not executed** |
| Desktop UI renders | Yes | Unverified |
| Playback (local + YouTube) | Yes | Unverified |
| Music library / scan | Yes | Unverified |
| Search | Yes | Unverified |
| Playlists | Yes | Unverified |
| Window resizing | Yes | Unverified |
| Keyboard shortcuts | Yes | Unverified |
| OS media keys / SMTC | Desired | **Known unimplemented** in code — expect fail until backend added |

**Prereqs:** JDK, MinGW, sibling ffmpeg Windows pack (see root README).

---

## iOS matrix

| Check | Required | Result |
|-------|----------|--------|
| Xcode build on macOS | Yes | **Not executed here** (Windows host) |
| UI | Yes | Unverified |
| Playback | Yes | Unverified |
| Audio session | Yes | Configured in code; **Unverified** |
| Background audio | Yes | Configured; **Unverified** |
| Navigation | Yes | Unverified |
| Search | Yes | Unverified |
| Playlists / Library | Yes | **Expect empty Library** until `mediaStore` wired |

**Honest limit:** Full iOS verification requires a **macOS + Xcode** environment and a device or Apple Silicon simulator.

---

## Shared functionality matrix

| Check | How to verify | Result |
|-------|----------------|--------|
| Models (media-store / feeds) | Unit tests / compile | Compile path OK for commonUi JVM; full test suite **not** run |
| Search / sort / filter | UI + engine | Unverified on device |
| Queue / repeat / shuffle | Player + UI | Logic exists in `:player`; runtime unverified |
| Settings persistence | Change theme, relaunch | Unverified |

**Earlier note:** `:commonUi:compileKotlinJvm` succeeded with some pre-existing-style compiler warnings — not a substitute for platform QA.

---

## Regression policy

After any UI or platform change:

1. Re-run the **affected** platform matrix (not “all green” by assumption).  
2. If Android playback/notification regresses → **fix before** more branding work.  
3. Never disable a feature to hide a failure.  
4. Update **this file** with date, command, pass/fail — do not leave stale SUCCESS claims.

---

## Template for future entries

```text
Date:
Platform: Android | Windows | iOS
Environment: (OS, device, JDK/Xcode)
Commands:
Results: (checklist)
Known failures:
Tester:
```

---

## Relation to implementation phases

Testing (plan phases 12–13) **blocks** any claim that the cross-platform music player is “complete.”  

Architecture and shells exist; **verification does not.**
