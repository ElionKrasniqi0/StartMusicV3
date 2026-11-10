# StartMusicV3 — Platform Compatibility Matrix

**Date:** 2026-11-10 (Phase B iOS audit refresh)  
**Based on:** repository inspection (not iOS runtime claims — Windows host)  
**Related:** [`STARTMUSIC_MULTIPLATFORM_ARCHITECTURE.md`](STARTMUSIC_MULTIPLATFORM_ARCHITECTURE.md), [`STARTMUSIC_MULTIPLATFORM_PLATFORMS.md`](STARTMUSIC_MULTIPLATFORM_PLATFORMS.md), [`STARTMUSIC_IOS_PHASE_B_READY.md`](STARTMUSIC_IOS_PHASE_B_READY.md), [`iosApp/README.md`](../iosApp/README.md)

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
| Background playback | FGS + MediaSession | In-process (app running) | `UIBackgroundModes: audio` + session `.playback`; **device unverified** |
| Lock screen / OS media controls | MediaSession | **No SMTC backend** (macOS desktop has Now Playing; Win = null) | `IosNowPlaying` / `MPRemoteCommandCenter`; **device unverified** |
| Audio focus / interruptions | `AudioFocusCoordinator` | OS / process | `IosAudioInterruptions` (interrupt + route); **device unverified** |
| Second / duplicate engine | Must not add | Must not add | Must not add |

---

## 3. Library & storage

| Capability | Android | Windows | iOS |
|------------|---------|---------|-----|
| SQLDelight media-store schema | Yes | Yes | Yes (`mediaStoreNativeDriver` in `:iosAppKotlin`) |
| Folder scan | SAF + scanner | Filesystem scanner | Wired (`UIDocumentPicker` + scoped bookmarks + `NoopMetadataExtractor`); **Mac QA pending** |
| User playlists (local DB) | Yes | Yes | Shared store schema; UI path shared; **device unverified** |
| Downloads DB / offline | Yes | Yes | Wired; **M4A-only** (`ffmpeg = null`; no CLI on iOS) |
| Settings persistence | SharedPreferences | SQLite AppStore | Shell settings path |
| Destructive DB migration needed for MP | **No** | **No** | **No** (wire existing schema) |

---

## 4. Music sources

| Source | Android | Windows | iOS |
|--------|---------|---------|-----|
| YouTube / YT Music browse & search (engine.js) | Yes | Yes | Yes |
| YouTube playback (player.js + MSE) | Yes | Yes | Yes |
| Local files | Yes | Yes | Wired (media-store + folder pick; NoopMetadata — no ffmpeg tags); **Mac runtime verify pending** |
| Account / cookies | WebView jar | CookieVault / QR link | Device-link seams |
| Fake / hardcoded catalog | No | No | No |

Respect provider terms; do not document bypasses as features.

---

## 5. UI / UX

| Capability | Android | Windows | iOS |
|------------|---------|---------|-----|
| Shared Discover / Search / NP / Settings | Yes | Yes | Yes |
| StartMusicV3 branding direction | Yes (resources/theme) | Shared UI | Display name + AppIcon `#7B5CFF`; see `iosApp/README.md` |
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
| ffmpeg sibling packs | Android ABIs | windows-x86_64 / arm64 | **libav static** `out/ios-arm64` + `ios-simulator-arm64` for MSE (CLI still no-op) |

---

## 7. Summary judgment

| Platform | Compatibility verdict |
|----------|----------------------|
| **Android** | Fully scaffolded product; preserve; verify after UI changes |
| **Windows** | Fully scaffolded desktop product; verify; SMTC optional later |
| **iOS** | Phase B: Library/downloads/OS audio wired in source; MSE needs Mac-built iOS ffmpeg packs + Xcode signing; **no runtime pass claimed from Windows** |

**Do not** force Android-specific APIs onto Windows or iOS.  
**Do not** force a framework migration to gain these platforms — they are already targeted by this architecture.
