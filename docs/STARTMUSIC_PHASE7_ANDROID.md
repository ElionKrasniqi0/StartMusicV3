# StartMusicV3 — Phase 7 Android

**Date:** 2026-11-08  
**Environment verdict:** Android SDK path is configured (`local.properties` → `C:/Users/IT/AppData/Local/Android/Sdk`, `ANDROID_HOME` set) but **the directory does not exist**. `./gradlew :androidApp:assembleDebug` fails with *SDK location not found*. No emulator or device was available (`adb` not on PATH). **Do not treat Android as verified on this machine.**

## Goal

Confirm the existing `:androidApp` shell meets Phase 7 product requirements (UI, navigation, background audio, permissions, performance, build) without rewriting the KMP + Compose stack.

## Coverage (code audit)

| Area | Status | Notes |
|------|--------|-------|
| Portrait / landscape | Done | Shared `App` + `NowPlaying` landscape layout; Activity `configChanges` keeps process/player |
| Responsive nav | Done | Drawer; wide inline Discover tabs; touch collapsible toolbar |
| Bottom navigation | N/A (by design) | Drawer + section chips / Discover tabs — not Material bottom nav (`STARTMUSIC_TRANSFORMATION` / product shell) |
| Edge-to-edge / system bars / dark theme | Done | `enableEdgeToEdge`, `safeDrawingPadding`, `SyncSystemBars`, theme prefs |
| Home / Search / Library / Playlists / Settings / Now Playing | Done | Playlists live under Library (smart + user lists), not a separate root section |
| Playback across navigation | Done | App-scoped `AppGraph` + `PlaybackService` outlive `MainActivity` |
| MediaSession + FGS + notification actions | Done | Media3 `MediaLibraryService` / `Media3PlayerAdapter`; prev / play-pause / next / stop |
| Lock screen / audio focus / noisy / calls | Done | `AudioFocusCoordinator` + public MediaStyle notification |
| Service lifecycle / no idle FGS | Done | Promote while playing; stop foreground + `stopSelf` when idle; session owner dedup |
| Permissions | Done | INTERNET, notifications, FGS types, WAKE_LOCK, CAMERA (QR), media read (playlist import), install packages (updates); runtime for notifications / camera / audio read |
| SAF library scan | Done | Wired in `YMusicApplication` (`SafDocumentsProvider` + `:media-scanner`) — README previously wrong |
| Rotation / no duplicate service | Done | `configChanges` + process-scoped graph + `sessionOwner` |
| `assembleDebug` / device test | **Blocked** | Missing SDK install; no device/emulator run this phase |

## Build commands (when SDK is installed)

```bash
# local.properties must point at a real SDK (platforms android-36, build-tools, NDK 28.2.13676358 per androidApp/build.gradle.kts)
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:installDebug
```

Also required for a green native build: Android FFmpeg packs under sibling `ffmpeg-build` for `armeabi-v7a` / `arm64-v8a` / `x86` / `x86_64` (`FfmpegBuild.ANDROID_ABIS`). This machine currently only has `windows-x86_64` staged.

## SDK setup checklist (this machine)

1. Install Android Studio or command-line tools into `C:\Users\IT\AppData\Local\Android\Sdk` (matches `local.properties`).
2. Install at least: `platforms;android-36`, matching build-tools, `ndk;28.2.13676358`, platform-tools.
3. Accept licenses (`sdkmanager --licenses`).
4. Build Android ffmpeg packs in `ffmpeg-build`.
5. Re-run `assembleDebug`, then `installDebug` on a device/emulator and smoke-test background playback + notification actions.

## Key files

- `androidApp/src/main/kotlin/io/ymusic/android/MainActivity.kt`
- `androidApp/src/main/kotlin/io/ymusic/android/YMusicApplication.kt`
- `androidApp/src/main/kotlin/io/ymusic/android/PlaybackService.kt`
- `androidApp/src/main/kotlin/io/ymusic/android/AudioFocusCoordinator.kt`
- `androidApp/src/main/AndroidManifest.xml`
- `player/.../Media3PlayerAdapter.kt` (shared Media3 bridge)
