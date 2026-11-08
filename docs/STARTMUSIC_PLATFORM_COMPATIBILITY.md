# StartMusicV3 — Platform Compatibility Matrix

**Date:** 2026-11-08  
**Based on:** repository inspection (not runtime claims)  
**Related:** [`STARTMUSIC_MULTIPLATFORM_ARCHITECTURE.md`](STARTMUSIC_MULTIPLATFORM_ARCHITECTURE.md), [`STARTMUSIC_MULTIPLATFORM_PLATFORMS.md`](STARTMUSIC_MULTIPLATFORM_PLATFORMS.md)

Legend:

| Status | Meaning |
|--------|---------|
| **Yes** | Implemented in this tree for that platform |
| **Partial** | Present with known gaps |
| **No** | Not wired / not available |
| **N/A** | Not applicable on this OS |
| **Unverified** | Code present; runtime not confirmed in our testing log |

---

## 1. Framework / product shell

| Capability | Android | Windows (desktop JVM) | iOS |
|------------|---------|------------------------|-----|
| Kotlin Multiplatform shared modules | Yes | Yes | Yes |
| Compose Multiplatform shared UI (`:commonUi`) | Yes | Yes | Yes |
| Product shell module | `:androidApp` | `:desktopApp` | `iosApp` + `:iosAppKotlin` |
| Flutter / separate native rewrite needed | No | No | No |

---

## 2. Playback

| Capability | Android | Windows | iOS |
|------------|---------|---------|-----|
| Shared `Player` facade | Yes | Yes | Yes |
| Local file engine | ExoPlayer | libav / waveOut | AVFoundation |
| YouTube / MSE engine | Yes | Yes | Yes |
| Play / pause / seek / next / prev | Yes | Yes | Yes |
| Queue / shuffle / repeat | Yes | Yes | Yes |
| Background playback | FGS + MediaSession | In-process (app running) | Audio background mode configured |
| Lock screen / OS media controls | MediaSession | **No SMTC backend** (macOS desktop has Now Playing; Win = null) | Configured path; **Unverified** on device |
| Audio focus / interruptions | `AudioFocusCoordinator` | OS / process | AVAudioSession; **Unverified** detail |
| Second / duplicate engine | Must not add | Must not add | Must not add |

---

## 3. Library & storage

| Capability | Android | Windows | iOS |
|------------|---------|---------|-----|
| SQLDelight media-store schema | Yes | Yes | Schema shared; **graph `mediaStore = null`** |
| Folder scan | SAF + scanner | Filesystem scanner | **No** until store wired |
| User playlists (local DB) | Yes | Yes | **No** (blocked on store) |
| Downloads DB / offline | Yes | Yes | Partial (format limits; no ffmpeg) |
| Settings persistence | SharedPreferences | SQLite AppStore | Shell settings path |
| Destructive DB migration needed for MP | **No** | **No** | **No** (wire existing schema) |

---

## 4. Music sources

| Source | Android | Windows | iOS |
|--------|---------|---------|-----|
| YouTube / YT Music browse & search (engine.js) | Yes | Yes | Yes |
| YouTube playback (player.js + MSE) | Yes | Yes | Yes |
| Local files | Yes | Yes | No library wiring |
| Account / cookies | WebView jar | CookieVault / QR link | Device-link seams |
| Fake / hardcoded catalog | No | No | No |

Respect provider terms; do not document bypasses as features.

---

## 5. UI / UX

| Capability | Android | Windows | iOS |
|------------|---------|---------|-----|
| Shared Discover / Search / NP / Settings | Yes | Yes | Yes |
| StartMusicV3 branding direction | Yes (resources/theme) | Shared UI | Display name; icons/copy TBD |
| Desktop window resize / mouse / keyboard | N/A | Yes (shortcuts exist) | N/A |
| Mobile safe areas / touch | Yes | N/A | Yes (Compose host) |
| Identical layout forced on all form factors | No (correct) | No | No |

---

## 6. Build / tooling

| Capability | Android | Windows | iOS |
|------------|---------|---------|-----|
| Build on Windows dev PC | Yes (Gradle) | Yes (`:desktopApp`) | **No full signed iOS** |
| Build on macOS | Yes | Desktop JVM possible | **Yes (required for iOS)** |
| CI release workflow jobs | Yes (`release.yml`) | Yes | **No iOS app job found** |
| ffmpeg sibling packs | Android ABIs | windows-x86_64 / arm64 | N/A (ffmpeg no-op) |

---

## 7. Summary judgment

| Platform | Compatibility verdict |
|----------|----------------------|
| **Android** | Fully scaffolded product; preserve; verify after UI changes |
| **Windows** | Fully scaffolded desktop product; verify; SMTC optional later |
| **iOS** | Shared UI + playback engines present; **Library gap**; toolchain/signing on macOS; runtime claims gated on device tests |

**Do not** force Android-specific APIs onto Windows or iOS.  
**Do not** force a framework migration to gain these platforms — they are already targeted by this architecture.
